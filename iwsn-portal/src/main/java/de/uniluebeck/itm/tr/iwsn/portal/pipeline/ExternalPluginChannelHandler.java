package de.uniluebeck.itm.tr.iwsn.portal.pipeline;

import com.google.protobuf.MessageLite;
import de.uniluebeck.itm.tr.iwsn.messages.EventAck;
import de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse;
import de.uniluebeck.itm.tr.iwsn.messages.Progress;
import de.uniluebeck.itm.tr.iwsn.messages.Response;
import de.uniluebeck.itm.tr.iwsn.portal.externalplugins.ExternalPluginService;
import org.jboss.netty.channel.SimpleChannelHandler;

import javax.inject.Inject;

/**
 * The ExternalPluginChannelHandler receives messages from the ExternalPluginService and routes them downstream (towards
 * gateways) or upstream (towards user / other testbed internal logic).
 */
public class ExternalPluginChannelHandler extends SimpleChannelHandler {

	private final ExternalPluginService externalPluginService;

	@Inject
	public ExternalPluginChannelHandler(ExternalPluginService externalPluginService) {
		this.externalPluginService = externalPluginService;
	}

	private void sendToExternalPlugins(final MessageLite request) {
		externalPluginService.onRequest(request);
	}

	private void sendToExternalPlugins(final Progress progress) {
		externalPluginService.onSingleNodeProgress(progress);
	}

	private void sendToExternalPlugins(final Response response) {
		externalPluginService.onSingleNodeResponse(response);
	}

	private void sendToExternalPlugins(final GetChannelPipelinesResponse getChannelPipelinesResponse) {
		externalPluginService.onGetChannelPipelinesResponse(getChannelPipelinesResponse);
	}

	private void sendToExternalPlugins(final Event event) {
		externalPluginService.onEvent(event);
	}

	private void sendToExternalPlugins(final EventAck eventAck) {
		externalPluginService.onEventAck(eventAck);
	}
}
