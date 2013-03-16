package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.devicedb.DeviceDB;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.portal.RequestIdProvider;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.util.NetworkUtils;
import de.uniluebeck.itm.tr.util.SettableFutureMap;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.wsn.*;
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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Maps.newHashMap;
import static de.uniluebeck.itm.tr.devicedb.WiseMLConverter.convertToWiseML;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.*;
import static de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3.Converters.*;
import static eu.wisebed.wiseml.WiseMLHelper.serialize;

@WebService(name = "WSN", targetNamespace = "http://wisebed.eu/api/v3/wsn")
public class WSNServiceImpl extends AbstractService implements WSNService {

	private static final Logger log = LoggerFactory.getLogger(WSNServiceImpl.class);

	private final DeviceDB deviceDB;

	private final Reservation reservation;

	private final DeliveryManager deliveryManager;

	private final RequestIdProvider requestIdProvider;

	private final String reservationId;

	private ServicePublisherService jaxWsService;

	@Inject
	public WSNServiceImpl(final ServicePublisher servicePublisher,
						  final DeviceDB deviceDB,
						  final RequestIdProvider requestIdProvider,
						  @Assisted final String reservationId,
						  @Assisted final Reservation reservation,
						  @Assisted final DeliveryManager deliveryManager) {
		this.reservationId = reservationId;
		this.deviceDB = checkNotNull(deviceDB);
		this.requestIdProvider = checkNotNull(requestIdProvider);
		this.reservation = checkNotNull(reservation);
		this.deliveryManager = checkNotNull(deliveryManager);
		this.jaxWsService = servicePublisher.createJaxWsService("/soap/v3/wsn/" + reservationId, this);
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
							  final String controllerEndpointUrl) {

		log.debug("WSNServiceImpl.addController({})", controllerEndpointUrl);

		if (!"NONE".equals(controllerEndpointUrl)) {
			NetworkUtils.checkConnectivity(controllerEndpointUrl);
		}

		deliveryManager.addController(controllerEndpointUrl);

		if (reservation.getInterval().containsNow()) {
			deliveryManager.reservationStarted(reservation.getInterval().getStart(), controllerEndpointUrl);
		}

		if (reservation.getInterval().isBeforeNow()) {
			deliveryManager.reservationEnded(reservation.getInterval().getEnd(), controllerEndpointUrl);
		}
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
							  @WebParam(name = "nodeUrns", targetNamespace = "") List<NodeUrn> nodeUrns)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(newAreNodesAliveRequest(reservationId, requestId, nodeUrns));
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
									@WebParam(name = "links", targetNamespace = "") List<Link> links)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(
				newDisableVirtualLinksRequest(reservationId, requestId, convertLinksToMap(links))
		);
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
							 @WebParam(name = "nodeUrns", targetNamespace = "") List<NodeUrn> nodeUrns)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(newDisableNodesRequest(reservationId, requestId, nodeUrns));
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
									 @WebParam(name = "links", targetNamespace = "") List<Link> links)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(
				newDisablePhysicalLinksRequest(reservationId, requestId, convertLinksToMap(links))
		);
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
	public void disableVirtualization()
			throws VirtualizationNotSupportedFault_Exception, ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		reservation.disableVirtualization();
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
	public void enableVirtualization() throws VirtualizationNotSupportedFault_Exception,
			ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		reservation.enableVirtualization();
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
							@WebParam(name = "nodeUrns", targetNamespace = "") List<NodeUrn> nodeUrns)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(newEnableNodesRequest(reservationId, requestId, nodeUrns));
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
									@WebParam(name = "links", targetNamespace = "") List<Link> links)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(
				newEnablePhysicalLinksRequest(reservationId, requestId, convertLinksToMap(links))
		);
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
							  List<FlashProgramsConfiguration> configurations)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		for (FlashProgramsConfiguration configuration : configurations) {
			reservation.getReservationEventBus().post(newFlashImagesRequest(
					reservationId,
					requestId,
					configuration.getNodeUrns(),
					configuration.getProgram()
			)
			);
		}
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

		assertReservationIntervalMet();

		final long requestId = requestIdProvider.get();
		final Request request = newGetChannelPipelinesRequest(
				reservationId,
				requestId,
				nodeUrns
		);

		final Map<NodeUrn, SettableFuture<GetChannelPipelinesResponse.GetChannelPipelineResponse>> map = newHashMap();
		for (NodeUrn nodeUrn : nodeUrns) {
			map.put(nodeUrn, SettableFuture.<GetChannelPipelinesResponse.GetChannelPipelineResponse>create());
		}
		final SettableFutureMap<NodeUrn, GetChannelPipelinesResponse.GetChannelPipelineResponse> future =
				new SettableFutureMap<NodeUrn, GetChannelPipelinesResponse.GetChannelPipelineResponse>(map);

		final Object eventBusListener = new Object() {
			@Subscribe
			public void onResponse(GetChannelPipelinesResponse response) {
				if (response.getRequestId() == requestId) {
					for (GetChannelPipelinesResponse.GetChannelPipelineResponse p : response.getPipelinesList()) {

						final SettableFuture<GetChannelPipelinesResponse.GetChannelPipelineResponse> nodeFuture =
								map.get(new NodeUrn(p.getNodeUrn()));
						nodeFuture.set(p);
					}
				}
			}
		};

		reservation.getReservationEventBus().register(eventBusListener);
		reservation.getReservationEventBus().post(request);

		try {

			final Map<NodeUrn, GetChannelPipelinesResponse.GetChannelPipelineResponse> resultMap =
					future.get(30, TimeUnit.SECONDS);
			reservation.getReservationEventBus().unregister(eventBusListener);
			return convert(resultMap);

		} catch (Exception e) {
			throw propagate(e);
		}
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
		return serialize(convertToWiseML(deviceDB.getConfigsByNodeUrns(reservation.getNodeUrns()).values()));
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
			@WebParam(name = "controllerEndpointUrl", targetNamespace = "") String controllerEndpointUrl) {
		log.debug("WSNServiceImpl.removeController({})", controllerEndpointUrl);
		deliveryManager.removeController(controllerEndpointUrl);
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
						   @WebParam(name = "nodeUrns", targetNamespace = "") List<NodeUrn> nodeUrns)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(newResetNodesRequest(reservationId, requestId, nodeUrns));
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
					 @WebParam(name = "message", targetNamespace = "") byte[] message)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(
				newSendDownstreamMessageRequest(reservationId, requestId, nodeUrns, message)
		);
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
								   List<ChannelHandlerConfiguration> channelHandlerConfigurations)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(newSetChannelPipelinesRequest(
				reservationId,
				requestId,
				nodeUrns,
				convertCHCs(channelHandlerConfigurations)
		)
		);
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
										SerialPortParameters parameters) throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		throw new RuntimeException("TODO implement");
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
								   @WebParam(name = "links", targetNamespace = "") List<VirtualLink> links)
			throws ReservationNotRunningFault_Exception {
		assertReservationIntervalMet();
		reservation.getReservationEventBus().post(
				newEnableVirtualLinksRequest(reservationId, requestId, convertVirtualLinks(links))
		);
		// TODO remember virtual link mapping in specialized class that also delivers vlink messages to remote instance
		throw new RuntimeException("TODO only partially implemented");
	}

	@Override
	public URI getURI() {
		return jaxWsService.getURI();
	}

	private void assertReservationIntervalMet() throws ReservationNotRunningFault_Exception {
		if (!reservation.getInterval().containsNow()) {
			ReservationNotRunningFault fault = new ReservationNotRunningFault();
			final String message = reservation.getInterval().isBeforeNow() ?
					"Reservation interval is over" :
					"Reservation interval lies in the future";
			fault.setMessage(message);
			throw new RuntimeException(new ReservationNotRunningFault_Exception(message, fault));
		}
	}
}
