package de.itm.uniluebeck.tr.wiseml.merger.structures;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLElementStreamState;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLStreamHelper;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Created by IntelliJ IDEA.
 * User: jacob
 * Date: Aug 23, 2010
 * Time: 7:20:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class DataItem extends WiseMLElement {

    private String key;
    private String data;

    public DataItem(final XMLStreamReader reader) throws XMLStreamException {
        super(reader, "data");

        key = WiseMLStreamHelper.getAttributeValue(reader, "key", false);
        data = reader.getText();
    }

    public WiseMLElementStreamState createStreamState(final XMLStreamReader parent) {
        WiseMLElementStreamState result = super.createStreamState(parent);
        result.addAttribute("key", key);
        result.setText(data);
        return result;
    }

    public String getKey() {
        return this.key;
    }

    public String getData() {
        return this.data;
    }

}
