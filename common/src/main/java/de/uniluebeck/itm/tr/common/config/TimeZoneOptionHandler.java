package de.uniluebeck.itm.tr.common.config;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

import java.util.TimeZone;

public class TimeZoneOptionHandler extends OptionHandler<TimeZone> {

	public TimeZoneOptionHandler(final CmdLineParser parser, final OptionDef option,
									final Setter<? super TimeZone> setter) {
		super(parser, option, setter);
	}

	@Override
	public int parseArguments(final Parameters params) throws CmdLineException {
		try {
			setter.addValue(TimeZone.getTimeZone(params.getParameter(0)));
			return 1;
		} catch (Exception e) {
			throw new CmdLineException(owner, e);
		}
	}

	@Override
	public String getDefaultMetaVariable() {
		return "TIME_ZONE";
	}
}
