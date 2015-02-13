package de.uniluebeck.itm.tr.federator;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.federator.iwsn.IWSNFederatorService;
import de.uniluebeck.itm.tr.federator.rs.RSFederatorService;
import de.uniluebeck.itm.tr.federator.snaa.SNAAFederatorService;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.RestApiService;

public class FederatorServiceImpl extends AbstractService implements FederatorService {

	private final IWSNFederatorService iwsnFederatorService;

	private final RSFederatorService rsFederatorService;

	private final SNAAFederatorService snaaFederatorService;

	private final RestApiService restApiService;

	@Inject
	public FederatorServiceImpl(final IWSNFederatorService iwsnFederatorService,
								final RSFederatorService rsFederatorService,
								final SNAAFederatorService snaaFederatorService,
								final RestApiService restApiService) {
		this.iwsnFederatorService = iwsnFederatorService;
		this.rsFederatorService = rsFederatorService;
		this.snaaFederatorService = snaaFederatorService;
		this.restApiService = restApiService;
	}

	@Override
	protected void doStart() {
		try {

			snaaFederatorService.startAsync().awaitRunning();
			rsFederatorService.startAsync().awaitRunning();
			iwsnFederatorService.startAsync().awaitRunning();
			restApiService.startAsync().awaitRunning();;

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			if (restApiService.isRunning()) {
				restApiService.stopAsync().awaitTerminated();
			}
			if (iwsnFederatorService.isRunning()) {
				iwsnFederatorService.stopAsync().awaitTerminated();
			}
			if (rsFederatorService.isRunning()) {
				rsFederatorService.stopAsync().awaitTerminated();
			}
			if (snaaFederatorService.isRunning()) {
				snaaFederatorService.stopAsync().awaitTerminated();
			}
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}
