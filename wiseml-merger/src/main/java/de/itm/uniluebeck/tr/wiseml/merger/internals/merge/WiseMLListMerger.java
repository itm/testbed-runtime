package de.itm.uniluebeck.tr.wiseml.merger.internals.merge;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLListReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public abstract class WiseMLListMerger extends WiseMLTreeMerger implements WiseMLListReader {

	protected WiseMLListMerger(WiseMLTreeMerger parent,
			WiseMLTreeReader[] inputs, MergerConfiguration configuration,
			MergerResources resources) {
		super(parent, inputs, configuration, resources);
		// TODO Auto-generated constructor stub
	}
// TODO
	
	@Override
	public boolean isList() {
		return true;
	}

	@Override
	public boolean isMappedToTag() {
		return false;
	}

}
