package de.itm.uniluebeck.tr.wiseml.merger.internals;

import javax.xml.stream.XMLStreamReader;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;

public class MergingInfo {
	
	private MergerConfiguration config;
	private XMLStreamReader[] readers;
	
	public MergingInfo(MergerConfiguration config, XMLStreamReader[] readers) {
		super();
		this.config = config;
		this.readers = readers;
	}

	public MergerConfiguration getConfig() {
		return config;
	}

	public XMLStreamReader[] getReaders() {
		return readers;
	}

}
