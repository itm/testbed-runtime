package de.itm.uniluebeck.tr.wiseml.merger.internals.merge;

import java.util.List;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLAttribute;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLSequence;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public abstract class WiseMLListMerger extends WiseMLTreeMerger implements WiseMLTreeReader {

	private WiseMLSequence sequence;
	
	protected WiseMLListMerger(
			final WiseMLTreeMerger parent,
			final WiseMLTreeReader[] inputs, 
			final MergerConfiguration configuration,
			final MergerResources resources,
			final WiseMLSequence sequence) {
		super(parent, inputs, configuration, resources);
		this.sequence = sequence;
	}
	
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

	@Override
	public WiseMLSequence getSequence() {
		return sequence;
	}

}
