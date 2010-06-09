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

package de.uniluebeck.itm.tr.nodeapi;

import com.google.inject.Singleton;
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.tr.util.TimedCache;
import de.uniluebeck.itm.tr.util.TimedCacheListener;
import de.uniluebeck.itm.tr.util.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class NodeApi {

    private static final Logger log = LoggerFactory.getLogger(NodeApi.class);

    private int lastRequestID = 0;

    private Interaction interaction = new InteractionImpl(this);

    private LinkControl linkControl = new LinkControlImpl(this);

    private NetworkDescription networkDescription = new NetworkDescriptionImpl(this);

    private NodeControl nodeControl = new NodeControlImpl(this);

    private TimedCache<Integer, NodeApiCallback> callbackCache;

    private NodeApiDeviceAdapter deviceAdapter;

    private TimedCacheListener callbackCacheListener = new TimedCacheListener<Integer, NodeApiCallback>() {
        @Override
        public Tuple<Long, TimeUnit> timeout(Integer requestId, NodeApiCallback callback) {
            callback.timeout();
            return null;
        }
    };

    public NodeApi(NodeApiDeviceAdapter deviceAdapter, int defaultTimeout, TimeUnit defaultTimeUnit) {

        checkNotNull(deviceAdapter);
        checkNotNull(defaultTimeout);
        checkNotNull(defaultTimeUnit);

        this.callbackCache = new TimedCache<Integer, NodeApiCallback>(defaultTimeout, defaultTimeUnit);
        this.callbackCache.setListener(callbackCacheListener);

        this.deviceAdapter = deviceAdapter;
        this.deviceAdapter.setNodeApi(this);

    }

    public Interaction getInteraction() {
        return interaction;
    }

    public LinkControl getLinkControl() {
        return linkControl;
    }

    public NetworkDescription getNetworkDescription() {
        return networkDescription;
    }

    public NodeControl getNodeControl() {
        return nodeControl;
    }

    public NodeApiDeviceAdapter getDeviceAdapter() {
        return deviceAdapter;
    }

    /**
     * Creates a requestId in a thread-safe manner.
     *
     * @return a newly created request ID between 0 and 255
     */
    synchronized int nextRequestId() {
        return lastRequestID >= 255 ? (lastRequestID = 0) : ++lastRequestID;
    }

    void sendToNode(final int requestId, final NodeApiCallback callback, final ByteBuffer buffer) {

        checkNotNull(callback);

        if (log.isDebugEnabled()) {
            log.debug("Sending to node with request ID {}: {}", requestId, StringUtils.toHexString(buffer.array()));
        }

		callbackCache.put(requestId, callback);
		deviceAdapter.sendToNode(buffer);

    }

    void receiveFromNode(ByteBuffer packet) {

        checkNotNull(packet);

        byte[] packetBytes = packet.array();

        if (packetBytes.length < 3) {
            if (log.isWarnEnabled()) {
                log.warn("Received incomplete response packet: {}", StringUtils.toHexString(packetBytes));
            }
            return;
        }

        int requestId = (packetBytes[1] & 0xFF);
        byte responseCode = packetBytes[2];

        NodeApiCallback callback = callbackCache.remove(requestId);

        // if callback exists it means that the invocation did not yet time out
        if (callback != null) {

            byte[] responsePayload = null;

            if (packetBytes.length > 3) {
                responsePayload = new byte[packetBytes.length - 3];
                System.arraycopy(packetBytes, 3, responsePayload, 0, packetBytes.length - 3);
            }

            if (log.isDebugEnabled()) {
                log.debug("Received from node with request ID {} and response code {}: {}", new Object[]{requestId, responseCode, responsePayload});
            }

            if (responseCode == ResponseType.COMMAND_SUCCESS) {
                log.debug("Invoking callback.success() for request ID {}", requestId);
                callback.success(responsePayload);
            } else {
                log.debug("Invoking callback.failure() for request ID {}", requestId);
                callback.failure(responseCode, responsePayload);
            }

        } else if (log.isDebugEnabled()) {
            log.debug("Received message for unknown requestId: {}", StringUtils.toHexString(packetBytes));
        }
    }
}