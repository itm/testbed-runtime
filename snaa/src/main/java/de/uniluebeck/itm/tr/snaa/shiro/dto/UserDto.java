package de.uniluebeck.itm.tr.snaa.shiro.dto;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

@XmlRootElement
public class UserDto {

	private String email;

	private Set<RoleDto> roles;

	private String password;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = checkNotNull(email);
	}

	public Set<RoleDto> getRoles() {
		return roles;
	}

	public void setRoles(Set<RoleDto> roles) {
		this.roles = roles;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
