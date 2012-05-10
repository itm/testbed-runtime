package de.itm.uniluebeck.tr.wiseml.merger.internals.merge;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLSequence;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.ParserHelper;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReaderHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WiseMLTreeMerger implements WiseMLTreeReader {

	static final Logger log = LoggerFactory.getLogger(WiseMLTreeMerger.class);

	private WiseMLTreeReader[] inputs;
	protected boolean finished;
	protected WiseMLTreeMerger parent;
	protected WiseMLTreeReader currentChild;
	
	protected MergerConfiguration configuration;
	protected MergerResources resources;
	
	protected List<WiseMLTreeReader> queue;
	private boolean[] hold;
	
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
		
		this.hold = new boolean[inputs.length];
	}
	
	protected boolean isInputFinished(int input) {
		if (inputs[input] == null) {
			return true;
		}
		return inputs[input].isFinished();
	}
	
	protected void holdInput(int input) {
		hold[input] = true;
	}
	
	protected boolean nextSubInputReader(int input) {
		if (hold[input]) {
			hold[input] = false;
			return getSubInputReader(input) != null;
		}
		if (inputs[input] == null) {
			return false;
		}
		return inputs[input].nextSubElementReader();
	}
	
	protected WiseMLTreeReader getSubInputReader(int input) {
		if (inputs[input] == null) {
			return null;
		}
		return inputs[input].getSubElementReader();
	}
	
	protected int inputCount() {
		return inputs.length;
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
	
	protected final WiseMLTreeReader[] findSequenceReaders(WiseMLSequence sequence) {
		WiseMLTreeReader[] result = new WiseMLTreeReader[inputs.length];
		int numResults = 0;
		
		for (int i = 0; i < inputs.length; i++) {
			if (!nextSubInputReader(i)) {
				continue;
			}
			
			WiseMLTreeReader reader = getSubInputReader(i);
			if (reader.isList() && reader.getSequence().equals(sequence)) {
				result[i] = reader;
				numResults++;
			} else {
				holdInput(i);
			}
		}
		
		if (numResults == 0) {
			return null;
		}
		
		return result;
	}
	
	protected final WiseMLTreeReader[] findElementReaders(WiseMLTag tag) {
		WiseMLTreeReader[] result = new WiseMLTreeReader[inputs.length];
		int numResults = 0;
		
		for (int i = 0; i < inputs.length; i++) {
			if (!nextSubInputReader(i)) {
				continue;
			}
			
			WiseMLTreeReader reader = getSubInputReader(i);
			if (reader.isMappedToTag() && reader.getTag().equals(tag)) {
				result[i] = reader;
				numResults++;
			} else {
				holdInput(i);
			}
		}
		
		if (numResults == 0) {
			return null;
		}
		
		return result;
	}
	
	/*
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
*/

	protected final Map<WiseMLTag, Object> getStructures(final int input) {
		return ParserHelper.getStructures(inputs[input]);
	}
	
}
