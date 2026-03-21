package com.notegraph.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Рендеринг Markdown в HTML с поддержкой викилинков [[название]]
 * Использует JavaScript для обработки кликов по ссылкам
 */
public class MarkdownRenderer {

    public String renderToHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return wrapInHtmlTemplate("");
        }

        // 1. Обрабатываем викилинки [[название]]
        String html = processWikiLinks(markdown);

        // 2. Простой Markdown рендеринг
        html = processMarkdown(html);

        // 3. Оборачиваем в HTML шаблон
        return wrapInHtmlTemplate(html);
    }

    /**
     * Обработка викилинков [[название]] и [[название|алиас]]
     */
    private String processWikiLinks(String text) {
        // Сначала обрабатываем викилинки с алиасами: [[название|алиас]]
        Pattern aliasPattern = Pattern.compile("\\[\\[([^\\]|]+)\\|([^\\]]+)\\]\\]");
        Matcher aliasMatcher = aliasPattern.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (aliasMatcher.find()) {
            String noteTitle = aliasMatcher.group(1).trim();
            String aliasText = aliasMatcher.group(2).trim();
            String replacement = String.format(
                    "<a href=\"javascript:void(0)\" class=\"wiki-link\" onclick=\"window.app.openNote('%s')\">%s</a>",
                    escapeJs(noteTitle),
                    escapeHtml(aliasText)
            );
            aliasMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        aliasMatcher.appendTail(sb);
        text = sb.toString();

        // Потом обрабатываем обычные викилинки: [[название]]
        Pattern pattern = Pattern.compile("\\[\\[([^\\]]+)\\]\\]");
        Matcher matcher = pattern.matcher(text);
        sb = new StringBuffer();

        while (matcher.find()) {
            String noteTitle = matcher.group(1).trim();
            String replacement = String.format(
                    "<a href=\"javascript:void(0)\" class=\"wiki-link\" onclick=\"openNote('%s')\">%s</a>",
                    escapeJs(noteTitle),
                    escapeHtml(noteTitle)
            );
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Простой Markdown рендеринг
     */
    private String processMarkdown(String text) {
        return text
                // Заголовки
                .replaceAll("(?m)^### (.+)$", "<h3>$1</h3>")
                .replaceAll("(?m)^## (.+)$", "<h2>$1</h2>")
                .replaceAll("(?m)^# (.+)$", "<h1>$1</h1>")

                // Жирный текст
                .replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>")
                .replaceAll("__(.+?)__", "<strong>$1</strong>")

                // Курсив
                .replaceAll("\\*(.+?)\\*", "<em>$1</em>")
                .replaceAll("_(.+?)_", "<em>$1</em>")

                // Код
                .replaceAll("`(.+?)`", "<code>$1</code>")

                // Переносы строк
                .replace("\n\n", "</p><p>")
                .replace("\n", "<br>");
    }

    /**
     * HTML шаблон с JavaScript для обработки викилинков
     */
    private String wrapInHtmlTemplate(String content) {
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            padding: 20px;
            line-height: 1.6;
            color: #333;
            margin: 0;
        }
        
        p {
            margin: 0.8em 0;
        }
        
        h1 {
            font-size: 2em;
            margin: 0.8em 0 0.4em 0;
            font-weight: 600;
            color: #1a1a1a;
            border-bottom: 2px solid #e0e0e0;
            padding-bottom: 0.3em;
        }
        
        h2 {
            font-size: 1.5em;
            margin: 0.8em 0 0.4em 0;
            font-weight: 600;
            color: #1a1a1a;
        }
        
        h3 {
            font-size: 1.25em;
            margin: 0.8em 0 0.4em 0;
            font-weight: 600;
            color: #1a1a1a;
        }
        
        strong {
            font-weight: 600;
            color: #1a1a1a;
        }
        
        em {
            font-style: italic;
        }
        
        code {
            background-color: rgba(135, 131, 120, 0.15);
            color: #eb5757;
            border-radius: 3px;
            padding: 0.2em 0.4em;
            font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
            font-size: 0.85em;
        }
        
        /* Викилинки в стиле Obsidian */
        .wiki-link {
            color: #7c3aed;
            text-decoration: none;
            font-weight: 500;
            padding: 2px 4px;
            border-radius: 3px;
            cursor: pointer;
            transition: background-color 0.15s ease;
        }
        
        .wiki-link:hover {
            background-color: rgba(124, 58, 237, 0.1);
            text-decoration: underline;
        }
        
        /* Внешние ссылки */
        a:not(.wiki-link) {
            color: #2563eb;
            text-decoration: none;
        }
        
        a:not(.wiki-link):hover {
            text-decoration: underline;
        }
    </style>
    
    <script>
        /**
         * Функция вызывается при клике на викилинк
         * Передает название заметки в Java через window.javaApp
         */
        function openNote(noteTitle) {
            console.log('JavaScript: openNote вызван для "' + noteTitle + '"');
            
            // Проверяем наличие Java bridge
            if (window.javaApp && typeof window.javaApp.openNote === 'function') {
                console.log('JavaScript: вызываем window.javaApp.openNote()');
                window.javaApp.openNote(noteTitle);
            } else {
                console.error('JavaScript: window.javaApp.openNote не найден!');
                console.log('JavaScript: window.javaApp =', window.javaApp);
            }
        }
        
        // Логирование при загрузке страницы
        window.addEventListener('DOMContentLoaded', function() {
            console.log('JavaScript: DOM загружен');
            console.log('JavaScript: window.javaApp доступен:', !!window.javaApp);
        });
    </script>
</head>
<body>
<p>
""" + content + """
</p>
</body>
</html>
""";
    }

    /**
     * Экранирование для JavaScript строк
     */
    private String escapeJs(String str) {
        return str
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    /**
     * Экранирование для HTML
     */
    private String escapeHtml(String str) {
        return str
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}