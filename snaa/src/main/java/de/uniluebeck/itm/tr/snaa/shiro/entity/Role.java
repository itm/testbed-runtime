package de.uniluebeck.itm.tr.snaa.shiro.entity;

import javax.persistence.*;
import java.util.Set;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.FetchType.LAZY;

@Entity
@Table(name = "ROLES")
public class Role implements java.io.Serializable {

	@Id
	@Column(name = "NAME", unique = true, nullable = false, length = 150)
	private String name;

	@ManyToMany(fetch = LAZY, mappedBy = "roles")
	private Set<User> users;

	@OneToMany(fetch = LAZY, mappedBy = "role", cascade = ALL)
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

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final Role role = (Role) o;
		return name.equals(role.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}


