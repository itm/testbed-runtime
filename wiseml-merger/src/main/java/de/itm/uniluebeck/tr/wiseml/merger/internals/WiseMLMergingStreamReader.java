package de.itm.uniluebeck.tr.wiseml.merger.internals;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import java.io.StringReader;

public class WiseMLMergingStreamReader implements XMLStreamReader {
	
	private MergingInfo info;
    private XMLStreamReader target;

	public WiseMLMergingStreamReader(final MergingInfo info) {
		this.info = info;
        this.target = createHeadTarget();
	}

    public void nextState() {

        info.nextStage();

        switch (info.getStage()) {
            case SetupProperties:
                readSetupProperties();
                break;
            case SetupDefaultNodes:
                readSetupDefaultNodes();
                break;
            case SetupDefaultLinks:
                readSetupDefaultLinks();
                break;
            case SetupNodes:
                readSetupNodes();
                break;
            case SetupLinks:
                readSetupLinks();
                break;
            case Scenario:
                target = createSequenceTarget(this.info.getReaders(), "trace", "wiseml");
                break;
            case Trace:
                readTrace();
                break;
        }
    }

    private void readSetupProperties() {
        // TODO
    }

    private void readSetupDefaultNodes() {
        // TODO
    }

    private void readSetupDefaultLinks() {
        // TODO
    }

    private void readSetupNodes() {
        // TODO
    }

    private void readSetupLinks() {
        // TODO
    }

    private void readTrace() {
        // TODO
    }

    @Override
    public Object getProperty(String s) throws IllegalArgumentException {
        return target.getProperty(s);
    }

    @Override
    public int next() throws XMLStreamException {
        return target.next();
    }

    @Override
    public void require(int i, String s, String s1) throws XMLStreamException {
        target.require(i, s, s1);
    }

    @Override
    public String getElementText() throws XMLStreamException {
        return target.getElementText();
    }

    @Override
    public int nextTag() throws XMLStreamException {
        return target.nextTag();
    }

    @Override
    public boolean hasNext() throws XMLStreamException {
        return target.hasNext();
    }

    @Override
    public void close() throws XMLStreamException {
        target.close();
    }

    @Override
    public String getNamespaceURI(String s) {
        return target.getNamespaceURI(s);
    }

    @Override
    public boolean isStartElement() {
        return target.isStartElement();
    }

    @Override
    public boolean isEndElement() {
        return target.isEndElement();
    }

    @Override
    public boolean isCharacters() {
        return target.isCharacters();
    }

    @Override
    public boolean isWhiteSpace() {
        return target.isWhiteSpace();
    }

    @Override
    public String getAttributeValue(String s, String s1) {
        return target.getAttributeValue(s, s1);
    }

    @Override
    public int getAttributeCount() {
        return target.getAttributeCount();
    }

    @Override
    public QName getAttributeName(int i) {
        return target.getAttributeName(i);
    }

    @Override
    public String getAttributeNamespace(int i) {
        return target.getAttributeNamespace(i);
    }

    @Override
    public String getAttributeLocalName(int i) {
        return target.getAttributeLocalName(i);
    }

    @Override
    public String getAttributePrefix(int i) {
        return target.getAttributePrefix(i);
    }

    @Override
    public String getAttributeType(int i) {
        return target.getAttributeType(i);
    }

    @Override
    public String getAttributeValue(int i) {
        return target.getAttributeValue(i);
    }

    @Override
    public boolean isAttributeSpecified(int i) {
        return target.isAttributeSpecified(i);
    }

    @Override
    public int getNamespaceCount() {
        return target.getNamespaceCount();
    }

    @Override
    public String getNamespacePrefix(int i) {
        return target.getNamespacePrefix(i);
    }

    @Override
    public String getNamespaceURI(int i) {
        return target.getNamespaceURI(i);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return target.getNamespaceContext();
    }

    @Override
    public int getEventType() {
        return target.getEventType();
    }

    @Override
    public String getText() {
        return target.getText();
    }

    @Override
    public char[] getTextCharacters() {
        return target.getTextCharacters();
    }

    @Override
    public int getTextCharacters(int i, char[] chars, int i1, int i2) throws XMLStreamException {
        return target.getTextCharacters(i, chars, i1, i2);
    }

    @Override
    public int getTextStart() {
        return target.getTextStart();
    }

    @Override
    public int getTextLength() {
        return target.getTextLength();
    }

    @Override
    public String getEncoding() {
        return target.getEncoding();
    }

    @Override
    public boolean hasText() {
        return target.hasText();
    }

    @Override
    public Location getLocation() {
        return target.getLocation();
    }

    @Override
    public QName getName() {
        return target.getName();
    }

    @Override
    public String getLocalName() {
        return target.getLocalName();
    }

    @Override
    public boolean hasName() {
        return target.hasName();
    }

    @Override
    public String getNamespaceURI() {
        return target.getNamespaceURI();
    }

    @Override
    public String getPrefix() {
        return target.getPrefix();
    }

    @Override
    public String getVersion() {
        return target.getVersion();
    }

    @Override
    public boolean isStandalone() {
        return target.isStandalone();
    }

    @Override
    public boolean standaloneSet() {
        return target.standaloneSet();
    }

    @Override
    public String getCharacterEncodingScheme() {
        return target.getCharacterEncodingScheme();
    }

    @Override
    public String getPITarget() {
        return target.getPITarget();
    }

    @Override
    public String getPIData() {
        return target.getPIData();
    }

    private XMLStreamReader createHeadTarget() {
        return new ElementScanningDelegate(
                createReaderFromString(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<wiseml version=\"1.0\" xmlns=\"http://wisebed.eu/ns/wiseml/1.0\">" +
                        "<setup></setup></wiseml>", null),
                "setup") {
            @Override
            protected void onLocalNameApplies() {
                target = this;
                nextState();
            }
        };
    }

    private XMLStreamReader createSequenceTarget(final XMLStreamReader[] readers, final String... elementNames) {
        final int[] indexRef = new int[]{0};
        return new ElementScanningDelegate(readers[0], elementNames) {
            @Override
            protected void onLocalNameApplies() {
                indexRef[0]++;
                if (indexRef[0] < readers.length) {
                    target = readers[indexRef[0]];
                } else {
                    target = WiseMLMergingStreamReader.this;
                    nextState();
                }
            }
        };
    }

    private static XMLStreamReader createReaderFromString(String source, String nextElement) {
        try {
            XMLStreamReader target = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(source));
            while (nextElement != null && target.hasNext()) {
                if (target.getEventType() == XMLStreamConstants.START_ELEMENT) {
                    if (target.getLocalName().equals(nextElement)) {
                        break;
                    }
                }
                target.next();
            }
            return target;
        } catch (XMLStreamException e) {
            throw new IllegalArgumentException("exception while processing source", e);
        }
    }

}
