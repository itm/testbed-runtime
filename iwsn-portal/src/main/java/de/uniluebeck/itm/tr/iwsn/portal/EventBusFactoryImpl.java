package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.eventbus.EventBus;

public class EventBusFactoryImpl implements EventBusFactory {

	@Override
	public EventBus create(final String name) {
		return new EventBus(name);
	}
}
