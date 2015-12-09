package org.dasein.cloud.qingcloud.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

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
