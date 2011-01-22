package de.uniluebeck.itm.wsn.deviceutils.writers;

import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePacket;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePlainText;

public interface Writer {

	public abstract void write(MessagePacket packet);

	public abstract void write(MessagePlainText packet);

}