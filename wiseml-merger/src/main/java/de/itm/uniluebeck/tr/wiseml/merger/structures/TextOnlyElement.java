package de.itm.uniluebeck.tr.wiseml.merger.structures;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLElementStreamState;

import javax.xml.stream.XMLStreamReader;

/**
 * @author kuypers
 *         Date: 24.08.2010
 *         Time: 18:05:47
 */
public class TextOnlyElement extends WiseMLElement {

    private String text;

    public TextOnlyElement(final String elementLocalName, final String text) {
        super(elementLocalName);
        this.text = text;
    }

    public WiseMLElementStreamState createStreamState(final XMLStreamReader parent) {
        WiseMLElementStreamState result = super.createStreamState(parent);
        result.setText(text);
        return result;
    }
}
