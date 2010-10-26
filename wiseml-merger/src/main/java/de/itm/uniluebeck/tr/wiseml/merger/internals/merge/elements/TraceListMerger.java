package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.MergerResources;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.NamedItemListMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.WiseMLTreeMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public class TraceListMerger extends NamedItemListMerger {

	public TraceListMerger(
			final WiseMLTreeMerger parent,
			final WiseMLTreeReader[] inputs, 
			final MergerConfiguration configuration,
			final MergerResources resources) {
		super(
				parent, 
				inputs, 
				configuration, 
				resources, 
				WiseMLTag.trace);
		this.mergingMode = this.configuration.getTraceListMergingMode();
		this.customID = this.configuration.getCustomTraceID();
	}

	@Override
	protected WiseMLTreeReader createMerger(
			WiseMLTreeReader[] inputs, String id) {
		return new TraceMerger(this, inputs, configuration, resources, id);
	}

}
