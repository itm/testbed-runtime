package eu.wisebed.shibboauth;

import eu.wisebed.tools.TimeDiff;
import org.apache.commons.cli.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.net.URL;

/*
 * Copyright (C) 2010 by Dennis Pfisterer. This is free software; you can redistribute it and/or modify it under the
 * terms of the BSD License. Refer to the licence.txt file in the root of the source tree for further details.
 */

public class Main {

	private static Logger log = Logger.getLogger(Main.class);

	public static void main(String[] args) throws Exception {
		String url = "https://gridlab23.unibe.ch/portal/SNA/secretUserKey";
		boolean listIdps = false;
		boolean recheckAuth = false;
		ShibbolethAuthenticator sa = new ShibbolethAuthenticator();

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

			if (line.hasOption('v'))
				Logger.getRootLogger().setLevel(Level.DEBUG);

			if (line.hasOption('h'))
				usage(options);

			if (line.hasOption('l'))
				url = line.getOptionValue('l');

			if (line.hasOption('q') && line.hasOption('w'))
				sa.setProxy(line.getOptionValue('q'), Integer.parseInt(line.getOptionValue('w')));

			if (line.hasOption("z"))
				recheckAuth = true;

			if (line.hasOption('x')) {
				listIdps = true;
			} else {
				if (!line.hasOption('u') || !line.hasOption('p'))
					throw new Exception("Please supply username/password");

				if (line.hasOption('u')) {
					String user = line.getOptionValue('u');

					int atIndex = user.indexOf('@');
					if (atIndex == -1) {
						log.fatal("Username must be like username@idphost");
						throw new Exception("Username must be in like username@idphost");
					}

					sa.setUsername(user.substring(0, atIndex));
					sa.setIdpDomain(user.substring(atIndex + 1));
				}

				if (line.hasOption('p'))
					sa.setPassword(line.getOptionValue('p'));
			}

		} catch (Exception e) {
			log.fatal("Invalid command line: " + e, e);
			usage(options);
		}

		sa.setUrl(url);

		if (listIdps) {
			System.out.println("-----------------------------------------------");
			System.out.println("Available IDPs: ");
			System.out.println("-----------------------------------------------");
			for (URL idp : sa.getIDPs())
				System.out.println("\t" + idp);

		} else {
			sa.authenticate();
			System.err.println("Authentication " + (sa.isAuthenticated() ? "suceeded" : "failed"));
			if (sa.isAuthenticated())
				System.err.println("Secret user key: " + sa.getAuthenticationPageContent());

			TimeDiff time = new TimeDiff();
			while (recheckAuth && sa.isAuthenticated()) {
				Thread.sleep(60 * 1000);
				sa.checkForTimeout();
				if (sa.isAuthenticated())
					System.err.println("Still authenticated after " + time.m() + " mins.");
				else
					System.err.println("Authentication invalid after " + time.m() + " mins.");
			}

		}
	}

	private static void usage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(120, Main.class.getCanonicalName(), null, options, null);
		System.exit(1);
	}

}
