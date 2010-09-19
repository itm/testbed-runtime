package de.itm.uniluebeck.tr.wiseml.merger.internals.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLAttribute;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;

public abstract class WiseMLStructureReader implements WiseMLTreeReader {
	
	public static class Element implements WiseMLTreeReader {
		private WiseMLTag e_tag;
		private List<WiseMLAttribute> attributes;
		private String text;
		private Element[] subElements;
		private WiseMLTreeReader parent;
		
		private int subElementIndex;
		
		public Element(
				WiseMLTreeReader parent, 
				WiseMLTag e_tag, 
				WiseMLAttribute[] attributes, 
				Element[] subElements,
				String text) {
			this.parent = parent;
			this.e_tag = e_tag;
			this.attributes = (attributes == null)?new LinkedList<WiseMLAttribute>():Arrays.asList(attributes);
			this.subElements = (subElements == null)?new Element[0]:subElements;
			this.text = text;
			
			for (Element e : this.subElements) {
				e.parent = this;
			}
			
			this.subElementIndex = 0;
		}
		
		@Override
		public List<WiseMLAttribute> getAttributeList() {
			return attributes;
		}
		
		@Override
		public WiseMLTag getTag() {
			return e_tag;
		}
		
		@Override
		public String getText() {
			return text;
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
			return subElements[subElementIndex];
		}
		
		@Override
		public boolean isFinished() {
			return subElementIndex == subElements.length;
		}
		
		@Override
		public boolean isList() {
			return false;
		}
		
		@Override
		public boolean isMappedToTag() {
			return true;
		}
		
		@Override
		public boolean nextSubElementReader() {
			if (isFinished()) {
				return false;
			}
			
			subElementIndex++;
			return !isFinished();
		}
	}
	
	private Element element;
	
	protected WiseMLStructureReader(final Element element) {
		this.element = element;
	}

	public void exception(String message, Throwable throwable) {
		element.exception(message, throwable);
	}

	public List<WiseMLAttribute> getAttributeList() {
		return element.getAttributeList();
	}

	public WiseMLTreeReader getParentReader() {
		return element.getParentReader();
	}

	public WiseMLTreeReader getSubElementReader() {
		return element.getSubElementReader();
	}

	public WiseMLTag getTag() {
		return element.getTag();
	}

	public String getText() {
		return element.getText();
	}

	public int hashCode() {
		return element.hashCode();
	}

	public boolean isFinished() {
		return element.isFinished();
	}

	public boolean isList() {
		return element.isList();
	}

	public boolean isMappedToTag() {
		return element.isMappedToTag();
	}

	public boolean nextSubElementReader() {
		return element.nextSubElementReader();
	}
	
	protected static Element createPureTextElement(
			final WiseMLTreeReader parent, 
			final WiseMLTag tag, 
			final String text, 
			final WiseMLAttribute... attributes) {
		return new Element(parent, tag, attributes, null, text);
	}
	
	protected static Element[] createSubElementsFromReaders(
			final WiseMLStructureReader... readers) {
		List<Element> result = new ArrayList<Element>(readers.length);
		
		for (int i = 0; i < readers.length; i++) {
			if (readers[i] != null) {
				result.add(readers[i].element);
			}
		}
		
		return result.toArray(new Element[result.size()]);
	}
}
