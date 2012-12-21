package de.uniluebeck.itm.tr.iwsn.common.config;

import org.apache.log4j.Level;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class ConfigHelper {

	public static <T extends Config> T parseOrExit(final T config,
												   final Class<?> mainClass,
												   final String[] args) {

		CmdLineParser parser = new CmdLineParser(config);

		try {
			parser.parseArgument(args);
			if (config.help) {
				printHelpAndExit(parser, mainClass);
			}
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			printHelpAndExit(parser, mainClass);
		}

		return config;
	}

	public static void printHelpAndExit(CmdLineParser parser, final Class<?> mainClass) {
		System.err.print("Usage: java " + mainClass.getCanonicalName());
		parser.printSingleLineUsage(System.err);
		System.err.println();
		parser.printUsage(System.err);
		System.exit(1);
	}

	public static <T extends ConfigWithLogging> T setLogLevel(final T config) {
		if (config.logLevel != null) {
			org.apache.log4j.Logger.getRootLogger().setLevel(config.logLevel);
		} else if (config.verbose) {
			org.apache.log4j.Logger.getRootLogger().setLevel(Level.DEBUG);
		}
		return config;
	}
}
