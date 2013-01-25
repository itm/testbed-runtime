package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v2;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import de.uniluebeck.itm.nettyprotocols.HandlerFactory;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTracker;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerFactory;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.portal.PortalConfig;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.RequestIdProvider;
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.sm.ChannelHandlerDescription;
import eu.wisebed.api.v3.sm.ExperimentNotRunningFault_Exception;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.api.v3.sm.UnknownReservationIdFault_Exception;

import javax.jws.WebParam;
import javax.xml.ws.Holder;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.newAreNodesConnectedRequest;

public class SessionManagementImpl implements SessionManagement {

	private final DeliveryManager deliveryManager;

	private final PortalEventBus portalEventBus;

	private final RequestIdProvider requestIdProvider;

	private final ResponseTrackerFactory responseTrackerFactory;

	private final PortalConfig portalConfig;

	private final Set<HandlerFactory> handlerFactories;

	@Inject
	public SessionManagementImpl(final DeliveryManager deliveryManager,
								 final PortalEventBus portalEventBus,
								 final RequestIdProvider requestIdProvider,
								 final ResponseTrackerFactory responseTrackerFactory,
								 final PortalConfig portalConfig,
								 final Set<HandlerFactory> handlerFactories) {
		this.deliveryManager = checkNotNull(deliveryManager);
		this.portalEventBus = checkNotNull(portalEventBus);
		this.requestIdProvider = checkNotNull(requestIdProvider);
		this.responseTrackerFactory = checkNotNull(responseTrackerFactory);
		this.portalConfig = checkNotNull(portalConfig);
		this.handlerFactories = checkNotNull(handlerFactories);
	}

	@Override
	public void areNodesAlive(@WebParam(name = "requestId", targetNamespace = "") final long requestId,
							  @WebParam(name = "nodeUrns", targetNamespace = "") final List<NodeUrn> nodeUrns,
							  @WebParam(name = "controllerEndpointUrl", targetNamespace = "") final
							  String controllerEndpointUrl) {

		deliveryManager.addController(controllerEndpointUrl);

		final Request request = newAreNodesConnectedRequest(requestId, nodeUrns);
		final ResponseTracker responseTracker = responseTrackerFactory.create(request, portalEventBus);

		portalEventBus.post(request);

		responseTracker.addListener(new Runnable() {
			@Override
			public void run() {
				deliveryManager.removeController(controllerEndpointUrl);
			}
		}, sameThreadExecutor()
		);
	}

	@Override
	public void getConfiguration(
			@WebParam(name = "rsEndpointUrl", targetNamespace = "", mode = WebParam.Mode.OUT) final
			Holder<String> rsEndpointUrl,
			@WebParam(name = "snaaEndpointUrl", targetNamespace = "", mode = WebParam.Mode.OUT) final
			Holder<String> snaaEndpointUrl,
			@WebParam(name = "servedUrnPrefixes", targetNamespace = "", mode = WebParam.Mode.OUT) final
			Holder<List<NodeUrnPrefix>> servedUrnPrefixes,
			@WebParam(name = "options", targetNamespace = "", mode = WebParam.Mode.OUT) final
			Holder<List<KeyValuePair>> options) {

		rsEndpointUrl.value = portalConfig.rsEndpointUrl.toString();
		snaaEndpointUrl.value = portalConfig.snaaEndpointUrl.toString();
		servedUrnPrefixes.value = newArrayList(portalConfig.urnPrefix);

		final List<KeyValuePair> optionsList = Lists.newArrayList();
		for (String key : portalConfig.options.keySet()) {
			for (String value : portalConfig.options.get(key)) {
				final KeyValuePair keyValuePair = new KeyValuePair();
				keyValuePair.setKey(key);
				keyValuePair.setValue(value);
				optionsList.add(keyValuePair);
			}
		}
		options.value = optionsList;
	}

	@Override
	public String getInstance(
			@WebParam(name = "secretReservationKey", targetNamespace = "") final
			List<SecretReservationKey> secretReservationKey)
			throws ExperimentNotRunningFault_Exception, UnknownReservationIdFault_Exception {
		return null;  // TODO implement
	}

	@Override
	public String getNetwork() {



		return null;  // TODO implement
	}

	@Override
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
		}

		return list;
	}

	@Override
	public List<String> getSupportedVirtualLinkFilters() {
		return newArrayList();
	}
}
