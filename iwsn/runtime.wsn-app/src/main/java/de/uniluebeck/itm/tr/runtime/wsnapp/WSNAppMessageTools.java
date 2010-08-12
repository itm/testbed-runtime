package de.uniluebeck.itm.tr.runtime.wsnapp;

import de.uniluebeck.itm.tr.util.StringUtils;

/**
 * Created by IntelliJ IDEA. User: bimschas Date: 11.08.2010 Time: 16:05:43 TODO change
 */
public class WSNAppMessageTools {

	public static String toString(WSNAppMessages.Message message, boolean prependSourceNodeId) {
		StringBuilder builder = new StringBuilder();
		if (prependSourceNodeId) {
			builder.append(message.getSourceNodeId());
			builder.append(" => ");
		}
		if (message.hasBinaryMessage()) {
			builder.append("Binary[");
			builder.append("type=");
			builder.append(StringUtils.toHexString((byte) message.getBinaryMessage().getBinaryType()));
			builder.append(",data=");
			builder.append(
					StringUtils.toHexString(message.getBinaryMessage().getBinaryData().toByteArray())
			);
			builder.append("]");
		}
		if (message.hasTextMessage()) {
			builder.append("Text[");
			builder.append("level=");
			builder.append(message.getTextMessage().getMessageLevel());
			builder.append(",msg=\"");
			builder.append(message.getTextMessage().getMsg());
			builder.append("\"]");
		}
		return builder.toString();
	}

}
