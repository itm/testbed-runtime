package de.itm.uniluebeck.tr.wiseml.merger.structures;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLElementStreamState;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLStreamHelper;

import javax.xml.stream.XMLStreamReader;

/**
 * @author kuypers
 *         Date: 24.08.2010
 *         Time: 17:09:35
 */
public abstract class WiseMLElement {

    private String elementLocalName;

    public WiseMLElement(final XMLStreamReader reader, final String elementLocalName) {
        WiseMLStreamHelper.assertLocalName(reader, this.elementLocalName = elementLocalName);
    }

    public WiseMLElement(final String elementLocalName) {
        this.elementLocalName = elementLocalName;
    }

    public WiseMLElementStreamState createStreamState(final XMLStreamReader parent) {
        return new WiseMLElementStreamState(parent, elementLocalName);
    }

}
