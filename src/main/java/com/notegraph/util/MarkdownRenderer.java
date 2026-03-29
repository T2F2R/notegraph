package com.notegraph.util;

import com.notegraph.ui.FontManager;
import com.notegraph.ui.Theme;
import com.notegraph.ui.ThemeManager;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.autolink.AutolinkExtension;

import java.util.List;

public class MarkdownRenderer {

    private final ThemeManager themeManager = ThemeManager.getInstance();

    private final Parser parser = Parser.builder()
            .extensions(List.of(
                    TablesExtension.create(),
                    AutolinkExtension.create()
            ))
            .build();

    private final HtmlRenderer renderer = HtmlRenderer.builder()
            .escapeHtml(false)
            .build();

    public String renderToHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return wrapInHtmlTemplate("");
        }

        markdown = processWikiLinks(markdown);

        Node document = parser.parse(markdown);
        String html = renderer.render(document);

        html = processTaskLists(html);

        return wrapInHtmlTemplate(html);
    }

    private String processWikiLinks(String text) {
        text = text.replaceAll(
                "\\[\\[([^\\]|]+)\\|([^\\]]+)\\]\\]",
                "<a href='#' onclick=\"window.javaApp.openNote('$1'); return false;\" class=\"wiki-link\">$2</a>"
        );

        text = text.replaceAll(
                "\\[\\[([^\\]]+)\\]\\]",
                "<a href='#' onclick=\"window.javaApp.openNote('$1'); return false;\" class=\"wiki-link\">$1</a>"
        );

        return text;
    }

    private String processTaskLists(String html) {
        return html
                .replaceAll("<li>\\[x\\] (.+?)</li>",
                        "<li><input type='checkbox' checked disabled> $1</li>")
                .replaceAll("<li>\\[X\\] (.+?)</li>",
                        "<li><input type='checkbox' checked disabled> $1</li>")
                .replaceAll("<li>\\[ \\] (.+?)</li>",
                        "<li><input type='checkbox' disabled> $1</li>");
    }

    private String wrapInHtmlTemplate(String content) {
        Theme theme = themeManager.getCurrentTheme();
        FontManager fontManager = FontManager.getInstance();

        String bgColor = toHex(theme.background);
        String textColor = toHex(theme.text);
        String linkColor = toHex(theme.nodeColor);
        String borderColor = theme == Theme.DARK ? "#404040" : "#e0e0e0";

        String linkHoverBg = theme == Theme.DARK
                ? "rgba(124, 58, 237, 0.1)"
                : "rgba(74, 144, 226, 0.1)";

        String codeBg = "rgba(135, 131, 120, 0.15)";
        String codeColor = "#eb5757";

        String fontFamily = fontManager.getCurrentFontFamily();
        double fontSize = fontManager.getCurrentFontSize();

        String template = """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
body {
    font-family: '${FONT}', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    font-size: ${FONT_SIZE}px;
    padding: 20px;
    line-height: 1.6;
    color: ${TEXT};
    background-color: ${BG};
    margin: 0;
}

p { margin: 0.8em 0; }

h1 {
    font-size: 2em;
    margin: 0.8em 0 0.4em 0;
    font-weight: 600;
    color: ${TEXT};
    border-bottom: 2px solid ${BORDER};
    padding-bottom: 0.3em;
}

h2, h3 {
    font-weight: 600;
    color: ${TEXT};
}

h2 { font-size: 1.5em; margin: 0.8em 0 0.4em 0; }
h3 { font-size: 1.25em; margin: 0.8em 0 0.4em 0; }

strong { font-weight: 600; color: ${TEXT}; }
em { font-style: italic; }

code {
    background-color: ${CODE_BG};
    color: ${CODE_COLOR};
    border-radius: 4px;
    padding: 0.2em 0.4em;
    font-family: 'SFMono-Regular', Consolas, monospace;
    font-size: 0.85em;
}

pre {
    background-color: ${CODE_BG};
    padding: 12px;
    border-radius: 6px;
    overflow-x: auto;
    border: 1px solid ${BORDER};
}

pre code {
    background: none;
    color: inherit;
    padding: 0;
}

blockquote {
    border-left: 4px solid ${BORDER};
    padding-left: 12px;
    margin: 10px 0;
    color: #888;
}

ul, ol {
    padding-left: 20px;
}

li {
    margin: 4px 0;
}

input[type="checkbox"] {
    margin-right: 6px;
}

table {
    border-collapse: collapse;
    width: 100%;
    margin: 10px 0;
}

th, td {
    border: 1px solid ${BORDER};
    padding: 6px 10px;
}

th {
    background-color: ${CODE_BG};
}

.wiki-link {
    color: ${LINK};
    text-decoration: none;
    font-weight: 500;
    padding: 2px 4px;
    border-radius: 3px;
    cursor: pointer;
    transition: background-color 0.15s ease;
}

.wiki-link:hover {
    background-color: ${LINK_HOVER};
    text-decoration: underline;
}
</style>
</head>
<body>
${CONTENT}
</body>
</html>
""";

        return template
                .replace("${FONT}", fontFamily)
                .replace("${FONT_SIZE}", String.valueOf(fontSize))
                .replace("${TEXT}", textColor)
                .replace("${BG}", bgColor)
                .replace("${BORDER}", borderColor)
                .replace("${CODE_BG}", codeBg)
                .replace("${CODE_COLOR}", codeColor)
                .replace("${LINK}", linkColor)
                .replace("${LINK_HOVER}", linkHoverBg)
                .replace("${CONTENT}", content);
    }

    private String toHex(javafx.scene.paint.Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255)
        );
    }
}