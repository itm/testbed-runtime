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

package de.uniluebeck.itm.tr.rs;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import de.uniluebeck.itm.tr.federatorutils.FederationManager;
import de.uniluebeck.itm.tr.rs.federator.FederatorRS;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.tr.rs.persistence.gcal.GCalRSPersistence;
import de.uniluebeck.itm.tr.rs.persistence.inmemory.InMemoryRSPersistence;
import de.uniluebeck.itm.tr.rs.persistence.jpa.RSPersistenceJPAModule;
import de.uniluebeck.itm.tr.rs.singleurnprefix.ServedNodeUrnsProvider;
import de.uniluebeck.itm.tr.rs.singleurnprefix.SingleUrnPrefixRS;
import de.uniluebeck.itm.tr.rs.singleurnprefix.SingleUrnPrefixSOAPRS;
import de.uniluebeck.itm.tr.util.Logging;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.api.v3.snaa.SNAA;
import org.apache.commons.cli.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.Endpoint;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static com.google.inject.matcher.Matchers.annotatedWith;

@SuppressWarnings("restriction")
public class RSServer {

	static {
		Logging.setDebugLoggingDefaults();
	}

	private static HttpServer server;

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(RSServer.class);

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
				Logger.getRootLogger().setLevel(Level.DEBUG);
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
		int port = Integer.parseInt(props.getProperty("config.port", "8080"));

		startHttpServer(port);

		Iterable<String> rsNames = Splitter.on(",").trimResults().split(props.getProperty("config.rsnames", ""));

		String type, path;
		NodeUrnPrefix nodeUrnPrefix;
		for (String rs : rsNames) {

			type = props.getProperty(rs + ".type", "");
			path = props.getProperty(rs + ".path", "/rs/" + rs);

			if ("singleurnprefix".equals(type)) {

				nodeUrnPrefix = new NodeUrnPrefix(props.getProperty(rs + ".urnprefix", "urn:default:" + rs));
				startSingleUrnPrefixRS(path, nodeUrnPrefix, props, rs);

			} else if ("federator".equals(type)) {

				// endpoint URL -> Set<NodeUrnPrefix>
				ImmutableMap.Builder<URI, ImmutableSet<NodeUrnPrefix>> urnPrefixSetBuilder = ImmutableMap.builder();

				final String federatesString = props.getProperty(rs + ".federates", "");
				final Iterable<String> federates = Splitter.on(",").trimResults().split(federatesString);
				for (String federatedName : federates) {

					final String urnPrefixesString = props.getProperty(rs + "." + federatedName + ".urnprefixes");
					final String rsEndpointUrlString = props.getProperty(rs + "." + federatedName + ".endpointurl", "");

					final ImmutableSet<NodeUrnPrefix> urnPrefixes = parseNodeUrnPrefixSetFromCSV(urnPrefixesString);
					final URI rsEndpointUrl = URI.create(rsEndpointUrlString);

					urnPrefixSetBuilder.put(rsEndpointUrl, urnPrefixes);
				}

				startFederator(path, urnPrefixSetBuilder.build());

			} else {
				log.error("Found unknown type " + type + " for rs name " + rs + ". Ignoring...");
			}

		}

		return server;

	}

	private static void startSingleUrnPrefixRS(final String path,
											   final NodeUrnPrefix urnPrefix,
											   final Properties props,
											   final String propsPrefix)
			throws Exception {

		final String snaaEndpointUrl = props.getProperty(
				propsPrefix + ".snaaendpointurl",
				"http://localhost:8080/snaa/dummy1"
		);
		final String sessionManagementEndpointUrl = props.getProperty(propsPrefix + ".sessionmanagementendpointurl");

		final RSPersistence persistence = createRSPersistence(props, propsPrefix + ".persistence");

		final Injector injector = Guice.createInjector(new Module() {
			@Override
			public void configure(final Binder binder) {

				SNAA snaaInstance = WisebedServiceHelper.getSNAAService(snaaEndpointUrl);

				log.debug("Binding urnPrefix \"{}\", snaaEndpointUrl \"{}\", sessionManagementEndpointUrl \"{}\"",
						urnPrefix, snaaEndpointUrl, sessionManagementEndpointUrl
				);

				final ExecutorService executorService =
						MoreExecutors.getExitingExecutorService((ThreadPoolExecutor) Executors.newCachedThreadPool());

				final TimeLimiter timeLimiter = new SimpleTimeLimiter(executorService);

				binder.bind(TimeLimiter.class)
						.annotatedWith(Names.named("SingleUrnPrefixSOAPRS.timeLimiter"))
						.toInstance(timeLimiter);

				binder.bind(NodeUrnPrefix.class)
						.annotatedWith(Names.named("SingleUrnPrefixSOAPRS.urnPrefix"))
						.toInstance(urnPrefix);

				binder.bind(SNAA.class)
						.toInstance(snaaInstance);

				if (sessionManagementEndpointUrl == null) {

					binder.bind(SessionManagement.class)
							.toProvider(Providers.<SessionManagement>of(null));

					binder.bind(NodeUrn[].class)
							.annotatedWith(Names.named("SingleUrnPrefixSOAPRS.servedNodeUrns"))
							.toProvider(Providers.<NodeUrn[]>of(null));

				} else {

					binder.bind(SessionManagement.class)
							.toInstance(WisebedServiceHelper.getSessionManagementService(sessionManagementEndpointUrl));

					binder.bind(NodeUrn[].class)
							.annotatedWith(Names.named("SingleUrnPrefixSOAPRS.servedNodeUrns"))
							.toProvider(ServedNodeUrnsProvider.class);
				}


				binder.bind(RSPersistence.class)
						.toInstance(persistence);

				binder.bind(RS.class)
						.to(SingleUrnPrefixSOAPRS.class);

				binder.bind(RS.class)
						.annotatedWith(NonWS.class)
						.to(SingleUrnPrefixRS.class);

				binder.bindInterceptor(Matchers.any(), annotatedWith(AuthorizationRequired.class),
						new RSAuthorizationInterceptor(snaaInstance)
				);
			}
		}
		);

		RS rs = injector.getInstance(RS.class);

		HttpContext context = server.createContext(path);
		Endpoint endpoint = Endpoint.create(rs);
		endpoint.publish(context);

		log.debug("Started single urn prefix RS using persistence " + persistence + " on " + server.getAddress() + path
		);

	}

	private static RSPersistence createRSPersistence(Properties props, String propsPrefix) throws Exception {

		String persistenceType = props.getProperty(propsPrefix, "inmemory");

		if ("inmemory".equals(persistenceType)) {
			return new InMemoryRSPersistence();
		} else if ("gcal".equals(persistenceType)) {
			String userName = props.getProperty(propsPrefix + ".gcal.username");
			String password = props.getProperty(propsPrefix + ".gcal.password");
			return new GCalRSPersistence(userName, password);
		} else if ("jpa".equals(persistenceType)) {
			return createPersistenceDB(props, propsPrefix);
		}

		throw new IllegalArgumentException("Persistence type " + persistenceType + " unknown!");
	}

	private static void startHttpServer(int port) throws Exception {

		server = HttpServer.create(new InetSocketAddress(port), 5);
		// limit number of threads to 3 as that should be sufficient for a reservation system ;-)
		server.setExecutor(Executors.newCachedThreadPool(
				new ThreadFactoryBuilder().setNameFormat("RS-Thread %d").build()
		)
		);
		server.start();

	}

	private static ImmutableSet<NodeUrnPrefix> parseNodeUrnPrefixSetFromCSV(String str) {
		String[] split = str.split(",");
		ImmutableSet.Builder<NodeUrnPrefix> setBuilder = ImmutableSet.builder();
		for (String string : split) {
			setBuilder.add(new NodeUrnPrefix(string.trim()));
		}
		return setBuilder.build();
	}

	private static void usage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(120, RSServer.class.getCanonicalName(), null, options, null);
		System.exit(1);
	}

	private static void startFederator(String path, ImmutableMap<URI, ImmutableSet<NodeUrnPrefix>> prefixSet) {

		final Function<URI, RS> uriToRSEndpointFunction = new Function<URI, RS>() {
			@Override
			public RS apply(final URI uri) {
				return WisebedServiceHelper.getRSService(uri.toString());
			}
		};

		final ExecutorService executorService = Executors.newCachedThreadPool(
				new ThreadFactoryBuilder().setNameFormat("FederatorRS-Thread %d").build()
		);

		final FederationManager<RS> federationManager = new FederationManager<RS>(uriToRSEndpointFunction, prefixSet);
		final FederatorRS federator = new FederatorRS(federationManager, executorService);

		HttpContext context = server.createContext(path);
		Endpoint endpoint = Endpoint.create(federator);
		endpoint.publish(context);

		log.debug("Started federator RS on " + server.getAddress() + path);

	}

	private static RSPersistence createPersistenceDB(Properties props, String propsPrefix)
			throws IOException, RSFault_Exception {

		Map<String, String> properties = new HashMap<String, String>();
		for (Object key : props.keySet()) {
			if (!((String) key).startsWith(propsPrefix + "." + "properties")) {
				continue;
			}

			String persistenceKey = ((String) key).replaceAll(propsPrefix + "." + "properties" + ".", "");
			properties.put(persistenceKey, props.getProperty((String) key));
		}

		//set Database-Persistence-TimeZone if defined in config file, if not set default to GMT)
		TimeZone localTimeZone = TimeZone.getTimeZone("GMT");
		if (props.getProperty(propsPrefix + ".timezone") != null) {
			localTimeZone = TimeZone.getTimeZone(props.getProperty(propsPrefix + ".timezone"));
		}

		final Injector injector = Guice.createInjector(new RSPersistenceJPAModule(localTimeZone, properties));
		return injector.getInstance(RSPersistence.class);
	}

}