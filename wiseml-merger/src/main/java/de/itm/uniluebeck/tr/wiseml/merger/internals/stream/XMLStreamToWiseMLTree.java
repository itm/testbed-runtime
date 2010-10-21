package de.itm.uniluebeck.tr.wiseml.merger.internals.stream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLAttribute;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReaderException;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReaderHelper;

/**
 * Takes a XMLStreamReader and presents it as a WiseMLTreeReader.
 * Any comments, processing instructions and whitespaces are ignored.
 * 
 * @author Jacob Kuypers
 *
 */
public class XMLStreamToWiseMLTree implements WiseMLTreeReader {
	
	/**
	 * Maps a tag to a local name.
	 */
	private static Map<String,WiseMLTag> tagMap;
	
	/**
	 * Defines which tags start a sequence in a given parent tag.
	 */
	private static Map<WiseMLTag,List<Set<WiseMLTag>>> listTagSets;
	
	// create maps
	static {
		tagMap = new HashMap<String,WiseMLTag>();
		WiseMLTag[] tags = WiseMLTag.values();
		for (WiseMLTag tag : tags) {
			tagMap.put(tag.getLocalName().toLowerCase(), tag);
		}
		
		listTagSets = new HashMap<WiseMLTag,List<Set<WiseMLTag>>>();
		addList(WiseMLTag.wiseml, WiseMLTag.scenario);
		addList(WiseMLTag.wiseml, WiseMLTag.trace);
		addList(WiseMLTag.node, WiseMLTag.capability);
		addList(WiseMLTag.link, WiseMLTag.capability);
		addList(WiseMLTag.setup, WiseMLTag.node);
		addList(WiseMLTag.setup, WiseMLTag.link);
		addList(WiseMLTag.scenario, 
				WiseMLTag.timestamp, 
				WiseMLTag.enableNode, 
				WiseMLTag.disableNode,
				WiseMLTag.enableLink,
				WiseMLTag.disableLink,
				WiseMLTag.node);
		addList(WiseMLTag.node, WiseMLTag.data);
		addList(WiseMLTag.link, WiseMLTag.data);
	}

	private XMLStreamReader reader;

	private XMLStreamToWiseMLTree parent;
	
	private XMLStreamToWiseMLTree currentChild;
	
	private XMLStreamToWiseMLTree nextChild;
	
	private boolean omitTagSkipping;
	
	private boolean finished;
	
	private WiseMLTag tag;
	private String text;
	private List<WiseMLAttribute> attributeList;
	
	/**
	 * If the current object is supposed to read a list, the tag of
	 * each sub-element must be in this set. Otherwise, the list
	 * is pronounced finished.
	 */
	private Set<WiseMLTag> listTags;

	/**
	 * Creates a WiseMLTreeReader which reads from a
	 * XMLStreamReader.
	 * 
	 * @param reader Input stream, must be at START_DOCUMENT. If the stream is accessed outside this object, results are undefined.
	 */
	public XMLStreamToWiseMLTree(final XMLStreamReader reader) {
		this(null, skipToTag(reader, WiseMLTag.wiseml, true), null);
	}
	
	/**
	 * @param parent The parent object which will be returned by getParentReader
	 * @param reader The input stream, must be at START_ELEMENT for the element relevant to this object
	 * @param listTags Must be null if this object represents a list of elements
	 */
	private XMLStreamToWiseMLTree(
			final XMLStreamToWiseMLTree parent, 
			final XMLStreamReader reader, 
			final Set<WiseMLTag> listTags) {
		// precondition: reader must be at a start tag
		
		this.parent = parent;
		this.reader = reader;
		this.listTags = listTags;
		finished = false;
		currentChild = null;
		
		if (listTags != null) {
			// read the next element - it will be used later in nextSubElementReader
			nextChild = new XMLStreamToWiseMLTree(this, reader, findSet(localNameToTag(reader.getLocalName())));
		} else {
			// read local name
			tag = localNameToTag(reader.getLocalName());
			
			if (tag == null) {
				this.exception("unrecognized tag: "+reader.getLocalName(), null);
			}
			
			// read attributes
			attributeList = new ArrayList<WiseMLAttribute>(reader.getAttributeCount());
			for (int i = 0; i < reader.getAttributeCount(); i++) {
				attributeList.add(new WiseMLAttribute(
						reader.getAttributeLocalName(i), 
						reader.getAttributeValue(i)));
			}
			
			// read text if present
			if (tag.isTextOnly()) {
				try {
					text = reader.getElementText();
				} catch (XMLStreamException e) {
					this.exception("could not read text in <"+tag+">", e);
				}
				finished = true;
			}
		}
	}

	@Override
	public void exception(String message, Throwable throwable) {
		throw new WiseMLTreeReaderException(message, throwable, this);
	}

	@Override
	public WiseMLTreeReader getParentReader() {
		return parent;
	}

	@Override
	public WiseMLTreeReader getSubElementReader() {
		return currentChild;
	}

	@Override
	public boolean isFinished() {
		return finished;
	}

	@Override
	public boolean isList() {
		return tag == null;
	}

	@Override
	public boolean isMappedToTag() {
		return tag != null;
	}

	@Override
	public boolean nextSubElementReader() {
		if (finished) {
			return false;
		}
		
		if (currentChild != null) {
			// skip child in order to bring the input stream to the next element
			WiseMLTreeReaderHelper.skipToEnd(currentChild);
			currentChild = null;
		}

		if (isMappedToTag()) {
			if (reader.isEndElement() 
					&& localNameToTag(reader.getLocalName()).equals(this.tag)) {
				// the end tag has been reached, no need for skipping
				finished = true;
				return false;
			}
		} else {
			if (nextChild != null) {
				currentChild = nextChild;
				nextChild = null;
				return true;
			}
		}
		
		// go to the next tag
		if (omitTagSkipping) {
			omitTagSkipping = false;
		} else {
			skipToTag();
		}
		WiseMLTag nextTag = localNameToTag(reader.getLocalName()); //tagMap.get(reader.getLocalName());
		/*
		if (nextTag.equals(WiseMLTag.link)) {
			System.err.println("LINK");
		}
		*/
		if (isList()) {
			if (reader.isStartElement()) {
				if (listTags.contains(nextTag)) {
					currentChild = new XMLStreamToWiseMLTree(this, reader, findSet(nextTag));
					return true;
				} else {
					// next tag does not belong to this list => finished
					finished = true;
					
					// tell parent to omit skipping the next tag
					if (parent != null) {
						parent.omitTagSkipping = true;
					}
					
					return false;
				}
			} else {
				// end of parent tag reached, list finished
				finished = true;
				return false;
			}
		} else {
			if (reader.isStartElement()) {
				currentChild = new XMLStreamToWiseMLTree(this, reader, findSet(nextTag));
				return true;
			} else {
				if (nextTag.equals(this.tag)) {
					// end of current tag reached, list finished
					finished = true;
					return false;
				} else {
					throw new IllegalStateException();
				}
			}
		}
	}
	
	/**
	 * If the tag of a child element matches one from one of the
	 * sets defined for the tag of this object to start a new list,
	 * that set is returned.
	 * 
	 * @param tag Tag of a child element
	 * @return Set of tags for the next list or null for no list.
	 */
	private Set<WiseMLTag> findSet(WiseMLTag tag) {
		List<Set<WiseMLTag>> sets = listTagSets.get(this.tag);
		if (sets == null) {
			return null;
		}
		
		for (Set<WiseMLTag> set : sets) {
			if (set.contains(tag)) {
				return set;
			}
		}
		
		return null;
	}

	@Override
	public List<WiseMLAttribute> getAttributeList() {
		return attributeList;
	}

	@Override
	public WiseMLTag getTag() {
		return tag;
	}

	@Override
	public String getText() {
		return text;
	}
	
	/**
	 * Skips to the next start or end tag.
	 */
	private void skipToTag() {
		try {
			while (reader.hasNext()) {
				reader.next();
				if (reader.isStartElement() || reader.isEndElement()) {
					return;
				}
			}
			throw new XMLStreamException("unexpected end of stream", reader.getLocation());
		} catch (XMLStreamException e) {
			throw new WiseMLTreeReaderException("exception while skipping to next tag", e, this);
		}
	}
	
	/**
	 * Skips t a specific tag. All events other than START_ELEMENT and
	 * END_ELEMENT are ignored.
	 * @param reader Input stream.
	 * @param tag Tag to stop at or null if any tag is OK.
	 * @param start True if the tag must be a start tag.
	 * @return The input stream (reader argument).
	 */
	private static XMLStreamReader skipToTag(XMLStreamReader reader, WiseMLTag tag, boolean start) {
		try {
			while (reader.hasNext()) {
				reader.next();
				if (reader.isStartElement() && start || reader.isEndElement() && !start) {
					if (tag == null) {
						return reader;
					}
					if (reader.getLocalName().equals(tag.getLocalName())) {
						return reader;
					}
				}
			}
			if (reader.getLocation() == null) {
				throw new XMLStreamException("unexpected end of stream");
			} else {
				throw new XMLStreamException("unexpected end of stream", reader.getLocation());
			}
		} catch (XMLStreamException e) {
			throw new WiseMLTreeReaderException("exception while skipping to "+tag, e, null);
		}
	}
	
	/**
	 * Creates a simple rule which indicates where a list can occur
	 * and which tags it contains.
	 * Not that this approach only works because XML files produced by
	 * the WiseML 1.0 grammar are very simple.
	 * Future changes might require a new strategy at this point.
	 * 
	 * @param container
	 * @param listItemTags
	 */
	private static void addList(WiseMLTag container, WiseMLTag... listItemTags) {
		List<Set<WiseMLTag>> sets = listTagSets.get(container);
		if (sets == null) {
			sets = new LinkedList<Set<WiseMLTag>>();
			listTagSets.put(container, sets);
		}
		Set<WiseMLTag> set = new HashSet<WiseMLTag>();
		for (WiseMLTag tag : listItemTags) {
			set.add(tag);
		}
		sets.add(set);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (isList()) {
			sb.append("[list]");
			sb.append('[');
			for (WiseMLTag tag : listTags) {
				sb.append(tag);
				sb.append(' ');
			}
			sb.append(']');
		} else {
			sb.append("[tag]");
			sb.append('[');
			sb.append(tag);
			sb.append(']');
		}
		sb.append('{');
		if (currentChild != null) {
			sb.append(currentChild.toString());
		}
		sb.append('}');
		return sb.toString();
	}
	
	private static WiseMLTag localNameToTag(final String localName) {
		return tagMap.get(localName.toLowerCase());
	}
	
}
