package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import java.util.Collection;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLSequence;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.MergerResources;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.SortedListMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.WiseMLTreeMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements.NodePropertiesParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReaderHelper;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.elements.NodePropertiesReader;
import de.itm.uniluebeck.tr.wiseml.merger.structures.NodeProperties;

public class NodeListMerger extends SortedListMerger<NodeDefinition> {

	public NodeListMerger(
			final WiseMLTreeMerger parent,
			final WiseMLTreeReader[] inputs, 
			final MergerConfiguration configuration,
			final MergerResources resources) {
		super(parent, inputs, configuration, resources, WiseMLSequence.SetupNode);
	}

	@Override
	protected WiseMLTreeReader mergeItems(Collection<NodeDefinition> items) {
		NodeDefinition firstItem = null;
		String outputID = null;
		NodeProperties outputProperties = null;
		for (NodeDefinition item : items) {
			if (firstItem == null) {
				firstItem = item;
				outputID = firstItem.getId();
				outputProperties = firstItem.getNodeProperties();
			} else {
				if (!item.getNodeProperties().equals(firstItem.getNodeProperties())) {
					// conflict
					// TODO
				}
			}
		}
		
		return new NodePropertiesReader(this, outputID, outputProperties);
	}

	@Override
	protected NodeDefinition readNextItem(int inputIndex) {
		if (!nextSubInputReader(inputIndex)) {
			return null;
		}
		WiseMLTreeReader nodeReader = getSubInputReader(inputIndex);
		
		// parse properties and transform
		NodeProperties properties = 
			new NodePropertiesParser(nodeReader).getParsedStructure();
		properties = resources.getNodePropertiesTransformer().transform(
				properties, inputIndex);
		
		NodeDefinition result = new NodeDefinition(
				WiseMLTreeReaderHelper.getAttributeValue(
						nodeReader.getAttributeList(), "id"),
						properties,
						inputIndex);
		return result;
	}

}
