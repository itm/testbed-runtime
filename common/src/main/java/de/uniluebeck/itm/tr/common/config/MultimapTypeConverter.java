package de.uniluebeck.itm.tr.common.config;

import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.TypeLiteral;
import org.nnsoft.guice.rocoto.converters.AbstractConverter;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.size;

public class MultimapTypeConverter extends AbstractConverter<Multimap> {

	@Override
	public Object convert(final String valueString, final TypeLiteral<?> toType) {

		final Multimap<String, String> map = HashMultimap.create();

		final Splitter spaceSplitter = Splitter.on(" ").omitEmptyStrings().trimResults();
		final Splitter equalsSplitter = Splitter.on("=").omitEmptyStrings().trimResults();
		final Splitter commaSplitter = Splitter.on(",").omitEmptyStrings().trimResults();

		for (String keyValuePairString : spaceSplitter.split(valueString)) {

			final Iterable<String> keyAndValues = equalsSplitter.split(keyValuePairString);
			checkArgument(size(keyAndValues) == 2);

			final Iterator<String> keyAndValuesIterator = keyAndValues.iterator();
			final String key = keyAndValuesIterator.next();
			final String values = keyAndValuesIterator.next();

			for (String value  : commaSplitter.split(values)) {
				map.put(key, value);
			}
		}

		return map;
	}
}
