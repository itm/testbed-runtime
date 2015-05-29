package de.uniluebeck.itm.tr.iwsn.portal.pipeline;

import com.google.common.util.concurrent.AbstractService;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

public class PortalEventBusHandlerService extends AbstractService {

	private static final Set<Class> handledTypes = new HashSet<>();

	static {
		handledTypes.add(AreNodesAliveRequest.class);
		handledTypes.add(AreNodesConnectedRequest.class);
		handledTypes.add(DisableNodesRequest.class);
		handledTypes.add(DisableVirtualLinksRequest.class);
		handledTypes.add(DisablePhysicalLinksRequest.class);
		handledTypes.add(EnableNodesRequest.class);
		handledTypes.add(EnablePhysicalLinksRequest.class);
		handledTypes.add(EnableVirtualLinksRequest.class);
		handledTypes.add(FlashImagesRequest.class);
		handledTypes.add(GetChannelPipelinesRequest.class);
		handledTypes.add(ResetNodesRequest.class);
		handledTypes.add(SendDownstreamMessagesRequest.class);
		handledTypes.add(SetChannelPipelinesRequest.class);
		handledTypes.add(Progress.class);
		handledTypes.add(Response.class);
		handledTypes.add(GetChannelPipelinesResponse.class);
		handledTypes.add(UpstreamMessageEvent.class);
		handledTypes.add(DevicesAttachedEvent.class);
	}

	private final PortalEventBus portalEventBus;

	@Inject
	public PortalEventBusHandlerService(PortalEventBus portalEventBus) {
		this.portalEventBus = portalEventBus;
	}

	@Override
	protected void doStart() {
		try {
			portalEventBus.register(this);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			portalEventBus.unregister(this);
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}


}
