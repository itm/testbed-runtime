package de.itm.uniluebeck.tr.wiseml.merger.internals.tree;

public class WiseMLTreeReaderHelper {
	
	public static void skipToEnd(final WiseMLTreeReader reader) {
		while (!reader.isFinished()) {
			reader.nextSubElementReader();
		}
	}

}
