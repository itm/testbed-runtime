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
import eu.wisebed.testbed.api.wsn.v22.ExperimentNotRunningException_Exception;
import eu.wisebed.testbed.api.wsn.v22.SecretReservationKey;
import eu.wisebed.testbed.api.wsn.v22.SessionManagement;
import eu.wisebed.testbed.api.wsn.v22.UnknownReservationIdException_Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebParam;
import javax.jws.WebService;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Proxy for SessionManagment-Service, creates an manages WSN-Bindings
 */
@WebService(
		serviceName = "SessionManagementService",
		targetNamespace = Constants.NAMESPACE_SESSION_MANAGEMENT_SERVICE,
		portName = "SessionManagementPort",
		endpointInterface = Constants.ENDPOINT_INTERFACE_SESSION_MANGEMENT_SERVICE
)
public class SessionManagementDelegate implements SessionManagement {
    private Logger _log = LoggerFactory.getLogger(SessionManagementDelegate.class);
    private SessionManagement _delegate;
    private Map<String, Tuple<WSNBinding, Future>> _wsnInstances = Maps.newHashMap();
    private ControllerService _controller;
    private RS _reservationService;
    private ScheduledExecutorService _scheduler = Executors.newScheduledThreadPool(2);

    private SessionManagementDelegate(SessionManagement delegate) {
        _delegate = delegate;
    }

    public SessionManagementDelegate(SessionManagement sessionManagment, ControllerService controllerService) {
        this(sessionManagment);
        _controller = controllerService;
        _reservationService = RSServiceHelper.getRSService(controllerService.getReservationEndpoint());
    }

    @Override
    public String getInstance(@WebParam(name = "secretReservationKey", targetNamespace = "") List<SecretReservationKey> secretReservationKey,
                              @WebParam(name = "controller", targetNamespace = "") String controller)
            throws ExperimentNotRunningException_Exception, UnknownReservationIdException_Exception {
        String reservationHash =
                SessionManagementHelper.calculateWSNInstanceHash(secretReservationKey);
        if (!_wsnInstances.containsKey(reservationHash)) {
            WSNBinding binding =
                    new WSNBinding(secretReservationKey, _controller.getControllerUrnPrefix(), _controller.getWsnUrnPrefix());
            controller = binding.startController(controller);
            String instance = _delegate.getInstance(secretReservationKey, controller);
            binding.startWSN(instance);
            Iterator<IMessageListener> listener = _controller.getListenerIterator();
            while (listener.hasNext())
                binding.addMessageListener(listener.next());
            _wsnInstances.put(reservationHash,
                    new Tuple<WSNBinding, Future>(binding, configurateShutdown(binding, secretReservationKey)));
            _log.info("WSN-Service on {} created",
                    _wsnInstances.get(reservationHash).getFirst().getWSN());
        } else {
            _wsnInstances.get(reservationHash).getFirst().setController(controller);
            _log.info("New Controller set for WSN-Service on {}",
                    _wsnInstances.get(reservationHash).getFirst().getWSN());
        }
        return _wsnInstances.get(reservationHash).getFirst().getWSN();
    }

    @Override
    public void free(@WebParam(name = "secretReservationKey", targetNamespace = "") List<SecretReservationKey> secretReservationKey)
            throws ExperimentNotRunningException_Exception, UnknownReservationIdException_Exception {
        String reservationHash =
                SessionManagementHelper.calculateWSNInstanceHash(secretReservationKey);
        if (_wsnInstances.containsKey(reservationHash)) {
            _wsnInstances.get(reservationHash).getSecond().cancel(false);
            _log.info("WSN-Service on {} shutting down.",
                    _wsnInstances.get(reservationHash).getFirst().getWSN());
            _wsnInstances.get(reservationHash).getFirst()
                    .stop();
            _wsnInstances.remove(reservationHash);
        }
        _delegate.free(secretReservationKey);
    }

    @Override
    public String getNetwork() {
        return _delegate.getNetwork().toString();
    }

    /**
     * Schedules a new Binding for Shutdown, equivalent to reservationintervall
     * @param binding
     * @param reservationKey
     * @return
     */
    private Future configurateShutdown(WSNBinding binding, List<SecretReservationKey> reservationKey) {
        List<ConfidentialReservationData> reservationData = null;
        try {
            reservationData =
                    _reservationService.getReservation(convert(reservationKey));
        } catch (Exception e) {
			throw new RuntimeException(e);
        }
        long delay = 0;
        for (ConfidentialReservationData data : reservationData)
            delay = Math.max(data.getTo().toGregorianCalendar().getTimeInMillis() - System.currentTimeMillis(), delay);
        _log.debug("Shutdown of WSN-Binding in {} milliseconds", delay);
        String reservationHash = SessionManagementHelper.calculateWSNInstanceHash(
                reservationKey);
        return _scheduler.schedule(new ShutdownWSNRunnable(binding, reservationHash)
                , delay, TimeUnit.MILLISECONDS);
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
        _scheduler.shutdown();
        for (Tuple<WSNBinding, Future> tuple : _wsnInstances.values())
            tuple.getFirst().stop();
        _wsnInstances.clear();
    }

    private class ShutdownWSNRunnable implements Runnable {
        private WSNBinding _binding;
        private String _reservationHash;

        public ShutdownWSNRunnable(WSNBinding binding, String reservationHash) {
            _binding = binding;
            _reservationHash = reservationHash;
        }

        @Override
        public void run() {
            _log.info("WSN-Service on {} shutting down.",
                    _binding.getWSN());
            _binding.stop();
            _wsnInstances.remove(_reservationHash);
        }
    }
}
