package de.uniluebeck.itm.motelist;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.util.AbstractListenable;

import java.util.List;


public class DeviceObserverImpl extends AbstractListenable<DeviceObserverListener> implements DeviceObserver {

	private static final int CSV_INDEX_PORT = 1;

	private static final int CSV_INDEX_REFERENCE = 0;

	private static final int CSV_INDEX_TYPE = 2;

	private ImmutableList<DeviceInfo> oldInfos = ImmutableList.of();

	private final Splitter rowSplitter = Splitter.on("\n").trimResults();

	private final Splitter colSplitter = Splitter.on(",").trimResults();

	@Inject
	private MoteCsvProvider moteCsvProvider;

	@Override
	public ImmutableList<DeviceEvent> getMoteEvents() {

		final Iterable<String> rows = rowSplitter.split(moteCsvProvider.getMoteCsv());
		final ImmutableList.Builder<DeviceInfo> newStatesBuilder = ImmutableList.builder();

		for (String row : rows) {
			DeviceInfo info = parseRow(row);
			if (info != null) {
				newStatesBuilder.add(info);
			}
		}

		final ImmutableList<DeviceInfo> newInfos = newStatesBuilder.build();
		final ImmutableList<DeviceEvent> events = deriveEvents(newInfos);
		oldInfos = newInfos;
		return events;
	}

	private ImmutableList<DeviceEvent> deriveEvents(final List<DeviceInfo> newInfos) {

		final ImmutableList.Builder<DeviceEvent> resultBuilder = ImmutableList.builder();
		resultBuilder.addAll(deriveAttachedEvents(newInfos));
		resultBuilder.addAll(deriveRemovedEvents(newInfos));
		return resultBuilder.build();
	}


	private List<DeviceEvent> deriveAttachedEvents(final List<DeviceInfo> newInfos) {

		List<DeviceEvent> events = Lists.newArrayList();
		for (DeviceInfo newInfo : newInfos) {

			DeviceInfo oldInfo = getOldStateForPort(newInfo.port);
			if (oldInfo == null) {
				events.add(new DeviceEvent(DeviceEvent.Type.ATTACHED, newInfo));
			}
		}

		return events;
	}

	private List<DeviceEvent> deriveRemovedEvents(final List<DeviceInfo> newInfos) {

		List<DeviceEvent> events = Lists.newArrayList();
		for (DeviceInfo oldInfo : oldInfos) {

			boolean found = false;

			for (DeviceInfo newInfo : newInfos) {
				if (newInfo.port.equals(oldInfo.port)) {
					found = true;
				}
			}

			if (!found) {
				events.add(new DeviceEvent(DeviceEvent.Type.REMOVED, oldInfo));
			}
		}
		return events;
	}

	private DeviceInfo getOldStateForPort(final String port) {
		for (DeviceInfo oldInfo : oldInfos) {
			if (oldInfo.port.equals(port)) {
				return oldInfo;
			}
		}
		return null;
	}

	private DeviceInfo parseRow(final String row) {

		if ("".equals(row)) {
			return null;
		}

		final List<String> columns = Lists.newArrayList(colSplitter.split(row));
		if (columns.size() != 3) {
			throw new RuntimeException("Every row in the CSV file must have at least and at most three columns!");
		}

		return new DeviceInfo(
				columns.get(CSV_INDEX_PORT),
				columns.get(CSV_INDEX_REFERENCE),
				columns.get(CSV_INDEX_TYPE)
		);
	}

	@Override
	public void run() {
		for (DeviceEvent event : getMoteEvents()) {
			notifyListeners(event);
		}
	}

	private void notifyListeners(final DeviceEvent event) {
		for (DeviceObserverListener listener : listeners) {
			listener.deviceEvent(event);
		}
	}
}
