package de.uniluebeck.itm.tr.common;

import com.google.common.base.Function;
import eu.wisebed.api.v3.common.NodeUrnPrefix;

import javax.annotation.Nullable;

public class NodeUrnPrefixHelper {

	public static final Function<String, NodeUrnPrefix> STRING_TO_NODE_URN_PREFIX =
			new Function<String, NodeUrnPrefix>() {
				@Nullable
				@Override
				public NodeUrnPrefix apply(@Nullable final String nodeUrnPrefixString) {
					return nodeUrnPrefixString == null ? null : new NodeUrnPrefix(nodeUrnPrefixString);
				}
			};

	public static final Function<NodeUrnPrefix, String> NODE_URN_PREFIX_TO_STRING =
			new Function<NodeUrnPrefix, String>() {
				@Nullable
				@Override
				public String apply(@Nullable final NodeUrnPrefix nodeUrnPrefix) {
					return nodeUrnPrefix == null ? "null" : nodeUrnPrefix.toString();
				}
			};
}
