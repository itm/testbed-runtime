package de.itm.uniluebeck.tr.wiseml.merger.examples;

import de.itm.uniluebeck.tr.wiseml.merger.WiseMLMergerFactory;
import de.itm.uniluebeck.tr.wiseml.merger.XMLPipe;
import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileReader;


public class Example2 extends Examples {

	public static void main(String[] args) throws Exception {

		File fileA = new File("resources/2/a.xml");
		File fileB = new File("resources/2/b.xml");

		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLStreamReader inputA = inputFactory.createXMLStreamReader(new FileReader(fileA));
		XMLStreamReader inputB = inputFactory.createXMLStreamReader(new FileReader(fileB));

		MergerConfiguration config = new MergerConfiguration();
		XMLStreamReader merger = WiseMLMergerFactory.createMergingWiseMLStreamReader(config, inputA, inputB);

		XMLStreamWriter output = createStandardOutputStreamWriter("UTF-8");

		new XMLPipe(merger, output).streamUntilEnd();

	}

}
