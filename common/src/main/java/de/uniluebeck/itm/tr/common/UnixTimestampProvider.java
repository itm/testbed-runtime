package de.uniluebeck.itm.tr.common;

import org.joda.time.DateTime;

public class UnixTimestampProvider implements TimestampProvider {

	@Override
	public Long get() {
		return DateTime.now().getMillis();
	}
}
