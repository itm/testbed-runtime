package de.itm.uniluebeck.tr.wiseml.merger.internals.merge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLSequence;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public abstract class SortedListMerger<ListItem extends Comparable<ListItem>> 
extends WiseMLListMerger {
	
	private List<ListItem> bufferedItems;
	private List<ListItem> itemQueue;
	
	protected SortedListMerger(
			final WiseMLTreeMerger parent,
			final WiseMLTreeReader[] inputs, 
			final MergerConfiguration configuration,
			final MergerResources resources,
			final WiseMLSequence sequence) {
		super(parent, inputs, configuration, resources, sequence);
		
		bufferedItems = new ArrayList<ListItem>(inputs.length);
		for (int i = 0; i < inputs.length; i++) {
			bufferedItems.add(null);
		}
		
		itemQueue = new LinkedList<ListItem>();
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
	protected abstract WiseMLTreeReader mergeItems(Collection<ListItem> items);
	
	@Override
	protected void fillQueue() {
		// get items from inputs
		fetchNewItems();
		
		if (itemQueue.isEmpty()) {
			return;
		}
		
		// find equal items
		Collections.sort(itemQueue);
		List<ListItem> items = new LinkedList<ListItem>();
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
		WiseMLTreeReader nextReader = mergeItems(items);
		if (nextReader != null) {
			queue.add(nextReader);
		}
	}

	private void fetchNewItems() {
		for (int i = 0; i < inputCount(); i++) {
			if (bufferedItems.get(i) == null) {
				ListItem item = readNextItem(i);
				if (item != null) {
					bufferedItems.set(i, item);
					itemQueue.add(item);
				}
			}
		}
	}

}
