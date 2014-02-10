package de.uniluebeck.itm.tr.snaa.shiro.entity;

import javax.persistence.*;
import java.util.Set;

import static javax.persistence.CascadeType.ALL;

@Entity
@Table(name = "RESOURCEGROUPS")
public class ResourceGroup implements java.io.Serializable {

	@Id
	@Column(name = "NAME", unique = true, nullable = false, length = 40)
	private String name;

	@OneToMany(cascade = ALL, fetch = FetchType.LAZY, mappedBy = "resourceGroup")
	private Set<UrnResourceGroup> urnResourceGroups;

	@OneToMany(cascade = ALL, fetch = FetchType.LAZY, mappedBy = "resourceGroup")
	private Set<Permission> permissions;

	public ResourceGroup() {

	}

	public ResourceGroup(String name) {
		this.name = name;
	}

	public ResourceGroup(String name, Set<UrnResourceGroup> urnResourceGroups, Set<Permission> permissions) {
		this.name = name;
		this.urnResourceGroups = urnResourceGroups;
		this.permissions = permissions;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<UrnResourceGroup> getUrnResourceGroups() {
		return this.urnResourceGroups;
	}

	public void setUrnResourceGroups(Set<UrnResourceGroup> urnResourceGroups) {
		this.urnResourceGroups = urnResourceGroups;
	}

	public Set<Permission> getPermissions() {
		return this.permissions;
	}

	public void setPermissions(Set<Permission> permissions) {
		this.permissions = permissions;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ResourceGroup that = (ResourceGroup) o;
		return name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
