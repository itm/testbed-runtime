package de.itm.uniluebeck.tr.wiseml.merger.internals.parse;

import java.util.HashMap;
import java.util.Map;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public class ParserHelper {
	
	/**
	 * Retrieves all parseable structures from the reader until there are
	 * no more elements or an un-parseable element is encountered.
	 */
	public static Map<WiseMLTag,Object> getStructures(final WiseMLTreeReader reader) {
		final Map<WiseMLTag,Object> result = new HashMap<WiseMLTag,Object>();
		parseStructures(reader, new ParserCallback(){
			@Override
			public void nextStructure(WiseMLTag tag, Object structure) {
				result.put(tag, structure);
			}
		});
		return result;
	}
	
	public static void parseStructures(
			final WiseMLTreeReader reader, 
			final ParserCallback callback) {
		while (!reader.isFinished()) {
			if (reader.nextSubElementReader()) {
				if (reader.getSubElementReader().isMappedToTag()) {
					WiseMLTreeReader nextReader = reader.getSubElementReader();
					
					WiseMLElementParser<?> parser = ParserManager.sharedInstance().createParser(
							nextReader.getTag(), nextReader);
					
					if (parser == null) {
						break;
					}
					
					callback.nextStructure(nextReader.getTag(), parser.getParsedStructure());
					continue;
				}
			}
			break;
		}
	}

}
