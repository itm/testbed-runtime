package de.itm.uniluebeck.tr.wiseml.merger.internals.tree;

import java.util.List;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLAttribute;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLSequence;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;

public interface WiseMLTreeReader {
	
	public boolean isList();
	public boolean isMappedToTag();
	
	public WiseMLTreeReader getSubElementReader();
	public boolean nextSubElementReader();
	public WiseMLTreeReader getParentReader();
	public boolean isFinished();
	
	public void exception(String message, Throwable throwable);

	// only if mapped to a specific WiseML-tag
	public List<WiseMLAttribute> getAttributeList();
	public WiseMLTag getTag();
	public String getText();
	
	// only if mapped to a sequence
	public WiseMLSequence getSequence();

}
