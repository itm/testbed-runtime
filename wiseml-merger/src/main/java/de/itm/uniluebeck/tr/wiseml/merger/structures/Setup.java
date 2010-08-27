package de.itm.uniluebeck.tr.wiseml.merger.structures;

import de.itm.uniluebeck.tr.wiseml.merger.enums.Interpolation;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jacob
 * Date: Aug 23, 2010
 * Time: 7:06:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class Setup {

    private Coordinate origin;

    // timeinfo
    private DateTime start;
    private DateTime end;
    private String duration; // TODO: String?
    private String unit; // TODO: String?

    private Interpolation interpolation;

    private String coordinateType;

    private String description;

    private Node defaultNodeProperties;
    private Link defaultLinkProperties;

    private List<Node> nodes;
    private List<Link> links;

    public Setup(XMLStreamReader reader) throws XMLStreamException {
        if (!reader.getLocalName().equals("setup")) {
            throw new IllegalStateException("cursor does not point to setup tag");
        }

        reader.nextTag();

        while (!reader.getLocalName().equals("defaults") &&
                !reader.getLocalName().equals("scenario") &&
                !reader.getLocalName().equals("trace") &&
                !reader.getLocalName().equals("wiseml")) {

            if (reader.getLocalName().equals("origin")) {
                origin = new Coordinate(reader);
            } else if(reader.getLocalName().equals("timeinfo")) {
                // TODO: read timeinfo
            } else if(reader.getLocalName().equals("interpolation")) {
                interpolation = Interpolation.valueOf(reader.getText());
            } else if(reader.getLocalName().equals("coordinateType")) {
                coordinateType = reader.getText();
            } else if (reader.getLocalName().equals("description")) {
                description = reader.getText();
            } else {
                throw new XMLStreamException("unknown setup.properties element: "+reader.getLocalName());
            }
            reader.nextTag();
        }

    }

}
