package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1;

import com.google.common.util.concurrent.AbstractService;

public class RestApiServiceImpl extends AbstractService implements RestApiService {

	@Override
	protected void doStart() {
		try {
			// TODO implement
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			// TODO implement
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}
