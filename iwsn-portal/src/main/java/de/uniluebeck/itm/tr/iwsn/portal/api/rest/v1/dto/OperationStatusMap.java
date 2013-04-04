package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto;

import eu.wisebed.restws.jobs.JobNodeStatus;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

@XmlRootElement
public class OperationStatusMap {

	public Map<String, JobNodeStatus> operationStatus;

}
