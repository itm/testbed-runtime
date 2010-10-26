package de.itm.uniluebeck.tr.wiseml.merger.internals.merge;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.itm.uniluebeck.tr.wiseml.merger.config.ListMergingMode;
import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReaderHelper;

public abstract class NamedItemListMerger extends SortedListMerger<NamedListItem> {
	
	private WiseMLTag itemTag;
	protected ListMergingMode mergingMode;
	protected String customID;
	
	private boolean[] inputRead;
	private Set<String> knownIDs;
	
	protected NamedItemListMerger(
			final WiseMLTreeMerger parent,
			final WiseMLTreeReader[] inputs, 
			final MergerConfiguration configuration,
			final MergerResources resources,
			final WiseMLTag itemTag) {
		super(parent, inputs, configuration, resources);
		this.itemTag = itemTag;
		
		this.inputRead = new boolean[inputs.length];
		this.knownIDs = new HashSet<String>();
		
	}

	@Override
	protected NamedListItem readNextItem(int inputIndex) {
		WiseMLTreeReader input = inputs[inputIndex];
		if (input.isFinished()) {
			return null;
		}
		if (input.getSubElementReader() == null 
				&& !input.nextSubElementReader()) {
			return null;
		}
		WiseMLTreeReader nodeReader = input.getSubElementReader();
		
		String id = WiseMLTreeReaderHelper.getAttributeValue(
				nodeReader.getAttributeList(), "id");
		
		switch (mergingMode) {
		case NoMerging: {
			// prevent merging by generating unique IDs
			int suffix = 0;
			String nextID = id;
			while (knownIDs.contains(nextID)) {
				nextID = id + suffix;
			}
			knownIDs.add(nextID);
			return new NamedListItem(nextID, nodeReader);
		}
		case SameIDsSortedAlphanumerically:
			// pass ID as string
			return new NamedListItem(id, nodeReader);
		case SameIDsSortedNumerically:
			// pass ID as integer
			try {
				return new NamedListItem(Long.parseLong(id), nodeReader);
			} catch (NumberFormatException e) {
				exception("selected 'SameIDsSortedNumerically' for " + 
						itemTag + 
						" list but could not parse id as integer", e);
			}
		case SingleOutputItem:
			// pass custom ID, make sure each input list has one element at most
			if (inputRead[inputIndex]) {
				exception("selected 'SingleOutputItem' for " + 
						itemTag + 
						" list but provided multiple items on input " + 
						inputIndex, null);
			} else {
				inputRead[inputIndex] = true;
			}
			return new NamedListItem(customID, nodeReader);
		default:
			exception("unknown list merging mode: "+mergingMode, null);
			return null;
		}
	}

	@Override
	protected WiseMLTreeReader mergeItems(Collection<NamedListItem> items) {
		switch (mergingMode) {
		case NoMerging: 
		case SingleOutputItem: {
			if (items.size() != 1) {
				throw new IllegalStateException();
			}
			NamedListItem item = items.iterator().next();
			return transform(item.getAssociatedReader(), item.getID());
		}
		case SameIDsSortedAlphanumerically:
		case SameIDsSortedNumerically: {
			WiseMLTreeReader[] readers = new WiseMLTreeReader[items.size()];
			int index = 0;
			String id = null;
			for (NamedListItem item : items) {
				readers[index++] = item.getAssociatedReader();
				id = item.getID();
			}
			return createMerger(readers, id);
		}
		default:
			exception("unknown list merging mode: "+mergingMode, null);
			return null;
		}
	}
	
	protected WiseMLTreeReader transform(WiseMLTreeReader input, String id) {
		return input;
	}
	
	protected abstract WiseMLTreeReader createMerger(
			final WiseMLTreeReader[] inputs, String id);

}
