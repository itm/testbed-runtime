package de.itm.uniluebeck.tr.wiseml.merger.internals;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * @author kuypers
 *         Date: 24.08.2010
 *         Time: 15:51:09
 */
public class WiseMLStreamHelper {

    private WiseMLStreamHelper() {
        // empty
    }

    public static void assertLocalName(final XMLStreamReader reader, final String localName) {
        if (!localName.equals(reader.getLocalName())) {
            error(reader, "expected element '"+localName+"', got '"+reader.getLocalName()+"'");
        }
    }

    public static String getAttributeValue(final XMLStreamReader reader, final String attributeLocalName, final boolean optional) {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            if (attributeLocalName.equals(reader.getAttributeLocalName(i))) {
                return reader.getAttributeValue(i);
            }
        }
        if (optional) {
            return null;
        }
        error(reader, "no '"+attributeLocalName+"' attribute found");
        return null;
    }

    public static int findLocalName(final XMLStreamReader reader, final String... localNames) {
        for (int i = 0; i < localNames.length; i++) {
            if (localNames[i].equals(reader.getLocalName())) {
                return i;
            }
        }
        return -1;
    }

    public static void error(final XMLStreamReader reader, final String message) {
        throw new RuntimeException(message+"\nLocation: "+reader.getLocation());
    }

    public static void error(final XMLStreamReader reader, final String message, final Throwable throwable) {
        throw new RuntimeException(message+"\nLocation: "+reader.getLocation(), throwable);
    }

    public static boolean getBooleanValue(final XMLStreamReader reader) {
        return false; // TODO
    }

}
