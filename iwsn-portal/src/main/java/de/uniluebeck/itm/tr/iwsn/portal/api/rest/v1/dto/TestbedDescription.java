package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TestbedDescription {

	public String name;

	public String[] urnPrefixes;

	public String testbedBaseUri;

	public String sessionManagementEndpointUrl;
}