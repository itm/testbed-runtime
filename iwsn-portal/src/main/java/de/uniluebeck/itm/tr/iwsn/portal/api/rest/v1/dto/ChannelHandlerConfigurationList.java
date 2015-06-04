package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse;
import de.uniluebeck.itm.tr.iwsn.messages.MessageHeaderPair;
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.wsn.ChannelHandlerConfiguration;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;

@XmlRootElement
public class ChannelHandlerConfigurationList {

	public List<String> nodeUrns;

	public List<ChannelHandlerConfiguration> handlers;

	@SuppressWarnings("unused")
	public ChannelHandlerConfigurationList() {
		// reflection constructor
	}

	public ChannelHandlerConfigurationList(GetChannelPipelinesResponse.GetChannelPipelineResponse pipelineResponse) {
		this.nodeUrns = newArrayList(pipelineResponse.getNodeUrn());
		this.handlers = pipelineResponse.getHandlerConfigurationsList().stream().map(handler -> {
			ChannelHandlerConfiguration chc = new ChannelHandlerConfiguration();
			chc.setName(handler.getName());
			chc.getConfiguration().addAll(handler.getConfigurationList().stream().map(pair -> {
				KeyValuePair kv = new KeyValuePair();
				kv.setKey(pair.getKey());
				kv.setValue(pair.getValue());
				return kv;
			}).collect(Collectors.toList()));
			return chc;
		}).collect(Collectors.toList());
	}
}
