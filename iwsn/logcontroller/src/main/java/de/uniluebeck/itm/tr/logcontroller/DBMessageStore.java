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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import de.uniluebeck.itm.gtr.common.Service;
import eu.wisebed.testbed.api.messagestore.v1.Message;
import eu.wisebed.testbed.api.messagestore.v1.MessageStore;
import eu.wisebed.testbed.api.messagestore.v1.SecretReservationKey;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;

/**
 * Implementation of MessageStore-Interface using JPA 2.0
 */
@WebService(targetNamespace = "urn:MessageStore",
		endpointInterface = "de.uniluebeck.itm.tr.logcontroller.IMessageStore",
		portName = "MessageStorePort", serviceName = "MessageStore")
public class DBMessageStore implements MessageStore, Service {

	private static final Function<WSNMessage, Message> MESSAGE_CONVERT_FUNCTION = new Function<WSNMessage, Message>() {
		@Override
		public Message apply(WSNMessage from) {

			eu.wisebed.testbed.api.wsn.v22.Message message = WSNMessage.convertToXMLMessage(from);

			Message ret = new Message();
			ret.setBinaryData(message.getBinaryData());
			ret.setSourceNodeId(message.getSourceNodeId());
			ret.setTimestamp(message.getTimestamp());
			return ret;
		}
	};

	private EntityManagerFactory factory;

	public DBMessageStore(Map properties) {
		Preconditions.checkNotNull(properties, "Properties are null!");
		factory = Persistence.createEntityManagerFactory(Server.PERSISTENCE_CONTEXT, properties);
	}

	private synchronized EntityManager getManager() {
		return factory.createEntityManager();
	}

	@Override
	@WebMethod(exclude = true)
	public void start() throws Exception {

	}

	@Override
	@WebMethod(exclude = true)
	public void stop() {
		factory.close();
	}

	@Override
	public boolean hasMessages(
			@WebParam(name = "secretReservationKey", targetNamespace = "")
			final eu.wisebed.testbed.api.messagestore.v1.SecretReservationKey secretReservationKey) {
		EntityManager manager = getManager();
		try {
			Object count = manager.createQuery("select count(c) from BinaryMessage c where "
					+ "c.reservationKey = ?"
			).setParameter(1,
					secretReservationKey.getSecretReservationKey()
			).getSingleResult();
			return Integer.parseInt(count.toString()) > 0;
		} finally {
			manager.close();
		}
	}

	@Override
	public List<eu.wisebed.testbed.api.messagestore.v1.Message> fetchMessages(
			@WebParam(name = "secretReservationKey", targetNamespace = "") final
			List<eu.wisebed.testbed.api.messagestore.v1.SecretReservationKey> secretReservationKeys,
			@WebParam(name = "messageLimit", targetNamespace = "") final int messageLimit) {

		StringBuilder builder = new StringBuilder();
		builder.append("from WSNMessage a where 1 = 1");
		if (secretReservationKeys != null && secretReservationKeys.size() > 0) {
			builder.append(" and ( 0 = 1");
			for (SecretReservationKey secretReservationKey : secretReservationKeys) {
				builder.append(" or a.reservationKey = ?");
			}
			builder.append(")");
		}
		EntityManager manager = getManager();
		try {
			TypedQuery<WSNMessage> query = manager.createQuery(builder.toString(),
					WSNMessage.class
			);
			if (secretReservationKeys != null && secretReservationKeys.size() > 0) {
				for (SecretReservationKey key : secretReservationKeys) {
					query.setParameter(secretReservationKeys.indexOf(key) + 1, key.getSecretReservationKey());
				}
			}
			if (messageLimit > 0) {
				query.setMaxResults(messageLimit);
			}
			List<WSNMessage> result = query.getResultList();

			return Lists.transform(result, MESSAGE_CONVERT_FUNCTION);

		} finally {
			manager.close();
		}

	}
}
