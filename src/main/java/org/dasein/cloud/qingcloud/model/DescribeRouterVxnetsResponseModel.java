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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Jane Wang on 12/07/2015.
 *
 * @author Jane Wang
 * @since 2016.02.1
 */
public class DescribeRouterVxnetsResponseModel extends DescribeResponseModel {

	@JsonProperty("router_vxnet_set")
	private List<DescribeRouterVxnetsResponseItemModel> routerVxnetSet;
	
	@Override
	public String getAction() {
		return "DescribeRouterVxnets";
	}

	public List<DescribeRouterVxnetsResponseItemModel> getRouterVxnetSet() {
		return routerVxnetSet;
	}

	public void setRouterVxnetSet(
			List<DescribeRouterVxnetsResponseItemModel> routerVxnetSet) {
		this.routerVxnetSet = routerVxnetSet;
	}
	
	public static class DescribeRouterVxnetsResponseItemModel {

		@JsonProperty("router_id")
		private String routerId;
		@JsonProperty("manager_ip")
		private String managerIp;
		@JsonProperty("ip_network")
		private String ipNetwork;
		@JsonProperty("dyn_ip_end")
		private String dynIpEnd;
		@JsonProperty("dyn_ip_start")
		private String dynIpStart;
		@JsonProperty("vxnet_id")
		private String vxnetId;
		@JsonProperty("vxnet_name")
		private String vxnetName;
		@JsonProperty("create_time")
		private String createTime;
		@JsonProperty("features")
		private Integer features;
		
		public String getRouterId() {
			return routerId;
		}
		public void setRouterId(String routerId) {
			this.routerId = routerId;
		}
		public String getManagerIp() {
			return managerIp;
		}
		public void setManagerIp(String managerIp) {
			this.managerIp = managerIp;
		}
		public String getIpNetwork() {
			return ipNetwork;
		}
		public void setIpNetwork(String ipNetwork) {
			this.ipNetwork = ipNetwork;
		}
		public String getDynIpEnd() {
			return dynIpEnd;
		}
		public void setDynIpEnd(String dynIpEnd) {
			this.dynIpEnd = dynIpEnd;
		}
		public String getDynIpStart() {
			return dynIpStart;
		}
		public void setDynIpStart(String dynIpStart) {
			this.dynIpStart = dynIpStart;
		}
		public String getVxnetId() {
			return vxnetId;
		}
		public void setVxnetId(String vxnetId) {
			this.vxnetId = vxnetId;
		}
		public String getVxnetName() {
			return vxnetName;
		}
		public void setVxnetName(String vxnetName) {
			this.vxnetName = vxnetName;
		}
		public String getCreateTime() {
			return createTime;
		}
		public void setCreateTime(String createTime) {
			this.createTime = createTime;
		}
		public Integer getFeatures() {
			return features;
		}
		public void setFeatures(Integer features) {
			this.features = features;
		}
	}

}
