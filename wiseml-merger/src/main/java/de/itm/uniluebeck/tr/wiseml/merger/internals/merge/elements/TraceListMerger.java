package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import java.util.Collection;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.MergerResources;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.SortedListMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.WiseMLTreeMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public class TraceListMerger extends SortedListMerger<TraceDefinition> {

	public TraceListMerger(
			final WiseMLTreeMerger parent,
			final WiseMLTreeReader[] inputs, 
			final MergerConfiguration configuration,
			final MergerResources resources) {
		super(parent, inputs, configuration, resources);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected WiseMLTreeReader mergeItems(Collection<TraceDefinition> items) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected TraceDefinition readNextItem(int inputIndex) {
		// TODO Auto-generated method stub
		return null;
	}


}
