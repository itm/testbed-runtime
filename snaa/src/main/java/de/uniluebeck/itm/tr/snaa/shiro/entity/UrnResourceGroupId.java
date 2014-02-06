package de.uniluebeck.itm.tr.snaa.shiro.entity;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class UrnResourceGroupId implements java.io.Serializable {

	@Column(name = "URN", nullable = false)
	private String urn;

	@Column(name = "RESOURCEGROUP", nullable = false, length = 40)
	private String resourcegroup;

	public UrnResourceGroupId() {

	}

	public UrnResourceGroupId(String urn, String resourcegroup) {
		this.urn = urn;
		this.resourcegroup = resourcegroup;
	}

	public String getUrn() {
		return this.urn;
	}

	public void setUrn(String urn) {
		this.urn = urn;
	}

	public String getResourcegroup() {
		return this.resourcegroup;
	}

	public void setResourcegroup(String resourcegroup) {
		this.resourcegroup = resourcegroup;
	}

	public boolean equals(Object other) {
		if ((this == other)) {
			return true;
		}
		if ((other == null)) {
			return false;
		}
		if (!(other instanceof UrnResourceGroupId)) {
			return false;
		}
		UrnResourceGroupId castOther = (UrnResourceGroupId) other;

		return ((this.getUrn() == castOther.getUrn()) || (this.getUrn() != null && castOther.getUrn() != null && this
				.getUrn().equals(castOther.getUrn())))
				&& ((this.getResourcegroup() == castOther.getResourcegroup()) || (this
				.getResourcegroup() != null && castOther.getResourcegroup() != null && this.getResourcegroup()
				.equals(castOther.getResourcegroup())));
	}

	public int hashCode() {
		int result = 17;
		result = 37 * result + (getUrn() == null ? 0 : this.getUrn().hashCode());
		result = 37 * result + (getResourcegroup() == null ? 0 : this.getResourcegroup().hashCode());
		return result;
	}
}
