package de.uniluebeck.itm.wsn.deviceutils.writers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.LoggerFactory;

import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePacket;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePlainText;

public class CsvWriter implements Writer {
	private final static org.slf4j.Logger log = LoggerFactory.getLogger(CsvWriter.class);

	private final BufferedWriter output;

	private final DateTimeFormatter timeFormatter = DateTimeFormat.fullDateTime();

	public CsvWriter(OutputStream out) throws IOException {
		this.output = new BufferedWriter(new OutputStreamWriter(out));
		this.output.write("\"Time\";\"Type\";\"Content as String\";\"Content as Hex-Bytes\";\"Unix-Timestamp\"\n");
	}

	private void write(String type, byte[] content) {
		try {
			output.write("\"");
			output.write(timeFormatter.print(new DateTime()));
			output.write("\";\"");
			output.write(type);
			output.write("\";\"");
			output.write(new String(content));
			output.write("\";\"");
			output.write(StringUtils.toHexString(content));
			output.write("\";\"");
			output.write("" + (System.currentTimeMillis() / 1000));
			output.write("\"");
			output.newLine();
			output.flush();
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

	@Override
	public void shutdown() {
		try {
			output.flush();
		} catch (IOException e) {
			log.debug(" :" + e, e);
		}
	}

}
