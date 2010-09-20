package de.uniluebeck.itm.motelist;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.StringTokenizer;


class MoteListWin32 extends AbstractMoteList {

	private static final Logger log = LoggerFactory.getLogger(MoteListWin32.class);

	public MoteListWin32() {
		if (!SystemUtils.IS_OS_WINDOWS) {
			throw new RuntimeException("This class only works on Windows");
		}
	}

	protected Multimap<MoteType, String> parseMoteList(final BufferedReader in) {

		Multimap<MoteType, String> motes = LinkedListMultimap.create(3);
		String text;

		try {

			String reference, port, type;

			while ((text = in.readLine()) != null) {
				StringTokenizer tokenizer = new StringTokenizer(text, ",");


				if (tokenizer.countTokens() != 4) {
					log.warn("Unexpected token count of {} in line \"{}\"", tokenizer.countTokens(), text);
				} else {

					reference = tokenizer.nextToken(); // TODO we'll need that for TelosB soon
					port = tokenizer.nextToken();
					tokenizer.nextToken(); // Skip reference count 
					type = tokenizer.nextToken();

					//lin: XBOW Crossbow Telos Rev.B
					//win32: Crossbow Telos Rev.B
					if (type.contains("Telos")) {
						motes.put(MoteType.TELOSB, port.replaceAll("[_[^\\w\\d������\\+\\- ]]", ""));
					}

					// ITM Pacemate
					if (type.contains("Pacemate")) {
						motes.put(MoteType.PACEMATE, port.replaceAll("[_[^\\w\\d������\\+\\- ]]", ""));
					}

					// Coalesenses iSense
					if (type.contains("iSense")) {
						motes.put(MoteType.ISENSE, port.replaceAll("[_[^\\w\\d������\\+\\- ]]", ""));
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
		return "motelist-win32.exe";
	}

}
