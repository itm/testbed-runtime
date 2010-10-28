package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import java.util.Collection;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLSequence;
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
			final MergerResources resources,
			final WiseMLSequence sequence) {
		super(parent, inputs, configuration, resources, sequence);
	}

	@Override
	protected TimeStampedItemDefinition readNextItem(int inputIndex) {
		if (!nextSubInputReader(inputIndex)) {
			return null;
		}
		WiseMLTreeReader timestampReader = getSubInputReader(inputIndex);
		
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
		
		return new TimeStampedItemDefinition(timestamp, inputIndex);
	}
	
	@Override
	protected void fillQueue() {
		if (this.items == null) {
			super.fillQueue();
		} else {
			for (TimeStampedItemDefinition item : this.items) {
				int input = item.getInputIndex();
				if (nextSubInputReader(input)) {
					WiseMLTreeReader reader = getSubInputReader(input);
					
					System.out.println("next reader for input "+input+": "+reader);
					
					if (reader.getTag().equals(WiseMLTag.timestamp)) {
						holdInput(item.getInputIndex());
					} else {
						handleReader(reader, input);
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
