package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import java.util.Set;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.MergerResources;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.SortedListMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.WiseMLTreeMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public class ScenarioListMerger extends SortedListMerger<ScenarioDefinition> {

	public ScenarioListMerger(WiseMLTreeMerger parent,
			WiseMLTreeReader[] inputs, MergerConfiguration configuration,
			MergerResources resources) {
		super(parent, inputs, configuration, resources);
	}

	@Override
	protected WiseMLTreeReader mergeItems(Set<ScenarioDefinition> items) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ScenarioDefinition readNextItem(int inputIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	
}
