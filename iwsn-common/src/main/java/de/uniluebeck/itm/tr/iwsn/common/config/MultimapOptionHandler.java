package de.uniluebeck.itm.tr.iwsn.common.config;

import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.size;

public class MultimapOptionHandler extends OptionHandler<Multimap<String, String>> {

	protected MultimapOptionHandler(final CmdLineParser parser, final OptionDef option,
									final Setter<? super Multimap<String, String>> setter) {
		super(parser, option, setter);
	}

	@Override
	public int parseArguments(final Parameters params) throws CmdLineException {

		final Multimap<String, String> map = HashMultimap.create();

		final Splitter equalsSplitter = Splitter.on("=").omitEmptyStrings().trimResults();
		final Splitter commaSplitter = Splitter.on(",").omitEmptyStrings().trimResults();

		for (int i = 0; i < params.size(); i++) {

			final Iterable<String> keyAndValues = equalsSplitter.split(params.getParameter(i));
			checkArgument(size(keyAndValues) == 2);

			final Iterator<String> keyAndValuesIterator = keyAndValues.iterator();
			final String key = keyAndValuesIterator.next();
			final String values = keyAndValuesIterator.next();

			for (String value  : commaSplitter.split(values)) {
				map.put(key, value);
			}
		}

		return params.size();
	}

	@Override
	public String getDefaultMetaVariable() {
		return "OPTIONS";
	}
}
