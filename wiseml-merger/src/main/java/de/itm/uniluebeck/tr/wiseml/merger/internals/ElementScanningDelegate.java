package de.itm.uniluebeck.tr.wiseml.merger.internals;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import java.io.StringReader;

/**
 * Delegates calls to another XMLStreamReader until a certain element is reached.
 *
 * User: kuypers
 * Date: 19.08.2010
 * Time: 18:00:28
 * To change this template use File | Settings | File Templates.
 */
public abstract class ElementScanningDelegate implements XMLStreamReader {

    protected XMLStreamReader target;
    private String[] elementNames;

    public ElementScanningDelegate(XMLStreamReader target, String... elementNames) {
        this.target = target;
        this.elementNames = elementNames;
    }

    @Override
    public Object getProperty(String s) throws IllegalArgumentException {
        return target.getProperty(s);
    }

    private boolean localNameFound() {
        String elementName = target.getLocalName();
        for (int i = 0; i < elementNames.length; i++) {
            if (elementName.equals(elementNames[i])) {
                return true;
            }
        }
        return false;
    }

    protected abstract void onLocalNameApplies();

    @Override
    public int next() throws XMLStreamException {
        int result = target.next();
        if (result == XMLStreamConstants.START_ELEMENT && localNameFound()) {
            onLocalNameApplies();
            return target.next();
        }
        return result;
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
        int result = target.nextTag();
        if (localNameFound()) {
            onLocalNameApplies();
            return target.nextTag();
        }
        return result;
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



}
