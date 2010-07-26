package eu.wisebed.testbed.api.rs.v1;

import javax.xml.bind.annotation.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for publicReservationData complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="publicReservationData">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="from" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
 *         &lt;element name="nodeURNs" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded"/>
 *         &lt;element name="to" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
 *         &lt;element name="userData" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "publicReservationData", propOrder = {
		"from",
		"nodeURNs",
		"to",
		"userData"
})
@XmlSeeAlso({
		ConfidentialReservationData.class
})
public class PublicReservationData {

	@XmlElement(required = true)
	@XmlSchemaType(name = "dateTime")
	protected XMLGregorianCalendar from;
	@XmlElement(required = true)
	protected List<String> nodeURNs;
	@XmlElement(required = true)
	@XmlSchemaType(name = "dateTime")
	protected XMLGregorianCalendar to;
	protected String userData;

	/**
	 * Gets the value of the from property.
	 *
	 * @return possible object is
	 *         {@link XMLGregorianCalendar }
	 */
	public XMLGregorianCalendar getFrom() {
		return from;
	}

	/**
	 * Sets the value of the from property.
	 *
	 * @param value allowed object is
	 *              {@link XMLGregorianCalendar }
	 */
	public void setFrom(XMLGregorianCalendar value) {
		this.from = value;
	}

	/**
	 * Gets the value of the nodeURNs property.
	 * <p/>
	 * <p/>
	 * This accessor method returns a reference to the live list,
	 * not a snapshot. Therefore any modification you make to the
	 * returned list will be present inside the JAXB object.
	 * This is why there is not a <CODE>set</CODE> method for the nodeURNs property.
	 * <p/>
	 * <p/>
	 * For example, to add a new item, do as follows:
	 * <pre>
	 *    getNodeURNs().add(newItem);
	 * </pre>
	 * <p/>
	 * <p/>
	 * <p/>
	 * Objects of the following type(s) are allowed in the list
	 * {@link String }
	 */
	public List<String> getNodeURNs() {
		if (nodeURNs == null) {
			nodeURNs = new ArrayList<String>();
		}
		return this.nodeURNs;
	}

	/**
	 * Gets the value of the to property.
	 *
	 * @return possible object is
	 *         {@link XMLGregorianCalendar }
	 */
	public XMLGregorianCalendar getTo() {
		return to;
	}

	/**
	 * Sets the value of the to property.
	 *
	 * @param value allowed object is
	 *              {@link XMLGregorianCalendar }
	 */
	public void setTo(XMLGregorianCalendar value) {
		this.to = value;
	}

	/**
	 * Gets the value of the userData property.
	 *
	 * @return possible object is
	 *         {@link String }
	 */
	public String getUserData() {
		return userData;
	}

	/**
	 * Sets the value of the userData property.
	 *
	 * @param value allowed object is
	 *              {@link String }
	 */
	public void setUserData(String value) {
		this.userData = value;
	}

}
