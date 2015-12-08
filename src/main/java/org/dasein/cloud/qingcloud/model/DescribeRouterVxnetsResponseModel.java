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

}
