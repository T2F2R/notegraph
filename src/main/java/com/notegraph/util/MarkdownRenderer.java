package com.notegraph.util;

import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.ast.Node;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownRenderer {

    private final Parser parser;
    private final HtmlRenderer renderer;

    private static final Pattern WIKI_LINK_PATTERN =
            Pattern.compile("\\[\\[([^\\]]+)\\]\\]");

    public MarkdownRenderer() {

        MutableDataSet options = new MutableDataSet();

        options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();
    }

    public String renderToHtml(String markdown) {

        if (markdown == null) {
            markdown = "";
        }

        markdown = convertWikiLinks(markdown);

        Node document = parser.parse(markdown);
        String htmlBody = renderer.render(document);

        return buildHtmlPage(htmlBody);
    }

    private String convertWikiLinks(String text) {

        Matcher matcher = WIKI_LINK_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {

            String title = matcher.group(1).trim();

            String replacement =
                    "<a href=\"#\" class=\"wikilink\" data-note=\"" +
                            escape(title) +
                            "\">" +
                            title +
                            "</a>";

            matcher.appendReplacement(sb, replacement);
        }

        matcher.appendTail(sb);

        return sb.toString();
    }

    private String escape(String text) {
        return text.replace("\"", "&quot;");
    }

    private String buildHtmlPage(String body) {

        return """
        <html>
        <head>
        <meta charset="UTF-8">

        <style>

        body{
            font-family: Arial, sans-serif;
            padding:20px;
            line-height:1.6;
            color:#333;
            max-width:900px;
            margin:auto;
        }

        pre{
            background:#f4f4f4;
            padding:10px;
            border-radius:5px;
            overflow:auto;
        }

        code{
            background:#f4f4f4;
            padding:2px 4px;
            border-radius:3px;
        }

        blockquote{
            border-left:4px solid #ddd;
            padding-left:10px;
            color:#666;
        }

        table{
            border-collapse:collapse;
        }

        th,td{
            border:1px solid #ddd;
            padding:6px 10px;
        }

        a.wikilink{
            color:#2a5bd7;
            text-decoration:none;
            cursor:pointer;
        }

        a.wikilink:hover{
            text-decoration:underline;
        }

        </style>

        <script>

        document.addEventListener("click", function(e){

            let link = e.target.closest(".wikilink");

            if(link){

                e.preventDefault();

                let title = link.getAttribute("data-note");

                if(window.app && window.app.openNote){
                    window.app.openNote(title);
                }

            }

        });

        </script>

        </head>

        <body>
        %s
        </body>
        </html>
        """.formatted(body);
    }
}