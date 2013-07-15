package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
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
}
