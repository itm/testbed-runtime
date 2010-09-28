package de.itm.uniluebeck.tr.wiseml.merger.structures;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: jacob
 * Date: Aug 23, 2010
 * Time: 7:03:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class LinkProperties {

    private Boolean encrypted;
    private Boolean virtual;

    private RSSI rssi;

    private Map<String,Capability> capabilities;
    
    public LinkProperties() {
    	this.capabilities = new HashMap<String,Capability>();
    }

	public Boolean getEncrypted() {
		return encrypted;
	}

	public void setEncrypted(Boolean encrypted) {
		this.encrypted = encrypted;
	}

	public Boolean getVirtual() {
		return virtual;
	}

	public void setVirtual(Boolean virtual) {
		this.virtual = virtual;
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

	public RSSI getRssi() {
		return rssi;
	}

	public void setRssi(RSSI rssi) {
		this.rssi = rssi;
	}

	public Collection<Capability> getCapabilities() {
		return this.capabilities.values();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof LinkProperties)) {
			return false;
		}
		LinkProperties other = (LinkProperties)obj;
		return equals(this.encrypted, other.encrypted)
			&& equals(this.virtual, other.virtual)
			&& equals(this.rssi, other.rssi)
			&& this.capabilities.equals(other.capabilities);
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

}
