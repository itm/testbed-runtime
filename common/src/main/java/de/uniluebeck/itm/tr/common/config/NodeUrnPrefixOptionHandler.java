package de.uniluebeck.itm.tr.common.config;

import eu.wisebed.api.v3.common.NodeUrnPrefix;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

public class NodeUrnPrefixOptionHandler extends OptionHandler<NodeUrnPrefix> {

	public NodeUrnPrefixOptionHandler(final CmdLineParser parser, final OptionDef option,
										 final Setter<NodeUrnPrefix> setter) {
		super(parser, option, setter);
	}

	@Override
	public int parseArguments(final Parameters params) throws CmdLineException {
		setter.addValue(new NodeUrnPrefix(params.getParameter(0)));
		return 1;
	}

	@Override
	public String getDefaultMetaVariable() {
		return "NODE_URN_PREFIX";
	}
}
