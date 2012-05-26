package de.uniluebeck.itm.tr.iwsn.overlay.stats;

import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import de.uniluebeck.itm.tr.iwsn.overlay.application.TestbedApplication;
import de.uniluebeck.itm.tr.iwsn.overlay.application.TestbedApplicationFactory;

public class OverlayStatsApplicationFactory implements TestbedApplicationFactory {

	@Override
	public TestbedApplication create(TestbedRuntime testbedRuntime, String applicationName, Object configuration) {
		return new OverlayStatsApplication(testbedRuntime, applicationName, configuration);
	}

}
