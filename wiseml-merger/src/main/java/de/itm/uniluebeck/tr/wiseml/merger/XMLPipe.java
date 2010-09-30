package de.itm.uniluebeck.tr.wiseml.merger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public class XMLPipe {
	
	private final XMLStreamReader reader;
	private final XMLStreamWriter writer;
	
	private boolean atStart = true;
	
	public XMLPipe(final XMLStreamReader reader, final XMLStreamWriter writer) {
		this.reader = reader;
		this.writer = writer;
	}
	
	public boolean hasNext() throws XMLStreamException {
		return reader.hasNext();
	}
	
	private void writeCurrentEvent() throws XMLStreamException {
		switch (reader.getEventType()) {
		case XMLStreamConstants.START_DOCUMENT:
			writer.writeStartDocument(
					reader.getEncoding(), 
					reader.getVersion());
			break;
		case XMLStreamConstants.END_DOCUMENT:
			writer.writeEndDocument();
			writer.flush();
			break;
		case XMLStreamConstants.START_ELEMENT:
			writeStartElement(
					reader.getPrefix(), 
					reader.getLocalName(), 
					reader.getNamespaceURI());
			for (int i = 0; i < reader.getAttributeCount(); i++) {
				writeAttribute(
						reader.getAttributePrefix(i), 
						reader.getAttributeLocalName(i), 
						reader.getAttributeNamespace(i), 
						reader.getAttributeValue(i));
			}
			break;
		case XMLStreamConstants.END_ELEMENT:
			writer.writeEndElement();
			break;
/*		case XMLStreamConstants.NAMESPACE:
			writer.writeNamespace(reader.getNamespacePrefix(namespaceIndex),
					reader.getNamespaceURI(namespaceIndex));
			namespaceIndex++;
			break;*/
		case XMLStreamConstants.CHARACTERS:
			//writer.writeCharacters(new String(reader.getTextCharacters()));
			writer.writeCharacters(reader.getText());
			break;
		case XMLStreamConstants.CDATA:
			//writer.writeCData(new String(reader.getTextCharacters()));
			writer.writeCData(reader.getText());
			break;
		case XMLStreamConstants.COMMENT:
			writer.writeComment(reader.getText());
			break;
		case XMLStreamConstants.SPACE:
			writer.writeCharacters(reader.getText());
			break;
		case XMLStreamConstants.PROCESSING_INSTRUCTION:
			writer.writeProcessingInstruction(
					reader.getPITarget(), 
					reader.getPIData());
			break;
		case XMLStreamConstants.ENTITY_REFERENCE:
			writer.writeEntityRef(reader.getText());
			break;
		case XMLStreamConstants.DTD:
			writer.writeDTD(reader.getText());
			break;
		default:
			throw new IllegalStateException("unknown event type: " + 
					reader.getEventType());
		}
	}
	
	private void writeAttribute(
			final String prefix, 
			final String localName, 
			final String namespaceURI, 
			final String value) throws XMLStreamException {
		if (value == null) {
			throw new IllegalArgumentException("value cannot be null");
		}
		if (localName == null) {
			throw new IllegalArgumentException("localName cannot be null");
		}
		if (prefix == null || prefix.equals("")) {
			if (namespaceURI == null) {
				writer.writeAttribute(localName, value);
			} else {
				if (writer.getPrefix(namespaceURI) == null) {
					writer.writeAttribute(localName, value);
				} else {
					writer.writeAttribute(namespaceURI, localName, value);
				}
			}
		} else {
			writer.writeAttribute(prefix, namespaceURI, localName, value);
		}
	}
	
	private void writeStartElement(
			final String prefix, 
			final String localName, 
			final String namespaceURI) throws XMLStreamException {
		if (prefix == null || prefix.equals("")) {
			if (namespaceURI == null) {
				writer.writeStartElement(localName);
			} else {
				if (writer.getPrefix(namespaceURI) == null) {
					writer.writeStartElement(localName);
					writer.writeDefaultNamespace(namespaceURI);
				} else {
					writer.writeStartElement(namespaceURI, localName);
				}
			}
		} else {
			writer.writeStartElement(prefix, localName, namespaceURI);
		}
	}
	
	public int next() throws XMLStreamException {
		if (atStart) {
			writeCurrentEvent();
			atStart = false;
		}
		int nextEvent = reader.next();
		writeCurrentEvent();
		return nextEvent;
	}
	
	public int getEventType() {
		return reader.getEventType();
	}
	
	public void streamUntilEnd() throws XMLStreamException {
		while (hasNext()) {
			next();
		}
	}

}
