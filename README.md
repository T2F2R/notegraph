# NoteGraph

Система управления персональной базой знаний на основе файловой системы.

## Структура проекта

```
notegraph/
├── pom.xml                          # Maven конфигурация
├── src/
│   └── main/
│       ├── java/com/notegraph/
│       │   ├── model/               # Модели данных
│       │   │   └── Note.java
│       │   ├── repository/          # Репозитории
│       │   │   └── FileSystemNoteRepository.java
│       │   ├── service/             # Бизнес-логика
│       │   │   └── NoteServiceImpl.java
│       │   ├── controller/          # UI контроллеры
│       │   │   └── MainController.java
│       │   ├── util/                # Утилиты
│       │   │   ├── FileSystemManager.java
│       │   │   ├── NoteParser.java
│       │   │   ├── MetadataManager.java
│       │   │   ├── LinkIndexManager.java
│       │   │   └── MarkdownRenderer.java
│       │   └── Main.java            # Точка входа
│       └── resources/
│           ├── fxml/
│           │   └── MainWindow.fxml  # UI разметка
│           ├── css/
│           │   └── styles.css       # Стили
│           └── logback.xml          # Конфигурация логов
└── vault/                           # Хранилище заметок (создается автоматически)
    ├── .notegraph/
    │   ├── metadata.json            # Закладки, настройки
    │   └── index.json               # Индекс связей
    └── *.md                         # Файлы заметок
```

## Быстрый старт

### Требования

- Java 17 или выше
- Maven 3.6+

### Установка и запуск

```bash
# Клонируйте проект
cd notegraph

# Компиляция
mvn clean compile

# Запуск
mvn javafx:run
```

### Первый запуск

1. При первом запуске автоматически создается папка `vault/`
2. Нажмите кнопку "+" или `Ctrl+N` для создания первой заметки
3. Введите название и начните писать!

## Формат заметок

Каждая заметка - это `.md` файл с YAML frontmatter:

```markdown
---
title: "Моя заметка"
created: 2026-03-10T10:00:00
modified: 2026-03-10T15:30:00
tags: [работа, важное]
---

# Моя заметка

Это содержимое заметки в **Markdown**.

## Связи с другими заметками

Можно ссылаться: [[Другая заметка]]

Или с алиасом: [[Длинное название заметки|короткое имя]]
```

## Wikilinks

Создавайте связи между заметками:

```markdown
[[Название заметки]]              # Обычная ссылка
[[Название заметки|Алиас]]        # Ссылка с алиасом
```

В режиме Preview ссылки становятся кликабельными!

## Горячие клавиши

- `Ctrl+N` - Новая заметка
- `Ctrl+S` - Сохранить
- `Ctrl+F` - Поиск
- `Ctrl+Q` - Выход
- `Delete` - Удалить выбранную заметку
- `Ctrl+X / Ctrl+V` - Вырезать/Вставить
## Конфигурация

### Метаданные

Все метаданные хранятся в `vault/.notegraph/metadata.json`:

```json
{
  "bookmarks": ["note1.md", "note2.md"],
  "settings": {
    "theme": "light",
    "autosave_interval": 30
  },
  "recent_notes": ["note3.md", "note1.md"]
}
```

### Индекс связей

Индекс для быстрого поиска backlinks в `vault/.notegraph/index.json`:

```json
{
  "backlinks": {
    "Заметка А": ["Заметка Б", "Заметка В"]
  },
  "outgoing": {
    "Заметка Б": ["Заметка А"]
  }
}
```

## Сборка JAR

```bash
# Создать исполняемый JAR
mvn clean package

# Запустить JAR
java -jar target/notegraph-2.0-SNAPSHOT.jar
```

## API

### Создание заметки программно

```java
NoteServiceImpl noteService = new NoteServiceImpl();
Note note = noteService.createNote("Название", "Содержимое");
```

### Поиск заметки

```java
Optional<Note> note = noteService.getNoteByTitle("Название");
```

### Получение backlinks

```java
List<Note> backlinks = noteService.getBacklinks(note);
```

### Перемещение заметки

```java
Path targetFolder = Paths.get("vault/Новая папка");
noteService.moveNote(note, targetFolder);
```

## Вклад в проект

1. Fork проекта
2. Создайте ветку (`git checkout -b feature/amazing-feature`)
3. Commit изменения (`git commit -m 'Add amazing feature'`)
4. Push в ветку (`git push origin feature/amazing-feature`)
5. Откройте Pull Request

## Лицензия

MIT License - делайте что хотите!

---

**Сделано с ❤️ для управления знаниями**
