package de.itm.uniluebeck.tr.wiseml.merger.internals;

import de.itm.uniluebeck.tr.wiseml.merger.structures.WiseMLElement;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author kuypers
 *         Date: 24.08.2010
 *         Time: 17:27:57
 */
public class WiseMLElementStreamState implements XMLStreamReader {

    public static final int CREATION = -1;
    public static final int START = 1;
    public static final int CONTENTS = 2;
    public static final int END = 3;

    private class Attribute {

        private QName qname;
        private String value;
        // TODO

    }

    private XMLStreamReader parent;
    private WiseMLElementStreamState child;

    private String localName;
    private String text;
    private List<Attribute> attributes;
    private List<WiseMLElement> subElements;

    private int state;
    private int subElementIndex;
    private int eventType;

    public WiseMLElementStreamState(final XMLStreamReader parent, final String localName) {
        this.parent = parent;
        this.localName = localName;
        attributes = new ArrayList<Attribute>();
    }

    public void setReady() {
        assertCreationState();
        if (text == null) {
            text = "";
        }
        state = START;
    }

    private void updateChild() {
        if (subElementIndex == subElements.size()) {
            child = null;
        } else {
            child = subElements.get(subElementIndex).createStreamState(this);
            child.setReady();
        }
    }

    private void assertCreationState() {
        if (state != CREATION) {
            throw new IllegalStateException("not in mutable state");
        }
    }

    private void assertReadingState() {
        if (!(state >= START && state <= END)) {
            throw new IllegalStateException("not in streamable state");
        }
    }

    public void addAttribute(final String name, final String value) {
        assertCreationState();
        // TODO
    }

    public void setText(final String text) {
        assertCreationState();
        this.text = text;
    }

    public void addSubElement(final WiseMLElement element) {
        assertCreationState();
        if (subElements == null) {
            subElements = new ArrayList<WiseMLElement>();
        }
        subElements.add(element);
    }

    // TODO

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int next() throws XMLStreamException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void require(int type, String namespaceURI, String localName) throws XMLStreamException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getElementText() throws XMLStreamException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int nextTag() throws XMLStreamException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasNext() throws XMLStreamException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void close() throws XMLStreamException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isStartElement() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isEndElement() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isCharacters() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isWhiteSpace() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getAttributeValue(String namespaceURI, String localName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getAttributeCount() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public QName getAttributeName(int index) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getAttributeNamespace(int index) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getAttributeLocalName(int index) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getAttributePrefix(int index) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getAttributeType(int index) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getAttributeValue(int index) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isAttributeSpecified(int index) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getNamespaceCount() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getNamespacePrefix(int index) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getNamespaceURI(int index) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getEventType() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getText() {
        assertReadingState();
        if (state == CONTENTS && child != null) {
            return child.getText();
        }
        return text;
    }

    @Override
    public char[] getTextCharacters() {
        return new char[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getTextStart() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getTextLength() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getEncoding() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasText() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Location getLocation() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public QName getName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getLocalName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasName() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getNamespaceURI() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getPrefix() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getVersion() {
        return parent.getVersion();
    }

    @Override
    public boolean isStandalone() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean standaloneSet() {
        return parent.standaloneSet();
    }

    @Override
    public String getCharacterEncodingScheme() {
        return parent.getCharacterEncodingScheme();
    }

    @Override
    public String getPITarget() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getPIData() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
