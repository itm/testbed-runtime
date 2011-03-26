package de.uniluebeck.itm.wsn.deviceutils.writers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.slf4j.LoggerFactory;

import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePacket;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePlainText;

public class HumanReadableWriter implements Writer {
	private final static org.slf4j.Logger log = LoggerFactory.getLogger(HumanReadableWriter.class);
	
	private final BufferedWriter output;

	public HumanReadableWriter(OutputStream out) {
		this.output = new BufferedWriter(new OutputStreamWriter(out));
	}

	private void write(String type, byte[] content) {
		try {
			output.write("Type[");
			output.write(type);
			output.write("]: ");
			output.write(new String(content));
			output.write(" [Hex: ");
			output.write(StringUtils.toHexString(content));
			output.write("]");
			output.newLine();
			output.flush();
		} catch (IOException e) {
			log.warn("Unable to write messge:" + e, e);
		}
	}

	@Override
	public void write(MessagePacket packet) {
		write("" + packet.getType(), packet.getContent());
	}

	@Override
	public void write(MessagePlainText packet) {
		write("plaintext", packet.getContent());
	}

	@Override
	public void shutdown() {
		// Nothing to do
	}

}
