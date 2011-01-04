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

package de.uniluebeck.itm.gtr.messaging.srmr;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.messaging.Messages;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventAdapter;
import de.uniluebeck.itm.gtr.messaging.event.MessageEventListener;
import de.uniluebeck.itm.gtr.messaging.reliable.ReliableMessagingService;
import de.uniluebeck.itm.tr.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;


@Singleton
public class SingleRequestMultiResponseServiceImpl
		extends AbstractListenable<Triple<String, String, SingleRequestMultiResponseListener>>
		implements SingleRequestMultiResponseService {

	private TestbedRuntime testbedRuntime;

	private SecureIdGenerator secureIdGenerator = new SecureIdGenerator();

	private Random random = new Random();

	private static final String SRMRS_MSGTYPE_REQUEST = "SRMRS_REQUEST";

	private static final String SRMRS_MSGTYPE_RESPONSE = "SRMRS_RESPONSE";

	private static final String SRMRS_MSGTYPE_ACK = "SRMRS_ACK";

	@Inject
	public SingleRequestMultiResponseServiceImpl(TestbedRuntime testbedRuntime) {
		this.testbedRuntime = testbedRuntime;
	}

	private TimedCache<String, Tuple<Messages.SingleRequestMultipleResponseRequest, SingleRequestMultiResponseCallback>>
			timedCache =
			new TimedCache<String, Tuple<Messages.SingleRequestMultipleResponseRequest, SingleRequestMultiResponseCallback>>(
					1, TimeUnit.HOURS
			);

	private void send(Messages.Msg msg, int timeout, TimeUnit timeUnit,
					  final SingleRequestMultiResponseCallback callback,
					  boolean reliableRequest, boolean reliableResponses) {

		String requestId = secureIdGenerator.getNextId();

		Messages.SingleRequestMultipleResponseRequest request =
				Messages.SingleRequestMultipleResponseRequest.newBuilder()
						.setPayload(msg.toByteString())
						.setReliableRequest(reliableRequest)
						.setReliableResponses(reliableResponses)
						.setRequestId(requestId).build();

		Messages.Msg.Builder messageBuilder = Messages.Msg.newBuilder()
				.setTo(msg.getTo())
				.setFrom(msg.getFrom())
				.setMsgType(SRMRS_MSGTYPE_REQUEST)
				.setPayload(request.toByteString())
				.setPriority(msg.getPriority())
				.setValidUntil(msg.getValidUntil());

		// remember the request to match asynchronous replies later on
		Tuple<Messages.SingleRequestMultipleResponseRequest, SingleRequestMultiResponseCallback> requestTuple =
				new Tuple<Messages.SingleRequestMultipleResponseRequest, SingleRequestMultiResponseCallback>(request,
						callback
				);

		timedCache.put(requestId, requestTuple, timeout, timeUnit);

		if (reliableRequest) {

			messageBuilder.setReplyWith(msg.getFrom() + ":" + random.nextLong());
			testbedRuntime.getReliableMessagingService().sendAsync(
					messageBuilder.build(),
					new ReliableMessagingService.AsyncCallbackAdapter() {
						@Override
						public void failure(Exception exception) {
							callback.failure(exception);
						}
					}
			);

		} else {

			testbedRuntime.getUnreliableMessagingService().sendAsync(messageBuilder.build());

		}

	}

	@Override
	public void sendReliableRequestReliableResponse(Messages.Msg msg, int timeout, TimeUnit timeUnit,
													SingleRequestMultiResponseCallback callback) {
		send(msg, timeout, timeUnit, callback, true, true);
	}

	@Override
	public void sendReliableRequestUnreliableResponse(Messages.Msg msg, int timeout, TimeUnit timeUnit,
													  SingleRequestMultiResponseCallback callback) {
		send(msg, timeout, timeUnit, callback, true, false);
	}

	@Override
	public void sendUnreliableRequestReliableResponse(Messages.Msg msg, int timeout, TimeUnit timeUnit,
													  SingleRequestMultiResponseCallback callback) {
		send(msg, timeout, timeUnit, callback, false, true);
	}

	@Override
	public void sendUnreliableRequestUnreliableResponse(Messages.Msg msg, int timeout, TimeUnit timeUnit,
														SingleRequestMultiResponseCallback callback) {
		send(msg, timeout, timeUnit, callback, false, false);
	}

	@Override
	public void addListener(String urn, String msgType, SingleRequestMultiResponseListener listener) {
		addListener(new Triple<String, String, SingleRequestMultiResponseListener>(urn, msgType, listener));
	}

	@Override
	public void removeListener(SingleRequestMultiResponseListener listener) {

		ImmutableList.Builder<Triple<String, String, SingleRequestMultiResponseListener>> listBuilder =
				ImmutableList.builder();
		for (Triple<String, String, SingleRequestMultiResponseListener> t : listeners) {
			if (t.getThird() != listener) {
				listBuilder.add(t);
			}
		}
		listeners = listBuilder.build();

	}

	private static class ResponderImpl implements SingleRequestMultiResponseListener.Responder {

		private TestbedRuntime testbedRuntime;

		private Messages.Msg requestMsg;

		private Messages.SingleRequestMultipleResponseRequest request;

		private Random random = new Random();

		public ResponderImpl(TestbedRuntime testbedRuntime, Messages.Msg requestMsg,
							 Messages.SingleRequestMultipleResponseRequest request) {
			this.testbedRuntime = testbedRuntime;
			this.requestMsg = requestMsg;
			this.request = request;
		}

		@Override
		public void sendResponse(byte[] payload) {

			Messages.SingleRequestMultipleResponseResponse.Builder responseBuilder =
					Messages.SingleRequestMultipleResponseResponse.newBuilder()
							.setPayload(ByteString.copyFrom(payload))
							.setRequestId(request.getRequestId());

			Messages.Msg.Builder messageBuilder = Messages.Msg.newBuilder()
					.setTo(requestMsg.getFrom())
					.setFrom(requestMsg.getTo())
					.setMsgType(SRMRS_MSGTYPE_RESPONSE)
					.setPriority(requestMsg.getPriority())
					.setValidUntil(System.currentTimeMillis() + 5000)
					.setPayload(responseBuilder.build().toByteString());

			if (request.getReliableResponses()) {

				Messages.Msg response =
						messageBuilder.setReplyWith(requestMsg.getTo() + ":" + random.nextLong()).build();

				testbedRuntime.getReliableMessagingService()
						.sendAsync(response, new ReliableMessagingService.AsyncCallbackAdapter() {
							@Override
							public void failure(Exception e) {
								log.warn("Unable to deliver response: " + e, e);
							}
						}
						);

			} else {

				testbedRuntime.getUnreliableMessagingService().sendAsync(messageBuilder.build());

			}

		}

	}

	private static final Logger log = LoggerFactory.getLogger(SingleRequestMultiResponseService.class);

	private MessageEventListener messageEventListener = new MessageEventAdapter() {
		@Override
		public void messageReceived(Messages.Msg msg) {

			try {

				boolean isResponse = SRMRS_MSGTYPE_RESPONSE.equals(msg.getMsgType());
				boolean isRequest = SRMRS_MSGTYPE_REQUEST.equals(msg.getMsgType());

				if (isRequest) {

					log.trace("=== Request === {} ==== {}",
							Arrays.toString(testbedRuntime.getLocalNodeNames().toArray()), msg
					);
					// this happens on the request-receiver (server) side
					Messages.SingleRequestMultipleResponseRequest request =
							Messages.SingleRequestMultipleResponseRequest.newBuilder().mergeFrom(msg.getPayload())
									.build();

					// check if request has to be acknowledged
					if (request.getReliableRequest()) {
						Messages.Msg ack = Messages.Msg.newBuilder()
								.setFrom(msg.getTo())
								.setTo(msg.getFrom())
								.setMsgType(SRMRS_MSGTYPE_ACK)
								.setReplyTo(msg.getReplyWith())
								.setPriority(msg.getPriority())
								.setValidUntil(System.currentTimeMillis() + 5000).build();
						testbedRuntime.getUnreliableMessagingService().sendAsync(ack);
					}

					// check if one of our listeners is interested
					// TODO improve efficiency here

					Messages.Msg originalMsg = Messages.Msg.newBuilder().mergeFrom(request.getPayload()).build();

					boolean matchesNodeUrn;
					boolean matchesMsgType;

					for (Triple<String, String, SingleRequestMultiResponseListener> listener : listeners) {

						matchesNodeUrn = listener.getFirst().equals(msg.getTo());
						matchesMsgType = listener.getSecond().equals(originalMsg.getMsgType());

						if (matchesMsgType && matchesNodeUrn) {
							listener.getThird().receiveRequest(
									originalMsg,
									new ResponderImpl(testbedRuntime, msg, request)
							);
						}

					}

				} else if (isResponse) {

					log.trace("*** Response *** {} *** {}",
							Arrays.toString(testbedRuntime.getLocalNodeNames().toArray()), msg
					);

					// this happens on the requester-side (client)
					Messages.SingleRequestMultipleResponseResponse response =
							Messages.SingleRequestMultipleResponseResponse.newBuilder().mergeFrom(msg.getPayload())
									.build();

					Tuple<Messages.SingleRequestMultipleResponseRequest, SingleRequestMultiResponseCallback>
							requestTuple =
							timedCache.get(response.getRequestId());

					if (requestTuple != null) {

						// check if response has to be acknowledged
						if (requestTuple.getFirst().getReliableResponses()) {
							Messages.Msg ack = Messages.Msg.newBuilder()
									.setFrom(msg.getTo())
									.setTo(msg.getFrom())
									.setMsgType(SRMRS_MSGTYPE_ACK)
									.setReplyTo(msg.getReplyWith())
									.setPriority(msg.getPriority())
									.setValidUntil(System.currentTimeMillis() + 5000).build();
							testbedRuntime.getUnreliableMessagingService().sendAsync(ack);
						}

						// notify callback of original requester
						boolean done = requestTuple.getSecond().receive(response.getPayload().toByteArray());
						// remove from timed cache so that timeout won't occur later on
						if (done) {
							timedCache.remove(response.getRequestId());
						}

					} else {
						log.debug("Ignoring response to unknown requestTuple ID");
					}

				}
			} catch (InvalidProtocolBufferException e) {
				log.warn("Error while unmarshalling message: " + e, e);
			}

		}

	};

	TimedCacheListener<String, Tuple<Messages.SingleRequestMultipleResponseRequest, SingleRequestMultiResponseCallback>>
			timedCacheListener =
			new TimedCacheListener<String, Tuple<Messages.SingleRequestMultipleResponseRequest, SingleRequestMultiResponseCallback>>() {
				@Override
				public Tuple<Long, TimeUnit> timeout(String key,
													 Tuple<Messages.SingleRequestMultipleResponseRequest, SingleRequestMultiResponseCallback> value) {

					value.getSecond().timeout();

					// let the timed cache remove the element
					return null;
				}
			};

	@Override
	public void start() throws Exception {
		testbedRuntime.getMessageEventService().addListener(messageEventListener);
		timedCache.setListener(timedCacheListener);
	}

	@Override
	public void stop() {
		testbedRuntime.getMessageEventService().removeListener(messageEventListener);
		timedCache.clear();
		timedCache.setListener(null);
	}

}