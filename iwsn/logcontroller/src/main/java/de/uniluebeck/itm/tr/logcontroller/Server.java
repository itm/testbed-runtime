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
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or        *
 *   promote products derived from this software without specific prior written permission.                           *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/
package de.uniluebeck.itm.tr.logcontroller;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Main-class for the logcontroller
 */
public class Server {

	public final static String SESSIONMANAGEMENT_ENDPOINT = "sessionmanagement-endpoint";

	public final static String RESERVATION_ENDPOINT = "rs-endpoint";

	public final static String MESSAGESTORE_ENDPOINT = "messagestore-endpoint";

	public final static String SESSIONMANAGEMENT_PROXY = "sessionmanagement-proxy-endpoint";

	public final static String CONTROLLER_PROXY_PREFIX = "controller-proxy-endpoint-prefix";

	public final static String WSN_PROXY_PREFIX = "wsn-proxy-endpoint-prefix";

	public final static String MESSAGELISTENER = "messagelistener";

	public final static String PERSISTENCE_CONTEXT = "dblogger";

	private static Logger log = LoggerFactory.getLogger(Server.class);

	private static String propertyFile;

	private static ControllerService controller;

	public static void main(String[] args) {
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("f", "file", true, "Path to the configuration file");
		try {
			CommandLine line = parser.parse(options, args);
			if (line.hasOption("f")) {
				propertyFile = line.getOptionValue("f");
			} else {
				throw new MissingArgumentException("please supply argument -f");
			}
		} catch (Exception e) {
			log.error("invalid commandline: {}", e);
			usage(options);
			System.exit(1);
		}

		Properties props = new Properties();
		try {
			log.debug("Loading PropertyFile: {}", propertyFile);
			props.load(new FileReader(propertyFile));
		} catch (IOException e) {
			log.error("Propertyfile {1} not found!", propertyFile);
			System.exit(1);
		}

		controller = new ControllerService();
		log.debug("Instance of {} created", controller.getClass().getSimpleName());
		try {
			controller.init(props);
			log.info("{} initalized", controller.getClass().getSimpleName());
			controller.startup();
			log.info("{} succesfully started", controller.getClass().getSimpleName());
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				@Override
				public void run() {
					System.out.println("Received shutdown signal. Shutting down...");
					try {
						controller.shutdown();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			)
			);
			System.in.read();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				controller.shutdown();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * prints information about console-parameters
	 *
	 * @param options
	 */
	private static void usage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(120, Server.class.getCanonicalName(), null, options, null);
	}
}
