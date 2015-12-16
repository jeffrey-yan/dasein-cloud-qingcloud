package org.dasein.cloud.qingcloud.network.model;

import java.util.List;

import org.dasein.cloud.qingcloud.model.ResponseModel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AddRouterStaticsResponseModel extends ResponseModel {

	@JsonProperty("router_statics")
	private List<String> routerStatics;

	public List<String> getRouterStatics() {
		return routerStatics;
	}

	public void setRouterStatics(List<String> routerStatics) {
		this.routerStatics = routerStatics;
	}
}
