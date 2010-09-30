package de.itm.uniluebeck.tr.wiseml.merger.internals.stream;

import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.enums.PrefixOutput;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLAttribute;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public class WiseMLTreeToXMLStream implements XMLStreamReader {
	
	
	private static final int BUFFERED_INDENTATIONS = 12;
	private static final String NAMESPACE_URI =
		"http://wisebed.eu/ns/wiseml/1.0";
	private static final String ENCODING = "UTF-8";
	
	private class Event {
		
		protected final int type;
		
		public Event(final int type) {
			this.type = type;
		}
		
		public void activate() {
			eventType = this.type;
		}
		
	}
	
	private class TagEvent extends Event {
		
		private final String elementLocalName;
		private final String[] elementAttributeNames;
		private final String[] elementAttributeValues;
		private final String[] elementNamespaces;
		
		public TagEvent(
				final int type, final String localName, 
				final String[] attributeNames, final String[] attributeValues,
				final String[] namespaces) {
			super(type);
			this.elementLocalName = localName;
			this.elementAttributeNames = attributeNames;
			this.elementAttributeValues = attributeValues;
			this.elementNamespaces = namespaces;
		}
		
		public void activate() {
			super.activate();
			localName = this.elementLocalName;
			attributeNames = this.elementAttributeNames;
			attributeValues = this.elementAttributeValues;
			whitespace = false;
			namespaces = elementNamespaces;
		}
		
		@Override
		public String toString() {
			if (type == XMLStreamConstants.START_ELEMENT) {
				return "<"+elementLocalName+">";
			} else {
				return "</"+elementLocalName+">";
			}
		}
		
	}
	
	private class WhitespaceEvent extends Event {
		
		private final String whitespace;
		
		public WhitespaceEvent(final String whitespace) {
			super(XMLStreamConstants.SPACE);
			this.whitespace = whitespace;
		}
		
		public void activate() {
			super.activate();
			text = this.whitespace;
		}
		
		@Override
		public String toString() {
			return "(WS)";
		}
		
	}
	
	private class TextEvent extends Event {
		
		private final String characters;
		
		public TextEvent(final int type, final String characters) {
			super(type);
			this.characters = characters;
		}
		
		public void activate() {
			super.activate();
			text = this.characters;
		}
		
		@Override
		public String toString() {
			return "(TXT)";
		}
		
	}
	
	private class DocumentEvent extends Event {
		
		public DocumentEvent(final int type) {
			super(type);
		}
		
		public void activate() {
			super.activate();
			if (eventType == XMLStreamConstants.END_DOCUMENT) {
				finished = true;
			}
		}
		
		@Override
		public String toString() {
			return "(DOC)";
		}
		
	}
 	
	private WiseMLTreeReader reader;
	private MergerConfiguration config;
	
	private String[] indentations;
	
	private List<Event> eventQueue;
	
	/* STATE */
	private int eventType;
	private String localName;
	private String[] attributeNames;
	private String[] attributeValues;
	private String text;
	private boolean whitespace;
	private boolean finished;
	private String[] namespaces;
	
	private int level;
		
	private WiseMLTreeReader currentReader;
	
	public WiseMLTreeToXMLStream(
			final WiseMLTreeReader reader,
			final MergerConfiguration config) {
		this.reader = reader;
		this.config = config;
		
		this.currentReader = null;;
		
		generateIndentations();
		
		this.eventQueue = new LinkedList<Event>();
		startDocument();
		try {
			next();
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void queueEvent(final Event e) {
		this.eventQueue.add(e);
	}
	
	private Event nextEvent() {
		if (this.eventQueue.isEmpty()) {
			return null;
		}
		return this.eventQueue.remove(0);
	}
	
	private void startDocument() {
		queueEvent(new DocumentEvent(XMLStreamConstants.START_DOCUMENT));
	}
	
	private void endDocument() {
		queueEvent(new DocumentEvent(XMLStreamConstants.END_DOCUMENT));
	}
	
	private void startElement(
			final WiseMLTag tag, 
			final List<WiseMLAttribute> attributes, 
			final String text) {
		// get attributes
		String[] attributeNames = new String[attributes.size()];
		String[] attributeValues = new String[attributes.size()];
		for (int i = 0; i < attributes.size(); i++) {
			attributeNames[i] = attributes.get(i).getName();
			attributeValues[i] = attributes.get(i).getValue();
		}
		
		// handle whitespace
		String indent_str = indentation(level);
		if (indent_str != null) {
			queueEvent(new WhitespaceEvent(indent_str));
		}
		
		// add start tag
		queueEvent(new TagEvent(
				XMLStreamConstants.START_ELEMENT, 
				tag.getLocalName(),
				attributeNames,
				attributeValues,
				((tag.equals(WiseMLTag.wiseml)
						?
							new String[]{NAMESPACE_URI}
						:
							null))
				));
		
		// add text
		if (tag.isTextOnly()) {
			queueEvent(new TextEvent(
					XMLStreamConstants.CHARACTERS,
					text));
		}
		
		level++;
	}
	
	private void endElement(final WiseMLTag tag) {
		// handle whitespace
		if (!tag.isTextOnly()) {
			String indent_str = indentation(level);
			if (indent_str != null) {
				queueEvent(new WhitespaceEvent(indent_str));
			}
		}
		
		// add end tag
		queueEvent(new TagEvent(
				XMLStreamConstants.END_ELEMENT, 
				tag.getLocalName(),
				null,
				null,
				null));
		
		level--;
	}
	/*
	private void processCurrentReader() {
		if (currentReader.isList()) {
			if (currentReader.getSubElementReader() == null) {
				currentReader.nextSubElementReader();
			}
			if (currentReader.getSubElementReader() != null) {
				currentReader = currentReader.getSubElementReader();
				processCurrentReader();
			}
		} else {
			startElement(
					currentReader.getTag(), 
					currentReader.getAttributeList(), 
					currentReader.getText());
			
			if (!currentReader.isFinished()) {
				currentReader.nextSubElementReader();
			}
			
			if (currentReader.getSubElementReader() == null) {
				currentReader = currentReader.getSubElementReader();
				processCurrentReader();
			}
		}
	}
	*/
	private void introduceReader(final WiseMLTreeReader reader) {
		if (reader == null) {
			return;
		}
		currentReader = reader;
		if (reader.isList()) {
			introduceReader(getFirstChild(currentReader));
		} else {
			startElement(
					currentReader.getTag(), 
					currentReader.getAttributeList(), 
					currentReader.getText());
			
			introduceReader(getFirstChild(currentReader));
		}
	}
	
	private static WiseMLTreeReader getFirstChild(
			final WiseMLTreeReader reader) {
		if (reader.isFinished()) {
			return null;
		}
		if (reader.nextSubElementReader()) {
			return reader.getSubElementReader();
		}
		return null;
	}

	private void fillQueue() {
		if (currentReader == null) {
			introduceReader(reader);
		} else {
			while (eventQueue.isEmpty()) {
				if (currentReader.isFinished()) {
					if (currentReader.isMappedToTag()) {
						endElement(currentReader.getTag());
					}
					if (currentReader == reader) {
						endDocument();
						return;
					}
					currentReader = currentReader.getParentReader();
				} else {
					if (currentReader.nextSubElementReader()) {
						introduceReader(currentReader.getSubElementReader());
					}
				}
			}
		}
		if (eventQueue.isEmpty()) {
			finished = true;
		}
	}
	
	private void generateIndentations() {
		String[] indentations = new String[BUFFERED_INDENTATIONS];
		
		for (int i = 0; i < indentations.length; i++) {
			indentations[i] = indentation(i);
		}
	}
	
	private String indentation(int level) {
		if (config.getIndentation().preventWhitespace()) {
			return null;
		}
		if (indentations != null && level < indentations.length) {
			return indentations[level];
		}
		String indent_str = config.getIndentation().getIndentationElement();
		StringBuilder sb = new StringBuilder(1+level*indent_str.length());
		sb.append('\n');
		for (int i = 0; i < level; i++) {
			sb.append(indent_str);
		}
		return sb.toString();
	}

	@Override
	public void close() throws XMLStreamException {
		reader = null;
		indentations = null;
		eventQueue = null;
		localName = null;
		attributeNames = null;
		attributeValues = null;
		text = null;
		currentReader = null;
		finished = true;
	}
	
	private void checkStateForEvents(int... eventTypes) {
		for (int i = 0; i < eventTypes.length; i++) {
			if (eventType == eventTypes[i]) {
				return;
			}
		}
		throw new IllegalStateException(
				"illegal operation at current event type (" + 
				streamEventTypeToString(eventType)+")");
	}
	
	private void checkGeneralState() {
		if (finished) {
			throw new IllegalStateException("reader finished/closed");
		}
	}
	
	private QName toQName(String localName) {
		return new QName(NAMESPACE_URI, localName);
	}

	@Override
	public int getAttributeCount() {
		checkGeneralState();
		checkStateForEvents(
				XMLStreamConstants.START_ELEMENT,
				XMLStreamConstants.ATTRIBUTE);
		return attributeNames.length;
	}

	@Override
	public String getAttributeLocalName(int index) {
		checkGeneralState();
		checkStateForEvents(
				XMLStreamConstants.START_ELEMENT,
				XMLStreamConstants.ATTRIBUTE);
		return attributeNames[index];
	}

	@Override
	public QName getAttributeName(int index) {
		return toQName(getAttributeLocalName(index));
	}

	@Override
	public String getAttributeNamespace(int index) {
		return null;
	}
	
	private String getConfiguredPrefix() {
		switch (config.getPrefixOutput()) {
		case DefaultNamespaceOnly:
			return "";
		case WiseMLPrefix:
			return "wiseml";
		case CustomPrefix:
			return config.getCustomPrefix();
		default:
			throw new RuntimeException("unknown prefix output: "+config.getPrefixOutput());
		}
	}

	@Override
	public String getAttributePrefix(int index) {
		return getConfiguredPrefix();
	}

	@Override
	public String getAttributeType(int index) {
		checkGeneralState();
		checkStateForEvents(
				XMLStreamConstants.START_ELEMENT,
				XMLStreamConstants.ATTRIBUTE);
		throw new UnsupportedOperationException();
	}

	@Override
	public String getAttributeValue(int index) {
		checkGeneralState();
		checkStateForEvents(
				XMLStreamConstants.START_ELEMENT,
				XMLStreamConstants.ATTRIBUTE);
		return attributeValues[index];
	}

	@Override
	public String getAttributeValue(String namespaceURI, String localName) {
		checkGeneralState();
		checkStateForEvents(
				XMLStreamConstants.START_ELEMENT,
				XMLStreamConstants.ATTRIBUTE);
		if (!NAMESPACE_URI.equals(namespaceURI)) {
			return null;
		}
		for (int i = 0; i < attributeNames.length; i++) {
			if (attributeNames[i].equals(localName)) {
				return attributeValues[i];
			}
		}
		return null;
	}

	@Override
	public String getCharacterEncodingScheme() {
		return ENCODING;
	}

	@Override
	public String getElementText() throws XMLStreamException {
		checkGeneralState();
		checkStateForEvents(XMLStreamConstants.START_ELEMENT);
		StringBuilder sb = new StringBuilder();
		while (true) {
			switch (next()) {
			case XMLStreamConstants.CHARACTERS:
			case XMLStreamConstants.CDATA:
				sb.append(text);
				break;
			case XMLStreamConstants.END_ELEMENT:
				return sb.toString();
			default:
				streamException(
					"illegal event type, expected END_ELEMENT or text event");
			}
		}
	}

	@Override
	public String getEncoding() {
		return ENCODING;
	}

	@Override
	public int getEventType() {
		return eventType;
	}

	@Override
	public String getLocalName() {
		checkGeneralState();
		checkStateForEvents(START_ELEMENT, END_ELEMENT);
		return localName;
	}

	@Override
	public Location getLocation() {
		return new Location(){

			@Override
			public int getLineNumber() {
				return -1;
			}

			@Override
			public int getColumnNumber() {
				return -1;
			}

			@Override
			public int getCharacterOffset() {
				return 0;
			}

			@Override
			public String getPublicId() {
				return null;
			}

			@Override
			public String getSystemId() {
				return null;
			}
			
		};
	}

	@Override
	public QName getName() {
		return toQName(getLocalName());
	}

	@Override
	public NamespaceContext getNamespaceContext() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getNamespaceCount() {
		if (namespaces == null) {
			return 0;
		}
		return namespaces.length;
	}

	@Override
	public String getNamespacePrefix(int index) {
		if (namespaces == null || index > 0) {
			return null;
		}
		return getConfiguredPrefix();
	}

	@Override
	public String getNamespaceURI() {
		if (eventType == XMLStreamConstants.START_ELEMENT && localName.equals("wiseml")) {
			if (config.getPrefixOutput() == PrefixOutput.DefaultNamespaceOnly) {
				return NAMESPACE_URI;
			}
		}
		return null;
	}

	@Override
	public String getNamespaceURI(String prefix) {
		if (prefix.equals(getConfiguredPrefix())) {
			return NAMESPACE_URI;
		}
		return null;
	}

	@Override
	public String getNamespaceURI(int index) {
		if (namespaces == null || index > 0) {
			return null;
		}
		return namespaces[0];
	}

	@Override
	public String getPIData() {
		return null;
	}

	@Override
	public String getPITarget() {
		return null;
	}

	@Override
	public String getPrefix() {
		return getConfiguredPrefix();
	}

	@Override
	public Object getProperty(String name) throws IllegalArgumentException {
		return null;
	}

	@Override
	public String getText() {
		checkGeneralState();
		checkStateForEvents(
				XMLStreamConstants.CHARACTERS,
				XMLStreamConstants.SPACE);
		return text;
	}

	@Override
	public char[] getTextCharacters() {
		checkGeneralState();
		checkStateForEvents(
				XMLStreamConstants.CHARACTERS,
				XMLStreamConstants.SPACE);
		return text.toCharArray();
	}

	@Override
	public int getTextCharacters(int sourceStart, char[] target,
			int targetStart, int length) throws XMLStreamException {
		char[] array = getTextCharacters();
		System.arraycopy(array, sourceStart, target, targetStart, length);
		return length;
	}

	@Override
	public int getTextLength() {
		checkGeneralState();
		checkStateForEvents(
				XMLStreamConstants.CHARACTERS,
				XMLStreamConstants.SPACE);
		return text.length();
	}

	@Override
	public int getTextStart() {
		checkGeneralState();
		checkStateForEvents(
				XMLStreamConstants.CHARACTERS,
				XMLStreamConstants.SPACE);
		return 0;
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public boolean hasName() {
		switch (eventType) {
		case XMLStreamConstants.START_ELEMENT:
		case XMLStreamConstants.END_ELEMENT:
			return true;
		default:
			return false;
		}
	}

	@Override
	public boolean hasNext() throws XMLStreamException {
		return !finished;
	}

	@Override
	public boolean hasText() {
		switch (eventType) {
		case XMLStreamConstants.CHARACTERS:
			return true;
		default:
			return false;
		}
	}

	@Override
	public boolean isAttributeSpecified(int index) {
		checkGeneralState();
		checkStateForEvents(
				XMLStreamConstants.START_ELEMENT,
				XMLStreamConstants.ATTRIBUTE);
		return index >= 0 && index < attributeNames.length;
	}

	@Override
	public boolean isCharacters() {
		checkGeneralState();
		return eventType == XMLStreamConstants.CHARACTERS;
	}

	@Override
	public boolean isEndElement() {
		checkGeneralState();
		return eventType == XMLStreamConstants.END_ELEMENT;
	}

	@Override
	public boolean isStandalone() {
		return true;
	}

	@Override
	public boolean isStartElement() {
		checkGeneralState();
		return eventType == XMLStreamConstants.START_ELEMENT;
	}

	@Override
	public boolean isWhiteSpace() {
		checkGeneralState();
		return whitespace;
	}

	@Override
	public int next() throws XMLStreamException {
		checkGeneralState();
		if (!hasNext()) {
			throw new IllegalStateException("no more events");
		}
		Event nextEvent = nextEvent();
		nextEvent.activate();
		if (eventQueue.isEmpty()) {
			fillQueue();
		}
		return eventType;
	}

	@Override
	public int nextTag() throws XMLStreamException {
		checkGeneralState();
		while (true) {
			switch (next()) {
			case XMLStreamConstants.COMMENT:
			case XMLStreamConstants.PROCESSING_INSTRUCTION:
				break;
			case XMLStreamConstants.START_ELEMENT:
			case XMLStreamConstants.END_ELEMENT:
				return eventType;
			default:
				streamException("found non-whitespace event");
			}
		}
	}
	
	private void streamException(final String msg) throws XMLStreamException {
		throw new XMLStreamException(msg, getLocation());
	}

	@Override
	public void require(int type, String namespaceURI, String localName)
			throws XMLStreamException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean standaloneSet() {
		return true;
	}
	
	private static String streamEventTypeToString(final int eventType) {
		switch (eventType) {
		case XMLStreamConstants.ATTRIBUTE:
			return "ATTRIBUTE";
		case XMLStreamConstants.CDATA:
			return "CDATA";
		case XMLStreamConstants.CHARACTERS:
			return "CHARACTERS";
		case XMLStreamConstants.COMMENT:
			return "COMMENT";
		case XMLStreamConstants.DTD:
			return "DTD";
		case XMLStreamConstants.END_DOCUMENT:
			return "END_DOCUMENT";
		case XMLStreamConstants.END_ELEMENT:
			return "END_ELEMENT";
		case XMLStreamConstants.ENTITY_DECLARATION:
			return "ENTITY_DECLARATION";
		case XMLStreamConstants.ENTITY_REFERENCE:
			return "ENTITY_REFERENCE";
		case XMLStreamConstants.NAMESPACE:
			return "NAMESPACE";
		case XMLStreamConstants.NOTATION_DECLARATION:
			return "NOTATION_DECLARATION";
		case XMLStreamConstants.PROCESSING_INSTRUCTION:
			return "PROCESSING_INSTRUCTION";
		case XMLStreamConstants.SPACE:
			return "SPACE";
		case XMLStreamConstants.START_DOCUMENT:
			return "START_DOCUMENT";
		case XMLStreamConstants.START_ELEMENT:
			return "START_ELEMENT";
		default:
			return "unknown event type: "+eventType;
		}
	}

}
