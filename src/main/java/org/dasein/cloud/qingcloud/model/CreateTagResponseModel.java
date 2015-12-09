package org.dasein.cloud.qingcloud.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateTagResponseModel extends ResponseModel {

	@JsonProperty("tag_id")
	private String tagId;
	
	@Override
	public String getAction() {
		return "CreateTag";
	}

	public String getTagId() {
		return tagId;
	}

	public void setTagId(String tagId) {
		this.tagId = tagId;
	}

}
