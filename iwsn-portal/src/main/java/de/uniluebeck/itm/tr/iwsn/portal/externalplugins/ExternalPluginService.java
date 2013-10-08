package de.uniluebeck.itm.tr.iwsn.portal.externalplugins;

import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.tr.iwsn.messages.*;

public interface ExternalPluginService extends Service {

	void onRequest(Request request);

	void onSingleNodeProgress(SingleNodeProgress progress);

	void onSingleNodeResponse(SingleNodeResponse response);

	void onGetChannelPipelinesResponse(GetChannelPipelinesResponse getChannelPipelinesResponse);

	void onEvent(Event event);

	void onEventAck(EventAck eventAck);
}
