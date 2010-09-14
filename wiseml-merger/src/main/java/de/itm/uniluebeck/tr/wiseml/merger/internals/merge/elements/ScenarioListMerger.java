package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.MergerResources;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.WiseMLListMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.WiseMLTreeMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public class ScenarioListMerger extends WiseMLListMerger {

	public ScenarioListMerger(WiseMLTreeMerger parent,
			WiseMLTreeReader[] inputs, MergerConfiguration configuration,
			MergerResources resources) {
		super(parent, inputs, configuration, resources);
	}

	
	@Override
	public boolean nextSubElementReader() {
		// TODO Auto-generated method stub
		return false;
	}

}
