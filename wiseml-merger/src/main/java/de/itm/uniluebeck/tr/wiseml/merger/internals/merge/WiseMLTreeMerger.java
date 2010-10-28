package de.itm.uniluebeck.tr.wiseml.merger.internals.merge;

import java.util.LinkedList;
import java.util.List;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReaderHelper;

public abstract class WiseMLTreeMerger implements WiseMLTreeReader {
	
	protected WiseMLTreeReader[] inputs;
	protected boolean finished;
	protected WiseMLTreeMerger parent;
	protected WiseMLTreeReader currentChild;
	
	protected MergerConfiguration configuration;
	protected MergerResources resources;
	
	protected List<WiseMLTreeReader> queue;
	
	protected WiseMLTreeMerger(
			final WiseMLTreeMerger parent, 
			final WiseMLTreeReader[] inputs,
			final MergerConfiguration configuration,
			final MergerResources resources) {
		this.parent = parent;
		this.inputs = inputs;
		
		this.finished = false;
		this.currentChild = null;
		
		if (parent == null) {
			this.configuration = configuration;
			this.resources = resources;
		} else {
			this.configuration = parent.configuration;
			this.resources = parent.resources;
		}

		this.queue = new LinkedList<WiseMLTreeReader>();
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
		
		finished = true;
		
		return false;
	}
	
	protected abstract void fillQueue();
	
	@Override
	public boolean isFinished() {
		return finished;
	}

	@Override
	public WiseMLTreeReader getParentReader() {
		return parent;
	}

	@Override
	public WiseMLTreeReader getSubElementReader() {
		return currentChild;
	}

	@Override
	public void exception(String message, Throwable throwable) {
		throw new WiseMLTreeMergerException(message, throwable, this);
	}
	
	protected void warn(String message) {
		System.err.println("WARNING: "+message); // TODO
	}
	
	protected static WiseMLTreeReader[] getListReaders(
			final WiseMLTag tag,
			final WiseMLTreeReader[] inputs,
			final boolean[] skip) {
		for (int i = 0; i < skip.length; i++) {
			if (skip[i]) {
				inputs[i].nextSubElementReader();
				skip[i] = false;
			}
		}
		
		boolean found = false;
		WiseMLTreeReader[] nodeListInputs = new WiseMLTreeReader[inputs.length];
		for (int i = 0; i < inputs.length; i++) {
			if (inputs[i].isFinished()) {
				continue;
			}
			
			WiseMLTreeReader nextReader = inputs[i].getSubElementReader();
			
			
			if (nextReader.isFinished()) {
				inputs[i].nextSubElementReader();
				nextReader = inputs[i].getSubElementReader();
			}
			if (nextReader.isList()) {
				if (nextReader.getSubElementReader() == null) {
					if (!nextReader.nextSubElementReader()) {
						return null;
					}
				}
				if (nextReader.getSubElementReader().getTag().equals(tag)) {
					found = true;
					nodeListInputs[i] = nextReader;
					skip[i] = true;
				}
			}
		}
		return found?nodeListInputs:null;
	}

}
