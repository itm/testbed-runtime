package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.messages.AreNodesConnectedRequest;
import de.uniluebeck.itm.tr.iwsn.messages.MessageFactory;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.sm.NodeConnectionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.empty;

public class FederatorPortalEventBusAdapter extends AbstractService {

	private static final Logger log = LoggerFactory.getLogger(FederatorPortalEventBusAdapter.class);

	private final PortalEventBus portalEventBus;

	private final MessageFactory messageFactory;

	private final SessionManagementFederatorService smFederatorService;

	@Inject
	public FederatorPortalEventBusAdapter(final PortalEventBus portalEventBus,
										  final MessageFactory messageFactory,
										  final SessionManagementFederatorService smFederatorService) {
		this.portalEventBus = portalEventBus;
		this.messageFactory = messageFactory;
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
	public void on(final AreNodesConnectedRequest request) {
		log.trace("FederatorPortalEventBusAdapter.on({})", request);
		final List<NodeUrn> nodeUrns = Lists.transform(request.getHeader().getNodeUrnsList(), NodeUrn::new);
		final List<NodeConnectionStatus> response = smFederatorService.areNodesConnected(nodeUrns);
		for (NodeConnectionStatus status : response) {
			portalEventBus.post(messageFactory.response(
					empty(),
					empty(),
					request.getHeader().getCorrelationId(),
					newArrayList(status.getNodeUrn()),
					status.isConnected() ? 1 : 0,
					empty(),
					empty()
			));
		}
	}
}
