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
import java.util.Map;

import org.dasein.cloud.qingcloud.model.DescribeResponseModel;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Created by Jane Wang on 12/17/2015.
 *
 * @author Jane Wang
 * @since 2016.02.1
 */
public class DescribeSecurityGroupsResponseModel extends DescribeResponseModel {

	public static class DescribeSecurityGroupsResponseItemModel {
		
		@JsonProperty("security_group_id")
		private String securityGroupId;
		@JsonProperty("security_group_name")
		private String securityGroupName;
		@JsonProperty("description")
		private String description;
		@JsonProperty("is_applied")
		private Integer isApplied;
		@JsonProperty("is_default")
		private Integer isDefault;
		@JsonProperty("resources")
		private Map resources;
		@JsonProperty("create_time")
		private String createTime;
		
		public String getSecurityGroupId() {
			return securityGroupId;
		}
		public void setSecurityGroupId(String securityGroupId) {
			this.securityGroupId = securityGroupId;
		}
		public String getSecurityGroupName() {
			return securityGroupName;
		}
		public void setSecurityGroupName(String securityGroupName) {
			this.securityGroupName = securityGroupName;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public Integer getIsApplied() {
			return isApplied;
		}
		public void setIsApplied(Integer isApplied) {
			this.isApplied = isApplied;
		}
		public Integer getIsDefault() {
			return isDefault;
		}
		public void setIsDefault(Integer isDefault) {
			this.isDefault = isDefault;
		}
		public Map getResources() {
			return resources;
		}
		public void setResources(Map resources) {
			this.resources = resources;
		}
		public String getCreateTime() {
			return createTime;
		}
		public void setCreateTime(String createTime) {
			this.createTime = createTime;
		}
	}
	
	@JsonProperty("security_group_set")
	private List<DescribeSecurityGroupsResponseItemModel> securityGroupSet;

	public List<DescribeSecurityGroupsResponseItemModel> getSecurityGroupSet() {
		return securityGroupSet;
	}

	public void setSecurityGroupSet(
			List<DescribeSecurityGroupsResponseItemModel> securityGroupSet) {
		this.securityGroupSet = securityGroupSet;
	}
}
