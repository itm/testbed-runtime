package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class FlashProgramsRequest {

	@XmlElement(name = "configurations")
	public List<FlashTask> configurations;

	public static class FlashTask {

		@XmlElement(name = "nodeUrns")
		public List<String> nodeUrns;

		@XmlElement(name = "image")
		public String image;
	}

}
