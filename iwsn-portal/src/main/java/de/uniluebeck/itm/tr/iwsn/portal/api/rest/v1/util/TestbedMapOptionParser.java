package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util;

import com.google.common.io.Files;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.TestbedMap;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

import java.io.File;
import java.nio.charset.Charset;

public class TestbedMapOptionParser extends OptionHandler<TestbedMap> {

	public TestbedMapOptionParser(CmdLineParser parser, OptionDef option, Setter<? super TestbedMap> setter) {
		super(parser, option, setter);
	}

	@Override
	public int parseArguments(final Parameters params) throws CmdLineException {
		String fileName = params.getParameter(0);
		final File file = new File(fileName);
		try {
			TestbedMap testbedMap = JSONHelper.fromJSON(
					Files.toString(file, Charset.defaultCharset()),
					TestbedMap.class
			);
			setter.addValue(testbedMap);
		} catch (Exception e) {
			throw new CmdLineException(owner,
					"The supplied testbed list file \"" + file.getAbsolutePath() + "\" could not be parsed! Reason: " + e, e
			);
		}
		return 1;
	}

	@Override
	public String getDefaultMetaVariable() {
		return null;
	}

}
