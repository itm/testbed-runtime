package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLAttribute;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.MergerResources;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.WiseMLElementMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.WiseMLTreeMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public class TraceMerger extends WiseMLElementMerger {

	public TraceMerger(
			final WiseMLTreeMerger parent, 
			final WiseMLTreeReader[] inputs,
			final MergerConfiguration configuration, 
			final MergerResources resources,
			final String id) {
		super(parent, inputs, configuration, resources, WiseMLTag.trace);
		this.attributeList.add(new WiseMLAttribute("id", id));
	}

	@Override
	protected void fillQueue() {
		WiseMLTreeReader[] readers = new WiseMLTreeReader[inputs.length];
		for (int i = 0; i < readers.length; i++) {
			if (inputs[i].isFinished()) {
				return;
			}
			if (inputs[i].getSubElementReader() == null 
					&& !inputs[i].nextSubElementReader()) {
				return;
			}
			readers[i] = inputs[i].getSubElementReader();
		}
		queue.add(new TraceItemListMerger(
				this, readers, configuration, resources));
	}

}
