package de.uniluebeck.itm.tr.plugins.defaultimage;

import org.joda.time.Duration;

public interface NodeStatusTrackerFactory {

	NodeStatusTracker create(Duration minUnreservedDuration);

}
