package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.Guice;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.common.config.ConfigWithLoggingAndProperties;
import de.uniluebeck.itm.util.logging.Logging;

import static de.uniluebeck.itm.tr.common.config.ConfigHelper.parseOrExit;
import static de.uniluebeck.itm.tr.common.config.ConfigHelper.setLogLevel;
import static de.uniluebeck.itm.util.propconf.PropConfBuilder.buildConfig;

public class DeviceDBServer {

	static {
		Logging.setLoggingDefaults();
	}

	public static void main(String[] args) {

		final ConfigWithLoggingAndProperties config = setLogLevel(
				parseOrExit(new ConfigWithLoggingAndProperties(), DeviceDBServer.class, args),
				"de.uniluebeck.itm"
		);

		final CommonConfig commonConfig = buildConfig(CommonConfig.class, config.config);
		final DeviceDBConfig deviceDBConfig = buildConfig(DeviceDBConfig.class, config.config);
		final DeviceDBServerModule module = new DeviceDBServerModule(commonConfig, deviceDBConfig);
		final DeviceDBRestService deviceDBRestService = Guice.createInjector(module).getInstance(DeviceDBRestService.class);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (deviceDBRestService.isRunning()) {
					deviceDBRestService.stopAndWait();
				}
			}
		}
		);

		deviceDBRestService.startAndWait();
	}

}
