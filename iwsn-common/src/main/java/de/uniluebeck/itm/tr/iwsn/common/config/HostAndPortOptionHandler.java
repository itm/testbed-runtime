package de.uniluebeck.itm.tr.iwsn.common.config;

import com.google.common.net.HostAndPort;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

public class HostAndPortOptionHandler extends OptionHandler<HostAndPort> {

	public HostAndPortOptionHandler(final CmdLineParser parser, final OptionDef option,
									   final Setter<? super HostAndPort> setter) {
		super(parser, option, setter);
	}

	@Override
	public int parseArguments(final Parameters params) throws CmdLineException {
		try {
			setter.addValue(HostAndPort.fromString(params.getParameter(0)));
			return 1;
		} catch (Exception e) {
			throw new CmdLineException(owner, e);
		}
	}

	@Override
	public String getDefaultMetaVariable() {
		return "HOSTANDPORT";
	}
}
