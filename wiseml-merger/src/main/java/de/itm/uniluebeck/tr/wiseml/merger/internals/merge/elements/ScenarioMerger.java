package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.MergerResources;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.WiseMLElementMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.WiseMLTreeMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public abstract class ScenarioMerger extends WiseMLElementMerger {

	public ScenarioMerger(WiseMLTreeMerger parent, WiseMLTreeReader[] inputs,
			MergerConfiguration configuration, MergerResources resources,
			WiseMLTag tag) {
		super(parent, inputs, configuration, resources, tag);
		// TODO Auto-generated constructor stub
	}
// TODO
}
