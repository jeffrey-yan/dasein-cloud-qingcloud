package org.dasein.cloud.qingcloud.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeleteTagsResponseModel extends ResponseModel {

	@JsonProperty("tags")
	private List<String> tags;
	
	@Override
	public String getAction() {
		return "DeleteTags";
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

}
