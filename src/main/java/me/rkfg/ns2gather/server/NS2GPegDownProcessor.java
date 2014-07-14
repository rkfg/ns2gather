package me.rkfg.ns2gather.server;

import java.util.Map;

import org.pegdown.Extensions;
import org.pegdown.LinkRenderer;
import org.pegdown.ParsingTimeoutException;
import org.pegdown.PegDownProcessor;
import org.pegdown.VerbatimSerializer;
import org.pegdown.ast.RootNode;

public class NS2GPegDownProcessor extends PegDownProcessor {

    public NS2GPegDownProcessor() {
        super(Extensions.AUTOLINKS | Extensions.SMARTYPANTS | Extensions.STRIKETHROUGH | Extensions.SUPPRESS_ALL_HTML);
    }

    @Override
    public RootNode parseMarkdown(char[] markdownSource) {
        return parser.parse(markdownSource);
    }

    public String markdownToHtml(char[] markdownSource, LinkRenderer linkRenderer, Map<String, VerbatimSerializer> verbatimSerializerMap) {
        try {
            RootNode astRoot = parseMarkdown(markdownSource);
            return new NS2GToHtmlSerializer(linkRenderer, verbatimSerializerMap).toHtml(astRoot);
        } catch (ParsingTimeoutException e) {
            return null;
        }
    }

}
