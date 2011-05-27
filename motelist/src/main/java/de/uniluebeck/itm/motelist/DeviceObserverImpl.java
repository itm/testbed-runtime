/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or        *
 *   promote products derived from this software without specific prior written permission.                           *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.motelist;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.util.AbstractListenable;

import java.util.List;

class DeviceObserverImpl extends AbstractListenable<DeviceObserverListener> implements DeviceObserver {

	private static final int CSV_INDEX_PORT = 1;

	private static final int CSV_INDEX_REFERENCE = 0;

	private static final int CSV_INDEX_TYPE = 2;

	private ImmutableList<DeviceInfo> oldInfos = ImmutableList.of();

	private final Splitter rowSplitter = Splitter.on("\n").trimResults();

	private final Splitter colSplitter = Splitter.on(",").trimResults();

	@Inject
	private DeviceObserverCsvProvider deviceObserverCsvProvider;

	@Override
	public ImmutableList<DeviceEvent> getEvents() {

		final Iterable<String> rows = rowSplitter.split(deviceObserverCsvProvider.getMoteCsv());
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

	@Override
	public void run() {
		for (DeviceEvent event : getEvents()) {
			notifyListeners(event);
		}
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

	private void notifyListeners(final DeviceEvent event) {
		for (DeviceObserverListener listener : listeners) {
			listener.deviceEvent(event);
		}
	}
}
