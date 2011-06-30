package de.uniluebeck.itm.wisebed.cmdlineclient;

import com.google.common.util.concurrent.ValueFuture;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;
import javax.xml.ws.RequestWrapper;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class WisebedClient {

	private static final Logger log = LoggerFactory.getLogger(WisebedClient.class);

	@WebService(name = "Controller", targetNamespace = "urn:ControllerService")
	private static class WisebedController implements Controller {

		private WSNAsyncWrapper wsn;

		@WebMethod
		@Oneway
		@RequestWrapper(localName = "experimentEnded", targetNamespace = "urn:ControllerService",
				className = "eu.wisebed.api.controller.ExperimentEnded")
		public void experimentEnded() {

			log.info("Experiment ended");
		}

		@WebMethod
		@Oneway
		@RequestWrapper(localName = "receive", targetNamespace = "urn:ControllerService",
				className = "eu.wisebed.api.controller.Receive")
		public void receive(
				@WebParam(name = "msg", targetNamespace = "")
				List<Message> msg) {

		}

		@WebMethod
		@Oneway
		@RequestWrapper(localName = "receiveNotification", targetNamespace = "urn:ControllerService",
				className = "eu.wisebed.api.controller.ReceiveNotification")
		public void receiveNotification(@WebParam(name = "msg", targetNamespace = "") List<String> msgs) {
			for (String msg : msgs) {
				log.info("{}", msg);
			}
		}

		@WebMethod
		@Oneway
		@RequestWrapper(localName = "receiveStatus", targetNamespace = "urn:ControllerService",
				className = "eu.wisebed.api.controller.ReceiveStatus")
		public void receiveStatus(
				@WebParam(name = "status", targetNamespace = "") List<RequestStatus> requestStatuses) {
			wsn.receive(requestStatuses);
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

	private final SessionManagement sessionManagement;

	private final SNAA snaa;

	private final RS rs;

	private ExecutorService executor;

	public WisebedClient(final String sessionManagementEndpointUrl) {

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

		final ValueFuture<List<AuthenticationKey>> future = ValueFuture.create();
		Runnable authenticationRunnable = new Runnable() {

			public void run() {
				try {
					final List<SecretAuthenticationKey> secretAuthenticationKeyList = snaa.authenticate(
							TypeConverter.convertCredentials(authenticationCredentialsList)
					);
					future.set(TypeConverter.convertAuthenticationKeys(secretAuthenticationKeyList));
				} catch (Exception e) {
					future.setException(e);
				}
			}
		};
		executor.execute(authenticationRunnable);

		return future;
	}

	/*public Future<List<ReservationKey>> fetchReservations(final AuthenticationKey... authenticationKeys) {



		rs.getConfidentialReservations(TypeConverter.convertAuthenticationKeysToRS(authenticationKeys));

	}*/

	private Future<Tuple<String, WisebedController>> startController() {

		final ValueFuture<Tuple<String, WisebedController>> future = ValueFuture.create();

		Runnable startControllerRunnable = new Runnable() {
			public void run() {
				final Vector<String> externalHostIps = BeanShellHelper.getExternalHostIps();
				String controllerEndpointUrl;

				while (true) {
					int randomUnprivilegedPort = UrlUtils.getRandomUnprivilegedPort();
					controllerEndpointUrl = "http://" + externalHostIps.get(0) + ":" + randomUnprivilegedPort + "/";
					Endpoint endpoint;
					final WisebedController controller = new WisebedController();
					try {
						endpoint = Endpoint.publish(controllerEndpointUrl, controller);
					} catch (Exception e) {
						log.warn("" + e, e);
						continue;
					}
					future.set(new Tuple<String, WisebedController>(controllerEndpointUrl, controller));
					endpoint.setExecutor(executor);
					break;
				}

			}
		};
		executor.execute(startControllerRunnable);

		return future;
	}

	public Future<WSNAsyncWrapper> connectToExperiment(final ReservationKey... reservationKeyList) {

		final ValueFuture<WSNAsyncWrapper> future = ValueFuture.create();

		Runnable connectRunnable = new Runnable() {
			public void run() {
				try {

					log.info("Connecting to experiment...");

					final List<SecretReservationKey> secretReservationKeyList =
							TypeConverter.convertReservationKeysToSM(reservationKeyList);

					final Tuple<String, WisebedController> controllerData = startController().get();
					final String controllerEndpointUrl = controllerData.getFirst();
					final WisebedController controller = controllerData.getSecond();

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

	public void shutdown() {
		ExecutorUtils.shutdown(executor, 3, TimeUnit.SECONDS);
	}

}
