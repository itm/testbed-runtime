package de.uniluebeck.itm.wsn.deviceutils.writers;

import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePacket;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePlainText;

public interface Writer {

	public void write(MessagePacket packet);

	public void write(MessagePlainText packet);

	public void shutdown(); 
	
}