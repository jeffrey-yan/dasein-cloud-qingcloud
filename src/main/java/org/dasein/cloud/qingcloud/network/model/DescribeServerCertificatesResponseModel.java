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

import org.dasein.cloud.qingcloud.model.ResponseModel;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Jane Wang on 12/28/2015.
 *
 * @author Jane Wang
 * @since 2016.02.1
 */
public class DescribeServerCertificatesResponseModel extends ResponseModel {

	@JsonProperty("server_certificate_set")
	private List<DescribeServerCertificatesResponseItemModel> serverCertificateSet;
	
	public List<DescribeServerCertificatesResponseItemModel> getServerCertificateSet() {
		return serverCertificateSet;
	}

	public void setServerCertificateSet(
			List<DescribeServerCertificatesResponseItemModel> serverCertificateSet) {
		this.serverCertificateSet = serverCertificateSet;
	}

	public static class DescribeServerCertificatesResponseItemModel {
		
		@JsonProperty("server_certificate_id")
		private String serverCertificateId;
		@JsonProperty("server_certificate_name")
		private String serverCertificateName;
		@JsonProperty("private_key")
		private String privateKey;
		@JsonProperty("certificate_content")
		private String certificateContent;
		@JsonProperty("description")
		private String description;
		@JsonProperty("create_time")
		private String create_time;
		
		public String getServerCertificateId() {
			return serverCertificateId;
		}
		public void setServerCertificateId(String serverCertificateId) {
			this.serverCertificateId = serverCertificateId;
		}
		public String getServerCertificateName() {
			return serverCertificateName;
		}
		public void setServerCertificateName(String serverCertificateName) {
			this.serverCertificateName = serverCertificateName;
		}
		public String getPrivateKey() {
			return privateKey;
		}
		public void setPrivateKey(String privateKey) {
			this.privateKey = privateKey;
		}
		public String getCertificateContent() {
			return certificateContent;
		}
		public void setCertificateContent(String certificateContent) {
			this.certificateContent = certificateContent;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public String getCreate_time() {
			return create_time;
		}
		public void setCreate_time(String create_time) {
			this.create_time = create_time;
		}
	}
}
