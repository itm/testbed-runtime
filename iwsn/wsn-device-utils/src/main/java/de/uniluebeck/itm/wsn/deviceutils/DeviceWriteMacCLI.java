package de.uniluebeck.itm.wsn.deviceutils;


import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.wsn.devicedrivers.DeviceFactory;
import de.uniluebeck.itm.wsn.devicedrivers.generic.*;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceWriteMacCLI {

	private static final Logger log = LoggerFactory.getLogger(DeviceWriteMacCLI.class);

	public static void main(String[] args) throws Exception {

		Logging.setLoggingDefaults();

		org.apache.log4j.Logger itmLogger = org.apache.log4j.Logger.getLogger("de.uniluebeck.itm");
		org.apache.log4j.Logger wisebedLogger = org.apache.log4j.Logger.getLogger("eu.wisebed");
        org.apache.log4j.Logger coaLogger = org.apache.log4j.Logger.getLogger("com.coalesenses");

		itmLogger.setLevel(Level.INFO);
		wisebedLogger.setLevel(Level.INFO);
		coaLogger.setLevel(Level.INFO);

		if (args.length < 3) {
			System.out.println(
					"Usage: " + DeviceWriteMacCLI.class.getSimpleName() + " SENSOR_TYPE SERIAL_PORT MAC_ADRESS"
			);
			System.out.println(
					"Example: " + DeviceWriteMacCLI.class.getSimpleName() + " isense /dev/ttyUSB0 0x1234"
			);
			System.exit(1);
		}

		final iSenseDevice device = DeviceFactory.create(args[0], args[1]);

		final Thread cancellationHook = new Thread(new Runnable() {
			@Override
			public void run() {
				device.cancelOperation(Operation.WRITE_MAC);
			}
		}
		);

		device.registerListener(new iSenseDeviceListenerAdapter() {

			private int lastProgress = -1;

			@Override
			public void operationDone(final Operation op, final Object result) {
				lastProgress = -1;
				if (op == Operation.WRITE_MAC) {
					if (result instanceof Exception) {
						log.error("Setting MAC address failed with Exception: " + result, (Exception) result);
						Runtime.getRuntime().removeShutdownHook(cancellationHook);
						System.exit(1);
					} else {
						log.info("Progress: {}%", 100);
						log.info("Setting MAC address done!");
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
				log.info("Setting MAC address was canceled!");
				lastProgress = -1;
			}
		}
		);

		long macAddressLower16 = StringUtils.parseHexOrDecLong(args[2]);
		MacAddress macAddress = new MacAddress(new byte[] {
				0,
				0,
				0,
				0,
				0,
				0,
				(byte) (0xFF & (macAddressLower16 >> 8)),
				(byte) (0xFF & (macAddressLower16))
		});

		try {

			Runtime.getRuntime().addShutdownHook(cancellationHook);

			device.triggerSetMacAddress(macAddress, true);

		} catch (Exception e) {
			log.error("Error while flashing device. Reason: {}", e.getMessage());
			Runtime.getRuntime().removeShutdownHook(cancellationHook);
			System.exit(1);
		}

	}

}
