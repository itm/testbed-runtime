package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.eventbus.EventBus;

public interface EventBusFactory {

	EventBus create(String name);

}
