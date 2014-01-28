package de.uniluebeck.itm.tr.snaa.shiro.entity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "USERS")
public class User implements java.io.Serializable {

	private String name;

	private String password;

	private String salt;

	private Set<Role> roles = new HashSet<Role>(0);

	public User() {
	}

	public User(String name) {
		this.name = name;
	}

	public User(String name, String password, String salt, Set<Role> roles) {
		this.name = name;
		this.password = password;
		this.salt = salt;
		this.roles = roles;
	}

	@Id
	@Column(name = "NAME", unique = true, nullable = false, length = 150)
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "PASSWORD", length = 1500)
	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Column(name = "SALT", length = 1500)
	public String getSalt() {
		return this.salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "USERS_ROLES", joinColumns = {
			@JoinColumn(name = "USER_NAME", nullable = false, updatable = false)
	}, inverseJoinColumns = {
			@JoinColumn(name = "ROLE_NAME", nullable = false, updatable = false)
	})
	public Set<Role> getRoles() {
		return this.roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}


}


