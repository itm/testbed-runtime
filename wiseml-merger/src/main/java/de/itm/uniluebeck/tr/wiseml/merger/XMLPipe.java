package de.itm.uniluebeck.tr.wiseml.merger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public class XMLPipe {
	
	private final XMLStreamReader reader;
	private final XMLStreamWriter writer;
	private int namespaceIndex = 0;
	
	public XMLPipe(final XMLStreamReader reader, final XMLStreamWriter writer) {
		this.reader = reader;
		this.writer = writer;
	}
	
	public boolean hasNext() throws XMLStreamException {
		return reader.hasNext();
	}
	
	public int next() throws XMLStreamException {
		switch (reader.getEventType()) {
		case XMLStreamConstants.START_DOCUMENT:
			writer.writeStartDocument(reader.getEncoding(), reader.getVersion());
			break;
		case XMLStreamConstants.END_DOCUMENT:
			writer.writeEndDocument();
			break;
		case XMLStreamConstants.START_ELEMENT:
			writer.writeStartElement(reader.getPrefix(), reader.getLocalName(), reader.getNamespaceURI());
			for (int i = 0; i < reader.getAttributeCount(); i++) {
				String prefix = reader.getAttributePrefix(i);
				String namespace = reader.getAttributeNamespace(i);
				String localName = reader.getAttributeLocalName(i);
				String value = reader.getAttributeValue(i);

				if (prefix == null || prefix.equals("")) {
					if (namespace == null) {
						writer.writeAttribute(localName, value);
					} else {
						writer.writeAttribute(namespace, localName, value);
					}
				} else {
					writer.writeAttribute(prefix, namespace, localName, value);
				}
			}
			break;
		case XMLStreamConstants.END_ELEMENT:
			writer.writeEndElement();
			break;
		case XMLStreamConstants.NAMESPACE:
			writer.writeNamespace(reader.getNamespacePrefix(namespaceIndex),
					reader.getNamespaceURI(namespaceIndex));
			namespaceIndex++;
			break;
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
			writer.writeProcessingInstruction(reader.getPITarget(), reader.getPIData());
			break;
		case XMLStreamConstants.ENTITY_REFERENCE:
			writer.writeEntityRef(reader.getText());
			break;
		case XMLStreamConstants.DTD:
			writer.writeDTD(reader.getText());
		}
		return reader.next();
	}
	
	public void streamUntilEnd() throws XMLStreamException {
		while (hasNext()) {
			next();
		}
	}

}
