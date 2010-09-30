package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import java.util.Collection;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.MergerResources;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.SortedListMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.WiseMLTreeMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements.NodePropertiesParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReaderHelper;
import de.itm.uniluebeck.tr.wiseml.merger.structures.NodeProperties;

public class ScenarioListMerger extends SortedListMerger<ScenarioDefinition> {

	public ScenarioListMerger(
			final WiseMLTreeMerger parent,
			final WiseMLTreeReader[] inputs, 
			final MergerConfiguration configuration,
			final MergerResources resources) {
		super(parent, inputs, configuration, resources);
	}

	@Override
	protected WiseMLTreeReader mergeItems(Collection<ScenarioDefinition> items) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ScenarioDefinition readNextItem(int inputIndex) {
		WiseMLTreeReader input = inputs[inputIndex];
		if (input.isFinished()) {
			return null;
		}
		if (input.getSubElementReader() == null 
				&& !input.nextSubElementReader()) {
			return null;
		}
		WiseMLTreeReader nodeReader = input.getSubElementReader();
		
		ScenarioDefinition result = new ScenarioDefinition(
				WiseMLTreeReaderHelper.getAttributeValue(
						nodeReader.getAttributeList(),
						"id"));
		
		input.nextSubElementReader();
		return result;
	}

	
}
