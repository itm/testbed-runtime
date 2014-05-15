package de.uniluebeck.itm.tr.snaa.shiro.entity;

import javax.persistence.*;
import java.util.Set;

import static javax.persistence.FetchType.LAZY;

@Entity
@Table(name = "USERS")
public class User implements java.io.Serializable {

	@Id
	@Column(name = "EMAIL", unique = true, nullable = false, length = 150)
	private String email;

	@Column(name = "PASSWORD", length = 1500)
	private String password;

	@Column(name = "SALT", length = 1500)
	private String salt;

	@ManyToMany(fetch = LAZY)
	@JoinTable(name = "USERS_ROLES", joinColumns = {
			@JoinColumn(name = "USER_EMAIL", nullable = false, updatable = false)
	}, inverseJoinColumns = {
			@JoinColumn(name = "ROLE_NAME", nullable = false, updatable = false)
	})
	private Set<Role> roles;

	public User() {
	}

	public User(String email) {
		this.email = email;
	}

	public User(String email, String password, String salt, Set<Role> roles) {
		this.email = email;
		this.password = password;
		this.salt = salt;
		this.roles = roles;
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
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

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final User user = (User) o;
		return email.equals(user.email);
	}

	@Override
	public int hashCode() {
		return email.hashCode();
	}
}


