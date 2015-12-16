package org.dasein.cloud.qingcloud.network.model;

import org.dasein.cloud.qingcloud.model.ResponseModel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateSecurityGroupResponseModel extends ResponseModel {

	@JsonProperty("security_group_id")
	private String securityGroupId;

	public String getSecurityGroupId() {
		return securityGroupId;
	}

	public void setSecurityGroupId(String securityGroupId) {
		this.securityGroupId = securityGroupId;
	}
}
