package org.dasein.cloud.qingcloud.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AssociateEipResponseModel extends ResponseModel {

	@JsonProperty("job_id")
	private String jobId;
	
	@Override
	public String getAction() {
		return "AssociateEip";
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

}