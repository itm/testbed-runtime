package de.itm.uniluebeck.tr.wiseml.merger.examples;

import java.io.File;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import de.itm.uniluebeck.tr.wiseml.merger.WiseMLMergerFactory;
import de.itm.uniluebeck.tr.wiseml.merger.XMLPipe;
import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLAttribute;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements.WiseMLMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.stream.WiseMLTreeToXMLStream;
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
			
			List<WiseMLAttribute> attributes = reader.getAttributeList();
			for (WiseMLAttribute attribute : attributes) {
				System.out.print(' ');
				System.out.print(attribute.getName());
				System.out.print("=\"");
				System.out.print(attribute.getValue());
				System.out.print('\"');
			}
			if (reader.getTag().isTextOnly()) {
				System.out.print('>');
				System.out.print(reader.getText());
			} else {
				System.out.println('>');
				while (!reader.isFinished()) {
					if (reader.nextSubElementReader()) {
						printWiseMLTree(reader.getSubElementReader());
					}
				}
				System.out.print(indentation);
			}
			System.out.print("</");
			System.out.print(reader.getTag());
			System.out.println('>');
		} else if (reader.isList()) {
			System.out.print(indentation);
			System.out.println("<!-- LIST BEGIN -->");
			while (!reader.isFinished()) {
				if (reader.nextSubElementReader()) {
					printWiseMLTree(reader.getSubElementReader());
				}
			}
			System.out.print(indentation);
			System.out.println("<!-- LIST END -->");
		} else {
			throw new IllegalStateException();
		}
	}
	
	public static void debug1(XMLStreamReader input) {
		XMLStreamToWiseMLTree xml2tree = new XMLStreamToWiseMLTree(input);
		printWiseMLTree(xml2tree);
		
	}
	
	public static void debug2(XMLStreamReader inputA, XMLStreamReader inputB) {
		printWiseMLTree(new WiseMLMerger(new WiseMLTreeReader[]{
				new XMLStreamToWiseMLTree(inputA),
				new XMLStreamToWiseMLTree(inputB),
		}, new MergerConfiguration()));
		
	}
	
	public static void debug3(XMLStreamReader input) {
		try {
			printEvent(input);
			while (input.hasNext()) {
				input.next();
				printEvent(input);
			}
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static void printEvent(XMLStreamReader reader) {
		switch (reader.getEventType()) {
		case XMLStreamConstants.ATTRIBUTE:
			System.out.println("ATTRIBUTE");
			break;
		case XMLStreamConstants.CDATA:
			System.out.println("CDATA");
			break;
		case XMLStreamConstants.CHARACTERS:
			System.out.println("CHARACTERS");
			break;
		case XMLStreamConstants.COMMENT:
			System.out.println("COMMENT");
			break;
		case XMLStreamConstants.DTD:
			System.out.println("DTD");
			break;
		case XMLStreamConstants.END_DOCUMENT:
			System.out.println("END_DOCUMENT");
			break;
		case XMLStreamConstants.END_ELEMENT:
			System.out.println("END_ELEMENT");
			break;
		case XMLStreamConstants.ENTITY_DECLARATION:
			System.out.println("ENTITY_DECLARATION");
			break;
		case XMLStreamConstants.ENTITY_REFERENCE:
			System.out.println("ENTITY_REFERENCE");
			break;
		case XMLStreamConstants.NAMESPACE:
			System.out.println("NAMESPACE");
			break;
		case XMLStreamConstants.NOTATION_DECLARATION:
			System.out.println("NOTATION_DECLARATION");
			break;
		case XMLStreamConstants.PROCESSING_INSTRUCTION:
			System.out.println("PROCESSING_INSTRUCTION");
			break;
		case XMLStreamConstants.SPACE:
			System.out.println("SPACE");
			break;
		case XMLStreamConstants.START_DOCUMENT:
			System.out.println("START_DOCUMENT");
			break;
		case XMLStreamConstants.START_ELEMENT:
			System.out.println("START_ELEMENT");
			break;
		default:
			System.out.println("unknown event type: "+reader.getEventType());
			break;
		}
	}

	public static void debug4(XMLStreamReader reader) throws Exception {
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		XMLStreamWriter output = outputFactory.createXMLStreamWriter(System.out);
		XMLPipe pipe = new XMLPipe(reader, output);
		pipe.streamUntilEnd();
	}

	public static void debug5(XMLStreamReader reader) throws Exception {
		WiseMLTreeReader tree = new XMLStreamToWiseMLTree(reader);
		XMLStreamReader input = new WiseMLTreeToXMLStream(tree, new MergerConfiguration());
		
		XMLStreamWriter output = createStandardOutputStreamWriter("UTF-8");
		XMLPipe pipe = new XMLPipe(input, output);
		pipe.streamUntilEnd();
	}

	public static void debug6(XMLStreamReader reader) throws Exception {
		while (true) {
			switch (reader.getEventType()) {
			case XMLStreamConstants.START_ELEMENT:
				printElementInfo(reader);
				for (int i = 0; i < reader.getAttributeCount(); i++) {
					printAttributeInfo(reader, i);
				}
				System.out.println();
				break;
			default:
				break;
			}
			if (reader.hasNext()) {
				reader.next();
			} else {
				break;
			}
		}
	}
	
	private static void printElementInfo(XMLStreamReader reader) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("Element")
			//.append(" at ").append(reader.getLocation())
			.append(":\n");
		sb.append('\t').append("QName: ").append(reader.getName()).append('\n');
		sb.append('\t').append("LocalName: ").append(reader.getLocalName()).append('\n');
		sb.append('\t').append("Prefix: ").append(reader.getPrefix()).append('\n');
		sb.append('\t').append("Namespace: ").append(reader.getNamespaceURI()).append('\n');
		sb.append('\t').append("# Namespaces: ").append(reader.getNamespaceCount()).append('\n');
		System.out.print(sb.toString());
	}
	
	private static void printAttributeInfo(XMLStreamReader reader, int index) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("Attribute #").append(index)
			.append(" for element ").append(reader.getLocalName())
			//.append(" at ").append(reader.getLocation())
			.append(":\n");
		sb.append('\t').append("QName: ").append(reader.getAttributeName(index)).append('\n');
		sb.append('\t').append("LocalName: ").append(reader.getAttributeLocalName(index)).append('\n');
		sb.append('\t').append("Prefix: ").append(reader.getAttributePrefix(index)).append('\n');
		sb.append('\t').append("Namespace: ").append(reader.getAttributeNamespace(index)).append('\n');
		sb.append('\t').append("Type: ").append(reader.getAttributeType(index)).append('\n');
		sb.append('\t').append("Value: ").append(reader.getAttributeValue(index)).append('\n');
		System.out.print(sb.toString());
	}
	
	private static OutputStreamWriter createStandardOutput(String encoding) throws Exception {
		return new OutputStreamWriter(System.out, encoding);
	}
	
	private static XMLStreamWriter createStandardOutputStreamWriter(String encoding) throws Exception {
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		XMLStreamWriter output = outputFactory.createXMLStreamWriter(createStandardOutput(encoding));
		return output;
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
