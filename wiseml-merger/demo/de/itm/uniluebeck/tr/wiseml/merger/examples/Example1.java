package de.itm.uniluebeck.tr.wiseml.merger.examples;

import java.io.File;
import java.io.FileReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import de.itm.uniluebeck.tr.wiseml.merger.WiseMLMergerFactory;
import de.itm.uniluebeck.tr.wiseml.merger.XMLPipe;
import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;

public class Example1 extends Examples {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		File fileA = new File("resources/1/a.wiseml");
		//File fileA = new File("resources/2/a.xml");
 		File fileB = new File("resources/1/b.wiseml");
		
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLStreamReader inputA = inputFactory.createXMLStreamReader(new FileReader(fileA));
		XMLStreamReader inputB = inputFactory.createXMLStreamReader(new FileReader(fileB));
		
		//debug1(inputA);
		//debug1(inputB);
		//debug2(inputA, inputB);
		//debug3(inputA);
		//debug4(inputA);
		//debug5(inputA);
		//debug6(inputA);
		//System.exit(0);
		
		MergerConfiguration config = new MergerConfiguration();
		XMLStreamReader merger = WiseMLMergerFactory.createMergingWiseMLStreamReader(config, inputA, inputB);

		XMLStreamWriter output = createStandardOutputStreamWriter("UTF-8");
		
		new XMLPipe(merger, output).streamUntilEnd();
	}

}
