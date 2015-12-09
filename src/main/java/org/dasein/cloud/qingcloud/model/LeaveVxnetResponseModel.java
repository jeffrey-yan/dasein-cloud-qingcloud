package org.dasein.cloud.qingcloud.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LeaveVxnetResponseModel extends ResponseModel {

	@JsonProperty("job_id")
	private String jobId;
	
	@Override
	public String getAction() {
		return "LeaveVxnet";
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}
	
}
