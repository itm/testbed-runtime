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

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import eu.wisebed.testbed.api.wsn.v22.Message;
import eu.wisebed.testbed.api.wsn.v22.MessageLevel;

import javax.persistence.*;

/**
 * base for jpa-entities
 */
@Entity
@Table(name = "WsnMessages")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
        name = "messagetype",
        discriminatorType = DiscriminatorType.STRING
)
public abstract class AbstractMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long id;
    public String reservationKey;
    public String timeStamp;
    public String sourceNodeId;

    /**
     * Converts XML-Message-type into jpa-entity
     *
     * @param message XML-Message
     * @return jpa-entity
     */
    public static AbstractMessage convertMessage(Message message) {
        AbstractMessage result = message.getBinaryMessage() == null ? new TextMessage() :
                new BinaryMessage();
        result.sourceNodeId = message.getSourceNodeId();
        result.timeStamp = message.getTimestamp().toString();
        if (result instanceof BinaryMessage) {
            BinaryMessage b = (BinaryMessage) result;
            b.binaryData = message.getBinaryMessage().getBinaryData();
            b.binaryType = message.getBinaryMessage().getBinaryType();
        } else {
            TextMessage t = (TextMessage) result;
            t.message = message.getTextMessage().getMsg();
            t.messageLevel = message.getTextMessage().getMessageLevel().toString();
        }
        return result;
    }

    public static Message convertAbstractMessage(AbstractMessage from) {
        Message mes = new Message();
        mes.setTimestamp(XMLGregorianCalendarImpl.parse(from.timeStamp));
        mes.setSourceNodeId(from.sourceNodeId);
        if (from instanceof TextMessage) {
            eu.wisebed.testbed.api.wsn.v22.TextMessage text = new
                    eu.wisebed.testbed.api.wsn.v22.TextMessage();
            TextMessage tmsg = (TextMessage) from;
            if (tmsg.messageLevel != null)
                text.setMessageLevel(MessageLevel.valueOf(tmsg.messageLevel));
            text.setMsg(tmsg.message);
            mes.setTextMessage(text);
        } else {
            eu.wisebed.testbed.api.wsn.v22.BinaryMessage binary = new
                    eu.wisebed.testbed.api.wsn.v22.BinaryMessage();
            BinaryMessage bmsg = (BinaryMessage) from;
            binary.setBinaryData(bmsg.binaryData);
            binary.setBinaryType(bmsg.binaryType);
            mes.setBinaryMessage(binary);
        }
        return mes;
    }
}
