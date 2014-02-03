package de.uniluebeck.itm.tr.snaa.shiro.dto;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

@XmlRootElement
public class UserDto {

	private String name;

	private Set<RoleDto> roles;

	private String password;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = checkNotNull(name);
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
