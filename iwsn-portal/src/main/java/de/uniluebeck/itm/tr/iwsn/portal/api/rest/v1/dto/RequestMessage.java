package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import de.uniluebeck.itm.tr.iwsn.messages.*;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.uniluebeck.itm.tr.iwsn.common.Base64Helper.encode;

@XmlRootElement
public class RequestMessage {


	@XmlAccessorType(XmlAccessType.FIELD)
	public static class AreNodesAlive {

		@XmlElement
		public List<String> nodeUrns;

		public AreNodesAlive(AreNodesAliveRequest request) {
			nodeUrns = request.getNodeUrnsList();
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class AreNodesConnected {

		@XmlElement
		public List<String> nodeUrns;

		public AreNodesConnected(AreNodesConnectedRequest request) {
			nodeUrns = request.getNodeUrnsList();
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class DisableNodes {

		@XmlElement
		public List<String> nodeUrns;

		public DisableNodes(DisableNodesRequest request) {
			nodeUrns = request.getNodeUrnsList();
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class EnableNodes {

		@XmlElement
		public List<String> nodeUrns;

		public EnableNodes(EnableNodesRequest request) {
			nodeUrns = request.getNodeUrnsList();
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class FlashImages {

		@XmlElement
		public List<String> nodeUrns;

		@XmlElement(required = true)
		public String imageBase64;

		public FlashImages(FlashImagesRequest request) {
			nodeUrns = request.getNodeUrnsList();
			imageBase64 = "data:application/octet-stream;base64," + encode(request.getImage().toByteArray());
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class ResetNodes {

		@XmlElement
		public List<String> nodeUrns;

		public ResetNodes(ResetNodesRequest request) {
			nodeUrns = request.getNodeUrnsList();
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Link {

		@XmlElement(required = true)
		public String sourceNodeUrn;

		@XmlElement(required = true)
		public String targetNodeUrn;

		public Link(de.uniluebeck.itm.tr.iwsn.messages.Link message) {
			sourceNodeUrn = message.getSourceNodeUrn();
			targetNodeUrn = message.getTargetNodeUrn();
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class EnableVirtualLinks {

		@XmlElement
		public List<Link> links;

		public EnableVirtualLinks(EnableVirtualLinksRequest request) {
			links = new ArrayList<Link>();
			for (de.uniluebeck.itm.tr.iwsn.messages.Link link : request.getLinksList()) {
				links.add(new Link(link));
			}
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class DisableVirtualLinks {

		@XmlElement
		public List<Link> links;

		public DisableVirtualLinks(DisableVirtualLinksRequest request) {
			links = new ArrayList<Link>();
			for (de.uniluebeck.itm.tr.iwsn.messages.Link link : request.getLinksList()) {
				links.add(new Link(link));
			}
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class DisablePhysicalLinks {

		@XmlElement
		public List<Link> links;

		public DisablePhysicalLinks(DisablePhysicalLinksRequest request) {
			links = new ArrayList<Link>();
			for (de.uniluebeck.itm.tr.iwsn.messages.Link link : request.getLinksList()) {
				links.add(new Link(link));
			}
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class EnablePhysicalLinks {

		@XmlElement
		public List<Link> links;

		public EnablePhysicalLinks(EnablePhysicalLinksRequest request) {
			links = new ArrayList<Link>();
			for (de.uniluebeck.itm.tr.iwsn.messages.Link link : request.getLinksList()) {
				links.add(new Link(link));
			}
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class SendDownstream {

		@XmlElement
		public List<String> targetNodeUrns;

		@XmlElement(required = true)
		public String messageBytesBase64;

		public SendDownstream(SendDownstreamMessagesRequest request) {
			targetNodeUrns = request.getTargetNodeUrnsList();
			messageBytesBase64 = encode(request.getMessageBytes().toByteArray());
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class GetChannelPipelines {

		@XmlElement
		public List<String> nodeUrns;

		public GetChannelPipelines(GetChannelPipelinesRequest request) {
			nodeUrns = request.getNodeUrnsList();
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class ChannelHandlerConfiguration {

		@XmlElement
		Map<String, String> configuration;

		@XmlElement(required = true)
		public String name;

		public ChannelHandlerConfiguration(de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration message) {
			name = message.getName();
			configuration = new HashMap<String, String>();
			for (de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration.KeyValuePair pair : message
					.getConfigurationList()) {
				configuration.put(pair.getKey(), pair.getValue());
			}
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class SetChannelPipelines {

		@XmlElement
		List<String> nodeUrns;

		@XmlElement
		List<ChannelHandlerConfiguration> channelHandlerConfigurations;

		public SetChannelPipelines(SetChannelPipelinesRequest request) {
			nodeUrns = request.getNodeUrnsList();
			channelHandlerConfigurations = new ArrayList<ChannelHandlerConfiguration>();
			for (de.uniluebeck.itm.tr.iwsn.messages.ChannelHandlerConfiguration config : request
					.getChannelHandlerConfigurationsList()) {
				channelHandlerConfigurations.add(new ChannelHandlerConfiguration(config));
			}
		}
	}

	@XmlElement(required = false, nillable = true)
	public AreNodesAlive areNodesAliveRequest;

	@XmlElement(required = false, nillable = true)
	public AreNodesConnected areNodesConnectedRequest;

	@XmlElement(required = false, nillable = true)
	public DisableNodes disableNodesRequest;

	@XmlElement(required = false, nillable = true)
	public DisableVirtualLinks disableVirtualLinksRequest;

	@XmlElement(required = false, nillable = true)
	public DisablePhysicalLinks disablePhysicalLinksRequest;

	@XmlElement(required = false, nillable = true)
	public EnableNodes enableNodesRequest;

	@XmlElement(required = false, nillable = true)
	public EnablePhysicalLinks enablePhysicalLinksRequest;

	@XmlElement(required = false, nillable = true)
	public EnableVirtualLinks enableVirtualLinksRequest;

	@XmlElement(required = false, nillable = true)
	public FlashImages flashImagesRequest;

	@XmlElement(required = false, nillable = true)
	public GetChannelPipelines getChannelPipelinesRequest;

	@XmlElement(required = false, nillable = true)
	public ResetNodes resetNodesRequest;

	@XmlElement(required = false, nillable = true)
	public SendDownstream sendDownstreamMessagesRequest;

	@XmlElement(required = false, nillable = true)
	public SetChannelPipelines setChannelPipelinesRequest;

	@XmlElement(required = true)
	public long requestId;

	@XmlElement(required = true)
	public String type;

	public RequestMessage(Request request) throws IllegalArgumentException {
		requestId = request.getRequestId();
		switch (request.getType()) {
			case ARE_NODES_ALIVE:
				areNodesAliveRequest = new AreNodesAlive(request.getAreNodesAliveRequest());
				type = "areNodesAliveRequest";
				break;
			case ARE_NODES_CONNECTED:
				areNodesConnectedRequest = new AreNodesConnected(request.getAreNodesConnectedRequest());
				type = "areNodesConnectedRequest";
				break;
			case DISABLE_NODES:
				disableNodesRequest = new DisableNodes(request.getDisableNodesRequest());
				type = "disableNodesRequest";
				break;
			case DISABLE_VIRTUAL_LINKS:
				disableVirtualLinksRequest = new DisableVirtualLinks(request.getDisableVirtualLinksRequest());
				type = "disableVirtualLinksRequest";
				break;
			case DISABLE_PHYSICAL_LINKS:
				disablePhysicalLinksRequest = new DisablePhysicalLinks(request.getDisablePhysicalLinksRequest());
				type = "disablePhysicalLinksRequest";
				break;
			case ENABLE_NODES:
				enableNodesRequest = new EnableNodes(request.getEnableNodesRequest());
				type = "enableNodesRequest";
				break;
			case ENABLE_VIRTUAL_LINKS:
				enableVirtualLinksRequest = new EnableVirtualLinks(request.getEnableVirtualLinksRequest());
				type = "enableVirtualLinksRequest";
				break;
			case ENABLE_PHYSICAL_LINKS:
				enablePhysicalLinksRequest = new EnablePhysicalLinks(request.getEnablePhysicalLinksRequest());
				type = "enablePhysicalLinksRequest";
				break;
			case FLASH_IMAGES:
				flashImagesRequest = new FlashImages(request.getFlashImagesRequest());
				type = "flashImagesRequest";
				break;
			case GET_CHANNEL_PIPELINES:
				getChannelPipelinesRequest = new GetChannelPipelines(request.getGetChannelPipelinesRequest());
				type = "getChannelPipelinesRequest";
				break;
			case RESET_NODES:
				resetNodesRequest = new ResetNodes(request.getResetNodesRequest());
				type = "resetNodesRequest";
				break;
			case SEND_DOWNSTREAM_MESSAGES:
				sendDownstreamMessagesRequest = new SendDownstream(request.getSendDownstreamMessagesRequest());
				type = "sendDownstreamMessagesRequest";
				break;
			case SET_CHANNEL_PIPELINES:
				setChannelPipelinesRequest = new SetChannelPipelines(request.getSetChannelPipelinesRequest());
				type = "setChannelPipelinesRequest";
				break;
			default:
				throw new IllegalArgumentException("Unsupported request type can't be converted to XML message");
		}
	}

}
