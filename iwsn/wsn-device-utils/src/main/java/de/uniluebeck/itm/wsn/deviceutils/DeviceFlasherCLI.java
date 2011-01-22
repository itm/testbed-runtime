package de.uniluebeck.itm.wsn.deviceutils;


import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.wsn.devicedrivers.DeviceFactory;
import de.uniluebeck.itm.wsn.devicedrivers.generic.IDeviceBinFile;
import de.uniluebeck.itm.wsn.devicedrivers.generic.Operation;
import de.uniluebeck.itm.wsn.devicedrivers.generic.iSenseDevice;
import de.uniluebeck.itm.wsn.devicedrivers.generic.iSenseDeviceListenerAdapter;
import de.uniluebeck.itm.wsn.devicedrivers.jennic.JennicBinFile;
import de.uniluebeck.itm.wsn.devicedrivers.jennic.JennicDevice;
import de.uniluebeck.itm.wsn.devicedrivers.pacemate.PacemateBinFile;
import de.uniluebeck.itm.wsn.devicedrivers.pacemate.PacemateDevice;
import de.uniluebeck.itm.wsn.devicedrivers.telosb.TelosbBinFile;
import de.uniluebeck.itm.wsn.devicedrivers.telosb.TelosbDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceFlasherCLI {

	private static final Logger log = LoggerFactory.getLogger(DeviceFlasherCLI.class);

	public static void main(String[] args) throws Exception {

		Logging.setLoggingDefaults();

		if (args.length < 3) {
			System.out.println(
					"Usage: " + DeviceFlasherCLI.class.getSimpleName() + " SENSOR_TYPE SERIAL_PORT IMAGE_FILE"
			);
			System.out.println(
					"Example: " + DeviceFlasherCLI.class.getSimpleName() + " isense /dev/ttyUSB0 demoapplication.bin"
			);
			System.exit(1);
		}

		final iSenseDevice device = DeviceFactory.create(args[0], args[1]);

		IDeviceBinFile iSenseBinFile = null;

		try {

			if (device instanceof JennicDevice) {
				iSenseBinFile = new JennicBinFile(args[2]);
			} else if (device instanceof TelosbDevice) {
				iSenseBinFile = new TelosbBinFile(args[2]);
			} else if (device instanceof PacemateDevice) {
				iSenseBinFile = new PacemateBinFile(args[2]);
			}

		} catch (Exception e) {
			log.error("" + e, e);
			return;
		}

		final Thread cancellationHook = new Thread(new Runnable() {
			@Override
			public void run() {
				device.cancelOperation(Operation.PROGRAM);
			}
		}
		);

		device.registerListener(new iSenseDeviceListenerAdapter() {
			@Override
			public void operationDone(final Operation op, final Object result) {
				if (op == Operation.PROGRAM) {
					if (result instanceof Exception) {
						log.error("Flashing node failed with Exception: " + result, (Exception) result);
						Runtime.getRuntime().removeShutdownHook(cancellationHook);
						System.exit(1);
					} else {
						log.info("Flashing node done!");
						Runtime.getRuntime().removeShutdownHook(cancellationHook);
						System.exit(0);
					}
				}
			}

			@Override
			public void operationProgress(final Operation op, final float fraction) {
				log.info("Progress: {}%", fraction * 100);
			}

			@Override
			public void operationCanceled(final Operation op) {
				log.info("Flashing was canceled!");
			}
		}
		);

		try {

			Runtime.getRuntime().addShutdownHook(cancellationHook);

			if (!device.triggerProgram(iSenseBinFile, true)) {
				log.error("Failed to trigger programming.");
				Runtime.getRuntime().removeShutdownHook(cancellationHook);
				System.exit(1);
			}

		} catch (Exception e) {
			log.error("Error while flashing device. Reason: {}", e.getMessage());
			Runtime.getRuntime().removeShutdownHook(cancellationHook);
			System.exit(1);
		}

	}

}
