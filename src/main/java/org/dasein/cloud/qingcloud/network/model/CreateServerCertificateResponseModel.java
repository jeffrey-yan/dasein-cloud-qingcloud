package org.dasein.cloud.qingcloud.network.model;

import org.dasein.cloud.qingcloud.model.ResponseModel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateServerCertificateResponseModel extends ResponseModel {

	@JsonProperty("server_certificate_id")
	private String serverCertificateId;

	public String getServerCertificateId() {
		return serverCertificateId;
	}

	public void setServerCertificateId(String serverCertificateId) {
		this.serverCertificateId = serverCertificateId;
	}
}
