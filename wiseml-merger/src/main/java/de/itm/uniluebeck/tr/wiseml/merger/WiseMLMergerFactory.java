package de.itm.uniluebeck.tr.wiseml.merger;

import java.util.List;

import javax.xml.stream.XMLStreamReader;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements.WiseMLMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.stream.WiseMLTreeToXMLStream;
import de.itm.uniluebeck.tr.wiseml.merger.internals.stream.XMLStreamToWiseMLTree;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

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

		// turn XMLStreamReaders into WiseMLTreeReaders
		WiseMLTreeReader[] treeReaders = new WiseMLTreeReader[inputReaders.length];
		for (int i = 0; i < inputReaders.length; i++) {
			treeReaders[i] = new XMLStreamToWiseMLTree(inputReaders[i]);
		}

		// merge multiple WiseMLTreeReaders into one
		WiseMLMerger merger = new WiseMLMerger(treeReaders, config);

		// turn the WiseMLMerger into a XMLStreamReader
		XMLStreamReader outputReader = new WiseMLTreeToXMLStream(merger, config);

		return outputReader;
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
		return createMergingWiseMLStreamReader(
				config, inputReaders.toArray(new XMLStreamReader[inputReaders.size()]));
	}

	public static final XMLStreamReader createMergingWiseMLStreamReader(
			final List<XMLStreamReader> inputReaders) {
		return createMergingWiseMLStreamReader(getDefaultConfiguration(),
				inputReaders);
	}

}
