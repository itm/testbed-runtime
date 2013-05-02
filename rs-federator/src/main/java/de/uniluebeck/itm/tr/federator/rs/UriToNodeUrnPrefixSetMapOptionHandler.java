package de.uniluebeck.itm.tr.federator.rs;

import com.google.common.base.Splitter;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.MapOptionHandler;
import org.kohsuke.args4j.spi.Setter;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

public class UriToNodeUrnPrefixSetMapOptionHandler extends MapOptionHandler {

	@SuppressWarnings("unused")
	public UriToNodeUrnPrefixSetMapOptionHandler(final CmdLineParser parser, final OptionDef option,
												 final Setter<? super Map<?, ?>> setter) {
		super(parser, option, setter);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void addToMap(final Map m, final String key, final String value) {
		final Set<NodeUrnPrefix> nodeUrnPrefixes = newHashSet();
		for (String nodeUrnPrefixString : Splitter.on(",").omitEmptyStrings().trimResults().split(value)) {
			nodeUrnPrefixes.add(new NodeUrnPrefix(nodeUrnPrefixString));
		}
		m.put(URI.create(key), nodeUrnPrefixes);
	}
}
