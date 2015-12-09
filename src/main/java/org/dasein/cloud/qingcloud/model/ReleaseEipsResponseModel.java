package org.dasein.cloud.qingcloud.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReleaseEipsResponseModel extends ResponseModel {

	@JsonProperty("job_id")
	private String jobId;
	
	@Override
	public String getAction() {
		return "ReleaseEips";
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

}
