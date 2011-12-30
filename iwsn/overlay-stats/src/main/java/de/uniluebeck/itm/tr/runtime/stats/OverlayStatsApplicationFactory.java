package de.uniluebeck.itm.tr.runtime.stats;

import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.application.TestbedApplication;
import de.uniluebeck.itm.gtr.application.TestbedApplicationFactory;

public class OverlayStatsApplicationFactory implements TestbedApplicationFactory {

	@Override
	public TestbedApplication create(TestbedRuntime testbedRuntime, String applicationName, Object configuration) {
		return new OverlayStatsApplication(testbedRuntime, applicationName, configuration);
	}

}
