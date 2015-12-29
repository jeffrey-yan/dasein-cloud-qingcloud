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
 * Created by Jane Wang on 12/18/2015.
 *
 * @author Jane Wang
 * @since 2016.02.1
 */
public class DescribeLoadBalancersResponseModel extends DescribeResponseModel {

	@JsonProperty("loadbalancer_set")
	private List<DescribeLoadBalancersResponseItemModel> loadbalancerSet;
	
	public List<DescribeLoadBalancersResponseItemModel> getLoadbalancerSet() {
		return loadbalancerSet;
	}

	public void setLoadbalancerSet(
			List<DescribeLoadBalancersResponseItemModel> loadbalancerSet) {
		this.loadbalancerSet = loadbalancerSet;
	}

	public static class DescribeLoadBalancersResponseItemModel {
		
		@JsonProperty("loadbalancer_id")
		private String loadbalancerId;
		@JsonProperty("loadbalancer_name")
		private String loadbalancerName;
		@JsonProperty("description")
		private String description;
		@JsonProperty("listeners")
		private List<DescribeLoadBalancerListener> listeners;
		@JsonProperty("is_applied")
		private Integer is_applied;
		@JsonProperty("status")
		private String status;
		@JsonProperty("transition_status")
		private String transitionStatus;
		@JsonProperty("eips")
		private List<DescribeLoadBalancerEip> eips;
		@JsonProperty("create_time")
		private String createTime;
		@JsonProperty("status_time")
		private String statusTime;
		@JsonProperty("security_group_id")
		private String securityGroupId;
		
		public String getLoadbalancerId() {
			return loadbalancerId;
		}
		public void setLoadbalancerId(String loadbalancerId) {
			this.loadbalancerId = loadbalancerId;
		}
		public String getLoadbalancerName() {
			return loadbalancerName;
		}
		public void setLoadbalancerName(String loadbalancerName) {
			this.loadbalancerName = loadbalancerName;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public List<DescribeLoadBalancerListener> getListeners() {
			return listeners;
		}
		public void setListeners(List<DescribeLoadBalancerListener> listeners) {
			this.listeners = listeners;
		}
		public Integer getIs_applied() {
			return is_applied;
		}
		public void setIs_applied(Integer is_applied) {
			this.is_applied = is_applied;
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
		public List<DescribeLoadBalancerEip> getEips() {
			return eips;
		}
		public void setEips(List<DescribeLoadBalancerEip> eips) {
			this.eips = eips;
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
		public String getSecurityGroupId() {
			return securityGroupId;
		}
		public void setSecurityGroupId(String securityGroupId) {
			this.securityGroupId = securityGroupId;
		}
		
		public static class DescribeLoadBalancerListener {
			@JsonProperty("loadbalancer_listener_id")
			private String loadBalancerListenerId;
			@JsonProperty("loadbalancer_listener_name")
			private String loadbalancerListenerName;
			@JsonProperty("balance_mode")
			private String balanceMode;
			@JsonProperty("session_sticky")
			private String sessionSticky;
			@JsonProperty("create_time")
			private String createTime;
			@JsonProperty("forwardfor")
			private Integer forwardfor;
			@JsonProperty("healthy_check_method")
			private String healthyCheckMethod;
			@JsonProperty("healthy_check_option")
			private String healthyCheckOption;
			@JsonProperty("listener_protocol")
			private String listenerProtocol;
			@JsonProperty("backend_protocol")
			private String backendProtocol;
			@JsonProperty("listener_port")
			private Integer listenerPort;
			
			public String getLoadBalancerListenerId() {
				return loadBalancerListenerId;
			}
			public void setLoadBalancerListenerId(String loadBalancerListenerId) {
				this.loadBalancerListenerId = loadBalancerListenerId;
			}
			public String getLoadbalancerListenerName() {
				return loadbalancerListenerName;
			}
			public void setLoadbalancerListenerName(String loadbalancerListenerName) {
				this.loadbalancerListenerName = loadbalancerListenerName;
			}
			public String getBalanceMode() {
				return balanceMode;
			}
			public void setBalanceMode(String balanceMode) {
				this.balanceMode = balanceMode;
			}
			public String getSessionSticky() {
				return sessionSticky;
			}
			public void setSessionSticky(String sessionSticky) {
				this.sessionSticky = sessionSticky;
			}
			public String getCreateTime() {
				return createTime;
			}
			public void setCreateTime(String createTime) {
				this.createTime = createTime;
			}
			public Integer getForwardfor() {
				return forwardfor;
			}
			public void setForwardfor(Integer forwardfor) {
				this.forwardfor = forwardfor;
			}
			public String getHealthyCheckMethod() {
				return healthyCheckMethod;
			}
			public void setHealthyCheckMethod(String healthyCheckMethod) {
				this.healthyCheckMethod = healthyCheckMethod;
			}
			public String getHealthyCheckOption() {
				return healthyCheckOption;
			}
			public void setHealthyCheckOption(String healthyCheckOption) {
				this.healthyCheckOption = healthyCheckOption;
			}
			public String getListenerProtocol() {
				return listenerProtocol;
			}
			public void setListenerProtocol(String listenerProtocol) {
				this.listenerProtocol = listenerProtocol;
			}
			public String getBackendProtocol() {
				return backendProtocol;
			}
			public void setBackendProtocol(String backendProtocol) {
				this.backendProtocol = backendProtocol;
			}
			public Integer getListenerPort() {
				return listenerPort;
			}
			public void setListenerPort(Integer listenerPort) {
				this.listenerPort = listenerPort;
			}
		}
		
		public static class DescribeLoadBalancerEip {
			
			@JsonProperty("eip_id")
			private String eipId;
			@JsonProperty("eip_name")
			private String eipName;
			@JsonProperty("eip_addr")
			private String eipAddr;
			
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
			public String getEipAddr() {
				return eipAddr;
			}
			public void setEipAddr(String eipAddr) {
				this.eipAddr = eipAddr;
			}
		}
	}
	
}
