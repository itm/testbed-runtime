package de.itm.uniluebeck.tr.wiseml.merger.internals.merge;

import java.util.ArrayList;
import java.util.List;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLAttribute;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public abstract class WiseMLElementMerger extends WiseMLTreeMerger {
	
	private WiseMLTag tag;
	protected List<WiseMLAttribute> attributeList;
	
	public WiseMLElementMerger(
			final WiseMLTreeMerger parent,
			final WiseMLTreeReader[] inputs, 
			final MergerConfiguration configuration,
			final MergerResources resources, 
			final WiseMLTag tag) {
		super(parent, inputs, configuration, resources);
		this.tag = tag;
		this.attributeList = new ArrayList<WiseMLAttribute>();
	}

	@Override
	public boolean isList() {
		return false;
	}

	@Override
	public boolean isMappedToTag() {
		return true;
	}

	@Override
	public List<WiseMLAttribute> getAttributeList() {
		return attributeList;
	}

	@Override
	public WiseMLTag getTag() {
		return tag;
	}
	/*
	protected static List<WiseMLElementReader> getSubElementReaders(final WiseMLElementReader reader) {
		List<WiseMLElementReader> result = new ArrayList<WiseMLElementReader>();
		while (!reader.isFinished()) {
			if (reader.nextSubElementReader()) {
				if (reader.getSubElementReader().isMappedToTag()) {
					result.add((WiseMLElementReader)reader.getSubElementReader());
					continue;
				}
			}
			break;
		}
		return result;
	}
	*/
	
	

	@Override
	public String getText() {
		return null; // TODO?
	}
	
	
}
