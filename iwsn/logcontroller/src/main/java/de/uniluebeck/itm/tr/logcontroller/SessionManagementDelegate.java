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

package de.uniluebeck.itm.tr.logcontroller;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.uniluebeck.itm.tr.util.Tuple;
import eu.wisebed.testbed.api.rs.RSServiceHelper;
import eu.wisebed.testbed.api.rs.v1.ConfidentialReservationData;
import eu.wisebed.testbed.api.rs.v1.RS;
import eu.wisebed.testbed.api.wsn.Constants;
import eu.wisebed.testbed.api.wsn.SessionManagementHelper;
import eu.wisebed.testbed.api.wsn.v22.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.Holder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Proxy for the SessionManagement service, creates and manages {@link WSNBinding} instances.
 */
@WebService(
		serviceName = "SessionManagementService",
		targetNamespace = Constants.NAMESPACE_SESSION_MANAGEMENT_SERVICE,
		portName = "SessionManagementPort",
		endpointInterface = Constants.ENDPOINT_INTERFACE_SESSION_MANGEMENT_SERVICE
)
public class SessionManagementDelegate implements SessionManagement {

	private static final Logger log = LoggerFactory.getLogger(SessionManagementDelegate.class);

	private SessionManagement delegate;

	private Map<String, Tuple<WSNBinding, Future>> wsnInstances = Maps.newHashMap();

	private ControllerService controller;

	private RS reservationService;

	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

	private SessionManagementDelegate(SessionManagement delegate) {
		this.delegate = delegate;
	}

	public SessionManagementDelegate(SessionManagement sessionManagement, ControllerService controllerService) {
		this(sessionManagement);
		controller = controllerService;
		reservationService = RSServiceHelper.getRSService(controllerService.getReservationEndpoint());
	}

	@Override
	public String getInstance(
			@WebParam(name = "secretReservationKey", targetNamespace = "")
			List<SecretReservationKey> secretReservationKey,
			@WebParam(name = "controller", targetNamespace = "") String controller)
			throws ExperimentNotRunningException_Exception, UnknownReservationIdException_Exception {
		String reservationHash =
				SessionManagementHelper.calculateWSNInstanceHash(secretReservationKey);
		if (!wsnInstances.containsKey(reservationHash)) {
			WSNBinding binding =
					new WSNBinding(secretReservationKey, this.controller.getControllerUrnPrefix(),
							this.controller.getWsnUrnPrefix()
					);
			controller = binding.startController(controller);
			String instance = delegate.getInstance(secretReservationKey, controller);
			binding.startWSN(instance);
			Iterator<IMessageListener> listener = this.controller.getListenerIterator();
			while (listener.hasNext()) {
				binding.addMessageListener(listener.next());
			}
			wsnInstances.put(reservationHash,
					new Tuple<WSNBinding, Future>(binding, configureShutdown(binding, secretReservationKey))
			);
			log.info("WSN-Service on {} created",
					wsnInstances.get(reservationHash).getFirst().getWSN()
			);
		} else {
			wsnInstances.get(reservationHash).getFirst().setController(controller);
			log.info("New Controller set for WSN-Service on {}",
					wsnInstances.get(reservationHash).getFirst().getWSN()
			);
		}
		return wsnInstances.get(reservationHash).getFirst().getWSN();
	}

	@Override
	public void free(
			@WebParam(name = "secretReservationKey", targetNamespace = "")
			List<SecretReservationKey> secretReservationKey)
			throws ExperimentNotRunningException_Exception, UnknownReservationIdException_Exception {
		String reservationHash =
				SessionManagementHelper.calculateWSNInstanceHash(secretReservationKey);
		if (wsnInstances.containsKey(reservationHash)) {
			wsnInstances.get(reservationHash).getSecond().cancel(false);
			log.info("WSN-Service on {} shutting down.",
					wsnInstances.get(reservationHash).getFirst().getWSN()
			);
			wsnInstances.get(reservationHash).getFirst()
					.stop();
			wsnInstances.remove(reservationHash);
		}
		delegate.free(secretReservationKey);
	}

	@Override
	public String getNetwork() {
		return delegate.getNetwork();
	}

	@Override
	public void getConfiguration(
			@WebParam(name = "rsEndpointUrl", targetNamespace = "", mode = WebParam.Mode.OUT) final
			Holder<String> rsEndpointUrl,
			@WebParam(name = "snaaEndpointUrl", targetNamespace = "", mode = WebParam.Mode.OUT) final
			Holder<String> snaaEndpointUrl,
			@WebParam(name = "options", targetNamespace = "", mode = WebParam.Mode.OUT) final
			Holder<List<KeyValuePair>> options) {
		delegate.getConfiguration(rsEndpointUrl, snaaEndpointUrl, options);
	}

	/**
	 * Schedules a new Binding for Shutdown, equivalent to reservation interval.
	 *
	 * @param binding
	 * @param reservationKey
	 *
	 * @return
	 */
	private Future configureShutdown(WSNBinding binding, List<SecretReservationKey> reservationKey) {
		List<ConfidentialReservationData> reservationData;
		try {
			reservationData =
					reservationService.getReservation(convert(reservationKey));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		long delay = 0;
		for (ConfidentialReservationData data : reservationData) {
			delay = Math.max(data.getTo().toGregorianCalendar().getTimeInMillis() - System.currentTimeMillis(), delay);
		}
		log.debug("Shutdown of WSN-Binding in {} milliseconds", delay);
		String reservationHash = SessionManagementHelper.calculateWSNInstanceHash(
				reservationKey
		);
		return scheduler.schedule(
				new ShutdownWSNRunnable(binding, reservationHash),
				delay,
				TimeUnit.MILLISECONDS
		);
	}

	private List<eu.wisebed.testbed.api.rs.v1.SecretReservationKey> convert(
			List<SecretReservationKey> secretReservationKey) {

		List<eu.wisebed.testbed.api.rs.v1.SecretReservationKey> retList =
				Lists.newArrayListWithCapacity(secretReservationKey.size());
		for (SecretReservationKey reservationKey : secretReservationKey) {
			retList.add(convert(reservationKey));
		}
		return retList;
	}

	private eu.wisebed.testbed.api.rs.v1.SecretReservationKey convert(SecretReservationKey reservationKey) {
		eu.wisebed.testbed.api.rs.v1.SecretReservationKey retSRK =
				new eu.wisebed.testbed.api.rs.v1.SecretReservationKey();
		retSRK.setSecretReservationKey(reservationKey.getSecretReservationKey());
		retSRK.setUrnPrefix(reservationKey.getUrnPrefix());
		return retSRK;
	}

	/**
	 * Stops all WSN-Instances
	 */
	public void dispose() {
		scheduler.shutdown();
		for (Tuple<WSNBinding, Future> tuple : wsnInstances.values()) {
			tuple.getFirst().stop();
		}
		wsnInstances.clear();
	}

	private class ShutdownWSNRunnable implements Runnable {

		private WSNBinding binding;

		private String reservationHash;

		public ShutdownWSNRunnable(WSNBinding binding, String reservationHash) {
			this.binding = binding;
			this.reservationHash = reservationHash;
		}

		@Override
		public void run() {
			log.info("WSN-Service on {} shutting down.",
					binding.getWSN()
			);
			binding.stop();
			wsnInstances.remove(reservationHash);
		}
	}
}
