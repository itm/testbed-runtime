package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

@XmlRootElement
public class OperationStatusMap {

	public Map<String, JobNodeStatus> operationStatus;

}
