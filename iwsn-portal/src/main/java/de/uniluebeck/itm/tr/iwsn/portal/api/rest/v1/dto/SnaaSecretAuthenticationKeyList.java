package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import eu.wisebed.api.v3.common.SecretAuthenticationKey;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class SnaaSecretAuthenticationKeyList {

	public List<SecretAuthenticationKey> secretAuthenticationKeys;

	public SnaaSecretAuthenticationKeyList() {

	}

	public SnaaSecretAuthenticationKeyList(List<SecretAuthenticationKey> secretAuthenticationKeys) {
		this.secretAuthenticationKeys = secretAuthenticationKeys;
	}

}