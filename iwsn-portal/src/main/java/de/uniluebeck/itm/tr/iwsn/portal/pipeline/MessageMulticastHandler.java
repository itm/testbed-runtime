package de.uniluebeck.itm.tr.iwsn.portal.pipeline;

import org.jboss.netty.channel.SimpleChannelHandler;

/**
 * The MessageMulticastHandler is used as a singleton and sits in every connection to testbed gateways. It takes
 * outgoing requests affecting a set of nodes that are (potentially) connected to different gateways. The requests are
 * then "split up" for the individual gateways so that one request is sent for each gateway with the request containing
 * only relevant information for the nodes currently connected to the gateway.
 *
 * The MessageMulticastHandler learns which devices are attached to which gateway at what time by observing the  event
 * flow through the communication channels (specially events dealing with attaching / detaching devices).
 */
public class MessageMulticastHandler extends SimpleChannelHandler {


}
