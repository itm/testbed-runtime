package de.uniluebeck.itm.tr.snaa.shiro.entity;

import javax.persistence.*;

@Entity
@Table(name = "URN_RESOURCEGROUPS")
public class UrnResourceGroup implements java.io.Serializable {

	@EmbeddedId
	@AttributeOverrides({
			@AttributeOverride(name = "urn", column = @Column(name = "URN", nullable = false)),
			@AttributeOverride(name = "resourcegroup",
					column = @Column(name = "RESOURCEGROUP", nullable = false, length = 40))
	})
	private UrnResourceGroupId id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "RESOURCEGROUP", nullable = false, insertable = false, updatable = false)
	private ResourceGroup resourceGroup;

	public UrnResourceGroup() {

	}

	public UrnResourceGroup(UrnResourceGroupId id, ResourceGroup resourceGroup) {
		this.id = id;
		this.resourceGroup = resourceGroup;
	}

	public UrnResourceGroupId getId() {
		return this.id;
	}

	public void setId(UrnResourceGroupId id) {
		this.id = id;
	}

	public ResourceGroup getResourceGroup() {
		return this.resourceGroup;
	}

	public void setResourceGroup(ResourceGroup resourceGroup) {
		this.resourceGroup = resourceGroup;
	}
}
