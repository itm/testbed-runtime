package de.uniluebeck.itm.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * @author Soenke Nommensen
 */
@XStreamAlias("configuration")
public class TestbedConfiguration implements Serializable {

    private String name;
    
    private String testbedUrl;
    
    private String description;
    
    private String snaaEndpointUrl;
    
    private String rsEndpointUrl;
    
    private String sessionmanagementEndointUrl;
    
	@XStreamImplicit(itemFieldName="urnPrefix")
    private List<String> urnPrefixList;
	
	@XStreamAsAttribute
    private boolean isFederated;

    public TestbedConfiguration(String name, String testbedUrl, String description,
            String snaaEndpointUrl, String rsEndpointUrl,
            String sessionmanagementEndointUrl, boolean isFederated) {
        this.name = name;
        this.testbedUrl = testbedUrl;
        this.description = description;
        this.snaaEndpointUrl = snaaEndpointUrl;
        this.rsEndpointUrl = rsEndpointUrl;
        this.sessionmanagementEndointUrl = sessionmanagementEndointUrl;
        this.urnPrefixList = new ArrayList<String>();
        this.isFederated = isFederated;
    }

    public String getName() {
        return name;
    }

    public String getTestbedUrl() {
        return testbedUrl;
    }

    public String getDescription() {
        return description;
    }

    public String getSnaaEndpointUrl() {
        return snaaEndpointUrl;
    }

    public String getRsEndpointUrl() {
        return rsEndpointUrl;
    }

    public String getSessionmanagementEndointUrl() {
        return sessionmanagementEndointUrl;
    }

    public List<String> getUrnPrefixList() {
        return urnPrefixList;
    }

    public boolean isFederated() {
        return isFederated;
    }

    @Override
    public String toString() {
        return getName();
    }
}
