package de.uniluebeck.itm.wsn.deviceutils;


import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.wsn.devicedrivers.DeviceFactory;
import de.uniluebeck.itm.wsn.devicedrivers.generic.*;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceReadMacCLI {

	private static final Logger log = LoggerFactory.getLogger(DeviceReadMacCLI.class);

	public static void main(String[] args) throws Exception {

		Logging.setLoggingDefaults();

		org.apache.log4j.Logger itmLogger = org.apache.log4j.Logger.getLogger("de.uniluebeck.itm");
		org.apache.log4j.Logger wisebedLogger = org.apache.log4j.Logger.getLogger("eu.wisebed");
        org.apache.log4j.Logger coaLogger = org.apache.log4j.Logger.getLogger("com.coalesenses");

		itmLogger.setLevel(Level.INFO);
		wisebedLogger.setLevel(Level.INFO);
		coaLogger.setLevel(Level.INFO);

		if (args.length < 2) {
			System.out.println(
					"Usage: " + DeviceReadMacCLI.class.getSimpleName() + " SENSOR_TYPE SERIAL_PORT"
			);
			System.out.println(
					"Example: " + DeviceReadMacCLI.class.getSimpleName() + " isense /dev/ttyUSB0"
			);
			System.exit(1);
		}

		final iSenseDevice device = DeviceFactory.create(args[0], args[1]);

		final Thread cancellationHook = new Thread(new Runnable() {
			@Override
			public void run() {
				device.cancelOperation(Operation.READ_MAC);
			}
		}
		);

		device.registerListener(new iSenseDeviceListenerAdapter() {

			private int lastProgress = -1;

			@Override
			public void operationDone(final Operation op, final Object result) {
				lastProgress = -1;
				if (op == Operation.READ_MAC) {
					if (result instanceof Exception) {
						log.error("Reading MAC address failed with Exception: " + result, (Exception) result);
						Runtime.getRuntime().removeShutdownHook(cancellationHook);
						System.exit(1);
					} else {
						log.info("Progress: {}%", 100);
						log.info("Reading MAC address done!");
						System.out.println(StringUtils.toHexString(((MacAddress) result).getMacBytes()));
						Runtime.getRuntime().removeShutdownHook(cancellationHook);
						System.exit(0);
					}
				}
			}

			@Override
			public void operationProgress(final Operation op, final float fraction) {
				int newProgress = (int) Math.floor(fraction * 100);
				if (lastProgress < newProgress) {
					lastProgress = newProgress;
					log.info("Progress: {}%", newProgress);
				}
			}

			@Override
			public void operationCanceled(final Operation op) {
				log.info("Reading MAC address was canceled!");
				lastProgress = -1;
			}
		}
		);

		try {

			Runtime.getRuntime().addShutdownHook(cancellationHook);

			device.triggerGetMacAddress(true);

		} catch (Exception e) {
			log.error("Error while flashing device. Reason: {}", e.getMessage());
			Runtime.getRuntime().removeShutdownHook(cancellationHook);
			System.exit(1);
		}

	}

}
