package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import eu.wisebed.restws.util.JSONHelper;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
public class RemoteExperimentConfiguration {

	public static class RemoteExperimentConfigurationEntry {

		@XmlElement(name = "nodeUrnsJsonFileUrl")
		public String nodeUrnsJsonFileUrl;

		@XmlElement(name = "binaryProgramUrl")
		public String binaryProgramUrl;
	}

	@XmlElement(name = "configurations")
	public List<RemoteExperimentConfigurationEntry> configurations;

	public static void main(String[] args) {
		RemoteExperimentConfiguration r = new RemoteExperimentConfiguration();
		r.configurations = new ArrayList<RemoteExperimentConfigurationEntry>();
		for (int i = 0; i < 3; ++i) {
			RemoteExperimentConfigurationEntry e = new RemoteExperimentConfigurationEntry();
			e.binaryProgramUrl = "http://www.example.com/" + i;
			e.nodeUrnsJsonFileUrl = "http://www.example.com/" + i;
			r.configurations.add(e);
		}
		
		System.out.println(JSONHelper.toJSON(r));
		
		NodeUrnList l = new NodeUrnList();
		l.nodeUrns = new ArrayList<String>();
		l.nodeUrns.add("11");
		l.nodeUrns.add("11");
		System.out.println(JSONHelper.toJSON(l));
		
	}
}
