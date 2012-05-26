package de.uniluebeck.itm.gtr;

public interface LocalNodeNameListener {

	void beforeLocalNodeNameAdded(String localNodeName);

	void afterLocalNodeNameAdded(String localNodeName);

	void beforeLocalNodeNameRemoved(String localNodeName);

	void afterLocalNodeNameRemoved(String localNodeName);

}
