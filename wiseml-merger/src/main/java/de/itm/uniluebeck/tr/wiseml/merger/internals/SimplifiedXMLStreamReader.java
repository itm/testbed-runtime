package de.itm.uniluebeck.tr.wiseml.merger.internals;

import javax.xml.stream.XMLStreamException;

/**
 * @author kuypers
 *         Date: 25.08.2010
 *         Time: 14:25:13
 */
public interface SimplifiedXMLStreamReader {

    public String getCharacterEncodingScheme();

    public String readCharacters() throws XMLStreamException;

    public String getElementName();

    public int getAttributeCount();

    public String getAttributeName(final int index);

    public String getAttributeValue(final int index);

    /**
     * Level in the XML tree, 0 is the root node.
     *
     * @return level
     */
    public int getLevel();

    /**
     * Enters the current element, increasing the level by one.
     *
     */
    public void enterElement();

    /**
     * Exits the current element.
     */
    public void exitElement() throws XMLStreamException;

    public boolean nextSubElement() throws XMLStreamException;

    public boolean cursorAtElement();

    public boolean cursorAtSubElement();

    public void close() throws XMLStreamException;

}
