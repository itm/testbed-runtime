package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserRegistrationDto {

	private String email;

	private String oldPassword;

	private String password;

	public String getEmail() {
		return email;
	}

	public void setEmail(final String email) {
		this.email = email;
	}

	public String getOldPassword() {
		return oldPassword;
	}

	public void setOldPassword(final String oldPassword) {
		this.oldPassword = oldPassword;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(final String password) {
		this.password = password;
	}
}
