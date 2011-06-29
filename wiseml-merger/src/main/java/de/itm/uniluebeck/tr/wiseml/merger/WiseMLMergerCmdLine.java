package de.itm.uniluebeck.tr.wiseml.merger;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;

import javax.xml.stream.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


/**
 * Command-line utility for merging large WiseML files.
 *
 * @author kuypers
 * Date: 10.08.2010
 * Time: 14:41:59
 */
public class WiseMLMergerCmdLine {

    public static void main(String[] args) {
        // TODO: use commons-cli, logging
        List<File> files = new ArrayList<File>();
        List<FileInputStream> inputStreams = new ArrayList<FileInputStream>();
        FileOutputStream outputStream = null;
        XMLInputFactory factory = XMLInputFactory.newInstance();
        Properties properties = null;
        boolean settingsNext = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (settingsNext) {
                try {
                    FileReader reader = new FileReader(arg);
                    properties = new Properties();
                    properties.load(reader);
                    reader.close();
                } catch (IOException e) {
                    System.err.println("error loading settings: "+e.getMessage());
                    return;
                }
                settingsNext = false;
            } else {
                if (arg.equals("-s") || arg.equals("--settings")) {
                    settingsNext = true;
                } else {
                    files.add(new File(arg));
                }
            }
        }

        if (files.size() < 2) {
            System.err.println("not enough inputs");
            return;
        }

        XMLStreamReader[] readers = new XMLStreamReader[files.size()-1];
        for (int i = 0; i < files.size()-1; i++) {
            try {
                        FileInputStream fis = new FileInputStream(files.get(i));
                        inputStreams.add(fis);
                        readers[i] = factory.createXMLStreamReader(fis);
                    } catch (FileNotFoundException e) {
                        System.err.println("error opening "+files.get(i)+": "+e.getMessage());
                        return;
                    } catch (XMLStreamException e) {
                        System.err.println("could not read "+files.get(i)+": "+e.getMessage());
                        return;
                    }
        }

        XMLStreamWriter writer = null;
        try {
            outputStream = new FileOutputStream(files.get(files.size()-1));
            writer = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);
        } catch (FileNotFoundException e) {
            System.err.println("could not read "+files.get(files.size()-1)+": "+e.getMessage());
            return;
        } catch (XMLStreamException e) {
            System.err.println("could not read "+files.get(files.size()-1)+": "+e.getMessage());
            return;
        }

        if (properties == null) {
            File propertiesFile = new File("merger.properties");
            if (propertiesFile.exists()) {
                properties = new Properties();
                try {
                    FileReader reader = new FileReader(propertiesFile);
                    properties = new Properties();
                    properties.load(reader);
                    reader.close();
                } catch (IOException e) {
                    System.err.println("error loading settings: "+e.getMessage());
                    return;
                }
            }
        }

        XMLStreamReader mergedStream = WiseMLMergerFactory.createMergingWiseMLStreamReader(
                new MergerConfiguration(properties), readers);

        try {
			new XMLPipe(mergedStream, writer).streamUntilEnd();
		} catch (XMLStreamException e1) {
			System.err.println(e1.getMessage());
			e1.printStackTrace();
		}

        try {
            writer.close();
            for (FileInputStream fis : inputStreams) {
                fis.close();
            }
        } catch (XMLStreamException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }

    }

}
