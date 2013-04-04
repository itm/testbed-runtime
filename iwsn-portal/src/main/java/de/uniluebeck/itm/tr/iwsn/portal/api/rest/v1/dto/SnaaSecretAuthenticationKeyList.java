package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util.Base64Helper;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util.JSONHelper.fromJSON;

@XmlRootElement
public class SnaaSecretAuthenticationKeyList {

	public List<SecretAuthenticationKey> secretAuthenticationKeys;

	public SnaaSecretAuthenticationKeyList() {
	}

	public SnaaSecretAuthenticationKeyList(String json) {
		try {
			this.secretAuthenticationKeys = fromJSON(
					Base64Helper.decode(json),
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