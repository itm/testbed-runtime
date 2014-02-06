package de.uniluebeck.itm.tr.snaa.shiro.entity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ROLES")
public class Role implements java.io.Serializable {

	@Id
	@Column(name = "NAME", unique = true, nullable = false, length = 150)
	private String name;

	@ManyToMany(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
	@JoinTable(name = "USERS_ROLES", joinColumns = {
			@JoinColumn(name = "ROLE_NAME", nullable = false, updatable = false)
	}, inverseJoinColumns = {
			@JoinColumn(name = "USER_NAME", nullable = false, updatable = false)
	})
	private Set<User> users;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "role")
	private Set<Permission> permissions;

	public Role() {

	}

	public Role(String name) {
		this.name = name;
	}

	public Role(String name, Set<User> users, Set<Permission> permissions) {
		this.name = name;
		this.users = users;
		this.permissions = permissions;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<User> getUsers() {
		return this.users;
	}

	public void setUsers(Set<User> users) {
		this.users = users;
	}

	public Set<Permission> getPermissions() {
		return this.permissions;
	}

	public void setPermissions(Set<Permission> permissions) {
		this.permissions = permissions;
	}
}


