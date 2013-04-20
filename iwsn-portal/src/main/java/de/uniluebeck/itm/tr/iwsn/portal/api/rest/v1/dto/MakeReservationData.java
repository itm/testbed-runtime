package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.util.DateTimeAdapter;
import eu.wisebed.api.v3.util.NodeUrnAdapter;
import org.joda.time.DateTime;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;

@XmlRootElement
public class MakeReservationData {

	@XmlElement(required = true, type = String.class)
	@XmlJavaTypeAdapter(NodeUrnAdapter.class)
	public List<NodeUrn> nodeUrns;

	@XmlElement(required = true, type = String.class)
	@XmlJavaTypeAdapter(DateTimeAdapter.class)
	@XmlSchemaType(name = "dateTime")
	public DateTime from;

	@XmlElement(required = true, type = String.class)
	@XmlJavaTypeAdapter(DateTimeAdapter.class)
	@XmlSchemaType(name = "dateTime")
	public DateTime to;

	public String description;

	public List<KeyValuePair> options;

}
