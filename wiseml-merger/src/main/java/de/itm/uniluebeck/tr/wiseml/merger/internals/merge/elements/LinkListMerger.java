package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import java.util.Collection;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.MergerResources;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.SortedListMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.WiseMLTreeMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public class LinkListMerger extends SortedListMerger<LinkDefinition> {

	protected LinkListMerger(WiseMLTreeMerger parent,
			WiseMLTreeReader[] inputs, MergerConfiguration configuration,
			MergerResources resources) {
		super(parent, inputs, configuration, resources);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected WiseMLTreeReader mergeItems(Collection<LinkDefinition> items) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected LinkDefinition readNextItem(int inputIndex) {
		// TODO Auto-generated method stub
		return null;
	}

}
