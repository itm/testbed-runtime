package de.uniluebeck.itm.wisebed.cmdlineclient;

import com.google.common.util.concurrent.ValueFuture;
import de.uniluebeck.itm.wisebed.cmdlineclient.protobuf.ProtobufControllerClient;
import de.uniluebeck.itm.wisebed.cmdlineclient.protobuf.ProtobufControllerClientListener;
import de.uniluebeck.itm.wisebed.cmdlineclient.wrapper.WSNAsyncWrapper;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.controller.RequestStatus;
import eu.wisebed.api.sm.SecretReservationKey;
import eu.wisebed.api.wsn.WSN;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Future;

public class WisebedProtobufClient extends WisebedClientBase {

	private static final Logger log = LoggerFactory.getLogger(WisebedProtobufClient.class);

	private final String protobufHost;

	private final int protobufPort;

	private ProtobufControllerClient protobufControllerClient;

	public WisebedProtobufClient(final String sessionManagementEndpointUrl, final String protobufHost,
								 final int protobufPort) {

		super(sessionManagementEndpointUrl);

		this.protobufHost = protobufHost;
		this.protobufPort = protobufPort;
	}

	@Override
	public Future<WSNAsyncWrapper> connectToExperiment(final List<SecretReservationKey> secretReservationKeyList) {
		final ValueFuture<WSNAsyncWrapper> future = ValueFuture.create();

		Runnable connectRunnable = new Runnable() {

			@Override
			public void run() {

				String wsnEndpointURL = null;
				try {
					wsnEndpointURL = sessionManagement.getInstance(secretReservationKeyList, "NONE");
				} catch (Exception e) {
					future.setException(e);
				}

				log.info("Got a WSN instance URL, endpoint is: {}", wsnEndpointURL);
				final WSN wsnService = WSNServiceHelper.getWSNService(wsnEndpointURL);
				final WSNAsyncWrapper wsn = WSNAsyncWrapper.of(wsnService);

				protobufControllerClient = ProtobufControllerClient.create(
						protobufHost, protobufPort, secretReservationKeyList
				);
				protobufControllerClient.addListener(new ProtobufControllerClientListener() {

					@Override
					public void onConnectionClosed() {
						log.debug("Connection closed.");
					}

					@Override
					public void onConnectionEstablished() {
						log.debug("Connection established.");
					}

					@Override
					public void experimentEnded() {
						log.info("Experiment ended");
						controllerManager.experimentEnded();
					}

					@Override
					public void receive(final List<Message> msg) {
						controllerManager.receive(msg);
					}

					@Override
					public void receiveNotification(final List<String> messages) {
						for (String message : messages) {
							log.info(message);
						}
						controllerManager.receiveNotification(messages);
					}

					@Override
					public void receiveStatus(final List<RequestStatus> requestStatuses) {
						wsn.receive(requestStatuses);
						controllerManager.receiveStatus(requestStatuses);
					}
				}
				);
				protobufControllerClient.connect();

				future.set(wsn);
			}
		};

		executor.execute(connectRunnable);

		return future;
	}
}
