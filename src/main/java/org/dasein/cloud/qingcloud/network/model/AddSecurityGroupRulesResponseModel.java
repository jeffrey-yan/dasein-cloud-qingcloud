package org.dasein.cloud.qingcloud.network.model;

import java.util.List;

import org.dasein.cloud.qingcloud.model.ResponseModel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AddSecurityGroupRulesResponseModel extends ResponseModel {

	@JsonProperty("security_group_rules")
	private List<String> securityGroupRules;

	public List<String> getSecurityGroupRules() {
		return securityGroupRules;
	}

	public void setSecurityGroupRules(List<String> securityGroupRules) {
		this.securityGroupRules = securityGroupRules;
	}
}
