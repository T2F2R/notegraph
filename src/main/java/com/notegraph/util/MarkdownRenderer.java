package com.notegraph.util;

import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.ast.Node;

public class MarkdownRenderer {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownRenderer() {
        MutableDataSet options = new MutableDataSet();
        options.set(HtmlRenderer.SOFT_BREAK, "<br/>");

        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();
    }

    public String renderToHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            markdown = "";
        }

        // 1. Парсим Markdown в HTML
        Node document = parser.parse(markdown);
        String htmlBody = renderer.render(document);

        // 2. ПОСЛЕ рендеринга заменяем викилинки на HTML ссылки
        // Сначала обрабатываем ссылки с алиасами: [[note|alias]]
        htmlBody = htmlBody.replaceAll(
                "\\[\\[([^\\]|]+)\\|([^\\]]+)\\]\\]",
                "<a href=\"javascript:void(0)\" class=\"wikilink\" onclick=\"if(window.app && window.app.openNote) window.app.openNote('$1'); return false;\">$2</a>"
        );

        // Потом обрабатываем обычные ссылки: [[note]]
        htmlBody = htmlBody.replaceAll(
                "\\[\\[([^\\]]+)\\]\\]",
                "<a href=\"javascript:void(0)\" class=\"wikilink\" onclick=\"if(window.app && window.app.openNote) window.app.openNote('$1'); return false;\">$1</a>"
        );

        // 3. Оборачиваем в HTML шаблон
        return buildHtmlTemplate(htmlBody);
    }

    private String buildHtmlTemplate(String body) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;
                    padding: 20px;
                    line-height: 1.6;
                    color: #333;
                    max-width: 900px;
                    margin: auto;
                }
        
                h1, h2, h3, h4, h5, h6 {
                    margin-top: 1.2em;
                    margin-bottom: 0.6em;
                }
        
                code {
                    background-color: #f4f4f4;
                    padding: 2px 6px;
                    border-radius: 3px;
                    font-family: 'Consolas', 'Monaco', monospace;
                }
        
                pre {
                    background-color: #f4f4f4;
                    padding: 12px;
                    border-radius: 6px;
                    overflow-x: auto;
                }
        
                pre code {
                    background: none;
                    padding: 0;
                }
        
                blockquote {
                    border-left: 4px solid #ddd;
                    padding-left: 15px;
                    color: #666;
                    margin: 10px 0;
                }
        
                ul, ol {
                    padding-left: 30px;
                }
        
                table {
                    border-collapse: collapse;
                    width: 100%;
                    margin: 10px 0;
                }
        
                th, td {
                    border: 1px solid #ddd;
                    padding: 8px;
                    text-align: left;
                }
        
                th {
                    background-color: #f4f4f4;
                    font-weight: bold;
                }
        
                /* Викилинки */
                .wikilink {
                    color: #6366f1;
                    text-decoration: none;
                    cursor: pointer;
                    font-weight: 500;
                }
        
                .wikilink:hover {
                    text-decoration: underline;
                    background-color: #f0f0ff;
                }
            </style>
        </head>
        <body>
        """ + body + """
            </body>
            </html>
        """;
    }
}