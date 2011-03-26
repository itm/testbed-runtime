package de.uniluebeck.itm.wsn.deviceutils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.wsn.devicedrivers.DeviceFactory;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePacket;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePlainText;
import de.uniluebeck.itm.wsn.devicedrivers.generic.iSenseDevice;
import de.uniluebeck.itm.wsn.devicedrivers.generic.iSenseDeviceListenerAdapter;
import de.uniluebeck.itm.wsn.deviceutils.writers.CsvWriter;
import de.uniluebeck.itm.wsn.deviceutils.writers.HumanReadableWriter;
import de.uniluebeck.itm.wsn.deviceutils.writers.WiseMLWriter;
import de.uniluebeck.itm.wsn.deviceutils.writers.Writer;

public class DeviceListenerCLI {
	private final static org.slf4j.Logger log = LoggerFactory.getLogger(DeviceListenerCLI.class);

	private static class MessageReceiver extends iSenseDeviceListenerAdapter {
		private Writer outWriter;

		public MessageReceiver(Writer outWriter) {
			this.outWriter = outWriter;
		}

		@Override
		public void receivePlainText(MessagePlainText p) {
			outWriter.write(p);
		}

		@Override
		public void receivePacket(MessagePacket p) {
			outWriter.write(p);
		}

	}

	/**
	 * @param args
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void main(String[] args) throws InterruptedException, IOException {

		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("p", "port", true, "Serial port to use");
		options.addOption("t", "type", true, "Device type");
		options.addOption("f", "format", true, "Optional: Output format, options: csv, wiseml");
		options.addOption("o", "outfile", true, "Optional: Redirect output to file");
		options.addOption("v", "verbose", false, "Optional: Verbose logging output (equal to -l DEBUG)");
		options.addOption("l", "logging", true, "Optional: Set logging level (one of [" + Level.TRACE + ","
				+ Level.DEBUG + "," + Level.INFO + "," + Level.WARN + "," + Level.ERROR + "])");
		options.addOption("h", "help", false, "Help output");

		Logging.setLoggingDefaults();

		iSenseDevice device = null;
		OutputStream outStream = System.out;
		Writer outWriter;

		try {

			CommandLine line = parser.parse(options, args);

			if (line.hasOption('v')) {
				Logger.getRootLogger().setLevel(Level.DEBUG);
				Logger.getLogger("de.uniluebeck.itm").setLevel(Level.DEBUG);
				Logger.getLogger("eu.wisebed").setLevel(Level.DEBUG);
			}

			if (line.hasOption('l')) {

				Level level = Level.toLevel(line.getOptionValue('l'));
				System.out.println("Setting log level to " + level);

				Logger.getRootLogger().setLevel(level);
				Logger.getLogger("de.uniluebeck.itm").setLevel(level);
				Logger.getLogger("eu.wisebed").setLevel(level);
			}

			if (line.hasOption('p') && line.hasOption('t')) {
				device = DeviceFactory.create(line.getOptionValue('t'), line.getOptionValue('p'));
			} else {
				throw new Exception("Please supply -p and -t");
			}

			if (line.hasOption('h')) {
				printUsageAndExit(options);
			}

			if (line.hasOption('o')) {
				String filename = line.getOptionValue('o');
				log.info("Using outfile " + filename);
				outStream = new FileOutputStream(filename);
			}

			if (line.hasOption('f')) {
				String format = line.getOptionValue('f');

				if ("csv".equals(format)) {
					outWriter = new CsvWriter(outStream);
				} else if ("wiseml".equals(format)) {
					outWriter = new WiseMLWriter(outStream, "node at " + line.getOptionValue('p'), true);
				} else {
					throw new Exception("Unknown format " + format);
				}

				log.info("Using format " + format);
			} else {
				outWriter = new HumanReadableWriter(outStream);
			}

			device.registerListener(new MessageReceiver(outWriter));

			log.info("Press any key to exit");
			while (System.in.available() <= 0) {
				Thread.sleep(100);
			}

			outWriter.shutdown();
			outStream.close();
			outStream.flush();

		} catch (Exception e) {
			log.error("Invalid command line: " + e);
			printUsageAndExit(options);
		}

		System.exit(0);
	}

	private static void printUsageAndExit(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(120, DeviceListenerCLI.class.getCanonicalName(), null, options, null);
		System.exit(1);
	}
}
