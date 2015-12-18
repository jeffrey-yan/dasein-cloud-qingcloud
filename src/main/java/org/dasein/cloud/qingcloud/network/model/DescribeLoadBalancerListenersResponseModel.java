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
public class DescribeLoadBalancerListenersResponseModel extends
		DescribeResponseModel {

	@JsonProperty("loadbalancer_listener_set")
	private List<DescribeLoadBalancerListenersResponseItemModel> loadbalancerListenerSet;
	
	public List<DescribeLoadBalancerListenersResponseItemModel> getLoadbalancerListenerSet() {
		return loadbalancerListenerSet;
	}

	public void setLoadbalancerListenerSet(
			List<DescribeLoadBalancerListenersResponseItemModel> loadbalancerListenerSet) {
		this.loadbalancerListenerSet = loadbalancerListenerSet;
	}

	public static class DescribeLoadBalancerListenersResponseItemModel {
		
		@JsonProperty("loadbalancer_listener_id")
		private String loadBalancerListenerId;
		@JsonProperty("loadbalancer_listener_name")
		private String loadbalancerListenerName;
		@JsonProperty("backends")
		private List<DescribeLoadBalancerListenerBackends> backends;
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
		@JsonProperty("listener_option")
		private Integer listenerOption;
		
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
		public List<DescribeLoadBalancerListenerBackends> getBackends() {
			return backends;
		}
		public void setBackends(List<DescribeLoadBalancerListenerBackends> backends) {
			this.backends = backends;
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
		public Integer getListenerOption() {
			return listenerOption;
		}
		public void setListenerOption(Integer listenerOption) {
			this.listenerOption = listenerOption;
		}
		
		public static class DescribeLoadBalancerListenerBackends {
			
			@JsonProperty("loadbalancer_backend_id")
			private String loadbalancerBackendId;
			@JsonProperty("loadbalancer_backend_name")
			private String loadbalancerBackendName;
			@JsonProperty("weight")
			private Integer weight;
			@JsonProperty("port")
			private Integer port;
			@JsonProperty("resource_id")
			private String resourceId;
			@JsonProperty("loadbalancer_listener_id")
			private String loadbalancerListenerId;
			@JsonProperty("loadbalancer_id")
			private String loadbalancerId;
			@JsonProperty("create_time")
			private String create_time;
			
			public String getLoadbalancerBackendId() {
				return loadbalancerBackendId;
			}
			public void setLoadbalancerBackendId(String loadbalancerBackendId) {
				this.loadbalancerBackendId = loadbalancerBackendId;
			}
			public String getLoadbalancerBackendName() {
				return loadbalancerBackendName;
			}
			public void setLoadbalancerBackendName(String loadbalancerBackendName) {
				this.loadbalancerBackendName = loadbalancerBackendName;
			}
			public Integer getWeight() {
				return weight;
			}
			public void setWeight(Integer weight) {
				this.weight = weight;
			}
			public Integer getPort() {
				return port;
			}
			public void setPort(Integer port) {
				this.port = port;
			}
			public String getResourceId() {
				return resourceId;
			}
			public void setResourceId(String resourceId) {
				this.resourceId = resourceId;
			}
			public String getLoadbalancerListenerId() {
				return loadbalancerListenerId;
			}
			public void setLoadbalancerListenerId(String loadbalancerListenerId) {
				this.loadbalancerListenerId = loadbalancerListenerId;
			}
			public String getLoadbalancerId() {
				return loadbalancerId;
			}
			public void setLoadbalancerId(String loadbalancerId) {
				this.loadbalancerId = loadbalancerId;
			}
			public String getCreate_time() {
				return create_time;
			}
			public void setCreate_time(String create_time) {
				this.create_time = create_time;
			}
		}
	}
}
