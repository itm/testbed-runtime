/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.iwsn.cmdline;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import de.uniluebeck.itm.tr.iwsn.IWSN;
import de.uniluebeck.itm.tr.iwsn.IWSNFactory;
import de.uniluebeck.itm.tr.iwsn.IWSNModule;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.tr.util.Tuple;
import org.apache.commons.cli.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import java.io.File;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static de.uniluebeck.itm.tr.util.FilePreconditions.checkFileExists;
import static de.uniluebeck.itm.tr.util.FilePreconditions.checkFileReadable;
import static de.uniluebeck.itm.tr.util.XmlFunctions.fileToRootElementFunction;
import static de.uniluebeck.itm.tr.util.XmlFunctions.xPathToBooleanEvaluationFunction;

public class IWSNServer {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(IWSNServer.class);

	public static void main(String[] args) throws Exception {
		Thread.currentThread().setName("Startup-Thread");
		try {
			start(args);
		} catch (Exception e) {
			log.error("Unable to start testbed runtime due to: {}", e.getMessage(), e);
			System.exit(1);
		}
	}

	public static void start(String[] args) throws Exception {

		File xmlFile = null;
		String nodeId = null;

		// create the command line parser
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("n", "nodeid", true,
				"Node ID to start (the id attribute of the node tag), if not set autodetect is tried"
		);
		options.addOption("f", "file", true, "The (XML) configuration file");
		options.addOption("v", "verbose", false, "Verbose logging output (equal to -l DEBUG)");
		options.addOption("l", "logging", true,
				"Set logging level (one of [" + Level.TRACE + "," + Level.DEBUG + "," + Level.INFO + "," + Level.WARN + "," + Level.ERROR + "])"
		);
		options.addOption("h", "help", false, "Help output");

		Logging.setLoggingDefaults();

		final org.slf4j.Logger log = LoggerFactory.getLogger(IWSNServer.class);

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

			if (line.hasOption('h')) {
				usage(options);
			}

			if (line.hasOption('f')) {

				String xmlFileString = line.getOptionValue('f');

				xmlFile = new File(xmlFileString);

				checkFileExists(xmlFile);
				checkFileReadable(xmlFile);

			} else {
				throw new Exception("Please supply -f");
			}

			if (line.hasOption('n')) {
				nodeId = line.getOptionValue('n');
			}

		} catch (Exception e) {
			log.error("Invalid command line: " + e, e);
			usage(options);
		}

		String[] nodeIds = nodeId != null ? new String[]{nodeId} :
				new String[]{
						InetAddress.getLocalHost().getCanonicalHostName(),
						InetAddress.getLocalHost().getHostName()
				};

		String nodeIdFoundInConfigurationFile = null;

		Node rootElement = fileToRootElementFunction().apply(xmlFile);
		for (String id : nodeIds) {

			String xPathExpression = "boolean(//nodes[@id=\"" + id + "\"])";
			boolean configurationTagForIdExists = xPathToBooleanEvaluationFunction().apply(
					new Tuple<String, Node>(xPathExpression, rootElement)
			);

			if (configurationTagForIdExists) {
				nodeIdFoundInConfigurationFile = id;
			}
		}

		checkArgument(nodeIdFoundInConfigurationFile != null,
				"No configuration for one of the overlay node \"%s\" found!", Arrays.toString(nodeIds)
		);

		final ScheduledExecutorService messageServerServiceScheduler = Executors.newScheduledThreadPool(
				1,
				new ThreadFactoryBuilder().setNameFormat("MessageServerService-Thread %d").build()
		);

		final ScheduledExecutorService reliableMessagingServiceScheduler = Executors.newScheduledThreadPool(
				1,
				new ThreadFactoryBuilder().setNameFormat("ReliableMessagingService-Thread %d").build()
		);

		final ExecutorService asyncEventBusExecutor = Executors.newCachedThreadPool(
				new ThreadFactoryBuilder().setNameFormat("AsyncEventBus-Thread %d").build()
		);

		final Injector injector = Guice.createInjector(
				new IWSNModule(
						asyncEventBusExecutor,
						messageServerServiceScheduler,
						reliableMessagingServiceScheduler
				)
		);

		final IWSNFactory iwsnFactory = injector.getInstance(IWSNFactory.class);
		final IWSN iwsn = iwsnFactory.create(xmlFile, nodeIdFoundInConfigurationFile);

		iwsn.start();

		Runnable shutdownRunnable = new Runnable() {
			@Override
			public void run() {
				iwsn.stop();
				ExecutorUtils.shutdown(asyncEventBusExecutor, 10, TimeUnit.SECONDS);
				ExecutorUtils.shutdown(messageServerServiceScheduler, 10, TimeUnit.SECONDS);
			}
		};

		Runtime.getRuntime().addShutdownHook(new Thread(shutdownRunnable, "Shutdown-Thread"));
	}

	private static void usage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(120, IWSNServer.class.getCanonicalName(), null, options, null);
		System.exit(1);
	}
}
