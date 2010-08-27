package de.uniluebeck.itm.tr.logcontroller;

import eu.wisebed.testbed.api.wsn.v211.Message;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * base for jpa-entities
 */
@MappedSuperclass
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
}
