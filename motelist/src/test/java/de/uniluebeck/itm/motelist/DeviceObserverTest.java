package de.uniluebeck.itm.motelist;


import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DeviceObserverTest {

	private DeviceObserver deviceObserver;

	@Mock
	private DeviceObserverCsvProvider deviceObserverCsvProvider;

	private final String device1Csv = "01234,/dev/ttyUSB0,isense";

	private final String device2Csv = "12345,/dev/ttyUSB1,telosb";

	private final String device3Csv = "23456,/dev/ttyUSB2,pacemate";

	private final String device4Csv = "34567,/dev/ttyUSB3,isense";

	private DeviceInfo device1Info = new DeviceInfo("/dev/ttyUSB0", "01234", "isense");

	private DeviceInfo device2Info = new DeviceInfo("/dev/ttyUSB1", "12345", "telosb");

	private DeviceInfo device3Info = new DeviceInfo("/dev/ttyUSB2", "23456", "pacemate");

	private DeviceInfo device4Info = new DeviceInfo("/dev/ttyUSB3", "34567", "isense");

	private final DeviceEvent device1AttachedEvent = new DeviceEvent(DeviceEvent.Type.ATTACHED, device1Info);

	private final DeviceEvent device2AttachedEvent = new DeviceEvent(DeviceEvent.Type.ATTACHED, device2Info);

	private final DeviceEvent device3AttachedEvent = new DeviceEvent(DeviceEvent.Type.ATTACHED, device3Info);

	private final DeviceEvent device4AttachedEvent = new DeviceEvent(DeviceEvent.Type.ATTACHED, device4Info);

	private final DeviceEvent device1RemovedEvent = new DeviceEvent(DeviceEvent.Type.REMOVED, device1Info);

	private final DeviceEvent device2RemovedEvent = new DeviceEvent(DeviceEvent.Type.REMOVED, device2Info);

	private final DeviceEvent device3RemovedEvent = new DeviceEvent(DeviceEvent.Type.REMOVED, device3Info);

	private final DeviceEvent device4RemovedEvent = new DeviceEvent(DeviceEvent.Type.REMOVED, device4Info);

	@Before
	public void setUp() {
		final Injector injector = Guice.createInjector(new Module() {
			@Override
			public void configure(final Binder binder) {
				binder.bind(DeviceObserverCsvProvider.class).toInstance(deviceObserverCsvProvider);
				binder.bind(DeviceObserver.class).to(DeviceObserverImpl.class);
			}
		}
		);
		deviceObserver = injector.getInstance(DeviceObserver.class);
	}

	@Test
	public void noDeviceFoundNoDeviceAttached() {
		setObserverStateForCsvRows();
		assertEquals(0, deviceObserver.getEvents().size());
	}

	@Test
	public void singleDeviceFoundWhenNoDevicesWereAttachedBefore() {

		setObserverStateForCsvRows();

		final ImmutableList<DeviceEvent> events = getObserverEventsForCsvRows(device1Csv);

		assertEquals(1, events.size());
		final DeviceEvent actualEvent = events.iterator().next();
		assertEquals(device1AttachedEvent, actualEvent);
	}

	@Test
	public void singleDeviceFoundWhenSomeDevicesWereAttachedBefore() {

		setObserverStateForCsvRows(device1Csv, device2Csv);

		final ImmutableList<DeviceEvent> events = getObserverEventsForCsvRows(device1Csv, device2Csv, device3Csv);

		assertEquals(1, events.size());
		final DeviceEvent actualEvent = events.iterator().next();
		assertEquals(device3AttachedEvent, actualEvent);
	}

	@Test
	public void multipleDevicesFoundWhenNoDevicesWereAttachedBefore() {

		setObserverStateForCsvRows();

		final ImmutableList<DeviceEvent> events = getObserverEventsForCsvRows(device1Csv, device2Csv);

		assertEquals(2, events.size());
		final UnmodifiableIterator<DeviceEvent> iterator = events.iterator();
		assertEquals(device1AttachedEvent, iterator.next());
		assertEquals(device2AttachedEvent, iterator.next());
	}

	@Test
	public void multipleDevicesFoundWhenMultipleDevicesWereAttachedBefore() {

		setObserverStateForCsvRows(device1Csv, device2Csv);

		final ImmutableList<DeviceEvent> events =
				getObserverEventsForCsvRows(device1Csv, device2Csv, device3Csv, device4Csv);
		assertEquals(2, events.size());
		final UnmodifiableIterator<DeviceEvent> iterator = events.iterator();
		assertEquals(device3AttachedEvent, iterator.next());
		assertEquals(device4AttachedEvent, iterator.next());
	}

	@Test
	public void singleDeviceRemovedWhenOnlyOneDeviceWasAttachedBefore() {

		setObserverStateForCsvRows(device1Csv);

		final ImmutableList<DeviceEvent> events = getObserverEventsForCsvRows();
		assertEquals(1, events.size());
		assertEquals(device1RemovedEvent, events.iterator().next());
	}

	@Test
	public void singleDeviceRemovedWhenMultipleDevicesWereAttachedBefore() {

		setObserverStateForCsvRows(device1Csv, device2Csv);

		final ImmutableList<DeviceEvent> events = getObserverEventsForCsvRows(device1Csv);
		assertEquals(1, events.size());
		assertEquals(device2RemovedEvent, events.iterator().next());
	}

	@Test
	public void multipleDevicesRemovedWhenOnlyTheseWereAttachedBefore() {

		setObserverStateForCsvRows(device1Csv, device2Csv);

		final ImmutableList<DeviceEvent> events = getObserverEventsForCsvRows();
		assertEquals(2, events.size());
		assertEquals(device1RemovedEvent, events.get(0));
		assertEquals(device2RemovedEvent, events.get(1));
	}

	@Test
	public void multipleDevicesRemovedWhenSomeDevicesBefore() {

		setObserverStateForCsvRows(device1Csv, device2Csv, device3Csv, device4Csv);

		final ImmutableList<DeviceEvent> events = getObserverEventsForCsvRows(device1Csv, device2Csv);
		assertEquals(2, events.size());
		assertEquals(device3RemovedEvent, events.get(0));
		assertEquals(device4RemovedEvent, events.get(1));
	}

	private ImmutableList<DeviceEvent> getObserverEventsForCsvRows(final String... csvRows) {
		setCsvProviderState(csvRows);
		return deviceObserver.getEvents();
	}

	private void setObserverStateForCsvRows(final String... csvRows) {
		setCsvProviderState(csvRows);
		deviceObserver.getEvents();
	}

	private void setCsvProviderState(final String... csvRows) {
		if (csvRows.length == 0) {
			when(deviceObserverCsvProvider.getMoteCsv()).thenReturn("");
		} else {
			when(deviceObserverCsvProvider.getMoteCsv()).thenReturn(Joiner.on("\n").join(csvRows));
		}
	}

}
