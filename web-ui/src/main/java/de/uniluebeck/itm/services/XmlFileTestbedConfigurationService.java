package de.uniluebeck.itm.services;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.XStream;

import de.uniluebeck.itm.model.TestbedConfiguration;

public class XmlFileTestbedConfigurationService implements TestbedConfigurationService {

	private XStream xstream;
	
	private Reader reader;
	
	public XmlFileTestbedConfigurationService() {
		this("src/main/webapp/testbed-configurations.xml");
	}
	
	public XmlFileTestbedConfigurationService(String path) {
		File file = new File(path);
		try {
			reader = new FileReader(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		xstream = new XStream();
		xstream.processAnnotations(TestbedConfiguration.class);
	}
	
	public List<TestbedConfiguration> getConfigurations() {
		List<TestbedConfiguration> result = new ArrayList<TestbedConfiguration>();
		ObjectInputStream in;
		try {
			in = xstream.createObjectInputStream(reader);
			boolean objects = true;
			while (objects) {
				try {
					TestbedConfiguration bed = (TestbedConfiguration) in.readObject();
					result.add(bed);
				} catch (EOFException e) {
					objects = false;
				}
			}
			in.close();
			return result;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

}
