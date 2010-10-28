package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLAttribute;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLSequence;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.MergerResources;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.WiseMLElementMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.WiseMLTreeMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public class ScenarioMerger extends WiseMLElementMerger {

	public ScenarioMerger(
			final WiseMLTreeMerger parent, 
			final WiseMLTreeReader[] inputs,
			final MergerConfiguration configuration, 
			final MergerResources resources,
			final String id) {
		super(parent, inputs, configuration, resources, WiseMLTag.scenario);
		this.attributeList.add(new WiseMLAttribute("id", id));
	}

	@Override
	protected void fillQueue() {
		/*
		WiseMLTreeReader[] readers = new WiseMLTreeReader[inputCount()];
		for (int i = 0; i < readers.length; i++) {
			if (nextSubInputReader(i)) {
				readers[i] = getSubInputReader(i);
			}
		}
		*/
		// TODO: problem: when is this supposed to finish?
		queue.add(new ScenarioItemListMerger(
				this, 
				findSequenceReaders(WiseMLSequence.ScenarioItem), 
				configuration, 
				resources));
		/*
		queue.add(new ScenarioItemListMerger(
				this, readers, configuration, resources));
			*/
	}
}
