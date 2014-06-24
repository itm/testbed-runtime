package de.uniluebeck.itm.tr.iwsn.gateway.eventqueue;


import com.leansoft.bigqueue.IBigQueue;
import de.uniluebeck.itm.eventstore.helper.EventStoreSerializationHelper;

import java.io.FileNotFoundException;
import java.io.IOException;

public interface GatewayEventQueueHelper {
    EventStoreSerializationHelper configureEventSerializationHelper() throws FileNotFoundException, ClassNotFoundException;

    IBigQueue createAndConfigureQueue() throws IOException;

}
