package de.uniluebeck.itm.tr.snaa.shibboleth;

import com.google.common.net.HostAndPort;
import com.google.inject.Guice;
import de.uniluebeck.itm.tr.snaa.SNAAServiceConfig;
import de.uniluebeck.itm.util.TimeDiff;
import de.uniluebeck.itm.util.logging.Logging;
import de.uniluebeck.itm.util.propconf.PropConfModule;
import org.apache.commons.cli.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.net.URL;
import java.util.Properties;

/*
 * Copyright (C) 2010 by Dennis Pfisterer. This is free software; you can redistribute it and/or modify it under the
 * terms of the BSD License. Refer to the licence.txt file in the root of the source tree for further details.
 */

public class ShibbolethAuthenticatorMain {

	static {
		Logging.setLoggingDefaults();
	}

	private static Logger log = Logger.getLogger(ShibbolethAuthenticatorMain.class);

	public static void main(String[] args) throws Exception {

		String url = "https://gridlab23.unibe.ch/portal/SNA/secretUserKey";
		boolean listIdps = false;
		boolean recheckAuth = false;
		HostAndPort proxy = null;
		String userAtIdpDomain = null;
		String password = null;

		// create the command line parser
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("u", "user", true, "Username in the form user@idphost");
		options.addOption("p", "pass", true, "Password");
		options.addOption("l", "url", true, "The URL to use, defaults to " + url);
		options.addOption("q", "proxyhost", true, "The IP/hostname of the proxy to use");
		options.addOption("w", "proxyport", true, "The portname of the proxy to use");
		options.addOption("v", "verbose", false, "Verbose logging output");
		options.addOption("x", "listidps", false, "List IDP URLs");
		options.addOption("z", "recheck", false, "Recheck the authentication every minute until aborted");
		options.addOption("h", "help", false, "Help output");

		try {
			CommandLine line = parser.parse(options, args);

			if (line.hasOption('v')) {
				Logger.getRootLogger().setLevel(Level.DEBUG);
			}

			if (line.hasOption('h')) {
				usage(options);
			}

			if (line.hasOption('l')) {
				url = line.getOptionValue('l');
			}

			if (line.hasOption('q') && line.hasOption('w')) {
				proxy = HostAndPort.fromParts(line.getOptionValue('q'), Integer.parseInt(line.getOptionValue('w')));
			}

			if (line.hasOption("z")) {
				recheckAuth = true;
			}

			if (line.hasOption('x')) {
				listIdps = true;
			} else {

				if (!line.hasOption('u') || !line.hasOption('p')) {
					throw new Exception("Please supply username/password");
				}

				if (line.hasOption('u')) {
					userAtIdpDomain = line.getOptionValue('u');
				}

				if (line.hasOption('p')) {
					password = line.getOptionValue('p');
				}
			}

		} catch (Exception e) {
			log.fatal("Invalid command line: " + e, e);
			usage(options);
		}


		final String finalUrl = url;
		final HostAndPort finalProxy = proxy;
		final Properties properties = new Properties();
		properties.put(SNAAServiceConfig.SHIBBOLETH_URL, finalUrl);
		properties.put(SNAAServiceConfig.SHIBBOLETH_PROXY, finalProxy == null ? "" : finalProxy.toString());
		final ShibbolethAuthenticator sa = Guice
				.createInjector(
						new PropConfModule(properties, SNAAServiceConfig.class),
						new ShibbolethAuthenticatorModule()
				).getInstance(ShibbolethAuthenticator.class);
		sa.setUserAtIdpDomain(userAtIdpDomain);
		sa.setPassword(password);

		if (listIdps) {
			System.out.println("-----------------------------------------------");
			System.out.println("Available IDPs: ");
			System.out.println("-----------------------------------------------");
			for (URL idp : sa.getIDPs()) {
				System.out.println("\t" + idp);
			}

		} else {
			sa.authenticate();
			System.err.println("Authentication " + (sa.isAuthenticated() ? "succeeded" : "failed"));
			if (sa.isAuthenticated()) {
				System.err.println("Secret user key: " + sa.getAuthenticationPageContent());
			}

			TimeDiff time = new TimeDiff();
			while (recheckAuth && sa.isAuthenticated()) {
				Thread.sleep(60 * 1000);
				sa.checkForTimeout();
				System.err.println("Still authenticated after " + time.m() + " minutes.");
			}

			if (!sa.isAuthenticated()) {
				System.err.println("Authentication invalid after " + time.m() + " minutes.");
			}
		}
	}

	private static void usage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(120, ShibbolethAuthenticatorMain.class.getCanonicalName(), null, options, null);
		System.exit(1);
	}

}
