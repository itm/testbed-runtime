package de.itm.uniluebeck.tr.wiseml.merger;

import javax.xml.stream.XMLStreamReader;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.MergingInfo;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLMergingStreamReader;

import java.util.List;

public class WiseMLMergerFactory {
	
	public static final MergerConfiguration getDefaultConfiguration() {
		return new MergerConfiguration();
	}
	
	public static final XMLStreamReader createMergingWiseMLStreamReader(
			final MergerConfiguration config, 
			final XMLStreamReader... inputReaders) {
		if (inputReaders.length == 0)
			return null;
		if (inputReaders.length == 1)
			return inputReaders[0];
		return new WiseMLMergingStreamReader(
				new MergingInfo(config, inputReaders));
	}
	
	public static final XMLStreamReader createMergingWiseMLStreamReader(
			final XMLStreamReader... inputReaders) {
		return createMergingWiseMLStreamReader(getDefaultConfiguration(), 
				inputReaders);
	}

    public static final XMLStreamReader createMergingWiseMLStreamReader(
			final MergerConfiguration config,
			final List<XMLStreamReader> inputReaders) {
		if (inputReaders.size() == 0)
			return null;
		if (inputReaders.size() == 1)
			return inputReaders.get(0);
		return new WiseMLMergingStreamReader(
				new MergingInfo(config, inputReaders.toArray(new XMLStreamReader[0])));
	}

	public static final XMLStreamReader createMergingWiseMLStreamReader(
			final List<XMLStreamReader> inputReaders) {
		return createMergingWiseMLStreamReader(getDefaultConfiguration(),
				inputReaders);
	}

}
