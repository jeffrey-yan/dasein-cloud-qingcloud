/*
 *  *
 *  Copyright (C) 2009-2015 Dell, Inc.
 *  See annotations for authorship information
 *
 *  ====================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ====================================================================
 *
 */
package org.dasein.cloud.qingcloud.network.model;

import java.util.List;

import org.dasein.cloud.qingcloud.model.DescribeResponseModel;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Jane Wang on 12/17/2015.
 *
 * @author Jane Wang
 * @since 2016.02.1
 */
public class DescribeSecurityGroupRulesResponseModel extends
		DescribeResponseModel {

	@JsonProperty("security_group_rule_set")
	private List<DescribeSecurityGroupRulesResponseItemModel> securityGroupRuleSet;
	
	public List<DescribeSecurityGroupRulesResponseItemModel> getSecurityGroupRuleSet() {
		return securityGroupRuleSet;
	}

	public void setSecurityGroupRuleSet(
			List<DescribeSecurityGroupRulesResponseItemModel> securityGroupRuleSet) {
		this.securityGroupRuleSet = securityGroupRuleSet;
	}

	public static class DescribeSecurityGroupRulesResponseItemModel {
		
		@JsonProperty("security_group_id")
		private String securityGroupId;
		@JsonProperty("security_group_rule_id")
		private String securityGroupRuleId;
		@JsonProperty("priority")
		private Integer priority;
		@JsonProperty("protocol")
		private String protocol;
		@JsonProperty("direction")
		private String direction;
		@JsonProperty("action")
		private String action;
		@JsonProperty("val1")
		private String val1;
		@JsonProperty("val2")
		private String val2;
		@JsonProperty("val3")
		private String val3;
		
		public String getSecurityGroupId() {
			return securityGroupId;
		}
		public void setSecurityGroupId(String securityGroupId) {
			this.securityGroupId = securityGroupId;
		}
		public String getSecurityGroupRuleId() {
			return securityGroupRuleId;
		}
		public void setSecurityGroupRuleId(String securityGroupRuleId) {
			this.securityGroupRuleId = securityGroupRuleId;
		}
		public Integer getPriority() {
			return priority;
		}
		public void setPriority(Integer priority) {
			this.priority = priority;
		}
		public String getProtocol() {
			return protocol;
		}
		public void setProtocol(String protocol) {
			this.protocol = protocol;
		}
		public String getAction() {
			return action;
		}
		public void setAction(String action) {
			this.action = action;
		}
		public String getVal1() {
			return val1;
		}
		public void setVal1(String val1) {
			this.val1 = val1;
		}
		public String getVal2() {
			return val2;
		}
		public void setVal2(String val2) {
			this.val2 = val2;
		}
		public String getVal3() {
			return val3;
		}
		public void setVal3(String val3) {
			this.val3 = val3;
		}
		public String getDirection() {
			return direction;
		}
		public void setDirection(String direction) {
			this.direction = direction;
		}
		
	}
}
