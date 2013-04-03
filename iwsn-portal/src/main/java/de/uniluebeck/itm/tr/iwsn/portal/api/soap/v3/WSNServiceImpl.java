package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.wsn.*;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;
import java.net.URI;
import java.util.List;


@WebService(name = "WSN", targetNamespace = "http://wisebed.eu/api/v3/wsn")
public class WSNServiceImpl extends AbstractService implements WSNService {

	private static final Logger log = LoggerFactory.getLogger(WSNServiceImpl.class);

	private WSN wsn;

	private ServicePublisherService jaxWsService;

	@Inject
	public WSNServiceImpl(final ServicePublisher servicePublisher,
						  @Assisted final String reservationId,
						  @Assisted final WSN wsn) {
		this.jaxWsService = servicePublisher.createJaxWsService("/v3/wsn/" + reservationId, this);
		this.wsn = wsn;
	}

	@Override
	protected void doStart() {
		try {
			jaxWsService.startAndWait();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			jaxWsService.stopAndWait();
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	@WebMethod
	@RequestWrapper(
			localName = "addController",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.AddController"
	)
	@ResponseWrapper(
			localName = "addControllerResponse",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.AddControllerResponse"
	)
	public void addController(@WebParam(name = "controllerEndpointUrl", targetNamespace = "")
							  final String controllerEndpointUrl,
							  @WebParam(name = "timestamp", targetNamespace = "") DateTime timestamp) {

		log.debug("WSNImpl.addController({})", controllerEndpointUrl);

		wsn.addController(controllerEndpointUrl, timestamp);
	}

	@Override
	@WebMethod
	@RequestWrapper(
			localName = "areNodesAlive",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.AreNodesAlive"
	)
	@ResponseWrapper(
			localName = "areNodesAliveResponse",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.AreNodesAliveResponse"
	)
	public void areNodesAlive(@WebParam(name = "requestId", targetNamespace = "") long requestId,
							  @WebParam(name = "nodeUrns", targetNamespace = "") List<NodeUrn> nodeUrns,
							  @WebParam(name = "timestamp", targetNamespace = "") DateTime timestamp)
			throws ReservationNotRunningFault_Exception {
		wsn.areNodesAlive(requestId, nodeUrns, timestamp);
	}

	@Override
	@WebMethod
	@RequestWrapper(
			localName = "destroyVirtualLinks",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.DestroyVirtualLinks"
	)
	@ResponseWrapper(
			localName = "destroyVirtualLinksResponse",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.DestroyVirtualLinksResponse"
	)
	public void disableVirtualLinks(@WebParam(name = "requestId", targetNamespace = "") long requestId,
									@WebParam(name = "links", targetNamespace = "") List<Link> links,
									@WebParam(name = "timestamp", targetNamespace = "") DateTime timestamp)
			throws ReservationNotRunningFault_Exception, VirtualizationNotEnabledFault_Exception {
		wsn.disableVirtualLinks(requestId, links, timestamp);
	}

	@Override
	@WebMethod
	@RequestWrapper(
			localName = "disableNodes",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.DisableNodes"
	)
	@ResponseWrapper(
			localName = "disableNodesResponse",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.DisableNodesResponse"
	)
	public void disableNodes(@WebParam(name = "requestId", targetNamespace = "") long requestId,
							 @WebParam(name = "nodeUrns", targetNamespace = "") List<NodeUrn> nodeUrns,
							 @WebParam(name = "timestamp", targetNamespace = "") DateTime timestamp)
			throws ReservationNotRunningFault_Exception, VirtualizationNotEnabledFault_Exception {
		wsn.disableNodes(requestId, nodeUrns, timestamp);
	}

	@Override
	@WebMethod
	@RequestWrapper(
			localName = "disablePhysicalLinks",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.DisablePhysicalLinks"
	)
	@ResponseWrapper(
			localName = "disablePhysicalLinksResponse",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.DisablePhysicalLinksResponse"
	)
	public void disablePhysicalLinks(@WebParam(name = "requestId", targetNamespace = "") long requestId,
									 @WebParam(name = "links", targetNamespace = "") List<Link> links,
									 @WebParam(name = "timestamp", targetNamespace = "") DateTime timestamp)
			throws ReservationNotRunningFault_Exception, VirtualizationNotEnabledFault_Exception {
		wsn.disablePhysicalLinks(requestId, links, timestamp);
	}

	@Override
	@WebMethod
	@RequestWrapper(
			localName = "disableVirtualization",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.DisableVirtualization"
	)
	@ResponseWrapper(
			localName = "disableVirtualizationResponse",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.DisableVirtualizationResponse"
	)
	public void disableVirtualization(@WebParam(name = "timestamp", targetNamespace = "") DateTime timestamp)
			throws VirtualizationNotSupportedFault_Exception, ReservationNotRunningFault_Exception {
		wsn.disableVirtualization(timestamp);
	}

	@Override
	@WebMethod
	@RequestWrapper(
			localName = "enableVirtualization",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.EnableVirtualization"
	)
	@ResponseWrapper(
			localName = "enableVirtualizationResponse",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.EnableVirtualizationResponse"
	)
	public void enableVirtualization(@WebParam(name = "timestamp", targetNamespace = "") DateTime timestamp)
			throws VirtualizationNotSupportedFault_Exception,
			ReservationNotRunningFault_Exception {
		wsn.enableVirtualization(timestamp);
	}

	@Override
	@WebMethod
	@RequestWrapper(
			localName = "enableNodes",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.EnableNodes"
	)
	@ResponseWrapper(
			localName = "enableNodesResponse",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.EnableNodesResponse"
	)
	public void enableNodes(@WebParam(name = "requestId", targetNamespace = "") long requestId,
							@WebParam(name = "nodeUrns", targetNamespace = "") List<NodeUrn> nodeUrns,
							@WebParam(name = "timestamp", targetNamespace = "") DateTime timestamp)
			throws ReservationNotRunningFault_Exception, VirtualizationNotEnabledFault_Exception {
		wsn.enableNodes(requestId, nodeUrns, timestamp);
	}

	@Override
	@WebMethod
	@RequestWrapper(
			localName = "enablePhysicalLinks",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.EnablePhysicalLinks"
	)
	@ResponseWrapper(
			localName = "enablePhysicalLinksResponse",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.EnablePhysicalLinksResponse"
	)
	public void enablePhysicalLinks(@WebParam(name = "requestId", targetNamespace = "") long requestId,
									@WebParam(name = "links", targetNamespace = "") List<Link> links,
									@WebParam(name = "timestamp", targetNamespace = "") DateTime timestamp)
			throws ReservationNotRunningFault_Exception, VirtualizationNotEnabledFault_Exception {
		wsn.enablePhysicalLinks(requestId, links, timestamp);
	}

	@Override
	@WebMethod
	@RequestWrapper(
			localName = "flashPrograms",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.FlashPrograms"
	)
	@ResponseWrapper(
			localName = "flashProgramsResponse",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.FlashProgramsResponse"
	)
	public void flashPrograms(@WebParam(name = "requestId", targetNamespace = "") long requestId,
							  @WebParam(name = "configurations", targetNamespace = "")
							  List<FlashProgramsConfiguration> configurations,
							  @WebParam(name = "timestamp", targetNamespace = "") DateTime timestamp)
			throws ReservationNotRunningFault_Exception {
		wsn.flashPrograms(requestId, configurations, timestamp);
	}

	@Override
	@WebMethod
	@WebResult(targetNamespace = "")
	@RequestWrapper(
			localName = "getChannelPipelines",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.GetChannelPipelines"
	)
	@ResponseWrapper(
			localName = "getChannelPipelinesResponse",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.GetChannelPipelinesResponse"
	)
	public List<ChannelPipelinesMap> getChannelPipelines(
			@WebParam(name = "nodeUrns", targetNamespace = "") List<NodeUrn> nodeUrns)
			throws ReservationNotRunningFault_Exception {
		return wsn.getChannelPipelines(nodeUrns);
	}

	@Override
	@WebMethod
	@WebResult(targetNamespace = "")
	@RequestWrapper(
			localName = "getNetwork",
			targetNamespace = "http://wisebed.eu/api/v3/common",
			className = "eu.wisebed.api.v3.common.GetNetwork"
	)
	@ResponseWrapper(
			localName = "getNetworkResponse",
			targetNamespace = "http://wisebed.eu/api/v3/common",
			className = "eu.wisebed.api.v3.common.GetNetworkResponse"
	)
	public String getNetwork() {
		return wsn.getNetwork();
	}

	@Override
	@WebMethod
	@RequestWrapper(
			localName = "removeController",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.RemoveController"
	)
	@ResponseWrapper(
			localName = "removeControllerResponse",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.RemoveControllerResponse"
	)
	public void removeController(
			@WebParam(name = "controllerEndpointUrl", targetNamespace = "") String controllerEndpointUrl,
			@WebParam(name = "timestamp", targetNamespace = "") DateTime timestamp) {
		log.debug("WSNImpl.removeController({})", controllerEndpointUrl);
		wsn.removeController(controllerEndpointUrl, timestamp);
	}

	@Override
	@WebMethod
	@RequestWrapper(
			localName = "resetNodes",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.ResetNodes"
	)
	@ResponseWrapper(
			localName = "resetNodesResponse",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.ResetNodesResponse"
	)
	public void resetNodes(@WebParam(name = "requestId", targetNamespace = "") long requestId,
						   @WebParam(name = "nodeUrns", targetNamespace = "") List<NodeUrn> nodeUrns,
						   @WebParam(name = "timestamp", targetNamespace = "") DateTime timestamp)
			throws ReservationNotRunningFault_Exception {
		wsn.resetNodes(requestId, nodeUrns, timestamp);
	}

	@Override
	@WebMethod
	@RequestWrapper(
			localName = "send",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.Send"
	)
	@ResponseWrapper(
			localName = "sendResponse",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.SendResponse"
	)
	public void send(@WebParam(name = "requestId", targetNamespace = "") long requestId,
					 @WebParam(name = "nodeUrns", targetNamespace = "") List<NodeUrn> nodeUrns,
					 @WebParam(name = "message", targetNamespace = "") byte[] message,
					 @WebParam(name = "timestamp", targetNamespace = "") DateTime timestamp)
			throws ReservationNotRunningFault_Exception {
		wsn.send(requestId, nodeUrns, message, timestamp);
	}

	@Override
	@WebMethod
	@RequestWrapper(
			localName = "setChannelPipeline",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.SetChannelPipeline"
	)
	@ResponseWrapper(
			localName = "setChannelPipelineResponse",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.SetChannelPipelineResponse"
	)
	public void setChannelPipeline(@WebParam(name = "requestId", targetNamespace = "") long requestId,
								   @WebParam(name = "nodeUrns", targetNamespace = "") List<NodeUrn> nodeUrns,
								   @WebParam(name = "channelHandlerConfigurations", targetNamespace = "")
								   List<ChannelHandlerConfiguration> channelHandlerConfigurations,
								   @WebParam(name = "timestamp", targetNamespace = "") DateTime timestamp)
			throws ReservationNotRunningFault_Exception {
		wsn.setChannelPipeline(requestId, nodeUrns, channelHandlerConfigurations, timestamp);
	}

	@Override
	@WebMethod
	@RequestWrapper(
			localName = "setSerialPortParameters",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.SetSerialPortParameters"
	)
	@ResponseWrapper(
			localName = "setSerialPortParametersResponse",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.SetSerialPortParametersResponse"
	)
	public void setSerialPortParameters(@WebParam(name = "nodeUrns", targetNamespace = "") List<NodeUrn> nodeUrns,
										@WebParam(name = "parameters", targetNamespace = "")
										SerialPortParameters parameters,
										@WebParam(name = "timestamp", targetNamespace = "") DateTime timestamp)
			throws ReservationNotRunningFault_Exception {
		wsn.setSerialPortParameters(nodeUrns, parameters, timestamp);
	}

	@Override
	@WebMethod
	@RequestWrapper(
			localName = "setVirtualLinks",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.SetVirtualLinks"
	)
	@ResponseWrapper(
			localName = "setVirtualLinksResponse",
			targetNamespace = "http://wisebed.eu/api/v3/wsn",
			className = "eu.wisebed.api.v3.wsn.SetVirtualLinksResponse"
	)
	public void enableVirtualLinks(@WebParam(name = "requestId", targetNamespace = "") long requestId,
								   @WebParam(name = "links", targetNamespace = "") List<VirtualLink> links,
								   @WebParam(name = "timestamp", targetNamespace = "") DateTime timestamp)
			throws ReservationNotRunningFault_Exception, VirtualizationNotEnabledFault_Exception {
		wsn.enableVirtualLinks(requestId, links, timestamp);
	}

	@Override
	public URI getURI() {
		return jaxWsService.getURI();
	}

}
