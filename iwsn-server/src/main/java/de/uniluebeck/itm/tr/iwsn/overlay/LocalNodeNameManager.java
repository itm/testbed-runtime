package de.uniluebeck.itm.tr.iwsn.overlay;

import com.google.common.collect.ImmutableSet;
import de.uniluebeck.itm.tr.util.Listenable;

public interface LocalNodeNameManager extends Listenable<LocalNodeNameListener> {

	LocalNodeNameManager addLocalNodeName(String localNodeName);

	ImmutableSet<String> getLocalNodeNames();

	LocalNodeNameManager removeLocalNodeName(String localNodeName);
}
