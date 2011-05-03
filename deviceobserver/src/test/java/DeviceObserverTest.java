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

import de.uniluebeck.itm.deviceobserver.DeviceEventListener;
import de.uniluebeck.itm.deviceobserver.exception.DeviceNotConnectableException;
import de.uniluebeck.itm.deviceobserver.exception.DeviceNotDisconnectableException;
import de.uniluebeck.itm.deviceobserver.factory.DeviceEventListenerFactory;
import de.uniluebeck.itm.deviceobserver.factory.DeviceObserverFactory;
import de.uniluebeck.itm.deviceobserver.impl.DeviceObserverImpl;
import de.uniluebeck.itm.deviceobserver.impl.MockDeviceEventListenerImpl;
import de.uniluebeck.itm.tr.util.Logging;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

public class DeviceObserverTest {

	DeviceObserverImpl deviceObserver;

	@Before
	public void setUp() {
		Logging.setLoggingDefaults();
		DeviceEventListenerFactory.setDeviceEventListenerImplementation(MockDeviceEventListenerImpl.class);
		deviceObserver = (DeviceObserverImpl) DeviceObserverFactory.createDeviceObserverInstance();
	}

	@Test
	public void testEventListenerSizeForValidInput() {
		//test for valid input
		//add 3 mock devices
		deviceObserver.setCsvDevices(CsvDeviceTestOutput.pacemate);
		deviceObserver.addListener();
		//test for 3 eventlisteners
		assertEquals(deviceObserver.getEventListeners().size(), 3);
		//check for removed devices
		deviceObserver.removeListener();
		assertEquals(deviceObserver.getEventListeners().size(), 3);
		//remove devices
		deviceObserver.setCsvDevices(new String[]{});
		deviceObserver.addListener();
		assertEquals(deviceObserver.getEventListeners().size(), 3);
		//check if all devices successfully removed
		deviceObserver.removeListener();
		assertEquals(deviceObserver.getEventListeners().size(), 0);
	}

	@Test
	public void testDeviceObserverForNotConnectableDevice(){
		//test for not valid input for not connectable device
		deviceObserver.setCsvDevices(CsvDeviceTestOutput.notConnectableDevice);
		deviceObserver.addListener();
		assertEquals(deviceObserver.getEventListeners().size(), 0);
		deviceObserver.removeListener();

	}

	@Test
	public void testDeviceEventListenerForNotDisConnectableDevice(){
		//test not connectable
		DeviceEventListener deviceEventListener = DeviceEventListenerFactory.createDeviceEventListenerInstance();
		try {
			deviceEventListener.connected(CsvDeviceTestOutput.notConnectableMap);
			fail("Should have Raised a NotConnectableException");
		} catch (DeviceNotConnectableException e) {

		}
		//test not disconnectable
		try {
			deviceEventListener.disconnected(CsvDeviceTestOutput.notConnectableMap);
			fail("Should have raised a NotDisconnectableException");
		} catch (DeviceNotDisconnectableException e){

		}
	}
}
