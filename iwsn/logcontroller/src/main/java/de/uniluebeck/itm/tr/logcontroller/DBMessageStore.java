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
import eu.wisebed.testbed.api.wsn.v211.Message;
import eu.wisebed.testbed.api.wsn.v211.MessageType;
import eu.wisebed.testbed.api.wsn.v211.SecretReservationKey;

import javax.jws.WebService;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: jenskluttig
 * Date: 05.09.2010
 * Time: 12:15:15
 * To change this template use File | Settings | File Templates.
 */
@WebService(name = "MessageStore")
public class DBMessageStore implements IMessageStore, Service {
    private EntityManagerFactory _factory;

    public DBMessageStore(Map properties) {
        Preconditions.checkNotNull(properties, "Properties are null!");
        _factory =
                Persistence.createEntityManagerFactory(Server.PERSISTENCE_CONTEXT, properties);
    }

    private synchronized EntityManager getManager() {
        return _factory.createEntityManager();
    }

    private Message[] internalFetchMessage(List<SecretReservationKey> keys,
                                           MessageType type,
                                           int limit) {
        StringBuilder builder = new StringBuilder();
        builder.append("from TextMessage a where 1 = 1");
        if (keys != null && keys.size() > 0) {
            builder.append(" and ( 0 = 1");
            for (SecretReservationKey key : keys)
                builder.append(" or a.reservationKey = ?");
            builder.append(")");
        }
        if (type != null)
            builder.append(" and a.class = " + (type == MessageType.TEXT ?
                    TextMessage.class.getSimpleName() : BinaryMessage.class.getSimpleName()));
        EntityManager manager = getManager();
        try {
            TypedQuery<AbstractMessage> query = manager.createQuery(builder.toString(),
                    AbstractMessage.class);
            if (keys != null && keys.size() > 0)
                for (SecretReservationKey key : keys)
                    query.setParameter(keys.indexOf(key) + 1, key.getSecretReservationKey());
            if (limit > 0)
                query.setMaxResults(limit);
            List<AbstractMessage> result = query.getResultList();
            Message[] ret = new Message[result.size()];
            return Lists.transform(result, new Function<AbstractMessage, Message>() {
                @Override
                public Message apply(AbstractMessage from) {
                    return AbstractMessage.convertAbstractMessage(from);
                }
            }).toArray(ret);
        }
        finally {
            manager.close();
        }
    }

    @Override
    public boolean hasMessages(SecretReservationKey secretReservationKey) {
        EntityManager manager = getManager();
        try {
            Object count = manager.createQuery("select count(c) from BinaryMessage c where "
                    + "c.reservationKey = ?").setParameter(1,
                    secretReservationKey.getSecretReservationKey()).getSingleResult();
            return Integer.parseInt(count.toString()) > 0;
        }
        finally {
            manager.close();
        }
    }

    @Override
    public Message[] fetchMessages(List<SecretReservationKey> secretReservationKey, MessageType messageType, int limit) {
        return internalFetchMessage(secretReservationKey, messageType, limit);
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() {
        _factory.close();
    }
}
