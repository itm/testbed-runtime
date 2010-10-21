package de.uniluebeck.itm.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Soenke Nommensen
 */
public class TestbedConfiguration {

    private String name;
    private String testbedUrl;
    private String description;
    private String snaaEndpointUrl;
    private String rsEndpointUrl;
    private String sessionmanagementEndointUrl;
    private List<String> urnPrefixList;
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
        this.urnPrefixList = new ArrayList();
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
}
