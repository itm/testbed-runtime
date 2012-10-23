package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.inject.Guice;
import com.google.inject.Injector;
import de.uniluebeck.itm.tr.util.Logging;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Gateway {

	private static final Logger log = LoggerFactory.getLogger(Gateway.class);

	static {
		Logging.setLoggingDefaults();
	}

	public static void main(String[] args) {
		final GatewayConfig gatewayConfig = parseCmdLineOptions(args);
		final GatewayModule gatewayModule = new GatewayModule(gatewayConfig);
		final Injector injector = Guice.createInjector(gatewayModule);
		final GatewayEventBus gatewayEventBus = injector.getInstance(GatewayEventBus.class);
		gatewayEventBus.startAndWait();
		log.info("Gateway started!");
	}

	private static GatewayConfig parseCmdLineOptions(final String[] args) {

		GatewayConfig options = new GatewayConfig();
		CmdLineParser parser = new CmdLineParser(options);

		try {
			parser.parseArgument(args);
			if (options.help) {
				printHelpAndExit(parser);
			}
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			printHelpAndExit(parser);
		}

		return options;
	}

	private static void printHelpAndExit(CmdLineParser parser) {
		System.err.print("Usage: java " + Gateway.class.getCanonicalName());
		parser.printSingleLineUsage(System.err);
		System.err.println();
		parser.printUsage(System.err);
		System.exit(1);
	}

}
