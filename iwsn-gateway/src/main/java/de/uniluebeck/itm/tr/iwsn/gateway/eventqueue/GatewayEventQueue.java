package de.uniluebeck.itm.tr.iwsn.gateway.eventqueue;


import de.uniluebeck.itm.tr.iwsn.messages.Message;
import org.jboss.netty.channel.Channel;

public interface GatewayEventQueue {

    void channelConnected(Channel channel);

    void channelDisconnected();

    void enqueue(Message message);


}
