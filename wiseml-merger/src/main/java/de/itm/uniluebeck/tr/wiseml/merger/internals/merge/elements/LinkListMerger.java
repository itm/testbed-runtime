package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import java.util.Collection;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.MergerResources;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.SortedListMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.WiseMLTreeMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements.LinkPropertiesParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReaderHelper;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.elements.LinkPropertiesReader;
import de.itm.uniluebeck.tr.wiseml.merger.structures.LinkProperties;

public class LinkListMerger extends SortedListMerger<LinkDefinition> {

	public LinkListMerger(
			final WiseMLTreeMerger parent,
			final WiseMLTreeReader[] inputs, 
			final MergerConfiguration configuration,
			final MergerResources resources) {
		super(parent, inputs, configuration, resources);
	}

	@Override
	protected WiseMLTreeReader mergeItems(Collection<LinkDefinition> items) {
		LinkDefinition firstItem = null;
		String outputSource = null;
		String outputTarget = null;
		LinkProperties outputProperties = null;
		for (LinkDefinition item : items) {
			if (firstItem == null) {
				firstItem = item;
				outputSource = firstItem.getSource();
				outputTarget = firstItem.getTarget();
				outputProperties = firstItem.getLinkProperties();
			} else {
				if (!item.getLinkProperties().equals(firstItem.getLinkProperties())) {
					// conflict
					// TODO
				}
			}
		}
		return new LinkPropertiesReader(
				this, outputSource, outputTarget, outputProperties);
	}

	@Override
	protected LinkDefinition readNextItem(int inputIndex) {
		WiseMLTreeReader input = inputs[inputIndex];
		if (input.isFinished()) {
			return null;
		}
		if (input.getSubElementReader() == null && !input.nextSubElementReader()) {
			return null;
		}
		WiseMLTreeReader linkReader = input.getSubElementReader();
		
		// parse properties and transform
		LinkProperties properties = 
			new LinkPropertiesParser(linkReader).getParsedStructure();
		properties = resources.getLinkPropertiesTransformer().transform(
				properties, inputIndex);
		
		LinkDefinition result = new LinkDefinition(
				WiseMLTreeReaderHelper.getAttributeValue(
						linkReader.getAttributeList(), "source"),
				WiseMLTreeReaderHelper.getAttributeValue(
						linkReader.getAttributeList(), "target"),
				properties,
				inputIndex);
		input.nextSubElementReader();
		return result;
	}

}
