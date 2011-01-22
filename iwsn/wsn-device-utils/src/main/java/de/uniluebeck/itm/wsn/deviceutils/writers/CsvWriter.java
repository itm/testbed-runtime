package de.uniluebeck.itm.wsn.deviceutils.writers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.slf4j.LoggerFactory;

import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePacket;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePlainText;

public class CsvWriter implements Writer {
	private final static org.slf4j.Logger log = LoggerFactory.getLogger(CsvWriter.class);

	private final BufferedWriter output;

	public CsvWriter(OutputStream out) throws IOException {
		this.output = new BufferedWriter(new OutputStreamWriter(out));
		this.output.write("\"Type\";\"Content as String\";\"Content as Hex-Bytes\"");
	}

	private void write(String type, byte[] content) {
		try {
			output.write("\"");
			output.write(type);
			output.write("\";\"");
			output.write(new String(content));
			output.write("\";\"");
			output.write(StringUtils.toHexString(content));
			output.write("\"");
			output.newLine();
		} catch (IOException e) {
			log.warn("Unable to write messge:" + e, e);
		}
	}

	/* (non-Javadoc)
	 * @see de.uniluebeck.itm.wsn.deviceutils.MessageListenerOutWriter#write(de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePacket)
	 */
	@Override
	public void write(MessagePacket packet) {
		write("" + packet.getType(), packet.getContent());
	}

	/* (non-Javadoc)
	 * @see de.uniluebeck.itm.wsn.deviceutils.MessageListenerOutWriter#write(de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePlainText)
	 */
	@Override
	public void write(MessagePlainText packet) {
		write("plaintext", packet.getContent());
	}

}
