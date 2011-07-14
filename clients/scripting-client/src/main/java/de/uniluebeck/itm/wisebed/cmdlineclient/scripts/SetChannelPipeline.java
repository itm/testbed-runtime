package de.uniluebeck.itm.wisebed.cmdlineclient.scripts;

import com.google.common.base.Splitter;
import de.itm.uniluebeck.tr.wiseml.WiseMLHelper;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.wisebed.cmdlineclient.*;
import de.uniluebeck.itm.wisebed.cmdlineclient.jobs.JobResult;
import de.uniluebeck.itm.wisebed.cmdlineclient.wrapper.WSNAsyncWrapper;
import eu.wisebed.api.common.KeyValuePair;
import eu.wisebed.api.sm.SecretReservationKey;
import eu.wisebed.api.wsn.ChannelHandlerConfiguration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.RuntimeErrorException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static de.uniluebeck.itm.wisebed.cmdlineclient.BeanShellHelper.parseSecretReservationKeys;

public class SetChannelPipeline {

	private static final Logger log = LoggerFactory.getLogger(SetChannelPipeline.class);

	public static void main(String[] args) throws Exception {

		String propertiesFile = args[0];
		WisebedBeanShellLauncher.parseAndSetProperties(propertiesFile);

		Logging.setLoggingDefaults();

		final String useProtobufStr = System.getProperty("testbed.useprotobuf");
		final boolean useProtobuf = useProtobufStr != null ? Boolean.parseBoolean(useProtobufStr) : false;
		final String secretReservationKeys = System.getProperty("testbed.secretreservationkeys");
		final String sessionManagementEndpointUrl = System.getProperty("testbed.sm.endpointurl");
		final String configFileName = System.getProperty("testbed.configfile");
		final List<String> nodeUrns = newArrayList();

		final File configFile = new File(configFileName);

		if (!configFile.exists()) {
			throw new RuntimeException("Configuration file \"" + configFile.getAbsolutePath() + "\" does not exist!");
		} else if (configFile.isDirectory()) {
			throw new RuntimeException("Configuration file \"" + configFile.getAbsolutePath() + "\" is a directory!");
		} else if (!configFile.canRead()) {
			throw new RuntimeException("Configuration file \"" + configFile.getAbsolutePath() + "\" can't be read!");
		}

		final WisebedClientBase client;
		if (useProtobuf) {

			final String protobufHost = System.getProperty("testbed.protobuf.hostname");
			final Integer protobufPort = Integer.parseInt(System.getProperty("testbed.protobuf.port"));
			client = new WisebedProtobufClient(sessionManagementEndpointUrl, protobufHost, protobufPort);

		} else {
			client = new WisebedClient(sessionManagementEndpointUrl);
		}

		final WSNAsyncWrapper wsn = client.connectToExperiment(parseSecretReservationKeys(secretReservationKeys)).get();

		final String nodeUrnsArgument = System.getProperty("testbed.nodeurns");

		if (nodeUrnsArgument != null && "portal".equalsIgnoreCase(nodeUrnsArgument.trim())) {

		} else if (nodeUrnsArgument != null) {

			final Splitter splitter = Splitter.on(",").omitEmptyStrings().trimResults();
			for (String nodeUrn : splitter.split(nodeUrnsArgument)) {
				nodeUrns.add(nodeUrn);
			}

		} else {
			nodeUrns.addAll(WiseMLHelper.getNodeUrns(wsn.getNetwork().get()));
		}


		final JobResult jobResult = wsn.setChannelPipeline(nodeUrns, create(configFile), 10, TimeUnit.SECONDS).get();
		log.info("{}", jobResult);

		client.shutdown();
		System.exit(0);
	}

	/**
	 * Loads the XML configuration file and returns a list of (handler-name, factory-name, (key,value)-pairs). The format
	 * is as follows:
	 * <p/>
	 * <pre>
	 * {@code
	 *
	 * <?xml version="1.0" encoding="UTF-8" ?>
	 * <itm-netty-handlerstack>
	 *   <handler factory="movedetect-time-protocol-handler"/>
	 *   <handler factory="movedetect-time-protocol-decoder">
	 *     <option key="1" value="1"/>
	 *     <option key="2" value="2"/>
	 *   </handler>
	 *   <handler factory="movedetect-time-protocol-encoder">
	 *     <option key="11" value="11"/>
	 *     <option key="22" value="22"/>
	 *   </handler>
	 * </itm-netty-handlerstack>
	 * }
	 * </pre>
	 *
	 * @param configFile The configuration file to read.
	 *
	 * @return
	 *
	 * @throws Exception
	 */
	private static List<ChannelHandlerConfiguration> create(final File configFile) throws Exception {

		List<ChannelHandlerConfiguration> channelHandlerConfigurations = newArrayList();

		if (!configFile.exists()) {
			throw new FileNotFoundException("Configuration file " + configFile + " not found.");
		}

		XMLConfiguration config = new XMLConfiguration(configFile);

		@SuppressWarnings("unchecked")
		List<HierarchicalConfiguration> handlers = config.configurationsAt("handler");
		for (HierarchicalConfiguration sub : handlers) {

			final String factoryName = sub.getString("[@factory]");

			final ChannelHandlerConfiguration chc = new ChannelHandlerConfiguration();
			chc.setName(factoryName);

			@SuppressWarnings("unchecked")
			List<HierarchicalConfiguration> xmlOptions = sub.configurationsAt("option");

			for (HierarchicalConfiguration xmlOption : xmlOptions) {

				String optionKey = xmlOption.getString("[@key]");
				String optionValue = xmlOption.getString("[@value]");

				if (optionKey != null && optionValue != null && !"".equals(optionKey) && !"".equals(optionValue)) {
					log.debug("Option for handler {}: {} = {}", new Object[]{factoryName, optionKey, optionValue});
					final KeyValuePair pair = new KeyValuePair();
					pair.setKey(optionKey);
					pair.setValue(optionValue);
					chc.getConfiguration().add(pair);
				}
			}

			channelHandlerConfigurations.add(chc);
		}

		return channelHandlerConfigurations;
	}
}
