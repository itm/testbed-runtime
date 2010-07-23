package eu.wisebed.testbed.api.wsn.v211;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for receive complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="receive">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="msg" type="{urn:CommonTypes}message"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "receive", namespace = "urn:ControllerService", propOrder = {
		"msg"
})
public class Receive {

	@XmlElement(required = true)
	protected Message msg;

	/**
	 * Gets the value of the msg property.
	 *
	 * @return possible object is
	 *         {@link Message }
	 */
	public Message getMsg() {
		return msg;
	}

	/**
	 * Sets the value of the msg property.
	 *
	 * @param value allowed object is
	 *              {@link Message }
	 */
	public void setMsg(Message value) {
		this.msg = value;
	}

}
