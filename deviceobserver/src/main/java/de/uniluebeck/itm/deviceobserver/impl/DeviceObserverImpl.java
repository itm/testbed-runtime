/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                  *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote *
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.deviceobserver.impl;

import de.uniluebeck.itm.deviceobserver.DeviceEventListener;
import de.uniluebeck.itm.deviceobserver.DeviceObserver;
import de.uniluebeck.itm.deviceobserver.exception.DeviceNotConnectableException;
import de.uniluebeck.itm.deviceobserver.exception.DeviceNotDisconnectableException;
import de.uniluebeck.itm.deviceobserver.util.CSVMoteListExtractor;
import de.uniluebeck.itm.deviceobserver.util.DeviceObserverUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This class is for checking of connected or disconnected devices
 * the main entry point is the start() method
 */
public class DeviceObserverImpl implements DeviceObserver {

	private Map<DeviceEventListener, Map<String, String>> deviceEventListenerMap;
	private final static Logger logger = LoggerFactory.getLogger(DeviceObserverImpl.class);
	private final long initialDelay;
	private final long period;
	private String[] csvDevices = null;
	private final ScheduledThreadPoolExecutor executor;
	private List<Class> deviceEventListeners;

	/**
	 * Default constructor
	 */
	public DeviceObserverImpl() {
		this(0, 5);
	}

	/**
	 * optional constructor setting the initial delay and period for the timing of the executor
	 *
	 * @param initialDelay defines the initial delay when to run the first test of devices
	 * @param period	   defines the period of checking for connection or disconnection
	 */
	public DeviceObserverImpl(final long initialDelay, final long period) {
		this.executor = new ScheduledThreadPoolExecutor(1);
		this.deviceEventListenerMap = new HashMap<DeviceEventListener, Map<String, String>>();
		this.initialDelay = initialDelay;
		this.period = period;
		this.deviceEventListeners = new ArrayList<Class>();
	}

	protected synchronized void setCsvDevices(String[] csv) {
		this.csvDevices = csv;
	}

	/**
	 * creates a map containing the device as key-value pairs
	 *
	 * @param device device as string with csv
	 * @return
	 */
	private synchronized Map<String, String> createDeviceMap(String device) {
		Map<String, String> deviceMap = new HashMap<String, String>();
		try {
			StringTokenizer tokenizer = new StringTokenizer(device, ",");
			deviceMap.put(DeviceObserverUtils.KEY_REFERENCE_ID, tokenizer.nextToken());
			deviceMap.put(DeviceObserverUtils.KEY_DEVICE_PORT, tokenizer.nextToken());
			deviceMap.put(DeviceObserverUtils.KEY_DEVICE_TYPE, tokenizer.nextToken());
		} catch (Exception e) {
			logger.warn("Could not create DeviceMap: " + e.getMessage());
		}
		return deviceMap;
	}

	@Override
	public synchronized void start() {
		logger.info("Device Observer started...");
		Runnable runnableObserver = new Runnable() {
			@Override
			public void run() {
				//first update the csvDevice-String containing all devices extracted from the motelist-script
				setCsvDevices(CSVMoteListExtractor.getDeviceListAsCSV());
				//check for connected or disconnected devices and add or remove them
				addAndRemoveListener();
			}
		};
		executor.scheduleAtFixedRate(runnableObserver, initialDelay, period, TimeUnit.SECONDS);
	}

	@Override
	public synchronized void stop() {
		deviceEventListenerMap.clear();
		executor.shutdown();
		logger.info("Device Observer stopped...");
	}

	@Override
	public synchronized boolean addListener(DeviceEventListener deviceEventListener) {
		return deviceEventListeners.add(deviceEventListener.getClass());
	}

	@Override
	public synchronized boolean removeListener(DeviceEventListener deviceEventListener) {
		return deviceEventListeners.remove(deviceEventListener.getClass());
	}

	//check for to be removed Listeners
	protected synchronized void addAndRemoveListener() {
		addDeviceEventListeners();
		removeDeviceEventListeners();
	}

	public Set<DeviceEventListener> getEventListenerInstances() {
		return this.deviceEventListenerMap.keySet();
	}

	private synchronized void addDeviceEventListeners() {
		for (String line : csvDevices) {
			Map<String, String> deviceMap = createDeviceMap(line);
			if (containsDevice(deviceMap)) {
				continue;
			}

			if (deviceEventListeners.size() == 0) {
				logger.warn("No DeviceEventListener-Instance available!");
				continue;
			}
			for (Class listener : deviceEventListeners) {
				try {
					DeviceEventListener deviceEventListenerInstance = (DeviceEventListener) listener.newInstance();
					deviceEventListenerInstance.connected(deviceMap);
					deviceEventListenerMap.put(deviceEventListenerInstance, deviceMap);
				} catch (InstantiationException e) {
					logger.warn(e.getLocalizedMessage());
				} catch (IllegalArgumentException e) {
					logger.warn(e.getLocalizedMessage());
				} catch (IllegalAccessException e) {
					logger.warn(e.getLocalizedMessage());
				} catch (DeviceNotConnectableException e) {
					logger.warn("Could not connect device: " + deviceMap);
				}
			}
		}
	}

	private synchronized void removeDeviceEventListeners() {
		for (DeviceEventListener deviceEventListener : getToBeRemovedEventListeners()) {
			Map<String, String> deviceMap = this.deviceEventListenerMap.remove(deviceEventListener);

			try {
				deviceEventListener.disconnected(deviceMap);
			} catch (DeviceNotDisconnectableException e) {
				logger.warn("Could not disconnect device {}", deviceMap);
			}
		}
	}

	private synchronized List<DeviceEventListener> getToBeRemovedEventListeners() {
		List<DeviceEventListener> removedEventListeners = new ArrayList<DeviceEventListener>();
		for (DeviceEventListener deviceEventListener : deviceEventListenerMap.keySet()) {
			Map<String, String> deviceMap = deviceEventListenerMap.get(deviceEventListener);
			if (deviceRemoved(deviceMap)) {
				removedEventListeners.add(deviceEventListener);
			}
		}
		return removedEventListeners;
	}

	/**
	 * helper method to check if device already has an event listener
	 *
	 * @param deviceMap
	 * @return
	 */
	private boolean containsDevice(Map<String, String> deviceMap) {
		for (DeviceEventListener deviceEventListener : deviceEventListenerMap.keySet()) {
			if (deviceMap.equals(deviceEventListenerMap.get(deviceEventListener))) return true;
		}
		return false;

	}

	/**
	 * helper method to check if device is removed
	 *
	 * @param deviceMap
	 * @return
	 */
	private boolean deviceRemoved(Map<String, String> deviceMap) {
		for (String line : csvDevices) {
			Map<String, String> deviceCSVMap = createDeviceMap(line);
			if (deviceCSVMap.equals(deviceMap)) return false;
		}
		return true;
	}
}
