package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLAttribute;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLSequence;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.MergerResources;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.WiseMLTreeMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements.NodeItemParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.elements.CustomReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.elements.NodeItemReader;
import de.itm.uniluebeck.tr.wiseml.merger.structures.NodeItem;

public class ScenarioItemListMerger 
extends TimeStampedItemListMerger {

	public ScenarioItemListMerger(
			final WiseMLTreeMerger parent,
			final WiseMLTreeReader[] inputs, 
			final MergerConfiguration configuration,
			final MergerResources resources) {
		super(parent, inputs, configuration, resources, WiseMLSequence.ScenarioItem);
	}

	@Override
	protected void handleReader(WiseMLTreeReader reader, int inputIndex) {
		switch (reader.getTag()) {
		case enableNode:
		case disableNode:
		case enableLink:
		case disableLink: {
			this.queue.add(new CustomReader(
					this, 
					reader.getTag(), 
					reader.getAttributeList().toArray(new WiseMLAttribute[0]), 
					reader.getText()));
		}	break;
		case node: {
			NodeItem nodeItem = new NodeItemParser(reader).getParsedStructure();
			
			// transform
			nodeItem = this.resources.getNodeItemTransformer().transform(
					nodeItem, inputIndex);
			
			this.queue.add(new NodeItemReader(this, nodeItem));
		} 	break;
		default:
			reader.exception("unexpected tag: "+reader.getTag(), null);
		}
	}
	
}
