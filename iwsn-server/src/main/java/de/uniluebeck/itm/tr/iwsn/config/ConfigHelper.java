package de.uniluebeck.itm.tr.iwsn.config;

import de.uniluebeck.itm.tr.iwsn.portal.Portal;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class ConfigHelper {

	public static <T extends Config> T parseConfigOrPrintHelpAndExit(final T config, final String[] args) {

		CmdLineParser parser = new CmdLineParser(config);

		try {
			parser.parseArgument(args);
			if (config.help) {
				printHelpAndExit(parser);
			}
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			printHelpAndExit(parser);
		}

		return config;
	}

	public static void printHelpAndExit(CmdLineParser parser) {
		System.err.print("Usage: java " + Portal.class.getCanonicalName());
		parser.printSingleLineUsage(System.err);
		System.err.println();
		parser.printUsage(System.err);
		System.exit(1);
	}

}
