package de.uniluebeck.itm.gtr;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.uniluebeck.itm.tr.util.ListenerManager;
import de.uniluebeck.itm.tr.util.ListenerManagerImpl;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;

@Singleton
class LocalNodeNameManagerImpl implements LocalNodeNameManager {

	private ListenerManager<LocalNodeNameListener> listenerManager = new ListenerManagerImpl<LocalNodeNameListener>();

	private ImmutableSet<String> localNodeNames = ImmutableSet.of();

	@Override
	public void addLocalNodeName(final String localNodeName) {
		if (!localNodeNames.contains(localNodeName)) {
			notifyBeforeAdd(localNodeName);
			localNodeNames = ImmutableSet.<String>builder().addAll(localNodeNames).add(localNodeName).build();
			notifyAfterAdd(localNodeName);
		}
	}

	@Override
	public ImmutableSet<String> getLocalNodeNames() {
		return localNodeNames;
	}

	@Override
	public void removeLocalNodeName(final String localNodeName) {
		if (localNodeNames.contains(localNodeName)) {
			notifyBeforeRemove(localNodeName);
			localNodeNames = ImmutableSet.copyOf(Iterables.filter(localNodeNames, not(equalTo(localNodeName))));
			notifyAfterRemove(localNodeName);
		}
	}

	@Override
	public void addListener(final LocalNodeNameListener listener) {
		listenerManager.addListener(listener);
	}

	@Override
	public void removeListener(final LocalNodeNameListener listener) {
		listenerManager.removeListener(listener);
	}

	private void notifyBeforeAdd(final String localNodeName) {
		for (LocalNodeNameListener listener : listenerManager.getListeners()) {
			listener.beforeLocalNodeNameAdded(localNodeName);
		}
	}

	private void notifyAfterAdd(final String localNodeName) {
		for (LocalNodeNameListener listener : listenerManager.getListeners()) {
			listener.afterLocalNodeNameAdded(localNodeName);
		}
	}

	private void notifyBeforeRemove(final String localNodeName) {
		for (LocalNodeNameListener listener : listenerManager.getListeners()) {
			listener.beforeLocalNodeNameRemoved(localNodeName);
		}
	}

	private void notifyAfterRemove(final String localNodeName) {
		for (LocalNodeNameListener listener : listenerManager.getListeners()) {
			listener.afterLocalNodeNameRemoved(localNodeName);
		}
	}
}
