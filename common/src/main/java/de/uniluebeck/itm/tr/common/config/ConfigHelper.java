package de.uniluebeck.itm.tr.common.config;

import org.apache.log4j.Level;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import javax.annotation.Nullable;

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
		parser.setUsageWidth(120);
		System.err.println();
		parser.printUsage(System.err);
		System.exit(1);
	}

	public static <T extends ConfigWithLogging> T setLogLevel(final T config, @Nullable final String... pkgs) {

		Level level = config.logLevel != null ? config.logLevel : config.verbose ? Level.DEBUG : null;

		if (level != null) {
			if (config.logLibs || null == pkgs) {
				org.apache.log4j.Logger.getRootLogger().setLevel(level);
			} else {
				for (String pkg : pkgs) {
					org.apache.log4j.Logger.getLogger(pkg).setLevel(level);
				}
			}
		}

		return config;
	}
}
