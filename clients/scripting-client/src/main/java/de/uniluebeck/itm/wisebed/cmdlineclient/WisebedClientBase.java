package de.uniluebeck.itm.wisebed.cmdlineclient;

import com.google.common.util.concurrent.SettableFuture;

import de.uniluebeck.itm.tr.util.ListenerManagerImpl;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.tr.util.Tuple;
import de.uniluebeck.itm.tr.util.UrlUtils;
import de.uniluebeck.itm.wisebed.cmdlineclient.wrapper.WSNAsyncWrapper;
import eu.wisebed.api.common.KeyValuePair;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.controller.Controller;
import eu.wisebed.api.controller.RequestStatus;
import eu.wisebed.api.rs.RS;
import eu.wisebed.api.sm.SecretReservationKey;
import eu.wisebed.api.sm.SessionManagement;
import eu.wisebed.api.snaa.SNAA;
import eu.wisebed.api.snaa.SecretAuthenticationKey;
import eu.wisebed.testbed.api.rs.RSServiceHelper;
import eu.wisebed.testbed.api.snaa.helpers.SNAAServiceHelper;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;

import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public abstract class WisebedClientBase {

    protected static class ControllerManager extends ListenerManagerImpl<Controller> implements Controller {
        @Override
        public void experimentEnded() {
            for (Controller controller : listeners) {
                controller.experimentEnded();
            }
        }
        @Override
        public void receive(List<Message> msg) {
            for (Controller controller : listeners) {
                controller.receive(msg);
            }
        }
        @Override
        public void receiveNotification(List<String> msg) {
            for (Controller controller : listeners) {
                controller.receiveNotification(msg);
            }
        }
        @Override
        public void receiveStatus(List<RequestStatus> status) {
            for (Controller controller : listeners) {
                controller.receiveStatus(status);
            }
        }
    }
    
    protected ControllerManager controllerManager = new ControllerManager();

    protected final SessionManagement sessionManagement;

    protected final SNAA snaa;

    protected final RS rs;

    protected ExecutorService executor;

    public WisebedClientBase(final String sessionManagementEndpointUrl) {

        executor = Executors.newCachedThreadPool();
        sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointUrl);

        final Holder<String> rsEndpointUrl = new Holder<String>();
        final Holder<String> snaaEndpointUrl = new Holder<String>();
        final Holder<List<KeyValuePair>> options = new Holder<List<KeyValuePair>>();

        sessionManagement.getConfiguration(rsEndpointUrl, snaaEndpointUrl, options);
        rs = RSServiceHelper.getRSService(rsEndpointUrl.value);
        snaa = SNAAServiceHelper.getSNAAService(snaaEndpointUrl.value);
    }

    public Future<List<AuthenticationKey>> authenticate(
            final AuthenticationCredentials... authenticationCredentialsList) {

        final SettableFuture<List<AuthenticationKey>> future = SettableFuture.create();
        Runnable authenticationRunnable = new Runnable() {

            public void run() {
                try {
                    final List<SecretAuthenticationKey> secretAuthenticationKeyList =
                            snaa.authenticate(TypeConverter.convertCredentials(authenticationCredentialsList));
                    future.set(TypeConverter.convertAuthenticationKeys(secretAuthenticationKeyList));
                } catch (Exception e) {
                    future.setException(e);
                }
            }
        };
        executor.execute(authenticationRunnable);

        return future;
    }

    public Future<WSNAsyncWrapper> connectToExperiment(final ReservationKey... reservationKeyList) {
        return connectToExperiment(TypeConverter.convertReservationKeysToSM(reservationKeyList));
    }

    public abstract Future<WSNAsyncWrapper> connectToExperiment(
            final List<SecretReservationKey> secretReservationKeyList);

    public void addController(final Controller controller) {
        controllerManager.addListener(controller);
    }

    public void removeController(final Controller controller) {
        controllerManager.removeListener(controller);
    }

    /*
     * public Future<List<ReservationKey>> fetchReservations(final AuthenticationKey... authenticationKeys) {
     * rs.getConfidentialReservations(TypeConverter.convertAuthenticationKeysToRS(authenticationKeys));
     * 
     * }
     */

    public void shutdown() {
        ExecutorUtils.shutdown(executor, 3, TimeUnit.SECONDS);
    }
}
