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
public class DescribeRoutersResponseModel extends DescribeResponseModel {

	public static class DescribeRoutersResponseItemModel {

		public class Vxnet {
			
			@JsonProperty("vxnet_id")
			private String vxnetId;
			@JsonProperty("nic_id")
			private String nicId;
			
			public String getVxnetId() {
				return vxnetId;
			}
			public void setVxnetId(String vxnetId) {
				this.vxnetId = vxnetId;
			}
			public String getNicId() {
				return nicId;
			}
			public void setNicId(String nicId) {
				this.nicId = nicId;
			}
		}
		
		@JsonProperty("router_id")
		private String routerId;
		@JsonProperty("router_name")
		private String routerName;
		@JsonProperty("description")
		private String description;
		@JsonProperty("router_type")
		private Integer routerType;
		@JsonProperty("private_ip")
		private String privateIp;
		@JsonProperty("is_applied")
		private Integer isApplied;
		@JsonProperty("status")
		private String status;
		@JsonProperty("transition_status")
		private String transitionStatus;
		@JsonProperty("create_time")
		private String create_time;
		@JsonProperty("status_time")
		private String status_time;
		@JsonProperty("eip")
		private Map eip;
		@JsonProperty("vxnets")
		private List<Vxnet> vxnets;
		
		public String getRouterId() {
			return routerId;
		}
		public void setRouterId(String routerId) {
			this.routerId = routerId;
		}
		public String getRouterName() {
			return routerName;
		}
		public void setRouterName(String routerName) {
			this.routerName = routerName;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public Integer getRouterType() {
			return routerType;
		}
		public void setRouterType(Integer routerType) {
			this.routerType = routerType;
		}
		public String getPrivateIp() {
			return privateIp;
		}
		public void setPrivateIp(String privateIp) {
			this.privateIp = privateIp;
		}
		public Integer getIsApplied() {
			return isApplied;
		}
		public void setIsApplied(Integer isApplied) {
			this.isApplied = isApplied;
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
		public String getCreate_time() {
			return create_time;
		}
		public void setCreate_time(String create_time) {
			this.create_time = create_time;
		}
		public String getStatus_time() {
			return status_time;
		}
		public void setStatus_time(String status_time) {
			this.status_time = status_time;
		}
		public Map getEip() {
			return eip;
		}
		public void setEip(Map eip) {
			this.eip = eip;
		}
		public List<Vxnet> getVxnets() {
			return vxnets;
		}
		public void setVxnets(List<Vxnet> vxnets) {
			this.vxnets = vxnets;
		}
	}
	
	@JsonProperty("router_set")
	private List<DescribeRoutersResponseItemModel> routerSet;

	public List<DescribeRoutersResponseItemModel> getRouterSet() {
		return routerSet;
	}

	public void setRouterSet(List<DescribeRoutersResponseItemModel> routerSet) {
		this.routerSet = routerSet;
	}

}
