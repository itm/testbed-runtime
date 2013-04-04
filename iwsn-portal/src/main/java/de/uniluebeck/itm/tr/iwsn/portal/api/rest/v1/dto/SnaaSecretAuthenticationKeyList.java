package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.restws.util.Base64Helper;
import eu.wisebed.restws.util.JSONHelper;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class SnaaSecretAuthenticationKeyList {

	public List<SecretAuthenticationKey> secretAuthenticationKeys;

	public SnaaSecretAuthenticationKeyList() {
	}

	public SnaaSecretAuthenticationKeyList(String json) {
		try {
			this.secretAuthenticationKeys = JSONHelper.fromJSON(Base64Helper.decode(json),
					SnaaSecretAuthenticationKeyList.class
			).secretAuthenticationKeys;
		} catch (Exception e) {
			this.secretAuthenticationKeys = null;
		}
	}

	public SnaaSecretAuthenticationKeyList(List<SecretAuthenticationKey> secretAuthenticationKeys) {
		this.secretAuthenticationKeys = secretAuthenticationKeys;
	}

}