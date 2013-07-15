package de.uniluebeck.itm.tr.common.config;

import de.uniluebeck.itm.util.logging.LogLevel;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

public class LogLevelOptionHandler extends OptionHandler<LogLevel> {

	public LogLevelOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super LogLevel> setter) {
		super(parser, option, setter);
	}

	@Override
	public int parseArguments(final Parameters parameters) throws CmdLineException {
		setter.addValue(LogLevel.toLevel(parameters.getParameter(0)));
		return 1;
	}

	@Override
	public String getDefaultMetaVariable() {
		return "LEVEL";
	}

}