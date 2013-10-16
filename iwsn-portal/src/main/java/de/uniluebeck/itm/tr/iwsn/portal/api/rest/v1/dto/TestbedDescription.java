package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class TestbedDescription {

	public String name;

	public List<String> urnPrefixes;

	public String testbedBaseUri;

	public String sessionManagementEndpointUrl;

	public boolean isFederator;
}