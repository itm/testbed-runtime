
package eu.wisebed.api.rs;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for confidentialReservationData complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="confidentialReservationData">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:RSService}publicReservationData">
 *       &lt;sequence>
 *         &lt;element name="data" type="{urn:RSService}data" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "confidentialReservationData", propOrder = {
    "data"
})
public class ConfidentialReservationData
    extends PublicReservationData
{

    @XmlElement(required = true)
    protected List<Data> data;

    /**
     * Gets the value of the data property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the data property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getData().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Data }
     * 
     * 
     */
    public List<Data> getData() {
        if (data == null) {
            data = new ArrayList<Data>();
        }
        return this.data;
    }

}
