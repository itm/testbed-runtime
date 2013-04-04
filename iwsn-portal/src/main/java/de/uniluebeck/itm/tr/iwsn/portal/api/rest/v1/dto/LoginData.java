package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import eu.wisebed.api.v3.snaa.AuthenticationTriple;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class LoginData {
	public List<AuthenticationTriple> authenticationData;
}