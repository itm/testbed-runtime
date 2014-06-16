package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import de.uniluebeck.itm.util.scheduler.SchedulerService;

public class EventBusFactoryImpl implements EventBusFactory {

	private final SchedulerService schedulerService;

	@Inject
	public EventBusFactoryImpl(final SchedulerService schedulerService) {
		this.schedulerService = schedulerService;
	}

	@Override
	public EventBus create(final String name) {
		return new AsyncEventBus(name, schedulerService);
	}
}
