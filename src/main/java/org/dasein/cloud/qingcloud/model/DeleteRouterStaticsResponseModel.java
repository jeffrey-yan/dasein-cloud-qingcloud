package org.dasein.cloud.qingcloud.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeleteRouterStaticsResponseModel extends ResponseModel {

	@JsonProperty("router_statics")
	private List<String> routerStatics;

	public List<String> getRouterStatics() {
		return routerStatics;
	}

	public void setRouterStatics(List<String> routerStatics) {
		this.routerStatics = routerStatics;
	}
}
