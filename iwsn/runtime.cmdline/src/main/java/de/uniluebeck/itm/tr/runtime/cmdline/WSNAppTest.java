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
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
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

package de.uniluebeck.itm.tr.runtime.cmdline;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.NamingThreadFactory;
import de.uniluebeck.itm.tr.util.StringUtils;
import eu.wisebed.testbed.api.rs.v1.SecretAuthenticationKey;
import eu.wisebed.testbed.api.wsn.Constants;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;
import eu.wisebed.testbed.api.wsn.v211.*;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebService;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.ws.Endpoint;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by IntelliJ IDEA. User: bimschas Date: 17.05.2010 Time: 14:34:46 TODO change
 */
public class WSNAppTest {

	private static final Logger log = LoggerFactory.getLogger(WSNAppTest.class);

	@WebService(
			serviceName = "ControllerService",
			targetNamespace = Constants.NAMESPACE_CONTROLLER_SERVICE,
			portName = "ControllerPort",
			endpointInterface = Constants.ENDPOINT_INTERFACE_CONTROLLER_SERVICE
	)
	public static class MyController implements Controller {

		private final Multimap<String, Integer> pendingRequests = HashMultimap.create(1000, 3);

		private final Object notifier = new Object();

		private int received = 0;

		private int receivedStatuses = 0;

		private int requested = 0;

		public void addPendingRequest(String requestId, Integer... finishCodes) {

			synchronized (pendingRequests) {
				for (Integer finishCode : finishCodes) {
					pendingRequests.put(requestId, finishCode);
				}
			}

			requested++;
		}

		public void receive(Message msg) {

			log.trace("Received message: " + StringUtils.jaxbMarshal(msg));
			received++;

		}

		public void receiveStatus(RequestStatus status) {

			log.trace("Received status update: " + StringUtils.jaxbMarshal(status));
			receivedStatuses++;

			Collection<Integer> finishCodesForRequestId;
			synchronized (pendingRequests) {
				finishCodesForRequestId = pendingRequests.get(status.getRequestId());
			}

			if (finishCodesForRequestId == null) {
				log.warn("No pending request for request ID {} found.", status.getRequestId());
				return;
			}

			for (Status s : status.getStatus()) {

				if (!finishCodesForRequestId.contains(s.getValue())) {
					log.info("Progress[" + s.getValue() + "] of nodeId" + s.getNodeId() + ", message" + s.getMsg());
				} else {
					log.info("Done[" + s.getValue() + "] of nodeId" + s.getNodeId() + ", message" + s.getMsg());
					synchronized (pendingRequests) {
						pendingRequests.removeAll(status.getRequestId());
					}
				}
			}

		}

	}

	private static final MyController controller = new MyController();

	public static void main(String[] args) throws Exception {

		org.apache.log4j.Logger.getRootLogger().setLevel(Level.DEBUG);

		//This is the endpoint of our local
		String localControllerEndpoint = "http://daniel-bimschass-macbook-pro.local:9090/testcontroller";

		//The endpoint of the Session Management
		String sessionManamentEndpoint = "http://daniel-bimschass-macbook-pro.local:10011/sessions";

		List<eu.wisebed.testbed.api.rs.v1.SecretReservationKey> reservation =
				generateFakeReservationKeys("urn:wisebed:testbeduzl1:", "dummy-secret-reservation-key");

		SessionManagement sm = WSNServiceHelper.getSessionManagementService(sessionManamentEndpoint);

		List<SecretReservationKey> secretReservationKeyList = copyRsToWsn(reservation);

		if (log.isDebugEnabled()) {
			log.debug("Using the following parameters for get instance: {}, {}", StringUtils.jaxbMarshal(secretReservationKeyList), localControllerEndpoint);
		}
		final String wsnApiEndpoint = sm.getInstance(secretReservationKeyList, localControllerEndpoint);

		log.info("Got an WSN instance URL, endpoint is: " + wsnApiEndpoint);
		final WSN wsn = WSNServiceHelper.getWSNService(wsnApiEndpoint);

		Endpoint endpoint = Endpoint.create(controller);
		endpoint.setExecutor(Executors.newCachedThreadPool());
		endpoint.publish(localControllerEndpoint);

		wsn.addController(localControllerEndpoint);

		Thread.sleep(2000);

		final String sourceNode = "urn:wisebed:testbeduzl1:1";
		final String targetNode = "urn:wisebed:testbeduzl1:2";

		// ==== TEST VIRTUAL LINKS ====
		for (int i = 0; i < 5; i++) {

			log.debug("+++ Setting virtual link from 1 to 2");
			String requestId = wsn.setVirtualLink(sourceNode, targetNode, wsnApiEndpoint, null, null);
			controller.addPendingRequest(requestId, 1, -1, 0);

			ExecutorService executorService =
					Executors.newFixedThreadPool(5, new NamingThreadFactory("WSNAppTest-Thread %d"));
			List<Callable<Void>> callables = new ArrayList<Callable<Void>>(i * 100);

			for (int j = 0; j < i * 100; j++) {

				callables.add(new Callable<Void>() {
					@Override
					public Void call() throws Exception {

						final DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
						final GregorianCalendar now = (GregorianCalendar) GregorianCalendar.getInstance();

						final BinaryMessage binaryMessage = new BinaryMessage();
						binaryMessage.setBinaryData(new byte[]{0, 1, 2, 3});
						binaryMessage.setBinaryType((byte) 0);

						final Message message = new Message();
						message.setSourceNodeId(sourceNode);
						message.setTimestamp(datatypeFactory.newXMLGregorianCalendar(now));
						message.setBinaryMessage(binaryMessage);

						wsn.send(Arrays.asList(targetNode), message);
						return null;
					}
				}
				);

			}

			// fork and join
			log.debug("============ Running {} send calls =============", i * 100);
			long before = System.currentTimeMillis();
			for (Future<Void> voidFuture : executorService.invokeAll(callables)) {
				voidFuture.get();
			}
			log.debug("+++++++++++++ Needed {} ms for {} send calls ++++++++++++++",
					(System.currentTimeMillis() - before), i * 100
			);

			Thread.sleep(10000);

			log.debug("--- Destroying virtual link from 1 to 2");
			controller.addPendingRequest(wsn.destroyVirtualLink(sourceNode, targetNode));

			Thread.sleep(10000);

		}

		// ==== TEST PERFORMANCE ====
		/*ExecutorService executorService =
				Executors.newFixedThreadPool(5, new NamingThreadFactory("WSNAppTest-Thread %d"));
		List<Callable<Void>> callables = new ArrayList<Callable<Void>>(1000);
		for (int i = 0; i < 1000; i++) {
			callables.add(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					String sourceNode = "urn:wisebed:testbeduzl1:1";
					String targetNode = "urn:wisebed:testbeduzl1:2";
					wsn.setVirtualLink(sourceNode, targetNode, wsnApiEndpoint, null, null);
					return null;
				}
			}
			);
		}
		executorService.invokeAll(callables);
		*/

		/*while (controller.pendingRequests.size() > 0) {

			log.warn("====================================");
			log.warn("= Requested: " + controller.requested);
			log.warn("= Received: " + controller.received);
			log.warn("= Received Statuses: " + controller.receivedStatuses);
			log.warn("====================================");

			log.debug("Still waiting for all requests to finish (pending: " + controller.pendingRequests.size() + ")");
			synchronized (controller.notifier) {
				controller.notifier.wait(1000);
			}
		}*/

		log.debug("Done...");

	}

	public static List<SecretReservationKey> copyRsToWsn(List<eu.wisebed.testbed.api.rs.v1.SecretReservationKey> keys) {
		List<eu.wisebed.testbed.api.wsn.v211.SecretReservationKey> newKeys = new ArrayList<SecretReservationKey>();

		for (eu.wisebed.testbed.api.rs.v1.SecretReservationKey key : keys) {
			eu.wisebed.testbed.api.wsn.v211.SecretReservationKey newKey =
					new eu.wisebed.testbed.api.wsn.v211.SecretReservationKey();
			newKey.setSecretReservationKey(key.getSecretReservationKey());
			newKey.setUrnPrefix(key.getUrnPrefix());
			newKeys.add(newKey);
		}

		return newKeys;
	}

	public static GetInstance createGetInstance(String controller,
												Collection<SecretReservationKey> secretReservationKeys) {
		GetInstance gi = new GetInstance();

		gi.setController(controller);
		gi.getSecretReservationKey().addAll(secretReservationKeys);

		return gi;
	}

	public List<SecretAuthenticationKey> generateFakeSNAAAuthentication(String urnPrefix, String username,
																		String secretAuthenticationKey) {
		List<SecretAuthenticationKey> secretAuthKeys = new ArrayList<SecretAuthenticationKey>();

		SecretAuthenticationKey key = new SecretAuthenticationKey();
		key.setSecretAuthenticationKey(secretAuthenticationKey);
		key.setUrnPrefix(urnPrefix);
		key.setUsername(username);

		secretAuthKeys.add(key);
		return secretAuthKeys;
	}

	public static List<eu.wisebed.testbed.api.rs.v1.SecretReservationKey> generateFakeReservationKeys(String urnPrefix,
																									  String secretReservationKey) {
		List<eu.wisebed.testbed.api.rs.v1.SecretReservationKey> reservations =
				new ArrayList<eu.wisebed.testbed.api.rs.v1.SecretReservationKey>();

		eu.wisebed.testbed.api.rs.v1.SecretReservationKey key = new eu.wisebed.testbed.api.rs.v1.SecretReservationKey();
		key.setSecretReservationKey(secretReservationKey);
		key.setUrnPrefix(urnPrefix);

		reservations.add(key);

		return reservations;
	}

	public List<eu.wisebed.testbed.api.rs.v1.SecretAuthenticationKey> copySnaaToRs(
			List<SecretAuthenticationKey> snaaKeys) {
		List<eu.wisebed.testbed.api.rs.v1.SecretAuthenticationKey> secretAuthKeys =
				new ArrayList<eu.wisebed.testbed.api.rs.v1.SecretAuthenticationKey>();

		for (SecretAuthenticationKey snaaKey : snaaKeys) {
			eu.wisebed.testbed.api.rs.v1.SecretAuthenticationKey key =
					new eu.wisebed.testbed.api.rs.v1.SecretAuthenticationKey();
			key.setSecretAuthenticationKey(snaaKey.getSecretAuthenticationKey());
			key.setUrnPrefix(snaaKey.getUrnPrefix());
			key.setUsername(snaaKey.getUsername());

			secretAuthKeys.add(key);
		}

		return secretAuthKeys;
	}

}
