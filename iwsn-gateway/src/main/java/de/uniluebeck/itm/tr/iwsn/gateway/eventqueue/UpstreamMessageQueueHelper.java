package de.uniluebeck.itm.tr.iwsn.gateway.eventqueue;


import com.google.protobuf.MessageLite;
import com.leansoft.bigqueue.IBigQueue;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.util.serialization.MultiClassSerializationHelper;

import java.io.IOException;

public interface UpstreamMessageQueueHelper {

    MultiClassSerializationHelper<Message> configureEventSerializationHelper() throws IOException, ClassNotFoundException;

    IBigQueue createAndConfigureQueue() throws IOException;

}
