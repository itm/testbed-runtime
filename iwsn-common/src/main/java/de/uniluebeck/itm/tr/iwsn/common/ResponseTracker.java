package de.uniluebeck.itm.tr.iwsn.common;

import de.uniluebeck.itm.tr.iwsn.messages.SingleNodeResponse;
import de.uniluebeck.itm.tr.util.ListenableFutureMap;
import eu.wisebed.api.v3.common.NodeUrn;

public interface ResponseTracker extends ListenableFutureMap<NodeUrn, SingleNodeResponse> {

}
