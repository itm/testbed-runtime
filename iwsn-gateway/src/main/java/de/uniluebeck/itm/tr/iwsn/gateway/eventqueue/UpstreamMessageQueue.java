package de.uniluebeck.itm.tr.iwsn.gateway.eventqueue;


import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.tr.iwsn.messages.MessageHeaderPair;
import org.jboss.netty.channel.Channel;

import java.util.Optional;

public interface UpstreamMessageQueue extends Service {

	/**
	 * Enqueues a message.
	 *
	 * @param message the message
	 */
    void enqueue(Message message);

	/**
	 * Dequeues the next message from the queue or an empty Optional if the queue is empty.
	 *
	 * @return next message or an empty Optional
	 */
    Optional<Message> dequeue();

}
