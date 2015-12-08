package org.dasein.cloud.qingcloud.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DescribeRoutersResponseModel extends DescribeResponseModel {

	@JsonProperty("router_set")
	private List<DescribeRoutersResponseItemModel> routerSet;
	
	@Override
	public String getAction() {
		return "DescribeRouters";
	}

	public List<DescribeRoutersResponseItemModel> getRouterSet() {
		return routerSet;
	}

	public void setRouterSet(List<DescribeRoutersResponseItemModel> routerSet) {
		this.routerSet = routerSet;
	}

}
