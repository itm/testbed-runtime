package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

@XmlRootElement
public class TestbedMap {

	public static class Testbed {

		public String name;

		public String[] urnPrefixes;

		public String sessionManagementEndpointUrl;
	}

	public Map<String, Testbed> testbedMap;

}
