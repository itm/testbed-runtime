package de.uniluebeck.itm.tr.iwsn.gateway.eventqueue;


import com.leansoft.bigqueue.IBigQueue;

import java.io.IOException;

public interface UpstreamMessageQueueFactory {

	IBigQueue create(String queueName) throws IOException;

}
