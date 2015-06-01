package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.base.Function;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.messages.SingleNodeResponse;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.sm.NodeConnectionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;

public class FederatorPortalEventBusAdapter extends AbstractService {

	private static final Logger log = LoggerFactory.getLogger(FederatorPortalEventBusAdapter.class);

	private final PortalEventBus portalEventBus;

	private final SessionManagementFederatorService smFederatorService;

	@Inject
	public FederatorPortalEventBusAdapter(final PortalEventBus portalEventBus,
										  final SessionManagementFederatorService smFederatorService) {
		this.portalEventBus = portalEventBus;
		this.smFederatorService = smFederatorService;
	}

	@Override
	protected void doStart() {
		log.trace("FederatorPortalEventBusAdapter.doStart()");
		try {
			portalEventBus.register(this);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		log.trace("FederatorPortalEventBusAdapter.doStop()");
		try {
			portalEventBus.unregister(this);
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Subscribe
	public void onRequest(final Request request) {
		log.trace("FederatorPortalEventBusAdapter.onRequest({})", request);
		switch (request.getType()) {
			case ARE_NODES_CONNECTED:
				final List<NodeUrn> nodeUrns = newArrayList(
						transform(request.getAreNodesConnectedRequest().getNodeUrnsList(), (Function<String, NodeUrn>) NodeUrn::new)
				);
				final List<NodeConnectionStatus> response = smFederatorService.areNodesConnected(nodeUrns);
				for (NodeConnectionStatus status : response) {
					final SingleNodeResponse singleNodeResponse = SingleNodeResponse.newBuilder()
							.setRequestId(request.getRequestId())
							.setNodeUrn(status.getNodeUrn().toString())
							.setStatusCode(status.isConnected() ? 1 : 0)
							.build();
					portalEventBus.post(singleNodeResponse);
				}
				break;
		}
	}
}
