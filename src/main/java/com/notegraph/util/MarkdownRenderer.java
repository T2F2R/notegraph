package com.notegraph.util;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

/**
 * Утилита для рендеринга Markdown в HTML.
 */
public class MarkdownRenderer {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownRenderer() {
        MutableDataSet options = new MutableDataSet();
        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
    }

    /**
     * Преобразование Markdown в HTML.
     *
     * @param markdown текст в формате Markdown
     * @return HTML строка
     */
    public String renderToHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }

        Node document = parser.parse(markdown);
        String html = renderer.render(document);

        // Обработка вики-ссылок [[название]]
        html = html.replaceAll("\\[\\[([^\\]]+)\\]\\]",
                "<a href='#' class='wiki-link' data-note='$1'>$1</a>");

        return wrapInHtmlTemplate(html);
    }

    /**
     * Обёртка HTML в полный шаблон документа.
     */
    private String wrapInHtmlTemplate(String content) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif; " +
                "       padding: 20px; line-height: 1.6; color: #333; }" +
                "h1, h2, h3, h4, h5, h6 { margin-top: 1em; margin-bottom: 0.5em; }" +
                "code { background-color: #f4f4f4; padding: 2px 6px; border-radius: 3px; font-family: monospace; }" +
                "pre { background-color: #f4f4f4; padding: 10px; border-radius: 5px; overflow-x: auto; }" +
                "pre code { background: none; padding: 0; }" +
                "blockquote { border-left: 4px solid #ddd; padding-left: 15px; color: #666; margin: 10px 0; }" +
                "a { color: #0066cc; text-decoration: none; }" +
                "a:hover { text-decoration: underline; }" +
                ".wiki-link { color: #6200ea; font-weight: 500; }" +
                ".wiki-link:hover { background-color: #f0e6ff; }" +
                "ul, ol { padding-left: 30px; }" +
                "table { border-collapse: collapse; width: 100%; margin: 10px 0; }" +
                "th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }" +
                "th { background-color: #f4f4f4; font-weight: bold; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                content +
                "</body>" +
                "</html>";
    }
}