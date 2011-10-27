package de.uniluebeck.itm.wisebed.cmdlineclient;

import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.tr.util.Tuple;
import de.uniluebeck.itm.tr.util.UrlUtils;
import de.uniluebeck.itm.wisebed.cmdlineclient.wrapper.WSNAsyncWrapper;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.controller.Controller;
import eu.wisebed.api.controller.RequestStatus;
import eu.wisebed.api.sm.SecretReservationKey;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.RequestWrapper;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Future;

public class WisebedClient extends WisebedClientBase {

    private static final Logger log = LoggerFactory.getLogger(WisebedClient.class);

    @WebService(name = "Controller", targetNamespace = "urn:ControllerService")
    public class WisebedController implements Controller {

        private WSNAsyncWrapper wsn;

        @WebMethod
        @Oneway
        @RequestWrapper(localName = "experimentEnded", targetNamespace = "urn:ControllerService",
                className = "eu.wisebed.api.controller.ExperimentEnded")
        public void experimentEnded() {

            log.info("Experiment ended");
            WisebedClient.this.controllerManager.experimentEnded();
        }

        @WebMethod
        @Oneway
        @RequestWrapper(localName = "receive", targetNamespace = "urn:ControllerService",
                className = "eu.wisebed.api.controller.Receive")
        public void receive(@WebParam(name = "msg", targetNamespace = "") List<Message> msg) {
            WisebedClient.this.controllerManager.receive(msg);
        }

        @WebMethod
        @Oneway
        @RequestWrapper(localName = "receiveNotification", targetNamespace = "urn:ControllerService",
                className = "eu.wisebed.api.controller.ReceiveNotification")
        public void receiveNotification(@WebParam(name = "msg", targetNamespace = "") List<String> messages) {
            for (String message : messages) {
                log.info("{}", message);
            }
            WisebedClient.this.controllerManager.receiveNotification(messages);
        }

        @WebMethod
        @Oneway
        @RequestWrapper(localName = "receiveStatus", targetNamespace = "urn:ControllerService",
                className = "eu.wisebed.api.controller.ReceiveStatus")
        public void receiveStatus(@WebParam(name = "status", targetNamespace = "") List<RequestStatus> requestStatuses) {
            wsn.receive(requestStatuses);
            WisebedClient.this.controllerManager.receiveStatus(requestStatuses);
        }

        @WebMethod(exclude = true)
        public WSNAsyncWrapper getWsn() {
            return wsn;
        }

        @WebMethod(exclude = true)
        public void setWsn(final WSNAsyncWrapper wsn) {
            this.wsn = wsn;
        }

    }

    public WisebedClient(final String sessionManagementEndpointUrl) {
        super(sessionManagementEndpointUrl);
    }

    @Override
    public Future<WSNAsyncWrapper> connectToExperiment(final List<SecretReservationKey> secretReservationKeyList) {

        final SettableFuture<WSNAsyncWrapper> future = SettableFuture.create();

        Runnable connectRunnable = new Runnable() {
            public void run() {
                try {

                    log.info("Connecting to experiment...");

                    final Tuple<String, WisebedClient.WisebedController> controllerData = startController().get();
                    final String controllerEndpointUrl = controllerData.getFirst();
                    final WisebedClient.WisebedController controller = controllerData.getSecond();

                    log.info("Started local controller endpoint at {}", controllerEndpointUrl);

                    final String wsnInstanceEndpointUrl =
                            sessionManagement.getInstance(secretReservationKeyList, controllerEndpointUrl);

                    log.info("Retrieved WSN endpoint at {}", wsnInstanceEndpointUrl);

                    final WSNAsyncWrapper wsn =
                            WSNAsyncWrapper.of(WSNServiceHelper.getWSNService(wsnInstanceEndpointUrl));

                    controller.setWsn(wsn);

                    future.set(wsn);

                } catch (Exception e) {
                    future.setException(e);
                }
            }
        };
        executor.execute(connectRunnable);

        return future;
    }

    private Future<Tuple<String, WisebedClient.WisebedController>> startController() {

        final SettableFuture<Tuple<String, WisebedClient.WisebedController>> future = SettableFuture.create();

        Runnable startControllerRunnable = new Runnable() {
            public void run() {
                final Vector<String> externalHostIps = BeanShellHelper.getExternalHostIps();
                String controllerEndpointUrl;

                while (true) {
                    int randomUnprivilegedPort = UrlUtils.getRandomUnprivilegedPort();
                    controllerEndpointUrl = "http://" + externalHostIps.get(0) + ":" + randomUnprivilegedPort + "/";
                    Endpoint endpoint;
                    final WisebedClient.WisebedController controller = new WisebedClient.WisebedController();
                    try {
                        endpoint = Endpoint.publish(controllerEndpointUrl, controller);
                    } catch (Exception e) {
                        log.warn("" + e, e);
                        continue;
                    }
                    future.set(new Tuple<String, WisebedClient.WisebedController>(controllerEndpointUrl, controller));
                    endpoint.setExecutor(executor);
                    break;
                }

            }
        };
        executor.execute(startControllerRunnable);

        return future;
    }

    @Override
    public void addController(Controller controller) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeController(Controller controller) {
        // TODO Auto-generated method stub

    }

}
