package de.itm.uniluebeck.tr.wiseml.merger.internals;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * @author kuypers
 *         Date: 25.08.2010
 *         Time: 16:51:15
 */
public class SimplifiedXMLStreamOutput implements XMLStreamReader {

    private SimplifiedXMLStreamReader reader;

    private int eventType;

    public SimplifiedXMLStreamOutput(final SimplifiedXMLStreamReader reader) {
        this.reader = reader;
        // TODO
        this.eventType = XMLStreamConstants.START_DOCUMENT;
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        return null;  // TODO
    }

    @Override
    public int next() throws XMLStreamException {
        return 0;  // TODO
    }

    @Override
    public void require(int type, String namespaceURI, String localName) throws XMLStreamException {
        // TODO
    }

    @Override
    public String getElementText() throws XMLStreamException {
        return reader.readCharacters(); // TODO
    }

    @Override
    public int nextTag() throws XMLStreamException {
        return 0;  // TODO
    }

    @Override
    public boolean hasNext() throws XMLStreamException {
        return false;  // TODO
    }

    @Override
    public void close() throws XMLStreamException {
        reader.close();
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return null;  
    }

    @Override
    public boolean isStartElement() {
        return false;  // TODO
    }

    @Override
    public boolean isEndElement() {
        return false;  // TODO
    }

    @Override
    public boolean isCharacters() {
        return false;  // TODO
    }

    @Override
    public boolean isWhiteSpace() {
        return false;  // TODO
    }

    @Override
    public String getAttributeValue(String namespaceURI, String localName) {
        return null;  // TODO
    }

    @Override
    public int getAttributeCount() {
        return 0;  // TODO
    }

    @Override
    public QName getAttributeName(int index) {
        return null;  // TODO
    }

    @Override
    public String getAttributeNamespace(int index) {
        return null;  // TODO
    }

    @Override
    public String getAttributeLocalName(int index) {
        return null;  // TODO
    }

    @Override
    public String getAttributePrefix(int index) {
        return null;  // TODO
    }

    @Override
    public String getAttributeType(int index) {
        return null;  // TODO
    }

    @Override
    public String getAttributeValue(int index) {
        return null;  // TODO
    }

    @Override
    public boolean isAttributeSpecified(int index) {
        return false;  // TODO
    }

    @Override
    public int getNamespaceCount() {
        return 0;  
    }

    @Override
    public String getNamespacePrefix(int index) {
        return null;  
    }

    @Override
    public String getNamespaceURI(int index) {
        return null;  
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return null;  
    }

    @Override
    public int getEventType() {
        return 0;  // TODO
    }

    @Override
    public String getText() {
        return null;  // TODO
    }

    @Override
    public char[] getTextCharacters() {
        return new char[0];  // TODO
    }

    @Override
    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException {
        return 0;  // TODO
    }

    @Override
    public int getTextStart() {
        return 0;  // TODO
    }

    @Override
    public int getTextLength() {
        return 0;  // TODO
    }

    @Override
    public String getEncoding() {
        return null;  // TODO
    }

    @Override
    public boolean hasText() {
        return false;  // TODO
    }

    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public QName getName() {
        return null;  // TODO
    }

    @Override
    public String getLocalName() {
        return null;  // TODO
    }

    @Override
    public boolean hasName() {
        return false;  // TODO
    }

    @Override
    public String getNamespaceURI() {
        return null;  // TODO
    }

    @Override
    public String getPrefix() {
        return null;  // TODO
    }

    @Override
    public String getVersion() {
        return null;  // TODO
    }

    @Override
    public boolean isStandalone() {
        return false;  // TODO
    }

    @Override
    public boolean standaloneSet() {
        return false;  // TODO
    }

    @Override
    public String getCharacterEncodingScheme() {
        return null;  // TODO
    }

    @Override
    public String getPITarget() {
        return null;  
    }

    @Override
    public String getPIData() {
        return null;  
    }
}
