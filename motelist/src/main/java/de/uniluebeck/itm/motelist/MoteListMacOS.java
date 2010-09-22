package de.uniluebeck.itm.motelist;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.StringTokenizer;


class MoteListMacOS extends AbstractMoteList {

	private static final Logger log = LoggerFactory.getLogger(MoteListMacOS.class);

	public MoteListMacOS() {
		if (!SystemUtils.IS_OS_MAC_OSX) {
			throw new RuntimeException("This class only works on Mac OS X");
		}
	}

	protected Multimap<MoteType, String> parseMoteList(final BufferedReader in) {

		Multimap<MoteType, String> motes = LinkedListMultimap.create(3);
		String text;

		try {

			//in.readLine(); // Information about how we had got this information.

			String reference, port, type;

			while ((text = in.readLine()) != null) {
				StringTokenizer tokenizer = new StringTokenizer(text, ",");


				if (tokenizer.countTokens() != 3) {
					log.warn("Unexpected token count of {} in line \"{}\"", tokenizer.countTokens(), text);
				} else {

					reference = tokenizer.nextToken();
					port = tokenizer.nextToken();
					type = tokenizer.nextToken();

					//lin: XBOW Crossbow Telos Rev.B
					//win32: Crossbow Telos Rev.B
					if (type.contains("Telos")) {
						motes.put(MoteType.TELOSB, port);
					}

					// ITM Pacemate
					if (type.contains("Pacemate")) {
						motes.put(MoteType.PACEMATE, port);
					}

					// Coalesenses iSense
					if (type.contains("iSense")) {
						motes.put(MoteType.ISENSE, port);
					}

				}
			}
		} catch (IOException e) {
			log.error("" + e, e);
		}

		return motes;
	}

	@Override
	public String getScriptName() {
		return "motelist-macos";
	}
}
