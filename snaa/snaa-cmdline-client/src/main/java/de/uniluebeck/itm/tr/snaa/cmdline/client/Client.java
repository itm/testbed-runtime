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

package de.uniluebeck.itm.tr.snaa.cmdline.client;

import eu.wisebed.testbed.api.snaa.helpers.SNAAServiceHelper;
import eu.wisebed.api.snaa.*;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Client {
	private static final Logger log = LoggerFactory.getLogger(Client.class);

	enum Operation {
		authenticate, authorize;
	}

	private static final String defaultSnaaUrl = "http://127.0.0.1:8080/snaa/dummy1";

	private static final String defaultUrnPrefix = "urn:wisebed:dummy";

	private static final String defaultAction = "reserve";

	private static final String defaultOperation = Operation.authenticate.toString();

	private static final String defaultSecretAuthenticationKey = "dummy-secret-key";

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) {
		URL url = null;
		String username = null;
		String password = null;
		String urnPrefix = null;
		String action = null;
		Operation operation = null;
		String secretAuthenticationKey = null;

		CommandLineParser parser = new PosixParser();
		Options options = new Options();

		options.addOption("l", "url", true, "URL of the SNAA, defaults to " + defaultSnaaUrl);
		options.addOption("u", "username", true, "Username");
		options.addOption("p", "password", true, "Password");
		options.addOption("o", "operation", true, "Operation to perform, possible values: "
				+ Arrays.toString(Operation.values()));
		options.addOption("a", "action", true, "Action string for authorization operation, defaults to: "
				+ defaultAction);
		options.addOption("s", "secretauthkey", true, "Secret auth key for authorization option, defauls to "
				+ defaultSecretAuthenticationKey);
		options.addOption("x", "urnprefix", true, "URN Prefix, defaults to " + defaultUrnPrefix);
		options.addOption("v", "verbose", false, "Verbose logging output");
		options.addOption("h", "help", false, "Help output");

		try {

			CommandLine line = parser.parse(options, args);

			if (line.hasOption('v'))
				org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.DEBUG);

			if (line.hasOption('h'))
				printUsageAndExit(options);

			url = new URL(getOptionalArgument(line, 'l', defaultSnaaUrl));
			urnPrefix = getOptionalArgument(line, 'x', defaultUrnPrefix);
			username = getMandatoryArgument(line, 'u');
			password = getMandatoryArgument(line, 'p');
			action = getOptionalArgument(line, 'a', defaultAction);
			secretAuthenticationKey = getOptionalArgument(line, 's', defaultSecretAuthenticationKey);
			operation = Operation.valueOf(getOptionalArgument(line, 'a', defaultOperation));

		} catch (Exception e) {
			log.error("Invalid command line: " + e);
			printUsageAndExit(options);
		}

		SNAA port = SNAAServiceHelper.getSNAAService(url.toString());

		if (operation == Operation.authenticate) {
			AuthenticationTriple auth1 = new AuthenticationTriple();

			auth1.setUrnPrefix(urnPrefix);
			auth1.setUsername(username);
			auth1.setPassword(password);

			List<AuthenticationTriple> authTriples = new ArrayList<AuthenticationTriple>();
			authTriples.add(auth1);

			System.out.println("Authenticating username[" + username + "], urnprefix[" + urnPrefix + "] at [" + url
					+ "]");
			try {
				List<SecretAuthenticationKey> list = port.authenticate(authTriples);

				System.out.println("Authentication suceeded, secret authentication key(s): ");
				for (SecretAuthenticationKey sak : list)
					System.out.println("\tuser[" + sak.getUsername() + "], urnprefix[" + sak.getUrnPrefix() + "], key["
							+ sak.getSecretAuthenticationKey() + "]");

			} catch (AuthenticationExceptionException e) {
				System.out.println("Authentication failed [" + e + "]");
				e.printStackTrace();
			} catch (SNAAExceptionException e) {
				System.out.println("Server reported error, authentication failed [" + e + "]");
				e.printStackTrace();
			}

		} else if (operation == Operation.authorize) {
			List<SecretAuthenticationKey> saks = new ArrayList<SecretAuthenticationKey>();
			SecretAuthenticationKey sak = new SecretAuthenticationKey();

			sak.setSecretAuthenticationKey(secretAuthenticationKey);
			saks.add(sak);

			try {
				Action actionObj = new Action();
				actionObj.setAction(action);
				boolean authorized = port.isAuthorized(saks, actionObj);
				System.out.println("Authorization " + (authorized ? "suceeded" : "failed"));

			} catch (SNAAExceptionException e) {
				System.out.println("Authorization failed, server reported error [" + e + "]");
				e.printStackTrace();
			}

		}

	}

	private static void printUsageAndExit(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(120, Client.class.getCanonicalName(), null, options, null);
		System.exit(1);
	}

	private static String getMandatoryArgument(CommandLine line, char argument) throws Exception {
		String tmp = getOptionalArgument(line, argument, null);
		if (tmp != null)
			return line.getOptionValue(argument);

		throw new Exception("Please supply -" + argument);
	}

	private static String getOptionalArgument(CommandLine line, char argument, String defaultValue) throws Exception {
		if (line.hasOption(argument))
			return line.getOptionValue(argument);
		else
			return defaultValue;
	}

}
