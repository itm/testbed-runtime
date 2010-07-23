package eu.wisebed.testbed.api.wsn.v211;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for receiveStatus complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="receiveStatus">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="status" type="{urn:ControllerService}requestStatus"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "receiveStatus", namespace = "urn:ControllerService", propOrder = {
		"status"
})
public class ReceiveStatus {

	@XmlElement(required = true)
	protected RequestStatus status;

	/**
	 * Gets the value of the status property.
	 *
	 * @return possible object is
	 *         {@link RequestStatus }
	 */
	public RequestStatus getStatus() {
		return status;
	}

	/**
	 * Sets the value of the status property.
	 *
	 * @param value allowed object is
	 *              {@link RequestStatus }
	 */
	public void setStatus(RequestStatus value) {
		this.status = value;
	}

}
