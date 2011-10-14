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

package de.uniluebeck.itm.wisebed.cmdlineclient;

import bsh.EvalError;
import bsh.Interpreter;
import de.uniluebeck.itm.tr.util.Logging;
import org.apache.commons.cli.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class WisebedBeanShellLauncher {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(WisebedBeanShellLauncher.class);

	private static List<String> importsForBeanShell = new ArrayList<String>();

	static {
		importsForBeanShell.add("import eu.wisebed.api.controller.*;");
		importsForBeanShell.add("import eu.wisebed.api.common.*;");
		importsForBeanShell.add("import eu.wisebed.api.rs.*;");
		importsForBeanShell.add("import eu.wisebed.api.snaa.*;");
		importsForBeanShell.add("import eu.wisebed.api.sm.*;");
		importsForBeanShell.add("import eu.wisebed.api.wsn.*;");

		importsForBeanShell.add("import eu.wisebed.testbed.api.wsn.WSNServiceHelper;");
		importsForBeanShell.add("import eu.wisebed.testbed.api.rs.RSServiceHelper;");
		importsForBeanShell.add("import eu.wisebed.testbed.api.snaa.helpers.SNAAServiceHelper;");

		importsForBeanShell.add("import de.uniluebeck.itm.wisebed.cmdlineclient.*;");
		importsForBeanShell.add("import de.uniluebeck.itm.wisebed.cmdlineclient.jobs.*");
		importsForBeanShell.add("import de.uniluebeck.itm.wisebed.cmdlineclient.protobuf.*");
		importsForBeanShell.add("import de.uniluebeck.itm.wisebed.cmdlineclient.wrapper.*");

		importsForBeanShell.add("import de.uniluebeck.itm.tr.util.*;");

		importsForBeanShell.add("import java.util.*;");
		importsForBeanShell.add("import java.util.concurrent.Future;");
		importsForBeanShell.add("import java.util.concurrent.TimeUnit;");
		importsForBeanShell.add("import java.util.concurrent.TimeoutException;");
		importsForBeanShell.add("import java.util.concurrent.ExecutionException;");

		importsForBeanShell.add("import de.itm.uniluebeck.tr.wiseml.WiseMLHelper;");

		importsForBeanShell.add("import javax.xml.datatype.DatatypeFactory;");
		importsForBeanShell.add("import javax.xml.datatype.XMLGregorianCalendar;");

		importsForBeanShell.add("import com.google.common.base.*;");
		importsForBeanShell.add("import com.google.common.collect.*;");

		importsForBeanShell.add("import org.joda.time.*");
	}


	public static void main(String[] args) throws IOException, EvalError {

		Logging.setLoggingDefaults(Level.INFO, new PatternLayout("%-11d{HH:mm:ss,SSS} %-5p - %m%n"));

		File beanShellFile = null;

		// create the command line parser
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("f", "file", true, "The bean shell script to execute");
		options.addOption("p", "properties", true, "A properties file to pass to the script as environment properties");
		options.addOption("v", "verbose", false, "Verbose logging output");
		options.addOption("h", "help", false, "Help output");

		try {
			CommandLine line = parser.parse(options, args);

			if (line.hasOption('v')) {
				Logger.getRootLogger().setLevel(Level.DEBUG);
			} else {
				Logger.getRootLogger().setLevel(Level.INFO);
			}

			if (line.hasOption('h')) {
				usage(options);
			}

			if (line.hasOption('p')) {
				parseAndSetProperties(line.getOptionValue('p'));
			}

			log.debug("Option for -f: " + line.getOptionValue('f'));

			if (line.hasOption('f')) {
				beanShellFile = new File(line.getOptionValue('f'));
			} else {
				throw new Exception("Please supply -f");
			}

		} catch (Exception e) {
			log.error("Invalid command line: " + e, e);
			usage(options);
		}

		// Run the bean shell file
		Interpreter i = new Interpreter();

		// Add a logger
		org.slf4j.Logger bshLogger = LoggerFactory.getLogger("BeanShellScript");
		log.debug("Adding logger to beanshell, use 'log' variable, api is like slf4j's");
		i.set("log", bshLogger);

		// Add a helper
		BeanShellHelper helper = new BeanShellHelper();
		log.debug("Adding helper to beanshell, use 'helper' variable");
		i.set("helper", helper);

		// Add some convenience imports
		for (String importStatement : importsForBeanShell) {
			log.debug("Adding the following default import: " + importStatement);
			i.eval(importStatement);
		}

		// Run the script
		try {
			assert beanShellFile != null;
			i.source(beanShellFile.getAbsolutePath());
		} catch (Exception e) {
			log.error("Error while running bean shell script: " + e, e);
			System.exit(1);
		}
	}

	public static void parseAndSetProperties(final String propertiesFileName) throws IOException {

		File propertiesFile = new File(propertiesFileName);

		if (!propertiesFile.exists() || !propertiesFile.canRead() || !propertiesFile.isFile()) {
			log.error("Properties file \"" +
					propertiesFile.getAbsolutePath() +
					"\" is either not existing, not a file or not readable!"
			);
			System.exit(1);
		}

		Properties properties = new Properties();
		properties.load(new FileInputStream(propertiesFile));

		for (Object key : properties.keySet()) {

			final String propertyKey = ((String) key).trim();
			final String propertyValue = ((String) properties.get(key)).trim();

			System.setProperty(propertyKey, propertyValue);
		}
	}

	private static void usage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(120, WisebedBeanShellLauncher.class.getCanonicalName(), null, options, null);
		System.exit(1);
	}

}
