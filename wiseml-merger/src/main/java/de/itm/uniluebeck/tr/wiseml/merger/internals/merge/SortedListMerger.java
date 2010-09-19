package de.itm.uniluebeck.tr.wiseml.merger.internals.merge;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public abstract class SortedListMerger<ListItem extends Comparable<ListItem>> 
extends WiseMLListMerger {
	
	private List<ListItem> bufferedItems;
	private SortedSet<ListItem> itemQueue;
	
	protected SortedListMerger(
			final WiseMLTreeMerger parent,
			final WiseMLTreeReader[] inputs, 
			final MergerConfiguration configuration,
			final MergerResources resources) {
		super(parent, inputs, configuration, resources);
		
		bufferedItems = new ArrayList<ListItem>(inputs.length);
		itemQueue = new TreeSet<ListItem>();
	}
	
	/**
	 * Reads the next item from one of the inputs.
	 * @param inputIndex Index of the input WiseMLTreeReader.
	 * @return The new item or null if there's no more items.
	 */
	protected abstract ListItem readNextItem(int inputIndex);
	
	/**
	 * Merges multiple items and produces a WiseMLTreeReader.
	 * @param items
	 * @return
	 */
	protected abstract WiseMLTreeReader mergeItems(Set<ListItem> items);
	
	@Override
	protected void fillQueue() {
		// get items from inputs
		fetchNewItems();
		
		if (itemQueue.isEmpty()) {
			return;
		}
		
		// find equal items
		Set<ListItem> items = new TreeSet<ListItem>();
		ListItem firstItem = null;
		for (ListItem item : itemQueue) {
			if (firstItem == null) {
				firstItem = item;
				items.add(item);
			} else {
				if (item.compareTo(firstItem) == 0) {
					items.add(item);
				} else {
					break;
				}
			}
		}
		
		// mark selected items
		for (ListItem item : items) {
			bufferedItems.set(bufferedItems.indexOf(item), null);
		}
		
		// remove items from item queue
		itemQueue.removeAll(items);
		
		// merge and add new reader to queue
		queue.add(mergeItems(items));
	}

	private void fetchNewItems() {
		for (int i = 0; i < inputs.length; i++) {
			if (bufferedItems.get(i) == null) {
				if (inputs[i] != null) {
					if (inputs[i].isFinished()) {
						inputs[i] = null;
						continue;
					}
					
					ListItem item = readNextItem(i);
					
					if (item == null) {
						inputs[i] = null;
						continue;
					}
					
					bufferedItems.set(i, item);
					itemQueue.add(item);
				}
			}
		}
	}

}
