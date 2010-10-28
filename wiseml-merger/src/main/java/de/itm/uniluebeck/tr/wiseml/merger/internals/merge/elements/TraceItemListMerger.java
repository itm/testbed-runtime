package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLSequence;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.MergerResources;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.WiseMLTreeMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements.LinkTraceItemParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements.NodeItemParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.elements.LinkTraceItemReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.elements.NodeItemReader;
import de.itm.uniluebeck.tr.wiseml.merger.structures.LinkTraceItem;
import de.itm.uniluebeck.tr.wiseml.merger.structures.NodeItem;


public class TraceItemListMerger extends TimeStampedItemListMerger {

	public TraceItemListMerger(
			WiseMLTreeMerger parent,
			WiseMLTreeReader[] inputs, 
			MergerConfiguration configuration,
			MergerResources resources) {
		super(parent, inputs, configuration, resources, WiseMLSequence.TraceItem);
	}

	@Override
	protected void handleReader(WiseMLTreeReader reader, int inputIndex) {
		switch (reader.getTag()) {
		case node: {
			NodeItem nodeItem = new NodeItemParser(reader).getParsedStructure();
			
			// transform
			nodeItem = this.resources.getNodeItemTransformer().transform(
					nodeItem, inputIndex);
			
			this.queue.add(new NodeItemReader(this, nodeItem));
		}	break;
		case link: {
			LinkTraceItem linkTraceItem = new LinkTraceItemParser(reader)
				.getParsedStructure();
			
			this.queue.add(new LinkTraceItemReader(this, linkTraceItem));
		}	break;
		default:
			reader.exception("unexpected tag: "+reader.getTag(), null);
		}
	}

}
