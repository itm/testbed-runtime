package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import de.uniluebeck.itm.tr.iwsn.messages.SingleNodeResponse;
import org.apache.shiro.codec.Base64;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static de.uniluebeck.itm.tr.iwsn.common.Base64Helper.encode;

@XmlRootElement
public class SingleNodeResponseMessage {

    @XmlElement
    public final String type = "singleNodeResponse";

    @XmlElement(required = true)
    public long requestId;

    @XmlElement(required = true)
    public String nodeUrn;

    @XmlElement(name = "responseBase64")
    public String response;

    @XmlElement
    public int statusCode;

    @XmlElement
    public String errorMessage;


    public SingleNodeResponseMessage(SingleNodeResponse response) {
        requestId = response.getRequestId();
        nodeUrn = response.getNodeUrn();
        this.response = encode(response.getResponse().toByteArray());
        statusCode = response.getStatusCode();
        errorMessage = response.getErrorMessage();
    }

    @Override
    public String toString() {
        return "SingleNodeResponseMessage{" +
                "type='" + type + '\'' +
                ", nodeUrn=" + nodeUrn + '\'' +
                ", requestId=" + requestId +
                '}';
    }
}
