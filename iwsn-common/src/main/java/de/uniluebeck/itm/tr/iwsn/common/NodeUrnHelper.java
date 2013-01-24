package de.uniluebeck.itm.tr.iwsn.common;

import com.google.common.base.Function;
import eu.wisebed.api.v3.common.NodeUrn;

import javax.annotation.Nullable;

public class NodeUrnHelper {

	public static final Function<String, NodeUrn> STRING_TO_NODE_URN =
			new Function<String, NodeUrn>() {
				@Nullable
				@Override
				public NodeUrn apply(@Nullable final String nodeUrnString) {
					return nodeUrnString == null ? null : new NodeUrn(nodeUrnString);
				}
			};

	public static final Function<NodeUrn, String> NODE_URN_TO_STRING =
			new Function<NodeUrn, String>() {
				@Nullable
				@Override
				public String apply(@Nullable final NodeUrn nodeUrn) {
					return nodeUrn == null ? "null" : nodeUrn.toString();
				}
			};
}
