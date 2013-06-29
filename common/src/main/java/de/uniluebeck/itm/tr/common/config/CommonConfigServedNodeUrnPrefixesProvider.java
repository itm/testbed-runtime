package de.uniluebeck.itm.tr.common.config;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import eu.wisebed.api.v3.common.NodeUrnPrefix;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

public class CommonConfigServedNodeUrnPrefixesProvider implements ServedNodeUrnPrefixesProvider {

	private final CommonConfig commonConfig;

	@Inject
	public CommonConfigServedNodeUrnPrefixesProvider(final CommonConfig commonConfig) {
		this.commonConfig = commonConfig;
	}

	@Override
	public Set<NodeUrnPrefix> get() {
		return newHashSet(commonConfig.getUrnPrefix());
	}
}
