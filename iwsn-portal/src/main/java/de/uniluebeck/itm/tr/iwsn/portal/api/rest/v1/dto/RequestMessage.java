package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import de.uniluebeck.itm.tr.iwsn.messages.*;
import org.joda.time.DateTime;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.uniluebeck.itm.tr.common.Base64Helper.encode;

@XmlRootElement
public class RequestMessage {


	@XmlAccessorType(XmlAccessType.FIELD)
	public static class AreNodesAlive {

		@XmlElement
		public List<String> nodeUrns;

		public AreNodesAlive(AreNodesAliveRequest request) {
			nodeUrns = request.getHeader().getNodeUrnsList();
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class AreNodesConnected {

		@XmlElement
		public List<String> nodeUrns;

		public AreNodesConnected(AreNodesConnectedRequest request) {
			nodeUrns = request.getHeader().getNodeUrnsList();
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class DisableNodes {

		@XmlElement
		public List<String> nodeUrns;

		public DisableNodes(DisableNodesRequest request) {
			nodeUrns = request.getHeader().getNodeUrnsList();
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class EnableNodes {

		@XmlElement
		public List<String> nodeUrns;

		public EnableNodes(EnableNodesRequest request) {
			nodeUrns = request.getHeader().getNodeUrnsList();
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class FlashImages {

		@XmlElement
		public List<String> nodeUrns;

		@XmlElement(required = true)
		public String imageBase64;

		public FlashImages(FlashImagesRequest request) {
			nodeUrns = request.getHeader().getNodeUrnsList();
			imageBase64 = "data:application/octet-stream;base64," + encode(request.getImage().toByteArray());
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class ResetNodes {

		@XmlElement
		public List<String> nodeUrns;

		public ResetNodes(ResetNodesRequest request) {
			nodeUrns = request.getHeader().getNodeUrnsList();
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
		public List<String> nodeUrns;

		@XmlElement(required = true)
		public String messageBytesBase64;

		public SendDownstream(SendDownstreamMessagesRequest request) {
			nodeUrns = request.getHeader().getTargetNodeUrnsList();
			messageBytesBase64 = encode(request.getMessageBytes().toByteArray());
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class GetChannelPipelines {

		@XmlElement
		public List<String> nodeUrns;

		public GetChannelPipelines(GetChannelPipelinesRequest request) {
			nodeUrns = request.getHeader().getNodeUrnsList();
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
			nodeUrns = request.getHeader().getNodeUrnsList();
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

	@XmlElement(required = true)
	public DateTime timestamp;

	public RequestMessage(final MessageHeaderPair pair) throws IllegalArgumentException {

		this.requestId = pair.header.getCorrelationId();
		this.timestamp = new DateTime(pair.header.getTimestamp());

		switch (pair.header.getType()) {
			case KEEP_ALIVE:
				break;
			case KEEP_ALIVE_ACK:
				break;
			case REQUEST_ARE_NODES_ALIVE:
				areNodesAliveRequest = new AreNodesAlive((AreNodesAliveRequest) pair.message);
				type = "areNodesAliveRequest";
				break;
			case REQUEST_ARE_NODES_CONNECTED:
				areNodesConnectedRequest = new AreNodesConnected((AreNodesConnectedRequest) pair.message);
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
			case REQUEST_DISABLE_NODES:
			case REQUEST_DISABLE_VIRTUAL_LINKS:
			case REQUEST_DISABLE_PHYSICAL_LINKS:
			case REQUEST_ENABLE_NODES:
			case REQUEST_ENABLE_PHYSICAL_LINKS:
			case REQUEST_ENABLE_VIRTUAL_LINKS:
			case REQUEST_FLASH_IMAGES:
			case REQUEST_GET_CHANNEL_PIPELINES:
			case REQUEST_RESET_NODES:
			case REQUEST_SEND_DOWNSTREAM_MESSAGES:
			case REQUEST_SET_CHANNEL_PIPELINES:
			case PROGRESS:
			case RESPONSE:
			case RESPONSE_GET_CHANNELPIPELINES:
			case EVENT_UPSTREAM_MESSAGE:
			case EVENT_DEVICES_ATTACHED:
			case EVENT_DEVICES_DETACHED:
			case EVENT_GATEWAY_CONNECTED:
			case EVENT_GATEWAY_DISCONNECTED:
			case EVENT_NOTIFICATION:
			case EVENT_RESERVATION_STARTED:
			case EVENT_RESERVATION_ENDED:
			case EVENT_RESERVATION_MADE:
			case EVENT_RESERVATION_CANCELLED:
			case EVENT_RESERVATION_OPENED:
			case EVENT_RESERVATION_CLOSED:
			case EVENT_RESERVATION_FINALIZED:
			case EVENT_DEVICE_CONFIG_CREATED:
			case EVENT_DEVICE_CONFIG_UPDATED:
			case EVENT_DEVICE_CONFIG_DELETED:
			case EVENT_ACK:
			default:
				throw new IllegalArgumentException("Unsupported request type can't be converted to XML message");
		}
	}

}
