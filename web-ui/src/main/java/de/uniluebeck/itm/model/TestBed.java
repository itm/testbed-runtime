package de.uniluebeck.itm.model;

import java.util.ArrayList;
import java.util.List;

public class TestBed {
	
	private String name;
	
	private String url;
	
	private String description;
	
	private String snaaUrl;
	
	private String rsUrl;
	
	private String sessionmanagementUrl;
	
	private final List<String> urnPrefixList = new ArrayList<String>();
	
	private boolean isFederated;
	
	private String username = "";
	
	private String password = "";
	
	public TestBed(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSnaaUrl() {
		return snaaUrl;
	}

	public void setSnaaUrl(String snaaUrl) {
		this.snaaUrl = snaaUrl;
	}

	public String getRsUrl() {
		return rsUrl;
	}

	public void setRsUrl(String rsUrl) {
		this.rsUrl = rsUrl;
	}

	public String getSessionmanagementUrl() {
		return sessionmanagementUrl;
	}

	public void setSessionmanagementUrl(String sessionmanagementUrl) {
		this.sessionmanagementUrl = sessionmanagementUrl;
	}

	public boolean isFederated() {
		return isFederated;
	}

	public void setFederated(boolean isFederated) {
		this.isFederated = isFederated;
	}

	public List<String> getUrnPrefixList() {
		return urnPrefixList;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
