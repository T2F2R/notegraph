-- Создание таблицы заметок
CREATE TABLE IF NOT EXISTS notes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL UNIQUE,
    content TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    
    CHECK (title != ''),
    CHECK (is_deleted IN (0, 1))
);

-- Создание таблицы связей между заметками
CREATE TABLE IF NOT EXISTS links (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    source_note_id INTEGER NOT NULL,
    target_note_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (source_note_id) REFERENCES notes(id) ON DELETE CASCADE,
    FOREIGN KEY (target_note_id) REFERENCES notes(id) ON DELETE CASCADE,
    
    UNIQUE (source_note_id, target_note_id),
    CHECK (source_note_id != target_note_id)
);

-- Создание таблицы тегов
CREATE TABLE IF NOT EXISTS tags (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    color TEXT DEFAULT '#808080',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CHECK (name != '')
);

-- Создание таблицы связей между заметками и тегами
CREATE TABLE IF NOT EXISTS note_tags (
    note_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (note_id, tag_id),
    FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

-- Создание индексов для оптимизации поиска

-- Индекс для полнотекстового поиска по заголовку
CREATE INDEX IF NOT EXISTS idx_notes_title ON notes(title);

-- Индекс для поиска по дате создания и изменения
CREATE INDEX IF NOT EXISTS idx_notes_created_at ON notes(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notes_updated_at ON notes(updated_at DESC);

-- Индекс для фильтрации неудаленных заметок
CREATE INDEX IF NOT EXISTS idx_notes_is_deleted ON notes(is_deleted);

-- Индексы для быстрого поиска связей
CREATE INDEX IF NOT EXISTS idx_links_source ON links(source_note_id);
CREATE INDEX IF NOT EXISTS idx_links_target ON links(target_note_id);

-- Композитный индекс для поиска связей между двумя заметками
CREATE INDEX IF NOT EXISTS idx_links_source_target ON links(source_note_id, target_note_id);

-- Индекс для поиска тегов по имени
CREATE INDEX IF NOT EXISTS idx_tags_name ON tags(name);

-- Индексы для связей заметок с тегами
CREATE INDEX IF NOT EXISTS idx_note_tags_note ON note_tags(note_id);
CREATE INDEX IF NOT EXISTS idx_note_tags_tag ON note_tags(tag_id);

-- Создание виртуальной таблицы для полнотекстового поиска (FTS5)
CREATE VIRTUAL TABLE IF NOT EXISTS notes_fts USING fts5(
    title,
    content,
    content=notes,
    content_rowid=id,
    tokenize='porter unicode61'
);

-- Триггеры для автоматического обновления FTS-индекса

-- Триггер для добавления новой заметки в FTS-индекс
CREATE TRIGGER IF NOT EXISTS notes_fts_insert AFTER INSERT ON notes BEGIN
    INSERT INTO notes_fts(rowid, title, content) 
    VALUES (new.id, new.title, new.content);
END;

-- Триггер для удаления заметки из FTS-индекса
CREATE TRIGGER IF NOT EXISTS notes_fts_delete AFTER DELETE ON notes BEGIN
    DELETE FROM notes_fts WHERE rowid = old.id;
END;

-- Триггер для обновления заметки в FTS-индексе
CREATE TRIGGER IF NOT EXISTS notes_fts_update AFTER UPDATE ON notes BEGIN
    DELETE FROM notes_fts WHERE rowid = old.id;
    INSERT INTO notes_fts(rowid, title, content) 
    VALUES (new.id, new.title, new.content);
END;

-- Триггер для автоматического обновления поля updated_at
CREATE TRIGGER IF NOT EXISTS notes_update_timestamp 
AFTER UPDATE ON notes
FOR EACH ROW
WHEN old.content != new.content OR old.title != new.title
BEGIN
    UPDATE notes SET updated_at = CURRENT_TIMESTAMP WHERE id = old.id;
END;

-- Создание таблицы для истории изменений
CREATE TABLE IF NOT EXISTS note_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    note_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE
);

-- Индекс для быстрого поиска истории по заметке
CREATE INDEX IF NOT EXISTS idx_note_history_note_id ON note_history(note_id, created_at DESC);

-- Триггер для сохранения истории изменений
CREATE TRIGGER IF NOT EXISTS notes_save_history 
AFTER UPDATE ON notes
FOR EACH ROW
WHEN old.content != new.content OR old.title != new.title
BEGIN
    INSERT INTO note_history(note_id, title, content, created_at)
    VALUES (old.id, old.title, old.content, old.updated_at);
END;

-- Создание таблицы настроек приложения
CREATE TABLE IF NOT EXISTS app_settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Вставка начальных настроек
INSERT OR IGNORE INTO app_settings (key, value) VALUES 
    ('theme', 'light'),
    ('font_size', '14'),
    ('auto_save_interval', '30'),
    ('show_preview', 'true'),
    ('graph_layout', 'force_directed');
