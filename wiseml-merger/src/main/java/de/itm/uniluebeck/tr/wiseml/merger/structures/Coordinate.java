package de.itm.uniluebeck.tr.wiseml.merger.structures;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLElementStreamState;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLStreamHelper;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Created by IntelliJ IDEA.
 * User: jacob
 * Date: Aug 23, 2010
 * Time: 7:03:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class Coordinate extends WiseMLElement {

    private Double x;
    private Double y;
    private Double z;
    private Double phi;
    private Double theta;

    public Coordinate(final XMLStreamReader reader) throws XMLStreamException {
        super(reader, "coordinate");

        try {
            reader.nextTag();
            x = Double.valueOf(reader.getText());

            reader.nextTag();
            y = Double.valueOf(reader.getText());

            reader.nextTag();
            while (WiseMLStreamHelper.findLocalName(reader, "z", "phi", "theta") >= 0) {
                if (reader.getLocalName().equals("z")) {
                    z = Double.valueOf(reader.getText());
                } else if (reader.getLocalName().equals("phi")) {
                    phi = Double.valueOf(reader.getText());
                } else if (reader.getLocalName().equals("theta")) {
                    theta = Double.valueOf(reader.getText());
                }
                reader.nextTag();
            }
        } catch (NumberFormatException e) {
            WiseMLStreamHelper.error(reader, "could not read '"+reader.getLocalName()+"' as a number", e);
        }
    }

    public WiseMLElementStreamState createStreamState(final XMLStreamReader parent) {
        WiseMLElementStreamState result = super.createStreamState(parent);
        result.addSubElement(new TextOnlyElement("x", x.toString()));
        result.addSubElement(new TextOnlyElement("y", y.toString()));
        if (z !=null) result.addSubElement(new TextOnlyElement("z", z.toString()));
        if (phi !=null) result.addSubElement(new TextOnlyElement("phi", phi.toString()));
        if (theta !=null) result.addSubElement(new TextOnlyElement("theta", theta.toString()));
        return result;
    }

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }

    public Double getZ() {
        return z;
    }

    public Double getPhi() {
        return phi;
    }

    public Double getTheta() {
        return theta;
    }
}
