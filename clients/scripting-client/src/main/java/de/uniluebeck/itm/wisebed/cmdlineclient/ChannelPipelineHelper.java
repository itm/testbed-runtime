package de.uniluebeck.itm.wisebed.cmdlineclient;

import eu.wisebed.api.common.KeyValuePair;
import eu.wisebed.api.wsn.ChannelHandlerConfiguration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class ChannelPipelineHelper {

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
	 * @param configFile
	 * 		The configuration file to read.
	 *
	 * @return
	 *
	 * @throws Exception
	 */
	public static List<ChannelHandlerConfiguration> loadConfiguration(final File configFile) throws Exception {

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
