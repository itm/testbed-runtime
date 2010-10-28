package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import java.util.Collection;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.MergerResources;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.SortedListMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.WiseMLTreeMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements.TimeStampParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.structures.TimeStamp;

public class ScenarioItemListMerger 
extends SortedListMerger<ScenarioItemDefinition>{

	public ScenarioItemListMerger(
			final WiseMLTreeMerger parent,
			final WiseMLTreeReader[] inputs, 
			final MergerConfiguration configuration,
			final MergerResources resources) {
		super(parent, inputs, configuration, resources);
	}

	@Override
	protected ScenarioItemDefinition readNextItem(int inputIndex) {
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
			throw new IllegalStateException("expected <timestamp>");
		}
		
		if (resources.getTimeInfo() == null) {
			exception("encountered timestamp, but missing timeinfo", null);
		}
		
		// read timestamp
		TimeStamp timestamp = new TimeStampParser(
				timestampReader,
				resources.getTimeInfo()).getParsedStructure();
		
		return new ScenarioItemDefinition(timestamp, inputs[inputIndex]);
	}

	@Override
	protected WiseMLTreeReader mergeItems(
			Collection<ScenarioItemDefinition> items) {
		// TODO Auto-generated method stub
		
		System.err.println("mergeItems");
		
		return null;
	}

}
