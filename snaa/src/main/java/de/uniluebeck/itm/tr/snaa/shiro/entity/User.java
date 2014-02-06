package de.uniluebeck.itm.tr.snaa.shiro.entity;

import javax.persistence.*;
import java.util.Set;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.FetchType.EAGER;

@Entity
@Table(name = "USERS")
public class User implements java.io.Serializable {

	@Id
	@Column(name = "NAME", unique = true, nullable = false, length = 150)
	private String name;

	@Column(name = "PASSWORD", length = 1500)
	private String password;

	@Column(name = "SALT", length = 1500)
	private String salt;

	@ManyToMany(fetch = EAGER, cascade = ALL)
	@JoinTable(name = "USERS_ROLES", joinColumns = {
			@JoinColumn(name = "USER_NAME", nullable = false, updatable = false)
	}, inverseJoinColumns = {
			@JoinColumn(name = "ROLE_NAME", nullable = false, updatable = false)
	})
	private Set<Role> roles;

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

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getSalt() {
		return this.salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

	public Set<Role> getRoles() {
		return this.roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}
}


