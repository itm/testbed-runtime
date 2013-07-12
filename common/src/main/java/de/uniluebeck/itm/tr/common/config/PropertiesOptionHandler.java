package de.uniluebeck.itm.tr.common.config;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class PropertiesOptionHandler extends OptionHandler<Properties> {

	public PropertiesOptionHandler(final CmdLineParser parser, final OptionDef option,
								   final Setter<? super Properties> setter) {
		super(parser, option, setter);
	}

	@Override
	public int parseArguments(final Parameters params) throws CmdLineException {

		final String filePath = params.getParameter(0);
		final File file = new File(filePath);
		final Properties properties = new Properties();

		try {

			properties.load(new FileReader(file));
			setter.addValue(properties);
			return 1;

		} catch (IOException e) {
			final String errorMessage = "The file \"" + file.getAbsolutePath() + "\" is not a valid properties File!";
			throw new CmdLineException(owner, errorMessage, e);
		}
	}

	@Override
	public String getDefaultMetaVariable() {
		return "PROPERTIES_FILE";
	}
}
