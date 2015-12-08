package org.dasein.cloud.qingcloud.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateRoutersResponseModel extends ResponseModel {

	@JsonProperty("routers")
	private List<String> routers;
	
	@Override
	public String getAction() {
		return "CreateRoutersResponse";
	}

	public List<String> getRouters() {
		return routers;
	}

	public void setRouters(List<String> routers) {
		this.routers = routers;
	}
}
