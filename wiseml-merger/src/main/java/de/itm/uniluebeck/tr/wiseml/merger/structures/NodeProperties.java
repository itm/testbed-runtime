package de.itm.uniluebeck.tr.wiseml.merger.structures;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class NodeProperties {

    private Coordinate position;
    private Boolean gateway;
    private String programDetails;
    private String nodeType;
    private String description;
	
	private Map<String,Capability> capabilities;
	
	public NodeProperties() {
		this.capabilities = new HashMap<String,Capability>();
	}

	public Coordinate getPosition() {
		return position;
	}

	public void setPosition(Coordinate position) {
		this.position = position;
	}

	public Boolean getGateway() {
		return gateway;
	}

	public void setGateway(Boolean gateway) {
		this.gateway = gateway;
	}

	public String getProgramDetails() {
		return programDetails;
	}

	public void setProgramDetails(String programDetails) {
		this.programDetails = programDetails;
	}

	public String getNodeType() {
		return nodeType;
	}

	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
    public void setCapability(String name, Capability capability) {
    	this.capabilities.put(name, capability);
    }
    
    public Capability getCapability(String name) {
    	return capabilities.get(name);
    }

	public void addCapability(Capability capability) {
		this.capabilities.put(capability.getName(), capability);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof NodeProperties)) {
			return false;
		}
		NodeProperties other = (NodeProperties)obj;
		return equals(this.capabilities, other.capabilities)
			&& equals(this.description, other.description)
			&& equals(this.gateway, other.gateway)
			&& equals(this.nodeType, other.nodeType)
			&& equals(this.position, other.position)
			&& equals(this.programDetails, other.programDetails);
	}
	
	private static boolean equals(Object a, Object b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.equals(b);
	}
	
	public Collection<Capability> getCapabilities() {
		return capabilities.values();
	}

}
