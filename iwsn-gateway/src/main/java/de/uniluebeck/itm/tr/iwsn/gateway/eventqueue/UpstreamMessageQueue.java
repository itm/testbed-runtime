package de.uniluebeck.itm.tr.iwsn.gateway.eventqueue;


import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import org.jboss.netty.channel.Channel;

public interface UpstreamMessageQueue extends Service {

    void channelConnected(Channel channel);

    void channelDisconnected();

    void enqueue(Message message);


}
