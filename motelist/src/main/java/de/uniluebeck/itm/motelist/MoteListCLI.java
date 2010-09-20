package de.uniluebeck.itm.motelist;

import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.tr.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MoteListCLI {

	private static final Logger log = LoggerFactory.getLogger(MoteListCLI.class);

	public static void main(String[] args) throws IOException {

		Logging.setLoggingDefaults();

		if (args.length > 1) {
			log.info("Searching for {} device with MAC address: {}", args[0], args[1]);
			MoteType type = MoteType.fromString(args[0]);
			if (type == null) {
				log.error("Unknown node type \"{}\" given.", args[0]);
				System.exit(1);
			}
			log.info("Found: {}", MoteListFactory.create().getMotePort(type, StringUtils.parseHexOrDecLong(args[1])));
		} else {
			log.info("Displaying all connected devices: \n{}", MoteListFactory.create().getMoteList());
		}

	}

}
