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

package de.uniluebeck.itm.tr.runtime.cmdline;

import com.google.classpath.ClassPath;
import com.google.classpath.ClassPathFactory;
import com.google.common.collect.ImmutableList;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.application.TestbedApplication;
import de.uniluebeck.itm.tr.XmlTestbedFactory;
import de.uniluebeck.itm.tr.util.Tuple;
import org.apache.commons.cli.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by IntelliJ IDEA. User: bimschas Date: 26.04.2010 Time: 16:50:56 TODO change
 */
public class Main {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(Main.class);
	
	/**
	 * @param args
	 *
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		/*try {

			String baseDir = System.getProperty("basedir");
			String libraryPath = System.getProperty("java.library.path");
			File libDir = new File(baseDir + File.separator + "lib");

			if (baseDir != null && libDir.isDirectory()) {
				System.setProperty("java.library.path", libDir + File.separator);
			}

		} catch (Exception e) {}*/

		start(args);

	}

	public static Tuple<TestbedRuntime, ImmutableList<TestbedApplication>> start(String[] args) throws Exception {

		log.debug("Starting with java.library.path=" + System.getProperty("java.library.path"));

		if (log.isDebugEnabled()) {
			ClassPathFactory factory = new ClassPathFactory();
			ClassPath classPath = factory.createFromJVM();
			for (String packageName : classPath.listResources("")) {
				log.debug(packageName);
			}
		}

		String xmlFile = null;
		String nodeId = null;

		// create the command line parser
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("n", "nodeid", true, "Node ID to start (the id attribute of the node tag)");
		options.addOption("f", "file", true, "The (XML) configuration file");
		options.addOption("v", "verbose", false, "Verbose logging output");
		options.addOption("h", "help", false, "Help output");

		try {

			CommandLine line = parser.parse(options, args);

			if (line.hasOption('v')) {
				Logger.getRootLogger().setLevel(Level.DEBUG);
			}

			if (line.hasOption('h')) {
				usage(options);
			}

			if (line.hasOption('f')) {
				xmlFile = line.getOptionValue('f');
			} else {
				throw new Exception("Please supply -f");
			}

			if (line.hasOption('n')) {
				nodeId = line.getOptionValue('n');
			} else {
				throw new Exception("Please supply -n");
			}

		} catch (Exception e) {
			log.error("Invalid command line: " + e, e);
			usage(options);
		}

		XmlTestbedFactory factory = new XmlTestbedFactory();
		Tuple<TestbedRuntime, ImmutableList<TestbedApplication>> listTuple = factory.create(xmlFile, nodeId);

		// start the testbed runtime
		log.debug("Starting testbed runtime");
		TestbedRuntime runtime = listTuple.getFirst();
		runtime.startServices();

		// start the applications running "on top"
		ImmutableList<TestbedApplication> testbedApplications = listTuple.getSecond();
		for (TestbedApplication testbedApplication : testbedApplications) {
			log.debug("Starting application \"{}\"", testbedApplication.getName());
			testbedApplication.start();
		}

		log.debug("Up and running. Hooray!");

		return new Tuple<TestbedRuntime, ImmutableList<TestbedApplication>>(runtime, testbedApplications);

	}

	private static void usage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(Main.class.getCanonicalName(), options);
		System.exit(1);
	}

}
