package de.itm.uniluebeck.tr.wiseml.merger.structures;

import de.itm.uniluebeck.tr.wiseml.merger.enums.DataType;
import de.itm.uniluebeck.tr.wiseml.merger.enums.Unit;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLElementStreamState;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLStreamHelper;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: jacob
 * Date: Aug 23, 2010
 * Time: 7:03:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class Link extends WiseMLElement {

    private String source;
    private String target;

    private Boolean encrypted;
    private Boolean virtual;

    private DataType rssiDataType;
    private Unit rssiUnit;
    private String rssiDefaultValue;

    private List<LinkCapability> capabilities;

    public Link(final XMLStreamReader reader) throws XMLStreamException {
        super(reader, "link");

        source = WiseMLStreamHelper.getAttributeValue(reader, "source", false);
        target = WiseMLStreamHelper.getAttributeValue(reader, "target", false);

        reader.nextTag();
        while (WiseMLStreamHelper.findLocalName(reader, "encrypted", "virtual", "rssi") >= 0) {
            if (reader.getLocalName().equals("encrypted")) {
                encrypted = Boolean.valueOf(WiseMLStreamHelper.getBooleanValue(reader));
            } else if (reader.getLocalName().equals("virtual")) {
                virtual = Boolean.valueOf(WiseMLStreamHelper.getBooleanValue(reader));
            } else if (reader.getLocalName().equals("rssi")) {
                rssiDataType = DataType.valueOf(WiseMLStreamHelper.getAttributeValue(reader, "datatype", false));
                rssiUnit = Unit.valueOf(WiseMLStreamHelper.getAttributeValue(reader, "unit", false));
                rssiDefaultValue = WiseMLStreamHelper.getAttributeValue(reader, "datatype", false);
            }
            reader.nextTag();
        }

        capabilities = new ArrayList<LinkCapability>();

        while (reader.getLocalName().equals("capability")) {
            capabilities.add(new LinkCapability(reader));
        }
    }

    public WiseMLElementStreamState createStreamState(final XMLStreamReader parent) {
        WiseMLElementStreamState result = super.createStreamState(parent);
        result.addAttribute("source", source);
        result.addAttribute("target", target);
        // TODO: encrypted
        // TODO: virtual
        // TODO: rssi...
        for (LinkCapability capability : capabilities) {
            result.addSubElement(capability);
        }
        return result;
    }

}
