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

package de.uniluebeck.itm.tr.iwsn.overlay.messaging.event;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.tr.iwsn.overlay.messaging.Messages;
import de.uniluebeck.itm.tr.util.Listenable;
import de.uniluebeck.itm.tr.util.ListenerManager;
import de.uniluebeck.itm.tr.util.ListenerManagerImpl;


/**
 * Service that allows to register as a listener to message events (received, sent, dropped), as well as to fire these
 * events.
 */
public abstract class MessageEventService extends AbstractService implements Listenable<MessageEventListener>, Service {

	protected final ListenerManager<MessageEventListener> listenerManager =
			new ListenerManagerImpl<MessageEventListener>();

	/**
	 * Asynchronously notifies listeners that the message {@code msg} was sent.
	 *
	 * @param msg
	 * 		the message that was sent
	 */
	public abstract void sent(Messages.Msg msg);

	/**
	 * Asynchronously notifies listeners that the message {@code msg} was dropped.
	 *
	 * @param msg
	 * 		the message that was dropped
	 */
	public abstract void dropped(Messages.Msg msg);

	/**
	 * Asynchronously notifies listeners that the message {@code msg} was received.
	 *
	 * @param msg
	 * 		the message that was received
	 */
	public abstract void received(Messages.Msg msg);

	@Override
	public void addListener(final MessageEventListener listener) {
		listenerManager.addListener(listener);
	}

	@Override
	public void removeListener(final MessageEventListener listener) {
		listenerManager.removeListener(listener);
	}
}
