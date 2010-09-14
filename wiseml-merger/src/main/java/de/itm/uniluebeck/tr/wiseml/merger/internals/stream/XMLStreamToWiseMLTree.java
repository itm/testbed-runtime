package de.itm.uniluebeck.tr.wiseml.merger.internals.stream;

import javax.xml.stream.XMLStreamReader;

import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public class XMLStreamToWiseMLTree implements WiseMLTreeReader {

	private XMLStreamReader reader;

	public XMLStreamToWiseMLTree(XMLStreamReader reader) {
		this.reader = reader;
	}

	@Override
	public void exception(String message, Throwable throwable) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public WiseMLTreeReader getParentReader() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WiseMLTreeReader getSubElementReader() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isFinished() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isList() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isMappedToTag() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean nextSubElementReader() {
		// TODO Auto-generated method stub
		return false;
	}
	
}
