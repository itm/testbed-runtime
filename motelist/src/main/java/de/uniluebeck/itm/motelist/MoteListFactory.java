package de.uniluebeck.itm.motelist;

import org.apache.commons.lang.SystemUtils;

public class MoteListFactory {

	public static MoteList create() {
		if (SystemUtils.IS_OS_MAC_OSX) {
			return new MoteListMacOS();
		} else if (SystemUtils.IS_OS_WINDOWS) {
			return new MoteListWin32();
		} else if (SystemUtils.IS_OS_LINUX) {
			return new MoteListLinux();
		} else {
			throw new RuntimeException("Only Linux, Mac OS X and Windows are supported by this version!");
		}
	}

}
