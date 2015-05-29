package de.uniluebeck.itm.tr.iwsn.common;

import de.uniluebeck.itm.tr.iwsn.messages.Header;
import de.uniluebeck.itm.tr.iwsn.messages.Response;
import de.uniluebeck.itm.util.concurrent.ProgressListenableFutureMap;
import eu.wisebed.api.v3.common.NodeUrn;

public interface ResponseTracker extends ProgressListenableFutureMap<NodeUrn, Response> {

	Header getRequestHeader();

}
