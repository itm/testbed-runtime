package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class SendMessageData {

	public String sourceNodeUrn;

	public List<String> targetNodeUrns;

	public String bytesBase64;

	@Override
	public String toString() {
		return "SendMessageData{" +
				"sourceNodeUrn='" + sourceNodeUrn + '\'' +
				", nodeUrns=" + targetNodeUrns +
				", bytesBase64='" + bytesBase64 + '\'' +
				'}';
	}
}
