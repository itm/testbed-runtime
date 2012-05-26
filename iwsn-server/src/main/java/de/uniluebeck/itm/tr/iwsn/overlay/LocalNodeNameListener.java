package de.uniluebeck.itm.tr.iwsn.overlay;

public interface LocalNodeNameListener {

	void beforeLocalNodeNameAdded(String localNodeName);

	void afterLocalNodeNameAdded(String localNodeName);

	void beforeLocalNodeNameRemoved(String localNodeName);

	void afterLocalNodeNameRemoved(String localNodeName);

}
