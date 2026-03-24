package com.notegraph.util;

import com.notegraph.ui.Theme;
import com.notegraph.ui.ThemeManager;

public class MarkdownRenderer {

    private final ThemeManager themeManager = ThemeManager.getInstance();

    public String renderToHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return wrapInHtmlTemplate("");
        }

        String html = processWikiLinks(markdown);
        html = processMarkdown(html);
        return wrapInHtmlTemplate(html);
    }

    private String processWikiLinks(String text) {
        text = text.replaceAll(
                "\\[\\[([^\\]|]+)\\|([^\\]]+)\\]\\]",
                "<a href='javascript:void(0)' onclick=\"window.javaApp.openNote('$1'); return false;\" class=\"wiki-link\">$2</a>"
        );

        text = text.replaceAll(
                "\\[\\[([^\\]]+)\\]\\]",
                "<a href='javascript:void(0)' onclick=\"window.javaApp.openNote('$1'); return false;\" class=\"wiki-link\">$1</a>"
        );

        return text;
    }

    private String processMarkdown(String text) {
        return text
                .replaceAll("(?m)^### (.+)$", "<h3>$1</h3>")
                .replaceAll("(?m)^## (.+)$", "<h2>$1</h2>")
                .replaceAll("(?m)^# (.+)$", "<h1>$1</h1>")
                .replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>")
                .replaceAll("__(.+?)__", "<strong>$1</strong>")
                .replaceAll("\\*(.+?)\\*", "<em>$1</em>")
                .replaceAll("_(.+?)_", "<em>$1</em>")
                .replaceAll("`(.+?)`", "<code>$1</code>")
                .replace("\n\n", "</p><p>")
                .replace("\n", "<br>");
    }

    private String wrapInHtmlTemplate(String content) {
        Theme theme = themeManager.getCurrentTheme();

        String bgColor = toHex(theme.background);
        String textColor = toHex(theme.text);
        String linkColor = toHex(theme.nodeColor);
        String linkHoverBg = theme == Theme.DARK ? "rgba(124, 58, 237, 0.1)" : "rgba(74, 144, 226, 0.1)";
        String codeBg = theme == Theme.DARK ? "rgba(135, 131, 120, 0.15)" : "rgba(135, 131, 120, 0.15)";
        String codeColor = "#eb5757";
        String borderColor = theme == Theme.DARK ? "#404040" : "#e0e0e0";

        return String.format("""
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            padding: 20px;
            line-height: 1.6;
            color: %s;
            background-color: %s;
            margin: 0;
        }
        
        p { margin: 0.8em 0; }
        
        h1 {
            font-size: 2em;
            margin: 0.8em 0 0.4em 0;
            font-weight: 600;
            color: %s;
            border-bottom: 2px solid %s;
            padding-bottom: 0.3em;
        }
        
        h2, h3 {
            font-weight: 600;
            color: %s;
        }
        
        h2 { font-size: 1.5em; margin: 0.8em 0 0.4em 0; }
        h3 { font-size: 1.25em; margin: 0.8em 0 0.4em 0; }
        
        strong { font-weight: 600; color: %s; }
        em { font-style: italic; }
        
        code {
            background-color: %s;
            color: %s;
            border-radius: 3px;
            padding: 0.2em 0.4em;
            font-family: 'SFMono-Regular', Consolas, monospace;
            font-size: 0.85em;
        }
        
        .wiki-link {
            color: %s;
            text-decoration: none;
            font-weight: 500;
            padding: 2px 4px;
            border-radius: 3px;
            cursor: pointer;
            transition: background-color 0.15s ease;
        }
        
        .wiki-link:hover {
            background-color: %s;
            text-decoration: underline;
        }
    </style>
</head>
<body>
<p>%s</p>
</body>
</html>
""",
                textColor, bgColor,
                textColor, borderColor,
                textColor,
                textColor,
                codeBg, codeColor,
                linkColor,
                linkHoverBg,
                content
        );
    }

    private String toHex(javafx.scene.paint.Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255)
        );
    }
}