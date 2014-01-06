package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import eu.wisebed.api.v3.wsn.ChannelHandlerConfiguration;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class ChannelHandlerConfigurationList {

	public List<String> nodeUrns;

	public List<ChannelHandlerConfiguration> handlers;

}
