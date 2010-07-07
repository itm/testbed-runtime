package de.uniluebeck.itm.motelist;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import de.uniluebeck.itm.tr.util.TimeDiff;
import de.uniluebeck.itm.tr.util.Tuple;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MacAddress;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePacket;
import de.uniluebeck.itm.wsn.devicedrivers.generic.Operation;
import de.uniluebeck.itm.wsn.devicedrivers.generic.iSenseDeviceListenerAdapter;
import de.uniluebeck.itm.wsn.devicedrivers.jennic.JennicDevice;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;
import java.util.StringTokenizer;


public class MoteList {

	private static final Logger log = LoggerFactory.getLogger(MoteList.class);

	private ProcessBuilder pb;

	/**
	 * port -> (type, mac)
	 */
	private BiMap<String, Tuple<String, MacAddress>> devices;

	public MoteList() throws IOException {
		if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC_OSX) {
			copyScriptToTmp();
			pb = new ProcessBuilder("/tmp/motelistv2", "-c");
			File tmpScript = new File("/tmp/motelistv2");
			tmpScript.setExecutable(true);
		}
	}

	private void copyScriptToTmp() throws IOException {

		InputStream from = getClass().getClassLoader().getResourceAsStream("motelistv2");
		FileOutputStream to = null;

		try {

			to = new FileOutputStream(new File("/tmp/motelistv2"));
			byte[] buffer = new byte[4096];
			int bytesRead;

			while ((bytesRead = from.read(buffer)) != -1) {
				to.write(buffer, 0, bytesRead);
			} // write

		} finally {
			if (from != null) {
				try {
					from.close();
				} catch (IOException e) {
					log.debug("" + e, e);
				}
			}
			if (to != null) {
				try {
					to.close();
				} catch (IOException e) {
					log.debug("" + e, e);
				}
			}
		}

	}

	public Multimap<String, String> getMoteList() {
		BufferedReader in;
		try {
			Process p = pb.start();
			// Eingabestream holen
			in = new BufferedReader(new InputStreamReader(p.getInputStream()));
			// parsing
			return parseMoteList(in);

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param type	   one of "isense", "telosb", "pacemate"
	 * @param macAddress
	 *
	 * @return {@code null} if not found, serial device (e.g. /dev/ttyUSB0) otherwise
	 */
	public String getMotePort(String type, int macAddress) {

		Multimap<String, String> moteList = getMoteList();

		/* port -> (type,mac) */
		if (devices == null) {

			devices = HashBiMap.create();

			for (final String port : moteList.get(type)) {

				log.debug("Probing {}", port);

				TimeDiff diff = new TimeDiff(1000);
				JennicDevice device = new JennicDevice(port);

				if (device.isConnected()) {
					device.registerListener(new iSenseDeviceListenerAdapter() {
						@Override
						public void receivePacket(final MessagePacket p) {
							// nothing to do
						}

						@Override
						public void operationDone(final Operation op, final Object result) {
							if (op == Operation.READ_MAC && result != null) {
								log.debug("Found iSense device on port {} with MAC address {}", port, result);
								devices.put(port, new Tuple<String, MacAddress>("isense", (MacAddress) result));
							}
						}
					}
					);
					device.triggerGetMacAddress(true);
				}

				while (!diff.isTimeout()) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						log.error("" + e, e);
					}
				}

				if (device.getOperation() == Operation.READ_MAC) {
					device.cancelOperation(Operation.READ_MAC);
				}

				// TODO pacemate and telosb

				device.shutdown();

			}

		}

		for (Map.Entry<String, Tuple<String, MacAddress>> entry : devices.entrySet()) {
			boolean sameType = entry.getValue().getFirst().equals(type);
			boolean sameMac = entry.getValue().getSecond().getMacLowest16() == macAddress;
			if (sameType && sameMac) {
				return entry.getKey(); // port
			}
		}

		return null;

	}

	private Multimap<String, String> parseMoteList(final BufferedReader in) {

		Multimap<String, String> motes = LinkedListMultimap.create(3);
		String text = "";

		try {

			in.readLine(); // Information about how we had got this information.

			String reference, port, type;

			while ((text = in.readLine()) != null) {

				StringTokenizer tokenizer = new StringTokenizer(text, ",");


				if (tokenizer.countTokens() != 3) {
					log.warn("Unexpected token count of {} in line \"{}\"", tokenizer.countTokens(), text);
				} else {

					reference = tokenizer.nextToken();
					port = tokenizer.nextToken();
					type = tokenizer.nextToken();

					if (type.contains("XBOW")) {
						motes.put("telosb", port);
					}
					if (type.contains("Pacemate")) {
						motes.put("pacemate", port);
					}
					if (type.contains("FTDI")) {
						motes.put("isense", port);
					}

				}
			}
		} catch (IOException e) {
			log.error("" + e, e);
		}

		return motes;
	}
}
