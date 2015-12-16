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
package org.dasein.cloud.qingcloud.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Jane Wang on 12/07/2015.
 *
 * @author Jane Wang
 * @since 2016.02.1
 */
public class DescribeEipsResponseModel extends DescribeResponseModel {

	@JsonProperty("eip_set")
	private List<DescribeEipsResponseItemModel> eipSet;

	public List<DescribeEipsResponseItemModel> getEipSet() {
		return eipSet;
	}
	public void setEipSet(List<DescribeEipsResponseItemModel> eipSet) {
		this.eipSet = eipSet;
	}
	
	public static class DescribeEipsResponseItemModel {

		@JsonProperty("eip_id")
		private String eipId;
		@JsonProperty("eip_name")
		private String eipName;
		@JsonProperty("description")
		private String description;
		@JsonProperty("bandwidth")
		private Integer bandwidth;
		@JsonProperty("billing_mode")
		private String billingMode;
		@JsonProperty("status")
		private String status;
		@JsonProperty("transition_status")
		private String transitionStatus;
		@JsonProperty("icp_codes")
		private String icpCodes;
		@JsonProperty("create_time")
		private String createTime;
		@JsonProperty("status_time")
		private String statusTime;
		@JsonProperty("resource")
		private Map resource;
		@JsonProperty("eip_group")
		private Map eipGroup;
		@JsonProperty("eip_addr")
		private String eipAddress;
		
		public String getEipId() {
			return eipId;
		}
		public void setEipId(String eipId) {
			this.eipId = eipId;
		}
		public String getEipName() {
			return eipName;
		}
		public void setEipName(String eipName) {
			this.eipName = eipName;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public Integer getBandwidth() {
			return bandwidth;
		}
		public void setBandwidth(Integer bandwidth) {
			this.bandwidth = bandwidth;
		}
		public String getBillingMode() {
			return billingMode;
		}
		public void setBillingMode(String billingMode) {
			this.billingMode = billingMode;
		}
		public String getStatus() {
			return status;
		}
		public void setStatus(String status) {
			this.status = status;
		}
		public String getTransitionStatus() {
			return transitionStatus;
		}
		public void setTransitionStatus(String transitionStatus) {
			this.transitionStatus = transitionStatus;
		}
		public String getIcpCodes() {
			return icpCodes;
		}
		public void setIcpCodes(String icpCodes) {
			this.icpCodes = icpCodes;
		}
		public String getCreateTime() {
			return createTime;
		}
		public void setCreateTime(String createTime) {
			this.createTime = createTime;
		}
		public String getStatusTime() {
			return statusTime;
		}
		public void setStatusTime(String statusTime) {
			this.statusTime = statusTime;
		}
		public Map getResource() {
			return resource;
		}
		public void setResource(Map resource) {
			this.resource = resource;
		}
		public Map getEipGroup() {
			return eipGroup;
		}
		public void setEipGroup(Map eipGroup) {
			this.eipGroup = eipGroup;
		}
		public String getEipAddress() {
			return eipAddress;
		}
		public void setEipAddress(String eipAddress) {
			this.eipAddress = eipAddress;
		}
	}
}
