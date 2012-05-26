package de.uniluebeck.itm.tr.iwsn.overlay;

import com.google.common.collect.ImmutableSet;
import de.uniluebeck.itm.tr.util.Listenable;

public interface LocalNodeNameManager extends Listenable<LocalNodeNameListener> {

	void addLocalNodeName(String localNodeName);

	ImmutableSet<String> getLocalNodeNames();

	void removeLocalNodeName(String localNodeName);
}
