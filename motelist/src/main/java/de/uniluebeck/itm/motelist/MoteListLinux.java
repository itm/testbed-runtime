package de.uniluebeck.itm.motelist;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import de.uniluebeck.itm.tr.util.Logging;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.StringTokenizer;


public class MoteListLinux extends AbstractMoteList {

	private static final Logger log = LoggerFactory.getLogger(MoteListLinux.class);

	private ProcessBuilder pb;

	public MoteListLinux() throws IOException {
		if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC_OSX) {
			copyScriptToTmp();
			pb = new ProcessBuilder("/tmp/motelistv2", "-c");
			File tmpScript = new File("/tmp/motelistv2");
			tmpScript.setExecutable(true);
		}
	}

	public static void main(String[] args) throws IOException {
		Logging.setLoggingDefaults();
		log.info("{}", new MoteListLinux().getMoteList());
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

	@Override
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
