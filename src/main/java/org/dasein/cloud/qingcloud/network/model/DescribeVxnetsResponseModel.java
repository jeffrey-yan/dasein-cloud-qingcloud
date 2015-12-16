/*
 *  *
 *  Copyright (C) 2009-2015 Dell, Inc.
 *  See annotations for authorship information
 *
 *  ====================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ====================================================================
 *
 */
package org.dasein.cloud.qingcloud.network.model;

import java.util.List;
import java.util.Map;

import org.dasein.cloud.qingcloud.model.DescribeResponseModel;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Jane Wang on 12/07/2015.
 *
 * @author Jane Wang
 * @since 2016.02.1
 */
public class DescribeVxnetsResponseModel extends DescribeResponseModel {

	@JsonProperty("vxnet_set")
	private List<DescribeVxnetsResponseItemModel> vxnetSet;

	public List<DescribeVxnetsResponseItemModel> getVxnetSet() {
		return vxnetSet;
	}

	public void setVxnetSet(List<DescribeVxnetsResponseItemModel> vxnetSet) {
		this.vxnetSet = vxnetSet;
	}
	
	public static class DescribeVxnetsResponseItemModel {

		@JsonProperty("vxnet_type")
		private Integer vxnetType;
		@JsonProperty("vxnet_id")
		private String vxnetId;
		@JsonProperty("vxnet_name")
		private String vxnetName;
		@JsonProperty("create_time")
		private String createTime;
		@JsonProperty("description")
		private String description;
		@JsonProperty("router")
		private Map router;
		@JsonProperty("instance_ids")
		private List<String> instanceIds;
		
		public Integer getVxnetType() {
			return vxnetType;
		}
		public void setVxnetType(Integer vxnetType) {
			this.vxnetType = vxnetType;
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
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public Map getRouter() {
			return router;
		}
		public void setRouter(Map router) {
			this.router = router;
		}
		public List<String> getInstanceIds() {
			return instanceIds;
		}
		public void setInstanceIds(List<String> instanceIds) {
			this.instanceIds = instanceIds;
		}
	}
}
