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
package org.dasein.cloud.qingcloud.network;

import java.util.Collections;
import java.util.Locale;

import org.dasein.cloud.AbstractCapabilities;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.network.IPAddressCapabilities;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.qingcloud.QingCloud;

/**
 * Created by Jane Wang on 12/07/2015.
 *
 * @author Jane Wang
 * @since 2016.02.1
 */
public class QingCloudIpAddressCapabilities extends
		AbstractCapabilities<QingCloud> implements IPAddressCapabilities {

	public QingCloudIpAddressCapabilities(QingCloud provider) {
		super(provider);
	}

	@Override
	public String getProviderTermForIpAddress(Locale locale) {
		return "Elastic IP";
	}

	@Override
	public Requirement identifyVlanForVlanIPRequirement()
			throws CloudException, InternalException {
		return Requirement.REQUIRED;
	}

	@Override
	public Requirement identifyVlanForIPRequirement() throws CloudException,
			InternalException {
		return Requirement.NONE;
	}

	@Override
	public Requirement identifyVMForPortForwarding() throws CloudException,
			InternalException {
		return Requirement.NONE;
	}

	@Override
	public boolean isAssigned(IPVersion version) throws CloudException,
			InternalException {
		if (version != null && version.equals(IPVersion.IPV4)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean canBeAssigned(VmState vmState) throws CloudException,
			InternalException {
		if (vmState != null && vmState.equals(VmState.RUNNING)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean isAssignablePostLaunch(IPVersion version)
			throws CloudException, InternalException {
		return false;
	}

	@Override
	public boolean isForwarding(IPVersion version) throws CloudException,
			InternalException {
		return false;
	}

	@Override
	public boolean isRequestable(IPVersion version) throws CloudException,
			InternalException {
		if (version != null && version.equals(IPVersion.IPV4)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Iterable<IPVersion> listSupportedIPVersions() throws CloudException,
			InternalException {
		return Collections.singletonList(IPVersion.IPV4);
	}

	@Override
	public boolean supportsVLANAddresses(IPVersion ofVersion)
			throws InternalException, CloudException {
		return true;
	}

}
