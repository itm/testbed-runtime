package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.MergerResources;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.NamedItemListMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.WiseMLTreeMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public class ScenarioListMerger extends NamedItemListMerger {

	protected ScenarioListMerger(
			final WiseMLTreeMerger parent,
			final WiseMLTreeReader[] inputs, 
			final MergerConfiguration configuration,
			final MergerResources resources) {
		super(
				parent, 
				inputs, 
				configuration, 
				resources, 
				WiseMLTag.scenario);
		this.mergingMode = this.configuration.getScenarioListMergingMode();
		this.customID = this.configuration.getCustomScenarioID();
	}

	@Override
	protected WiseMLTreeReader createMerger(
			WiseMLTreeReader[] inputs, String id) {
		return new ScenarioMerger(this, inputs, configuration, resources, id);
	}

}
