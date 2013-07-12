package de.uniluebeck.itm.tr.federator.utils;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.inject.TypeLiteral;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import org.nnsoft.guice.rocoto.converters.AbstractConverter;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

public class UriToNodeUrnPrefixSetMapTypeConverter extends AbstractConverter<URIToNodeUrnPrefixSetMap> {

	@Override
	public Object convert(final String value, final TypeLiteral<?> toType) {

		final Map<URI, Set<NodeUrnPrefix>> map = newHashMap();
		final Splitter spaceSplitter = Splitter.on(" ").omitEmptyStrings().trimResults();
		final Splitter equalsSplitter = Splitter.on("=").omitEmptyStrings().trimResults();
		final Splitter commaSplitter = Splitter.on(",").omitEmptyStrings().trimResults();

		for (String uriPrefixesPair : spaceSplitter.split(value)) {

			final Iterable<String> uriPrefixesIterable = equalsSplitter.split(uriPrefixesPair);
			final URI endpointUrl = URI.create(Iterables.get(uriPrefixesIterable, 0));
			final String prefixesString = Iterables.get(uriPrefixesIterable, 1);

			final Set<NodeUrnPrefix> nodeUrnPrefixes = newHashSet();
			for (String urnPrefixString : commaSplitter.split(prefixesString)) {
				nodeUrnPrefixes.add(new NodeUrnPrefix(urnPrefixString));
			}

			map.put(endpointUrl, nodeUrnPrefixes);
		}

		return new URIToNodeUrnPrefixSetMap(map);
	}
}
