package com.notegraph.service.impl;

import com.notegraph.model.Link;
import com.notegraph.model.Note;
import com.notegraph.repository.LinkRepository;
import com.notegraph.repository.NoteRepository;
import com.notegraph.repository.impl.LinkRepositoryImpl;
import com.notegraph.repository.impl.NoteRepositoryImpl;
import com.notegraph.service.LinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Реализация сервиса для работы со связями.
 */
public class LinkServiceImpl implements LinkService {
    private static final Logger logger = LoggerFactory.getLogger(LinkServiceImpl.class);

    // Паттерн для вики-ссылок: [[название заметки]]
    private static final Pattern WIKI_LINK_PATTERN = Pattern.compile("\\[\\[([^\\]]+)\\]\\]");

    private final LinkRepository linkRepository;
    private final NoteRepository noteRepository;

    public LinkServiceImpl() {
        this.linkRepository = new LinkRepositoryImpl();
        this.noteRepository = new NoteRepositoryImpl();
    }

    public LinkServiceImpl(LinkRepository linkRepository, NoteRepository noteRepository) {
        this.linkRepository = linkRepository;
        this.noteRepository = noteRepository;
    }

    @Override
    public Set<String> extractWikiLinks(String content) {
        Set<String> links = new HashSet<>();

        if (content == null || content.isEmpty()) {
            return links;
        }

        Matcher matcher = WIKI_LINK_PATTERN.matcher(content);
        while (matcher.find()) {
            String linkTitle = matcher.group(1).trim();
            if (!linkTitle.isEmpty()) {
                links.add(linkTitle);
            }
        }

        logger.debug("Извлечено {} вики-ссылок из текста", links.size());
        return links;
    }

    @Override
    public void updateLinksForNote(Integer noteId, String content) {
        if (noteId == null) {
            throw new IllegalArgumentException("ID заметки не может быть null");
        }

        // Извлекаем все вики-ссылки из содержимого
        Set<String> wikiLinks = extractWikiLinks(content);

        // Получаем текущие исходящие связи
        List<Link> currentLinks = linkRepository.findOutgoingLinks(noteId);
        Set<Integer> currentTargetIds = new HashSet<>();
        for (Link link : currentLinks) {
            currentTargetIds.add(link.getTargetNoteId());
        }

        // Находим заметки для каждой вики-ссылки
        Set<Integer> newTargetIds = new HashSet<>();
        for (String linkTitle : wikiLinks) {
            Optional<Note> targetNote = noteRepository.findByTitle(linkTitle);

            if (targetNote.isPresent()) {
                Integer targetId = targetNote.get().getId();

                // Не создаём ссылку на саму себя
                if (!targetId.equals(noteId)) {
                    newTargetIds.add(targetId);
                }
            } else {
                logger.debug("Заметка не найдена для вики-ссылки: {}", linkTitle);
            }
        }

        // Удаляем связи, которых больше нет в тексте
        for (Integer currentTargetId : currentTargetIds) {
            if (!newTargetIds.contains(currentTargetId)) {
                linkRepository.deleteLink(noteId, currentTargetId);
                logger.debug("Удалена связь: {} -> {}", noteId, currentTargetId);
            }
        }

        // Создаём новые связи
        for (Integer newTargetId : newTargetIds) {
            if (!currentTargetIds.contains(newTargetId)) {
                if (!linkRepository.existsLink(noteId, newTargetId)) {
                    Link link = new Link(noteId, newTargetId);
                    linkRepository.create(link);
                    logger.debug("Создана связь: {} -> {}", noteId, newTargetId);
                }
            }
        }

        logger.info("Обновлены связи для заметки {}: {} связей", noteId, newTargetIds.size());
    }

    @Override
    public Link createLink(Integer sourceNoteId, Integer targetNoteId) {
        if (sourceNoteId == null || targetNoteId == null) {
            throw new IllegalArgumentException("ID заметок не могут быть null");
        }

        if (sourceNoteId.equals(targetNoteId)) {
            throw new IllegalArgumentException("Заметка не может ссылаться сама на себя");
        }

        // Проверяем существование заметок
        noteRepository.findById(sourceNoteId)
                .orElseThrow(() -> new IllegalArgumentException("Исходная заметка не найдена: " + sourceNoteId));
        noteRepository.findById(targetNoteId)
                .orElseThrow(() -> new IllegalArgumentException("Целевая заметка не найдена: " + targetNoteId));

        // Проверяем, не существует ли уже такая связь
        if (linkRepository.existsLink(sourceNoteId, targetNoteId)) {
            throw new IllegalArgumentException("Связь уже существует");
        }

        Link link = new Link(sourceNoteId, targetNoteId);
        return linkRepository.create(link);
    }

    @Override
    public void deleteLink(Integer sourceNoteId, Integer targetNoteId) {
        if (sourceNoteId == null || targetNoteId == null) {
            throw new IllegalArgumentException("ID заметок не могут быть null");
        }

        linkRepository.deleteLink(sourceNoteId, targetNoteId);
        logger.info("Удалена связь: {} -> {}", sourceNoteId, targetNoteId);
    }

    @Override
    public List<Note> getOutgoingLinkedNotes(Integer noteId) {
        if (noteId == null) {
            throw new IllegalArgumentException("ID заметки не может быть null");
        }
        return linkRepository.findLinkedNotesOutgoing(noteId);
    }

    @Override
    public List<Note> getIncomingLinkedNotes(Integer noteId) {
        if (noteId == null) {
            throw new IllegalArgumentException("ID заметки не может быть null");
        }
        return linkRepository.findLinkedNotesIncoming(noteId);
    }

    @Override
    public int getOutgoingLinksCount(Integer noteId) {
        if (noteId == null) {
            return 0;
        }
        return linkRepository.countOutgoingLinks(noteId);
    }

    @Override
    public int getIncomingLinksCount(Integer noteId) {
        if (noteId == null) {
            return 0;
        }
        return linkRepository.countIncomingLinks(noteId);
    }

    @Override
    public boolean linkExists(Integer sourceNoteId, Integer targetNoteId) {
        if (sourceNoteId == null || targetNoteId == null) {
            return false;
        }
        return linkRepository.existsLink(sourceNoteId, targetNoteId);
    }
}