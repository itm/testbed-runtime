package de.uniluebeck.itm.tr.snaa.shiro.entity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ACTIONS")
public class Action implements java.io.Serializable {

	@Id
	@Column(name = "NAME", unique = true, nullable = false, length = 30)
	private String name;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "action")
	private Set<Permission> permissions;

	public Action() {

	}

	public Action(String name) {
		this.name = name;
	}

	public Action(String name, Set<Permission> permissions) {
		this.name = name;
		this.permissions = permissions;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Permission> getPermissions() {
		return this.permissions;
	}

	public void setPermissions(Set<Permission> permissions) {
		this.permissions = permissions;
	}
}


