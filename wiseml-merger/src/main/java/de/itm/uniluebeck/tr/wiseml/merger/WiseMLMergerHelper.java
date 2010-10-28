package de.itm.uniluebeck.tr.wiseml.merger;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;

public class WiseMLMergerHelper {
	
	public static final String mergeFromStrings(
			final MergerConfiguration config,
			final String... inputs) throws XMLStreamException {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		
		// make XMLStreamReaders from Strings
		List<XMLStreamReader> streamReaders = new ArrayList<XMLStreamReader>();
		for (String input : inputs) {
			streamReaders.add(inputFactory.createXMLStreamReader(
					new StringReader(input)));
		}
		
		// output stream
		StringWriter stringWriter = new StringWriter();
		XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(
				stringWriter);
		
		// create merging stream
		XMLStreamReader mergingReader = WiseMLMergerFactory
			.createMergingWiseMLStreamReader(config, streamReaders);
		
		// pipe merging stream to output stream
		new XMLPipe(mergingReader, streamWriter).streamUntilEnd();
		
		// return output string
		return stringWriter.getBuffer().toString();
	}
	
	public static final String mergeFromStrings(
			final MergerConfiguration config,
			final List<String> inputs) throws XMLStreamException {
		return mergeFromStrings(config, inputs.toArray(new String[0]));
	}

}
