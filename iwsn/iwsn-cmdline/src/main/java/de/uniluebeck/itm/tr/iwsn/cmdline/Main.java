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

import com.google.common.collect.ImmutableList;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.application.TestbedApplication;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.tr.util.Tuple;
import org.apache.commons.cli.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

public class Main {

    public static void main(String[] args) throws Exception {
        start(args);
    }

    public static Tuple<TestbedRuntime, ImmutableList<TestbedApplication>> start(String[] args) throws Exception {

        String xmlFile = null;
        String nodeId = null;

        // create the command line parser
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption("n", "nodeid", true, "Node ID to start (the id attribute of the node tag), if not set autodetect is tried");
        options.addOption("f", "file", true, "The (XML) configuration file");
        options.addOption("v", "verbose", false, "Verbose logging output (equal to -l DEBUG)");
        options.addOption("l", "logging", true,
                "Set logging level (one of [" + Level.TRACE + "," + Level.DEBUG + "," + Level.INFO + "," + Level.WARN + "," + Level.ERROR + "])"
        );
        options.addOption("h", "help", false, "Help output");

        Logging.setLoggingDefaults();

        final org.slf4j.Logger log = LoggerFactory.getLogger(Main.class);

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
                xmlFile = line.getOptionValue('f');
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

        String[] nodeIds;
        if (nodeId == null) {
            nodeIds = new String[]{InetAddress.getLocalHost().getCanonicalHostName(), InetAddress.getLocalHost().getHostName()};
        } else {
            nodeIds = new String[]{nodeId};
        }
        XmlTestbedFactory factory = new XmlTestbedFactory();
        Tuple<TestbedRuntime, ImmutableList<TestbedApplication>> listTuple = factory.create(xmlFile, nodeIds);

        // start the testbed runtime
        log.debug("Starting testbed runtime");

        final TestbedRuntime runtime = listTuple.getFirst();
        runtime.startServices();

        // start the applications running "on top"
        final ImmutableList<TestbedApplication> testbedApplications = listTuple.getSecond();
        for (TestbedApplication testbedApplication : testbedApplications) {
            log.debug("Starting application \"{}\"", testbedApplication.getName());
            testbedApplication.start();
        }

        log.debug("Up and running. Hooray!");

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                log.info("Received shutdown signal.");
                log.info("Stopping testbed runtime applications...");
                for (TestbedApplication testbedApplication : testbedApplications) {
                    try {
                        testbedApplication.stop();
                    } catch (Exception e) {
                        log.warn("Caught exception when shutting down testbed runtime application " + testbedApplication.getName() + ": {}", e);
                    }
                }
                log.info("Stopped testbed runtime applications!");
                log.info("Stopping testbed runtime...");
                runtime.stopServices();
                log.info("Stopped testbed runtime!");
            }
        }));

        return new Tuple<TestbedRuntime, ImmutableList<TestbedApplication>>(runtime, testbedApplications);

    }

    private static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(120, Main.class.getCanonicalName(), null, options, null);
        System.exit(1);
    }
}
