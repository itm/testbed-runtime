package eu.wisebed.motap.connector;

import com.coalesenses.otap.core.OtapDevice;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.tr.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;


public class WisebedMotapMain {

	private static final Logger log = LoggerFactory.getLogger(WisebedMotapMain.class);

	private static List<Long> macs;

	private String nodeUrn;

	private static final Object waitLock = new Object();

	private String nodeType = "isense";

	private String nodeSerialInterface = "/dev/tty.usbserial-000014FA";

	private static com.coalesenses.otap.core.OtapPlugin otapPlugin;

	private static final long PRESENCE_DETECT_TIMEOUT = 20000;

	private static boolean force = true;

	private static boolean all;

	public static void main(String[] args) {
		Logging.setLoggingDefaults();
		macs = new Vector<Long>();
		if (!args[0].equals("*")) {
			for (String arg : args) {
				macs.add(StringUtils.parseHexOrDecLongFromUrn(arg));
			}
		} else {
			all = true;
		}

		WisebedMotapMain motap = new WisebedMotapMain();
		motap.connect();

		otapPlugin.setChannel(12);

		otapPlugin.setOtapKey(null, false);
		otapPlugin.setMultihopSupportState(true);
		otapPlugin.setPresenceDetectState(true);
		long startTime = System.currentTimeMillis();
		List<Long> presentDeviceIds = new Vector<Long>();
		Set<OtapDevice> presentDevices = new HashSet<OtapDevice>();

		while (System.currentTimeMillis() - startTime < PRESENCE_DETECT_TIMEOUT) {
			synchronized (waitLock) {
				try {
					waitLock.wait(1000);
					for (Long mac : macs) {
						for (OtapDevice device : otapPlugin.getPresenceDetect().getDetectedDevices()) {
							Long id = new Long(device.getId());
							if (id.longValue() == mac.longValue()) {
								presentDeviceIds.add(id);
								presentDevices.add(device);
								break;
							}
						}
					}
					if (all) {
						for (OtapDevice device : otapPlugin.getPresenceDetect().getDetectedDevices()) {
							Long id = new Long(device.getId());
							if (!presentDeviceIds.contains(id)) {
								presentDeviceIds.add(id);
								presentDevices.add(device);
							}
						}
					}
					log.info("found the following {} devices: {}", presentDeviceIds.size(), presentDeviceIds);
					if (presentDeviceIds.size() == macs.size() && !all) {
						log.info("found all devices");
						break;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
				}
			}
		}
		log.info("found the following {} devices: {}", presentDeviceIds.size(), presentDeviceIds);
		if (presentDeviceIds.size() != 0 && (presentDeviceIds.size() == macs.size() || force)) {
			//for(int i = 0; )
			otapPlugin.setPresenceDetectState(false);
			otapPlugin.loadBinProgram(
					"/Users/maxpagel/Desktop/testbedTemplateApp.bin"
			);
			log.info("start programming");
			otapPlugin.setParticipatingDeviceList(presentDevices);
			otapPlugin.otapStart();

		} else {
			log.info("not all requested devices have been found. Aborting");
		}

	}

	private void connect() {



	}

}
