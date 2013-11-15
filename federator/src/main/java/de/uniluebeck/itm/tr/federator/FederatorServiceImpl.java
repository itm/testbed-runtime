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

			snaaFederatorService.startAndWait();
			rsFederatorService.startAndWait();
			iwsnFederatorService.startAndWait();
			restApiService.startAndWait();

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			if (restApiService.isRunning()) {
				restApiService.stopAndWait();
			}
			if (iwsnFederatorService.isRunning()) {
				iwsnFederatorService.stopAndWait();
			}
			if (rsFederatorService.isRunning()) {
				rsFederatorService.stopAndWait();
			}
			if (snaaFederatorService.isRunning()) {
				snaaFederatorService.stopAndWait();
			}
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}
