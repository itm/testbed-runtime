package de.itm.uniluebeck.tr.wiseml.merger.internals.tree;

public interface WiseMLTreeReader {
	
	public boolean isList();
	public boolean isMappedToTag();
	
	public WiseMLTreeReader getSubElementReader();
	public boolean nextSubElementReader();
	public WiseMLTreeReader getParentReader();
	
	public boolean isFinished();
	
	public void exception(String message, Throwable throwable);

}
