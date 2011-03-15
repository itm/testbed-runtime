/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package com.coalesenses.otap.core.cli;

import de.uniluebeck.itm.tr.util.StringUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.StringTokenizer;

/**
 *
 */
public class AbstractOtapCLI {

	private static final Logger log = LoggerFactory.getLogger(AbstractOtapCLI.class);

	protected Options createOptions() {

		Options options = new Options();

		// binary file
		Option programOption =
				new Option("b", "binary", true, "The binary file you want to flash on the nodes");
		programOption.setRequired(true);
		options.addOption(programOption);

		// channel
		Option channelOption =
				new Option("c", "channel", true,
						"The channel that should be used during the flash process (Default 12)"
				);
		channelOption.setRequired(false);
		options.addOption(channelOption);

		// otap
		Option otapOption =
				new Option("o", "OTAP", false, "Disable MOTAP support (default MOTAP enabled)");
		otapOption.setRequired(false);
		options.addOption(otapOption);

		// force
		Option forceOption =
				new Option("f", "force", false,
						"Force programming even if not all desired devices have been found (default false)"
				);
		forceOption.setRequired(false);
		options.addOption(forceOption);

		// devices
		Option devicesOption =
				new Option("d", "devices", true, "");
		devicesOption.setRequired(true);
		options.addOption(devicesOption);

		// help
		options.addOption("h", "help", false, "Help output");

		return options;

	}

	protected void parseCmdLine(CommandLine commandLine, Options options, OtapConfig config) {

		if (commandLine.hasOption('h')) {
			usage(options);
			System.exit(1);
		}

		if (commandLine.hasOption('b')) {
			config.program = commandLine.getOptionValue('b');
		}

		if (commandLine.hasOption('c')) {
			config.channel = Integer.parseInt(commandLine.getOptionValue('c'));
		}

		if (commandLine.hasOption('o')) {
			config.channel = Integer.parseInt(commandLine.getOptionValue('o'));
		}

		if (commandLine.hasOption('f')) {
			config.channel = Integer.parseInt(commandLine.getOptionValue('f'));
		}

		if (commandLine.hasOption('d')) {
			StringTokenizer tok = new StringTokenizer(commandLine.getOptionValue('d'), ",");
			while (tok.hasMoreTokens()) {
				String token = tok.nextToken();
				if (token.equals("*")) {
					config.all = true;
					config.macs.clear();
					break;
				} else {
					config.macs.add(StringUtils.parseHexOrDecLongFromUrn(token));
				}
			}

		}

	}

	protected void usage(Options options) {
		new HelpFormatter().printHelp(120, this.getClass().getCanonicalName(), null, options, null);
	}

}
