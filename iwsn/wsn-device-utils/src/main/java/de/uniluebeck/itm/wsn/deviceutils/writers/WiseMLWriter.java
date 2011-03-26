package de.uniluebeck.itm.wsn.deviceutils.writers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.LoggerFactory;

import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.tr.util.TimeDiff;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePacket;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePlainText;

public class WiseMLWriter implements Writer {

	private final static org.slf4j.Logger log = LoggerFactory.getLogger(WiseMLWriter.class);

	private final DateTimeFormatter timeFormatter = DateTimeFormat.fullDateTime();

	private final String nodeUrn;

	private BufferedWriter output;

	private boolean writeHeaderAndFooter;

	private boolean traceOpen = false;

	public WiseMLWriter(OutputStream out, String nodeUrn, boolean writeHeaderAndFooter) {
		this.output = new BufferedWriter(new OutputStreamWriter(out));
		this.writeHeaderAndFooter = writeHeaderAndFooter;
		this.nodeUrn = nodeUrn;

		if (writeHeaderAndFooter)
			writeHeader();
	}

	private void writeHeader() {
		try {
			output.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			output.newLine();
			output.write("<wiseml xmlns=\"http://wisebed.eu/ns/wiseml/1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://wisebed.eu/ns/wiseml/1.0\" version=\"1.0\">");
			output.newLine();

		} catch (IOException e) {
			log.warn("Unable to write WiseML header:" + e, e);
		}
	}

	private void checkNewTraceStart() throws IOException {
		if (output == null)
			return;

		if (!traceOpen) {
			checkTraceClose();
			output.write("<trace id=\"" + timeFormatter.print(System.currentTimeMillis()) + "\">\n");
			traceOpen = true;
		}

	}

	private void checkTraceClose() {
		if (output == null)
			return;

		if (traceOpen) {
			try {
				output.write("</trace>\n");
			} catch (IOException e) {
				log.debug(" :" + e, e);
			}
		}
	}

	private void writeFooter() {
		try {
			output.write("</wiseml>");
			output.newLine();
		} catch (IOException e) {
			log.warn("Unable to write WiseML header:" + e, e);
		}
	}

	private void write(String type, byte[] content) {
		if (output == null)
			return;

		try {
			checkNewTraceStart();
			
			output.write("\t<timestamp>" + System.currentTimeMillis() + "</timestamp>\n");

			output.write("\t<node id=\"" + nodeUrn + "\">");
			output.newLine();

			output.write("\t\t<data key=\"" + type + "\">" + new String(content) + "</data>");
			output.newLine();

			output.write("\t\t<data key=\"" + type + "-hex\">" + StringUtils.toHexString(content) + "</data>");
			output.newLine();

			output.write("\t</node>");
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
		if (output == null)
			return;

		checkTraceClose();

		if (writeHeaderAndFooter)
			writeFooter();

		try {
			output.flush();
		} catch (IOException e) {
			log.debug(" :" + e, e);
			
		}
		output = null;
	}

}
