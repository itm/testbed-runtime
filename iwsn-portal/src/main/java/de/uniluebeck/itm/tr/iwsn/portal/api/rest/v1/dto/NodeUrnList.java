package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class NodeUrnList {

	@XmlElement(name = "nodeUrns")
	public List<String> nodeUrns;

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("NodeUrnList [nodeUrns=");
		builder.append(nodeUrns);
		builder.append("]");
		return builder.toString();
	}
}
