package org.dasein.cloud.qingcloud.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DescribeVxnetsResponseModel extends DescribeResponseModel {

	@JsonProperty("vxnet_set")
	private List<DescribeVxnetsResponseItemModel> vxnetSet;
	
	@Override
	public String getAction() {
		return "DescribeVxnetsResponse";
	}

	public List<DescribeVxnetsResponseItemModel> getVxnetSet() {
		return vxnetSet;
	}

	public void setVxnetSet(List<DescribeVxnetsResponseItemModel> vxnetSet) {
		this.vxnetSet = vxnetSet;
	}
}
