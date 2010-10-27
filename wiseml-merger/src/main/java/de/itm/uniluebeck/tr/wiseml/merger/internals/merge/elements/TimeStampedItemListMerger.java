package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import java.util.Collection;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.MergerResources;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.SortedListMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.WiseMLTreeMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements.TimeStampParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.elements.TimeStampReader;
import de.itm.uniluebeck.tr.wiseml.merger.structures.TimeInfo;
import de.itm.uniluebeck.tr.wiseml.merger.structures.TimeStamp;

public abstract class TimeStampedItemListMerger 
extends SortedListMerger<TimeStampedItemDefinition> {
	
	private Collection<TimeStampedItemDefinition> items;

	public TimeStampedItemListMerger(
			final WiseMLTreeMerger parent,
			final WiseMLTreeReader[] inputs, 
			final MergerConfiguration configuration,
			final MergerResources resources) {
		super(parent, inputs, configuration, resources);
	}

	@Override
	protected TimeStampedItemDefinition readNextItem(int inputIndex) {
		WiseMLTreeReader input = inputs[inputIndex];
		if (input.isFinished()) {
			return null;
		}
		if (input.getSubElementReader() == null 
				&& !input.nextSubElementReader()) {
			return null;
		}
		WiseMLTreeReader timestampReader = input.getSubElementReader();
		
		if (!WiseMLTag.timestamp.equals(timestampReader.getTag())) {
			throw new IllegalStateException(
					"expected <timestamp>, got <"+timestampReader.getTag()+">");
		}
		
		if (resources.getTimeInfo() == null) {
			exception("encountered timestamp, but missing timeinfo", null);
		}
		
		// read timestamp
		TimeStamp timestamp = new TimeStampParser(
				timestampReader,
				resources.getTimeInfo()).getParsedStructure();
		
		inputs[inputIndex].nextSubElementReader();
		
		return new TimeStampedItemDefinition(
				timestamp, inputs[inputIndex], inputIndex);
	}
	
	@Override
	protected void fillQueue() {
		if (this.items == null) {
			super.fillQueue();
		} else {
			for (TimeStampedItemDefinition item : this.items) {
				WiseMLTreeReader reader = 
					item.getParentReader().getSubElementReader();
				if (reader != null) {
					if (!reader.getTag().equals(WiseMLTag.timestamp)) {
						handleReader(reader, item.getInputIndex());
						item.getParentReader().nextSubElementReader();
					}
				}
			}
			if (this.queue.isEmpty()) {
				this.items = null;
				super.fillQueue();
			}
		}
	}
	
	protected abstract void handleReader(WiseMLTreeReader reader, int inputIndex);

	@Override
	protected WiseMLTreeReader mergeItems(
			Collection<TimeStampedItemDefinition> items) {
		// save items
		this.items = items;
		
		TimeStampedItemDefinition item = items.iterator().next();
		TimeStamp timestamp = item.getTimeStamp();
		TimeInfo timeinfo = this.resources.getTimeInfo();
		
		timestamp.setStart(timeinfo.getStart());
		timestamp.setUnit(timeinfo.getUnit());
		
		// TODO: offsetDefined
		
		this.queue.add(new TimeStampReader(this, timestamp));
		
		/* nothing to return because data needs to be streamed into the 
		 * same list (managed by this merger)
		 */
		return null;
	}

}
