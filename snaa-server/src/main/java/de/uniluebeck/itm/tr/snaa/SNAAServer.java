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

package de.uniluebeck.itm.tr.snaa;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import de.uniluebeck.itm.tr.federatorutils.FederationManager;
import de.uniluebeck.itm.tr.snaa.dummy.DummySNAA;
import de.uniluebeck.itm.tr.snaa.federator.FederatorSNAA;
import de.uniluebeck.itm.tr.snaa.jaas.JAASSNAA;
import de.uniluebeck.itm.tr.snaa.shibboleth.ShibbolethProxy;
import de.uniluebeck.itm.tr.snaa.shibboleth.ShibbolethSNAAImpl;
import de.uniluebeck.itm.tr.snaa.shibboleth.ShibbolethSNAAModule;
import de.uniluebeck.itm.tr.snaa.wisebed.WisebedSnaaFederator;
import de.uniluebeck.itm.tr.util.Logging;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.snaa.SNAA;
import eu.wisebed.testbed.api.snaa.authorization.AlwaysAllowAuthorization;
import eu.wisebed.testbed.api.snaa.authorization.AttributeBasedAuthorization;
import eu.wisebed.testbed.api.snaa.authorization.IUserAuthorization;
import eu.wisebed.testbed.api.snaa.authorization.datasource.AuthorizationDataSource;
import eu.wisebed.testbed.api.snaa.authorization.datasource.ShibbolethDataSource;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.xml.ws.Endpoint;
import java.io.FileReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Sets.newHashSet;

@SuppressWarnings("restriction")
public class SNAAServer {

	static {
		Logging.setLoggingDefaults();
	}

	private static final Logger log = LoggerFactory.getLogger(SNAAServer.class);

	private static final String AUTHORIZATION_ATT = ".authorization";

	private static final String AUTHORIZATION_KEY_ATT = ".key";

	private static final String AUTHORIZATION_VAL_ATT = ".value";

	private static final String AUTHORIZATION_DATA_SOURCE = ".datasource";

	private static final String AUTHORIZATION_DATA_SOURCE_USERNAME = ".username";

	private static final String AUTHORIZATION_DATA_SOURCE_PASSWORD = ".password";

	private static final String AUTHORIZATION_DATA_SOURCE_URL = ".url";

	private static SNAA federator;

	private static HttpServer server;

	private static Injector shibbolethInjector = Guice.createInjector(new ShibbolethSNAAModule());

	private enum FederatorType {
		GENERIC, WISEBED
	}

	public static void main(String[] args) throws Exception {

		String propertyFile = null;

		// create the command line parser
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("f", "file", true, "The properties file");
		options.addOption("v", "verbose", false, "Verbose logging output");
		options.addOption("h", "help", false, "Help output");

		try {
			CommandLine line = parser.parse(options, args);

			if (line.hasOption('v')) {
				org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.DEBUG);
			}

			if (line.hasOption('h')) {
				usage(options);
			}

			if (line.hasOption('f')) {
				propertyFile = line.getOptionValue('f');
			} else {
				throw new Exception("Please supply -f");
			}

		} catch (Exception e) {
			log.error("Invalid command line: " + e, e);
			usage(options);
		}

		Properties props = new Properties();
		props.load(new FileReader(propertyFile));
		startFromProperties(props);

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				log.info("Received shutdown signal. Shutting down...");
				server.stop(3);
			}
		}
		)
		);

	}


	public static HttpServer startFromProperties(Properties props) throws Exception {
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.DEBUG);
		int port = Integer.parseInt(props.getProperty("config.port", "8080"));

		startHttpServer(port);

		Iterable<String> snaaNames = Splitter.on(",").trimResults().split(props.getProperty("config.snaas", ""));

		//set optional proxy for shibboleth
		ShibbolethProxy shibbolethProxy;
		shibbolethProxy = setOptionalShibbolethProxy(props);

		NodeUrnPrefix nodeUrnPrefix;
		String type, path;
		for (String snaaName : snaaNames) {

			type = props.getProperty(snaaName + ".type", "");
			path = props.getProperty(snaaName + ".path", "/snaa/" + snaaName);

			if ("dummy".equals(type)) {

				startDummySNAA(path);

			} else if ("shibboleth".equals(type)) {

				nodeUrnPrefix =
						new NodeUrnPrefix(props.getProperty(snaaName + ".urnprefix", "urn:default:" + snaaName));
				IUserAuthorization authorization = getAuthorizationModule(snaaName, props);
				String secretAuthkeyUrl = props.getProperty(snaaName + ".authorization.url");

				startShibbolethSNAA(
						path,
						nodeUrnPrefix,
						secretAuthkeyUrl,
						authorization,
						shibbolethInjector,
						shibbolethProxy
				);

			} else if ("jaas".equals(type)) {

				nodeUrnPrefix =
						new NodeUrnPrefix(props.getProperty(snaaName + ".urnprefix", "urn:default:" + snaaName));
				String jaasModuleName = props.getProperty(snaaName + ".module", null);
				String jaasConfigFile = props.getProperty(snaaName + ".configfile", null);

				IUserAuthorization authorization = getAuthorizationModule(snaaName, props);

				if (jaasConfigFile == null) {
					throw new Exception(("Supply a value for " + snaaName + ".configfile"));
				}

				if (jaasModuleName == null) {
					throw new Exception(("Supply a value for " + snaaName + ".module"));
				}

				startJAASSNAA(
						path,
						nodeUrnPrefix,
						jaasModuleName,
						jaasConfigFile,
						authorization
				);

			} else if ("wisebed-federator".equals(type) || "federator".equals(type)) {

				FederatorType fedType = FederatorType.GENERIC;
				String secretAuthKeyUrl = null;

				if ("wisebed-federator".equals(type)) {
					fedType = FederatorType.WISEBED;
					secretAuthKeyUrl = props.getProperty(snaaName + ".authentication.url");
				}

				// endpoint url -> Set<NodeUrnPrefix>
				ImmutableMap.Builder<URI, ImmutableSet<NodeUrnPrefix>> prefixSetBuilder = ImmutableMap.builder();

				final Iterable<String> federates = Splitter.on(",").trimResults().split(
						props.getProperty(snaaName + ".federates", "")
				);
				for (String federatedName : federates) {

					final String urnPrefixesProp = props.getProperty(snaaName + "." + federatedName + ".urnprefixes");
					final ImmutableSet<NodeUrnPrefix> urnPrefixes = parseNodeUrnPrefixesSetFromCSV(urnPrefixesProp);
					final URI snaaEndpointUrl = new URI(
							props.getProperty(snaaName + "." + federatedName + ".endpointurl")
					);

					prefixSetBuilder.put(snaaEndpointUrl, urnPrefixes);
				}


				final ImmutableMap<URI, ImmutableSet<NodeUrnPrefix>> endpointUrlsToUrnPrefixesMap =
						prefixSetBuilder.build();
				final Function<URI, SNAA> uriToSNAAEndpointFunction = new Function<URI, SNAA>() {
					@Override
					public SNAA apply(@Nullable final URI uri) {
						assert uri != null;
						return WisebedServiceHelper.getSNAAService(uri.toString());
					}
				};

				final FederationManager<SNAA> federationManager = new FederationManager<SNAA>(
						uriToSNAAEndpointFunction,
						endpointUrlsToUrnPrefixesMap
				);

				startFederator(
						fedType,
						path,
						secretAuthKeyUrl,
						shibbolethInjector,
						shibbolethProxy,
						federationManager
				);

			} else {
				log.error("Found unknown type " + type + " for snaa name " + snaaName + ". Ignoring...");
			}

		}

		return server;

	}

	private static ImmutableSet<NodeUrnPrefix> parseNodeUrnPrefixesSetFromCSV(final String urnPrefixesProp) {
		final Iterable<String> urnPrefixesStrings = Splitter.on(",").trimResults().split(urnPrefixesProp);
		final Iterable<NodeUrnPrefix> nodeUrnPrefixesIterable = Iterables.transform(
				urnPrefixesStrings,
				new Function<String, NodeUrnPrefix>() {
					@Override
					public NodeUrnPrefix apply(@Nullable final String s) {
						return new NodeUrnPrefix(s);
					}
				}
		);
		return ImmutableSet.copyOf(nodeUrnPrefixesIterable);
	}

	private static ShibbolethProxy setOptionalShibbolethProxy(Properties props) {
		String shibbolethProxyHost = props.getProperty("config.shibboleth.proxyHost");
		String shibbolethProxyPort = props.getProperty("config.shibboleth.proxyPort");
		if (shibbolethProxyHost != null && shibbolethProxyPort != null) {
			return new ShibbolethProxy(shibbolethProxyHost, Integer.parseInt(shibbolethProxyPort));
		}
		return null;
	}

	private static IUserAuthorization getAuthorizationModule(String snaaName, Properties props)
			throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {

		String authorizationClassName = props.getProperty(
				snaaName + ".authorization_class",
				AlwaysAllowAuthorization.class.getCanonicalName()
		);

		IUserAuthorization authorization = (IUserAuthorization) Class.forName(authorizationClassName).newInstance();

		if (AttributeBasedAuthorization.class.getCanonicalName().equals(authorizationClassName)) {
			Map<String, String> attributes = createAuthorizationAttributeMap(snaaName, props);
			((AttributeBasedAuthorization) authorization).setAttributes(attributes);
			((AttributeBasedAuthorization) authorization).setDataSource(getAuthorizationDataSource(snaaName, props));
		}

		return authorization;
	}

	private static void startJAASSNAA(String path, NodeUrnPrefix urnprefix, String jaasModuleName,
									  String jaasConfigFile,
									  IUserAuthorization authorization) {

		log.debug("Starting JAAS SNAA, path [" + path + "], prefix[" + urnprefix + "], jaasConfigFile["
				+ jaasConfigFile + "], jaasModuleName[" + jaasModuleName + "], authorization[" + authorization + "]"
		);

		System.setProperty("java.security.auth.login.config", jaasConfigFile);

		SNAA jaasSnaa = new JAASSNAA(urnprefix, jaasModuleName, authorization);

		HttpContext context = server.createContext(path);
		Endpoint endpoint = Endpoint.create(jaasSnaa);
		endpoint.publish(context);

		log.debug("Started JAAS SNAA on " + server.getAddress() + path);
	}

	private static void startDummySNAA(String path) {

		DummySNAA dummySNAA = new DummySNAA();

		HttpContext context = server.createContext(path);
		Endpoint endpoint = Endpoint.create(dummySNAA);
		endpoint.publish(context);

		log.info("Started dummy SNAA on " + server.getAddress() + path);

	}

	private static void startShibbolethSNAA(String path, NodeUrnPrefix prefix, String secretKeyURL,
											IUserAuthorization authorization, Injector injector,
											ShibbolethProxy shibbolethProxy) {

		log.debug("Starting Shibboleth SNAA, path [" + path + "], prefix[" + prefix + "], secretKeyURL[" + secretKeyURL
				+ "]"
		);

		ShibbolethSNAAImpl shibbolethSNAA = new ShibbolethSNAAImpl(
				newHashSet(prefix),
				secretKeyURL,
				authorization,
				injector,
				shibbolethProxy
		);

		HttpContext context = server.createContext(path);
		Endpoint endpoint = Endpoint.create(shibbolethSNAA);
		endpoint.publish(context);

		log.debug("Started shibboleth SNAA on " + server.getAddress() + path);

	}

	private static void startFederator(FederatorType type, String path, String secretAuthKeyURL, Injector injector,
									   ShibbolethProxy shibbolethProxy, FederationManager<SNAA> federationManager) {

		if (log.isDebugEnabled()) {
			log.debug("Starting Federator with the following prefixes: ");
			for (FederationManager.Entry<SNAA> entry : federationManager.getEntries()) {
				log.debug("SNAA endpoint URL =" + entry.endpointUrl + " <=> Node URN prefixes = " + entry.urnPrefixes);
			}
		}

		switch (type) {
			case GENERIC:
				federator = new FederatorSNAA(federationManager);
				break;
			case WISEBED:
				federator = new WisebedSnaaFederator(federationManager, secretAuthKeyURL, injector, shibbolethProxy);
				break;
		}

		HttpContext context = server.createContext(path);
		Endpoint endpoint = Endpoint.create(federator);
		endpoint.publish(context);

		log.debug("Started " + type + " federator SNAA on " + server.getAddress() + path);

	}

	private static void startHttpServer(int port) throws Exception {

		server = HttpServer.create(new InetSocketAddress(port), 5);
		// bind to a maximum of three threads which should be enough for an snaa system
		server.setExecutor(new ThreadPoolExecutor(
				1,
				3,
				60L, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>(),
				new ThreadFactoryBuilder().setNameFormat("SNAA-Thread %d").build()
		)
		);
		server.start();

	}

	private static void usage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(120, SNAAServer.class.getCanonicalName(), null, options, null);
		System.exit(1);
	}

	private static AuthorizationDataSource getAuthorizationDataSource(String snaaName, Properties props)
			throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		for (Object o : props.keySet()) {
			String dataSourceName = snaaName + AUTHORIZATION_ATT + AUTHORIZATION_DATA_SOURCE;
			if (o.equals(dataSourceName)) {
				AuthorizationDataSource dataSource =
						(AuthorizationDataSource) Class.forName(props.getProperty((String) o)).newInstance();

				String dataSourceUsername = props.getProperty(dataSourceName + AUTHORIZATION_DATA_SOURCE_USERNAME);
				String dataSourcePassword = props.getProperty(dataSourceName + AUTHORIZATION_DATA_SOURCE_PASSWORD);
				String dataSourceUrl = props.getProperty(dataSourceName + AUTHORIZATION_DATA_SOURCE_URL);

				if (dataSourceUsername != null) {
					dataSource.setUsername(dataSourceUsername);
				}
				if (dataSourcePassword != null) {
					dataSource.setPassword(dataSourcePassword);
				}
				if (dataSourceUrl != null) {
					dataSource.setUrl(dataSourceUrl);
				}

				return dataSource;
			}
		}
		//set default
		return new ShibbolethDataSource();
	}

	private static Map<String, String> createAuthorizationAttributeMap(String snaaName, Properties props) {
		Map<String, String> attributes = new HashMap<String, String>();

		List<String> keys = new LinkedList<String>();
		//getting keys from properties
		for (Object o : props.keySet()) {
			if (((String) o).startsWith(snaaName + AUTHORIZATION_ATT) && ((String) o).endsWith(AUTHORIZATION_KEY_ATT)) {
				keys.add((String) o);
			}
		}

		for (String k : keys) {
			String key = props.getProperty(k);
			//getting plain key-number from properties
			String plainKeyProperty = k.replaceAll(snaaName + AUTHORIZATION_ATT + ".", "");
			plainKeyProperty = plainKeyProperty.replaceAll(AUTHORIZATION_KEY_ATT, "");

			String keyPrefix = snaaName + AUTHORIZATION_ATT + "." + plainKeyProperty;

			//building value-property-string
			String value = props.getProperty(keyPrefix + AUTHORIZATION_VAL_ATT);

			//finally put key and values
			attributes.put(key, value);
		}

		return attributes;
	}
}
