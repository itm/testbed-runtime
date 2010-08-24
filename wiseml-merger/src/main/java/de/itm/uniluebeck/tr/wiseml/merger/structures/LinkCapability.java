package de.itm.uniluebeck.tr.wiseml.merger.structures;

import de.itm.uniluebeck.tr.wiseml.merger.enums.DataType;
import de.itm.uniluebeck.tr.wiseml.merger.enums.Unit;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLElementStreamState;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLStreamHelper;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Created by IntelliJ IDEA.
 * User: jacob
 * Date: Aug 23, 2010
 * Time: 7:04:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class LinkCapability extends WiseMLElement {

    private String name;

    private DataType dataType;

    private Unit unit;

    private String defaultValue;

    public LinkCapability(final XMLStreamReader reader) throws XMLStreamException {
        super(reader, "capability");

        reader.nextTag();
        WiseMLStreamHelper.assertLocalName(reader, "name");
        name = reader.getText();

        reader.nextTag();
        WiseMLStreamHelper.assertLocalName(reader, "dataType");
        dataType = DataType.valueOf(reader.getText());

        reader.nextTag();
        WiseMLStreamHelper.assertLocalName(reader, "unit");
        unit = Unit.valueOf(reader.getText());

        reader.nextTag();
        WiseMLStreamHelper.assertLocalName(reader, "default");
        defaultValue = reader.getText();
    }

    public WiseMLElementStreamState createStreamState(final XMLStreamReader parent) {
        WiseMLElementStreamState result = super.createStreamState(parent);
        result.addSubElement(new TextOnlyElement("name", name));
        result.addSubElement(new TextOnlyElement("dataType", dataType.toString()));
        result.addSubElement(new TextOnlyElement("unit", unit.toString()));
        result.addSubElement(new TextOnlyElement("default", defaultValue));
        return result;
    }

    public String getName() {
        return name;
    }

    public DataType getDataType() {
        return dataType;
    }

    public Unit getUnit() {
        return unit;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
