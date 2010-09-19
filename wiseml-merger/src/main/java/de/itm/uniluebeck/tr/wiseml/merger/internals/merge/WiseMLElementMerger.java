package de.itm.uniluebeck.tr.wiseml.merger.internals.merge;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLAttribute;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReaderHelper;

public abstract class WiseMLElementMerger extends WiseMLTreeMerger {
	
	private WiseMLTag tag;
	protected List<WiseMLTreeReader> queue;
	private List<WiseMLAttribute> attributeList;
	
	public WiseMLElementMerger(
			WiseMLTreeMerger parent,
			WiseMLTreeReader[] inputs, 
			MergerConfiguration configuration,
			MergerResources resources, 
			WiseMLTag tag) {
		super(parent, inputs, configuration, resources);
		this.tag = tag;
		this.queue = new LinkedList<WiseMLTreeReader>();
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
	
	@Override
	public boolean nextSubElementReader() {
		if (finished) {
			return false;
		}
		
		if (currentChild != null) {
			WiseMLTreeReaderHelper.skipToEnd(currentChild);
			currentChild = null;
		}
		
		if (queue.isEmpty()) {
			fillQueue();
		}
		
		if (!queue.isEmpty()) {
			currentChild = queue.remove(0);
			return true;
		}
		
		return false;
	}
	
	protected abstract void fillQueue();
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
