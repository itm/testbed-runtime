package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import de.uniluebeck.itm.eventstore.CloseableIterator;
import de.uniluebeck.itm.eventstore.EventContainer;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.tr.iwsn.messages.MessageHeaderPair;

import java.io.IOException;

final class CloseableMessageHeaderPairIterator implements CloseableIterator<MessageHeaderPair> {

	final CloseableIterator<EventContainer<Message>> iterator;

	public CloseableMessageHeaderPairIterator(CloseableIterator<EventContainer<Message>> iterator) {
		this.iterator = iterator;
	}

	@Override
	public void close() throws IOException {
		iterator.close();
	}

	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public MessageHeaderPair next() {
		return MessageHeaderPair.fromWrapped(iterator.next().getEvent());
	}
}
