package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Service;
import com.google.inject.Inject;
import com.google.inject.Provider;
import de.uniluebeck.itm.nettyprotocols.HandlerFactory;
import de.uniluebeck.itm.tr.common.EndpointManager;
import de.uniluebeck.itm.tr.common.IdProvider;
import de.uniluebeck.itm.tr.common.SessionManagementPreconditions;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTracker;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerFactory;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.*;
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.sm.ChannelHandlerDescription;
import eu.wisebed.api.v3.sm.NodeConnectionStatus;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.api.v3.sm.UnknownSecretReservationKeyFault;
import eu.wisebed.api.v3.wsn.WSN;
import eu.wisebed.wiseml.Wiseml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.Holder;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static eu.wisebed.api.v3.WisebedServiceHelper.createSMUnknownSecretReservationKeyFault;
import static eu.wisebed.wiseml.WiseMLHelper.serialize;
import static java.util.Optional.empty;

@WebService(
		name = "SessionManagement",
		endpointInterface = "eu.wisebed.api.v3.sm.SessionManagement",
		portName = "SessionManagementPort",
		serviceName = "SessionManagementService",
		targetNamespace = "http://wisebed.eu/api/v3/sm"
)
public class SessionManagementImpl implements SessionManagement {

	private static final Logger log = LoggerFactory.getLogger(SessionManagementImpl.class);

	private final PortalEventBus portalEventBus;

	private final ResponseTrackerFactory responseTrackerFactory;

	private final PortalServerConfig portalServerConfig;

	private final Set<HandlerFactory> handlerFactories;

	private final ReservationManager reservationManager;

	private final Provider<SessionManagementPreconditions> preconditions;

	private final WSNServiceFactory wsnServiceFactory;

	private final DeliveryManagerFactory deliveryManagerFactory;

	private final Map<String, WSNService> wsnInstances = newHashMap();

	private final Map<String, DeliveryManager> deliveryManagers = newHashMap();

	private final IdProvider requestIdProvider;

	private final WSNFactory wsnFactory;

	private final AuthorizingWSNFactory authorizingWSNFactory;

	private final CommonConfig commonConfig;

	private final Provider<Wiseml> wisemlProvider;

	private final EndpointManager endpointManager;

	private final MessageFactory messageFactory;

	@Inject
	public SessionManagementImpl(final CommonConfig commonConfig,
								 final PortalServerConfig portalServerConfig,
								 final PortalEventBus portalEventBus,
								 final ResponseTrackerFactory responseTrackerFactory,
								 final Set<HandlerFactory> handlerFactories,
								 final ReservationManager reservationManager,
								 final WSNServiceFactory wsnServiceFactory,
								 final AuthorizingWSNFactory authorizingWSNFactory,
								 final WSNFactory wsnFactory,
								 final DeliveryManagerFactory deliveryManagerFactory,
								 final IdProvider requestIdProvider,
								 final Provider<SessionManagementPreconditions> preconditions,
								 final Provider<Wiseml> wisemlProvider,
								 final EndpointManager endpointManager,
								 final MessageFactory messageFactory) {
		this.commonConfig = checkNotNull(commonConfig);
		this.portalServerConfig = checkNotNull(portalServerConfig);
		this.portalEventBus = checkNotNull(portalEventBus);
		this.responseTrackerFactory = checkNotNull(responseTrackerFactory);
		this.handlerFactories = checkNotNull(handlerFactories);
		this.reservationManager = checkNotNull(reservationManager);
		this.wsnServiceFactory = checkNotNull(wsnServiceFactory);
		this.authorizingWSNFactory = checkNotNull(authorizingWSNFactory);
		this.wsnFactory = checkNotNull(wsnFactory);
		this.deliveryManagerFactory = checkNotNull(deliveryManagerFactory);
		this.requestIdProvider = checkNotNull(requestIdProvider);
		this.preconditions = checkNotNull(preconditions);
		this.wisemlProvider = checkNotNull(wisemlProvider);
		this.endpointManager = checkNotNull(endpointManager);
		this.messageFactory = checkNotNull(messageFactory);
	}

	@Override
	@WebMethod
	@RequestWrapper(
			localName = "areNodesConnected",
			targetNamespace = "http://wisebed.eu/api/v3/sm",
			className = "eu.wisebed.api.v3.sm.AreNodesAlive"
	)
	@ResponseWrapper(
			localName = "areNodesConnectedResponse",
			targetNamespace = "http://wisebed.eu/api/v3/sm",
			className = "eu.wisebed.api.v3.sm.AreNodesAliveResponse"
	)
	public List<NodeConnectionStatus> areNodesConnected(
			@WebParam(name = "nodeUrns", targetNamespace = "") List<NodeUrn> nodeUrns) {

		final AreNodesConnectedRequest request = messageFactory.areNodesConnectedRequest(empty(), empty(), nodeUrns);
		final ResponseTracker responseTracker = responseTrackerFactory.create(request.getHeader(), portalEventBus);

		portalEventBus.post(request);

		try {

			final Map<NodeUrn, Response> responseMap = responseTracker.get(20, TimeUnit.SECONDS);
			final List<NodeConnectionStatus> connectionStatusList = newLinkedList();

			for (Map.Entry<NodeUrn, Response> entry : responseMap.entrySet()) {
				final NodeConnectionStatus status = new NodeConnectionStatus();
				status.setNodeUrn(entry.getKey());
				status.setConnected(entry.getValue().getStatusCode() == 1);
				connectionStatusList.add(status);
			}

			return connectionStatusList;

		} catch (Exception e) {
			throw propagate(e);
		}
	}

	@Override
	@WebMethod
	@RequestWrapper(
			localName = "getConfiguration",
			targetNamespace = "http://wisebed.eu/api/v3/sm",
			className = "eu.wisebed.api.v3.sm.GetConfiguration"
	)
	@ResponseWrapper(
			localName = "getConfigurationResponse",
			targetNamespace = "http://wisebed.eu/api/v3/sm",
			className = "eu.wisebed.api.v3.sm.GetConfigurationResponse"
	)
	public void getConfiguration(
			@WebParam(name = "rsEndpointUrl", targetNamespace = "", mode = WebParam.Mode.OUT)
			Holder<String> rsEndpointUrl,
			@WebParam(name = "snaaEndpointUrl", targetNamespace = "", mode = WebParam.Mode.OUT)
			Holder<String> snaaEndpointUrl,
			@WebParam(name = "servedUrnPrefixes", targetNamespace = "", mode = WebParam.Mode.OUT)
			Holder<List<NodeUrnPrefix>> servedUrnPrefixes,
			@WebParam(name = "options", targetNamespace = "", mode = WebParam.Mode.OUT)
			Holder<List<KeyValuePair>> options) {

		rsEndpointUrl.value = endpointManager.getRsEndpointUri().toString();
		snaaEndpointUrl.value = endpointManager.getSnaaEndpointUri().toString();
		servedUrnPrefixes.value = newArrayList(commonConfig.getUrnPrefix());

		final List<KeyValuePair> optionsList = Lists.newArrayList();
		for (String key : portalServerConfig.getConfigurationOptions().keySet()) {
			for (String value : portalServerConfig.getConfigurationOptions().get(key)) {
				final KeyValuePair keyValuePair = new KeyValuePair();
				keyValuePair.setKey(key);
				keyValuePair.setValue(value);
				optionsList.add(keyValuePair);
			}
		}
		options.value = optionsList;
	}

	@Override
	@WebMethod
	@WebResult(targetNamespace = "")
	@RequestWrapper(
			localName = "getInstance",
			targetNamespace = "http://wisebed.eu/api/v3/sm",
			className = "eu.wisebed.api.v3.sm.GetInstance"
	)
	@ResponseWrapper(
			localName = "getInstanceResponse",
			targetNamespace = "http://wisebed.eu/api/v3/sm",
			className = "eu.wisebed.api.v3.sm.GetInstanceResponse"
	)
	public String getInstance(@WebParam(name = "secretReservationKey", targetNamespace = "")
							  List<SecretReservationKey> secretReservationKeys)
			throws UnknownSecretReservationKeyFault {

		preconditions.get().checkGetInstanceArguments(secretReservationKeys, true);

		for (Iterator<SecretReservationKey> it = secretReservationKeys.iterator(); it.hasNext(); ) {
			SecretReservationKey current = it.next();
			if (!commonConfig.getUrnPrefix().equals(current.getUrnPrefix())) {
				it.remove();
			}
		}

		final Reservation reservation;

		try {

			reservation = reservationManager.getReservation(newHashSet(secretReservationKeys));

		} catch (ReservationUnknownException e) {
			final String message =
					"Secret reservation keys \"" + Joiner.on(",").join(secretReservationKeys) + "\" are unknown!";
			throw createSMUnknownSecretReservationKeyFault(message, secretReservationKeys.get(0), e);
		}

		return getOrCreateAndStartWSNServiceInstance(reservation).getURI().toString();
	}

	private WSNService getOrCreateAndStartWSNServiceInstance(final Reservation reservation) {

		WSN wsn;
		AuthorizingWSN authorizingWSN;
		WSNService wsnService;
		DeliveryManager deliveryManager;

		synchronized (wsnInstances) {
			synchronized (deliveryManagers) {

				wsnService = wsnInstances.get(reservation.getSerializedKey());

				if (wsnService != null) {
					return wsnService;
				}

				deliveryManager = deliveryManagerFactory.create(reservation);
				deliveryManagers.put(reservation.getSerializedKey(), deliveryManager);

				wsn = wsnFactory.create(reservation, deliveryManager);
				authorizingWSN = authorizingWSNFactory.create(reservation, wsn);
				wsnService = wsnServiceFactory.create(reservation, authorizingWSN);
				wsnInstances.put(reservation.getSerializedKey(), wsnService);
			}
		}

		if (deliveryManager.state() == Service.State.NEW) {
			deliveryManager.startAsync().awaitRunning();
		}

		if (wsnService.state() == Service.State.NEW) {
			wsnService.startAsync().awaitRunning();
		}

		return wsnService;
	}

	@Subscribe
	public void onReservationStarted(final ReservationStartedEvent event) {
		log.trace("SessionManagementImpl.onReservationStarted({})", event);
		synchronized (wsnInstances) {
			synchronized (deliveryManagers) {

				final WSNService wsnService = wsnInstances.get(event.getHeader().getSerializedReservationKey());
				if (wsnService != null && !wsnService.isRunning()) {
					wsnService.startAsync().awaitRunning();
				}

				final DeliveryManager deliveryManager = deliveryManagers.get(event.getHeader().getSerializedReservationKey());
				if (deliveryManager != null && !deliveryManager.isRunning()) {
					deliveryManager.startAsync().awaitRunning();
				}
			}
		}
	}

	@Subscribe
	public void onReservationEnded(final ReservationEndedEvent event) {

		synchronized (wsnInstances) {
			synchronized (deliveryManagers) {

				final WSNService wsnService = wsnInstances.get(event.getHeader().getSerializedReservationKey());
				if (wsnService != null && wsnService.isRunning()) {
					wsnService.stopAsync().awaitTerminated();
				}

				final DeliveryManager deliveryManager = deliveryManagers.get(event.getHeader().getSerializedReservationKey());
				if (deliveryManager != null && deliveryManager.isRunning()) {
					deliveryManager.stopAsync().awaitTerminated();
				}
			}
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
		return serialize(wisemlProvider.get());
	}

	@Override
	@WebMethod
	@WebResult(targetNamespace = "")
	@RequestWrapper(
			localName = "getSupportedChannelHandlers",
			targetNamespace = "http://wisebed.eu/api/v3/sm",
			className = "eu.wisebed.api.v3.sm.GetSupportedChannelHandlers"
	)
	@ResponseWrapper(
			localName = "getSupportedChannelHandlersResponse",
			targetNamespace = "http://wisebed.eu/api/v3/sm",
			className = "eu.wisebed.api.v3.sm.GetSupportedChannelHandlersResponse"
	)
	public List<ChannelHandlerDescription> getSupportedChannelHandlers() {

		final List<ChannelHandlerDescription> list = newArrayList();

		for (HandlerFactory handlerFactory : handlerFactories) {

			final ChannelHandlerDescription channelHandlerDescription = new ChannelHandlerDescription();
			channelHandlerDescription.setName(handlerFactory.getName());
			channelHandlerDescription.setDescription(handlerFactory.getDescription());

			for (String key : handlerFactory.getConfigurationOptions().keySet()) {
				for (String value : handlerFactory.getConfigurationOptions().get(key)) {

					final KeyValuePair keyValuePair = new KeyValuePair();
					keyValuePair.setKey(key);
					keyValuePair.setValue(value);

					channelHandlerDescription.getConfigurationOptions().add(keyValuePair);
				}
			}

			list.add(channelHandlerDescription);
		}

		return list;
	}

	@Override
	@WebMethod
	@WebResult(targetNamespace = "")
	@RequestWrapper(
			localName = "getSupportedVirtualLinkFilters",
			targetNamespace = "http://wisebed.eu/api/v3/sm",
			className = "eu.wisebed.api.v3.sm.GetSupportedVirtualLinkFilters"
	)
	@ResponseWrapper(
			localName = "getSupportedVirtualLinkFiltersResponse",
			targetNamespace = "http://wisebed.eu/api/v3/sm",
			className = "eu.wisebed.api.v3.sm.GetSupportedVirtualLinkFiltersResponse"
	)
	public List<String> getSupportedVirtualLinkFilters() {
		return newArrayList();
	}

	@Override
	@WebMethod
	@WebResult(targetNamespace = "")
	@RequestWrapper(
			localName = "getVersion",
			targetNamespace = "http://wisebed.eu/api/v3/sm",
			className = "eu.wisebed.api.v3.sm.GetVersion"
	)
	@ResponseWrapper(
			localName = "getVersionResponse",
			targetNamespace = "http://wisebed.eu/api/v3/sm",
			className = "eu.wisebed.api.v3.sm.GetVersionResponse"
	)
	public String getVersion() {
		return "3.0";
	}
}
