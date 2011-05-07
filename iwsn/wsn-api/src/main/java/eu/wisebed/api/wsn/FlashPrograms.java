
package eu.wisebed.api.wsn;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for flashPrograms complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="flashPrograms">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="nodeIds" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded"/>
 *         &lt;element name="programIndices" type="{http://www.w3.org/2001/XMLSchema}int" maxOccurs="unbounded"/>
 *         &lt;element name="programs" type="{urn:WSNService}program" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "flashPrograms", propOrder = {
    "nodeIds",
    "programIndices",
    "programs"
})
public class FlashPrograms {

    @XmlElement(required = true)
    protected List<String> nodeIds;
    @XmlElement(type = Integer.class)
    protected List<Integer> programIndices;
    @XmlElement(required = true)
    protected List<Program> programs;

    /**
     * Gets the value of the nodeIds property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the nodeIds property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getNodeIds().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getNodeIds() {
        if (nodeIds == null) {
            nodeIds = new ArrayList<String>();
        }
        return this.nodeIds;
    }

    /**
     * Gets the value of the programIndices property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the programIndices property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getProgramIndices().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Integer }
     * 
     * 
     */
    public List<Integer> getProgramIndices() {
        if (programIndices == null) {
            programIndices = new ArrayList<Integer>();
        }
        return this.programIndices;
    }

    /**
     * Gets the value of the programs property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the programs property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPrograms().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Program }
     * 
     * 
     */
    public List<Program> getPrograms() {
        if (programs == null) {
            programs = new ArrayList<Program>();
        }
        return this.programs;
    }

}
