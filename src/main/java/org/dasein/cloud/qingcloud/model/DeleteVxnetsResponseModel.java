package org.dasein.cloud.qingcloud.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeleteVxnetsResponseModel extends ResponseModel {

	@JsonProperty("vxnets")
	private List<String> vxnets;
	
	@Override
	public String getAction() {
		return "DeleteVxnets";
	}

	public List<String> getVxnets() {
		return vxnets;
	}

	public void setVxnets(List<String> vxnets) {
		this.vxnets = vxnets;
	}

}
