package de.itm.uniluebeck.tr.wiseml.merger.internals.merge;

import java.util.List;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLAttribute;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public abstract class WiseMLListMerger extends WiseMLTreeMerger implements WiseMLTreeReader {

	protected WiseMLListMerger(WiseMLTreeMerger parent,
			WiseMLTreeReader[] inputs, MergerConfiguration configuration,
			MergerResources resources) {
		super(parent, inputs, configuration, resources);
		// TODO Auto-generated constructor stub
	}
// TODO
	
	@Override
	public final boolean isList() {
		return true;
	}

	@Override
	public final boolean isMappedToTag() {
		return false;
	}

	@Override
	public final List<WiseMLAttribute> getAttributeList() {
		return null;
	}

	@Override
	public final WiseMLTag getTag() {
		return null;
	}

	@Override
	public final String getText() {
		return null;
	}

}
