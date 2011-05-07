
package eu.wisebed.api.wsn;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for program complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="program">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="metaData" type="{urn:WSNService}programMetaData" minOccurs="0"/>
 *         &lt;element name="program" type="{http://www.w3.org/2001/XMLSchema}base64Binary"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "program", propOrder = {
    "metaData",
    "program"
})
public class Program {

    protected ProgramMetaData metaData;
    @XmlElement(required = true)
    protected byte[] program;

    /**
     * Gets the value of the metaData property.
     * 
     * @return
     *     possible object is
     *     {@link ProgramMetaData }
     *     
     */
    public ProgramMetaData getMetaData() {
        return metaData;
    }

    /**
     * Sets the value of the metaData property.
     * 
     * @param value
     *     allowed object is
     *     {@link ProgramMetaData }
     *     
     */
    public void setMetaData(ProgramMetaData value) {
        this.metaData = value;
    }

    /**
     * Gets the value of the program property.
     * 
     * @return
     *     possible object is
     *     byte[]
     */
    public byte[] getProgram() {
        return program;
    }

    /**
     * Sets the value of the program property.
     * 
     * @param value
     *     allowed object is
     *     byte[]
     */
    public void setProgram(byte[] value) {
        this.program = ((byte[]) value);
    }

}
