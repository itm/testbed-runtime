package de.uniluebeck.itm.tr.plugins.defaultimage;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.common.rest.RestApplicationBase;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

public class NodeStatusTrackerApplication extends RestApplicationBase {

	private final NodeStatusTrackerResource nodeStatusTrackerResource;

	@Inject
	public NodeStatusTrackerApplication(
			final NodeStatusTrackerResource nodeStatusTrackerResource) {
		this.nodeStatusTrackerResource = nodeStatusTrackerResource;
	}

	@Override
	public Set<?> getSingletonsInternal() {
		return newHashSet(nodeStatusTrackerResource);
	}
}
