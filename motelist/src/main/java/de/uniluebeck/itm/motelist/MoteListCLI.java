package de.uniluebeck.itm.motelist;

import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.tr.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

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

            Map<String, String> telosBReferenceToMACMap = null;
            if (args.length == 3) {
                telosBReferenceToMACMap = readTelosBReferenceToMACMap(args[2]);
                log.info("Using Telos B USB chip ID to MAC mapping: {}", telosBReferenceToMACMap);
            }

            MoteList moteList = MoteListFactory.create(telosBReferenceToMACMap);
            log.info("Found: {}", moteList.getMotePort(type, StringUtils.parseHexOrDecLong(args[1])));

		} else if (args.length == 1) {

            Map<String, String> telosBReferenceToMACMap = null;
            if (args.length == 3) {
                telosBReferenceToMACMap = readTelosBReferenceToMACMap(args[2]);
                log.info("Using Telos B USB chip ID to MAC mapping: {}", telosBReferenceToMACMap);
            }

            log.info("Displaying all connected devices: \n{}", MoteListFactory.create(telosBReferenceToMACMap).getMoteList());

        } else {
            log.info("Displaying all connected devices: \n{}", MoteListFactory.create(null).getMoteList());
        }

	}

    private static Map<String, String> readTelosBReferenceToMACMap(String arg) {
        try {
            Map<String, String> map = new HashMap<String, String>();
            BufferedReader reader = new BufferedReader(new FileReader(arg));
            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                String[] split = currentLine.split(",");
                if (split.length == 2) {
                    map.put(split[0], split[1]);
                }
            }
            return map;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
