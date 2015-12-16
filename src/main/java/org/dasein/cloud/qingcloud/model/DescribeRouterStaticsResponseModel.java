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
package org.dasein.cloud.qingcloud.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Jane Wang on 12/07/2015.
 *
 * @author Jane Wang
 * @since 2016.02.1
 */
public class DescribeRouterStaticsResponseModel extends ResponseModel {
	
	@JsonProperty("router_static_set")
	private List<DescribeRouterStaticsResponseItemModel> routerStaticSet;
	
	public List<DescribeRouterStaticsResponseItemModel> getRouterStaticSet() {
		return routerStaticSet;
	}

	public void setRouterStaticSet(
			List<DescribeRouterStaticsResponseItemModel> routerStaticSet) {
		this.routerStaticSet = routerStaticSet;
	}

	public static class DescribeRouterStaticsResponseItemModel {

		@JsonProperty("router_id")
		private String routerId;
		@JsonProperty("vxnet_id")
		private String vxnetId;
		@JsonProperty("static_type")
		private Integer staticType;
		@JsonProperty("router_static_id")
		private String routerStaticId;
		@JsonProperty("create_time")
		private String createTime;
		@JsonProperty("val1")
		private String val1;
		@JsonProperty("val2")
		private String val2;
		@JsonProperty("val3")
		private String val3;
		@JsonProperty("val4")
		private String val4;
		@JsonProperty("val5")
		private String val5;
		
		public String getRouterId() {
			return routerId;
		}
		public void setRouterId(String routerId) {
			this.routerId = routerId;
		}
		public String getVxnetId() {
			return vxnetId;
		}
		public void setVxnetId(String vxnetId) {
			this.vxnetId = vxnetId;
		}
		public Integer getStaticType() {
			return staticType;
		}
		public void setStaticType(Integer staticType) {
			this.staticType = staticType;
		}
		public String getRouterStaticId() {
			return routerStaticId;
		}
		public void setRouterStaticId(String routerStaticId) {
			this.routerStaticId = routerStaticId;
		}
		public String getCreateTime() {
			return createTime;
		}
		public void setCreateTime(String createTime) {
			this.createTime = createTime;
		}
		public String getVal1() {
			return val1;
		}
		public void setVal1(String val1) {
			this.val1 = val1;
		}
		public String getVal2() {
			return val2;
		}
		public void setVal2(String val2) {
			this.val2 = val2;
		}
		public String getVal3() {
			return val3;
		}
		public void setVal3(String val3) {
			this.val3 = val3;
		}
		public String getVal4() {
			return val4;
		}
		public void setVal4(String val4) {
			this.val4 = val4;
		}
		public String getVal5() {
			return val5;
		}
		public void setVal5(String val5) {
			this.val5 = val5;
		}
	}
}
