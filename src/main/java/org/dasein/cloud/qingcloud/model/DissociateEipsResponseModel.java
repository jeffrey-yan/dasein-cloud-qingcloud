package org.dasein.cloud.qingcloud.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DissociateEipsResponseModel extends ResponseModel {

	@JsonProperty("job_id")
	private String jobId;
	
	@Override
	public String getAction() {
		return "DissociateEips";
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

}
