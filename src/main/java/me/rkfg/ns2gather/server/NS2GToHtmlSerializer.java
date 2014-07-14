package me.rkfg.ns2gather.server;

import java.util.Map;

import org.pegdown.LinkRenderer;
import org.pegdown.LinkRenderer.Rendering;
import org.pegdown.ToHtmlSerializer;
import org.pegdown.VerbatimSerializer;

public class NS2GToHtmlSerializer extends ToHtmlSerializer {

    public NS2GToHtmlSerializer(LinkRenderer linkRenderer, Map<String, VerbatimSerializer> verbatimSerializerMap) {
        super(linkRenderer, verbatimSerializerMap);
    }

    @Override
    protected void printLink(Rendering rendering) {
        printer.print('<').print('a');
        printAttribute("href", rendering.href);
        rendering.withAttribute("target", "_blank");
        for (LinkRenderer.Attribute attr : rendering.attributes) {
            printAttribute(attr.name, attr.value);
        }
        printer.print('>').print(rendering.text).print("</a>");
    }

    protected void printAttribute(String name, String value) {
        printer.print(' ').print(name).print('=').print('"').print(value).print('"');
    }

    protected void printImageTag(LinkRenderer.Rendering rendering) {
        printer.print("<img");
        printAttribute("src", rendering.href);
        printAttribute("title", rendering.text);
        for (LinkRenderer.Attribute attr : rendering.attributes) {
            printAttribute(attr.name, attr.value);
        }
        printer.print("\"/>");
    }

}
