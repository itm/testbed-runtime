package de.itm.uniluebeck.tr.wiseml.merger.internals;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.LinkedList;
import java.util.List;

/**
 * @author kuypers
 *         Date: 25.08.2010
 *         Time: 14:44:37
 */
public class SimplifiedXMLStreamInput implements SimplifiedXMLStreamReader {

    private class ElementData {
        String name;
        String[] attributeNames;
        String[] attributeValues;
    }

    private XMLStreamReader reader;
    private LinkedList<ElementData> elementStack;

    public SimplifiedXMLStreamInput(final XMLStreamReader reader) throws XMLStreamException {
        this.reader = reader;
        this.elementStack = new LinkedList<ElementData>();

        if (reader.getEventType() != XMLStreamConstants.START_DOCUMENT) {
           throw new XMLStreamException("reader needs to be at START_DOCUMENT", reader.getLocation());
        }
        skipToTag(true);

        elementStack.add(readElementData());
    }

    private ElementData readElementData() {
        ElementData result = new ElementData();
        result.name = reader.getLocalName();
        result.attributeNames = new String[reader.getAttributeCount()];
        result.attributeValues = new String[reader.getAttributeCount()];
        for (int i = 0; i < result.attributeNames.length; i++) {
            result.attributeNames[i] = reader.getAttributeLocalName(i);
            result.attributeValues[i] = reader.getAttributeValue(i);
        }
        return result;
    }

    private void skipToTag() throws XMLStreamException {
        while (reader.getEventType() != XMLStreamConstants.START_ELEMENT
                && reader.getEventType() != XMLStreamConstants.END_ELEMENT
                && reader.getEventType() != XMLStreamConstants.END_DOCUMENT) {
            reader.next();
        }
    }

    private void skipToTag(final boolean start) throws XMLStreamException {
        while (reader.getEventType() != XMLStreamConstants.START_ELEMENT) {
            reader.next();
        }
    }

    private void skipToTag(final boolean start, final String elementName) throws XMLStreamException {
        while (true) {
            if ((start && reader.getEventType() == XMLStreamConstants.START_ELEMENT
                    || !start && reader.getEventType() == XMLStreamConstants.END_ELEMENT)
                && reader.getLocalName().equals(elementName)) {
                return;
            }
            reader.next();
        }
    }

    @Override
    public String getCharacterEncodingScheme() {
        return reader.getCharacterEncodingScheme();
    }

    @Override
    public String readCharacters() throws XMLStreamException {
        return reader.getElementText(); // TODO: skip comments etc but merge character sections
    }

    @Override
    public String getElementName() {
        return elementStack.getLast().name;
    }

    @Override
    public int getLevel() {
        return elementStack.size();
    }

    @Override
    public int getAttributeCount() {
        return reader.getAttributeCount();
    }

    @Override
    public String getAttributeName(int index) {
        return reader.getAttributeLocalName(index);
    }

    @Override
    public String getAttributeValue(int index) {
        return reader.getAttributeValue(index);
    }

    @Override
    public void enterElement() {
        if (!cursorAtSubElement()) {
            return; // TODO: exception?
        }
        elementStack.add(readElementData());
    }

    @Override
    public void exitElement() throws XMLStreamException {
        if (!cursorAtElement()) {
            return; // TODO: exception?
        }
        skipToTag(false, elementStack.getLast().name);
        elementStack.removeLast();
    }

    @Override
    public boolean nextSubElement() throws XMLStreamException {
        if (cursorAtSubElement()) {
            skipToTag(false, reader.getLocalName()); // go to end of current element
            skipToTag(); // find next tag
        }
        return cursorAtSubElement();
    }

    @Override
    public boolean cursorAtElement() {
        return !elementStack.isEmpty();
    }

    @Override
    public boolean cursorAtSubElement() {
        if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
            if (elementStack.isEmpty()) {
                return true;
            }
            if (!reader.getLocalName().equals(elementStack.getLast().name)) {
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public void close() throws XMLStreamException {
        reader.close();
    }
}
