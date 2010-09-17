package de.itm.uniluebeck.tr.wiseml.merger.examples;

import java.io.File;
import java.io.FileReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import de.itm.uniluebeck.tr.wiseml.merger.WiseMLMergerFactory;
import de.itm.uniluebeck.tr.wiseml.merger.XMLPipe;
import de.itm.uniluebeck.tr.wiseml.merger.internals.stream.XMLStreamToWiseMLTree;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public class Example1 {
	
	private static int getDepth(WiseMLTreeReader reader) {
		if (reader.getParentReader() == null) {
			return 0;
		}
		return getDepth(reader.getParentReader())+1;
	}
	
	private static String indentation(int level) {
		StringBuilder sb = new StringBuilder(level);
		while (level-- > 0) {
			sb.append('\t');
		}
		return sb.toString();
	}
	
	private static void printWiseMLTree(WiseMLTreeReader reader) {
		String indentation = indentation(getDepth(reader));
		if (reader.isMappedToTag()) {
			System.out.print(indentation);
			System.out.print('<');
			System.out.print(reader.getTag());
			System.out.println('>');
			if (reader.getTag().isTextOnly()) {
				System.out.print(indentation);
				System.out.println(reader.getText());
			}
			while (!reader.isFinished()) {
				if (reader.nextSubElementReader()) {
					printWiseMLTree(reader.getSubElementReader());
				}
			}
			System.out.print(indentation);
			System.out.print("</");
			System.out.print(reader.getTag());
			System.out.println('>');
		} else if (reader.isList()) {
			System.out.print(indentation);
			System.out.println("( LIST BEGIN )");
			while (!reader.isFinished()) {
				if (reader.nextSubElementReader()) {
					printWiseMLTree(reader.getSubElementReader());
				}
			}
			System.out.print(indentation);
			System.out.println("( LIST END )");
		} else {
			throw new IllegalStateException();
		}
	}
	
	private static void debug1(XMLStreamReader input) {
		XMLStreamToWiseMLTree xml2tree = new XMLStreamToWiseMLTree(input);
		printWiseMLTree(xml2tree);
		
		System.exit(0);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		File fileA = new File("resources/1/a.wiseml");
		File fileB = new File("resources/1/b.wiseml");
		
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLStreamReader inputA = inputFactory.createXMLStreamReader(new FileReader(fileA));
		XMLStreamReader inputB = inputFactory.createXMLStreamReader(new FileReader(fileB));
		
		debug1(inputA);
		
		XMLStreamReader merger = WiseMLMergerFactory.createMergingWiseMLStreamReader(inputA, inputB);

		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		XMLStreamWriter output = outputFactory.createXMLStreamWriter(System.out);
		
		new XMLPipe(merger, output).streamUntilEnd();
	}

}
