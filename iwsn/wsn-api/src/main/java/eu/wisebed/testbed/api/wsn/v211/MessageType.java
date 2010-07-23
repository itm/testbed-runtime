package eu.wisebed.testbed.api.wsn.v211;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for messageType.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;simpleType name="messageType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="TEXT"/>
 *     &lt;enumeration value="BINARY"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 */
@XmlType(name = "messageType", namespace = "urn:CommonTypes")
@XmlEnum
public enum MessageType {

	TEXT,
	BINARY;

	public String value() {
		return name();
	}

	public static MessageType fromValue(String v) {
		return valueOf(v);
	}

}
