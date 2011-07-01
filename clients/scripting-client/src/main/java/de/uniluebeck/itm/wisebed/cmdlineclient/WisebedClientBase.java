package de.uniluebeck.itm.wisebed.cmdlineclient;

import com.google.common.util.concurrent.ValueFuture;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.tr.util.Tuple;
import de.uniluebeck.itm.tr.util.UrlUtils;
import de.uniluebeck.itm.wisebed.cmdlineclient.wrapper.WSNAsyncWrapper;
import eu.wisebed.api.rs.RS;
import eu.wisebed.api.sm.SecretReservationKey;
import eu.wisebed.api.sm.SessionManagement;
import eu.wisebed.api.snaa.SNAA;
import eu.wisebed.api.snaa.SecretAuthenticationKey;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;

import javax.xml.ws.Endpoint;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA. User: bimschas Date: 01.07.11 Time: 06:57 TODO change
 */
public class WisebedClientBase {

	protected final SessionManagement sessionManagement;

	protected final SNAA snaa;

	protected final RS rs;

	protected ExecutorService executor;

	public WisebedClientBase(
			final String sessionManagementEndpointUrl) {
		executor = Executors.newCachedThreadPool();
		sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointUrl);
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

	private Future<Tuple<String, WisebedClient.WisebedController>> startController() {

		final ValueFuture<Tuple<String, WisebedClient.WisebedController>> future = ValueFuture.create();

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

	public Future<WSNAsyncWrapper> connectToExperiment(final ReservationKey... reservationKeyList) {

		final ValueFuture<WSNAsyncWrapper> future = ValueFuture.create();

		Runnable connectRunnable = new Runnable() {
			public void run() {
				try {

					log.info("Connecting to experiment...");

					final List<SecretReservationKey> secretReservationKeyList =
							TypeConverter.convertReservationKeysToSM(reservationKeyList);

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

	public void shutdown() {
		ExecutorUtils.shutdown(executor, 3, TimeUnit.SECONDS);
	}
}
