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
import org.dasein.cloud.VisibleScope;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.VLANCapabilities;
import org.dasein.cloud.qingcloud.QingCloud;
import org.dasein.cloud.util.NamingConstraints;

/**
 * Created by Jane Wang on 12/08/2015.
 *
 * @author Jane Wang
 * @since 2016.02.1
 */
public class QingCloudVlanCapabilities extends AbstractCapabilities<QingCloud>
		implements VLANCapabilities {

	public QingCloudVlanCapabilities(QingCloud provider) {
		super(provider);
	}

	@Override
	public boolean allowsNewNetworkInterfaceCreation() throws CloudException,
			InternalException {
		return false;
	}

	@Override
	public boolean allowsNewVlanCreation() throws CloudException,
			InternalException {
		return true;
	}

	@Override
	public boolean allowsNewRoutingTableCreation() throws CloudException,
			InternalException {
		return false;
	}

	@Override
	public boolean allowsNewSubnetCreation() throws CloudException,
			InternalException {
		return true;
	}

	@Override
	public boolean allowsMultipleTrafficTypesOverSubnet()
			throws CloudException, InternalException {
		return false;
	}

	@Override
	public boolean allowsMultipleTrafficTypesOverVlan() throws CloudException,
			InternalException {
		return false;
	}

	@Override
	public boolean allowsDeletionOfReservedSubnets() throws CloudException,
			InternalException {
		return true;
	}

	@Override
	public int getMaxNetworkInterfaceCount() throws CloudException,
			InternalException {
		return 0;
	}

	@Override
	public int getMaxVlanCount() throws CloudException, InternalException {
		return 2;
	}

	@Override
	public String getProviderTermForNetworkInterface(Locale locale) {
		return null;
	}

	@Override
	public String getProviderTermForSubnet(Locale locale) {
		return "VxNet";
	}

	@Override
	public String getProviderTermForVlan(Locale locale) {
		return "Router";
	}

	@Override
	public Requirement getRoutingTableSupport() throws CloudException,
			InternalException {
		return Requirement.NONE;
	}

	@Override
	public Requirement getSubnetSupport() throws CloudException,
			InternalException {
		return Requirement.REQUIRED;
	}

	@Override
	public VisibleScope getVLANVisibleScope() {
		return VisibleScope.ACCOUNT_DATACENTER;
	}

	@Override
	public Requirement identifySubnetDCRequirement() throws CloudException,
			InternalException {
		return Requirement.REQUIRED;
	}

	@Override
	public boolean isNetworkInterfaceSupportEnabled() throws CloudException,
			InternalException {
		return false;
	}

	@Override
	public boolean isSubnetDataCenterConstrained() throws CloudException,
			InternalException {
		return true;
	}

	@Override
	public boolean isVlanDataCenterConstrained() throws CloudException,
			InternalException {
		return true;
	}

	@Override
	public Iterable<IPVersion> listSupportedIPVersions() throws CloudException,
			InternalException {
		return Collections.singletonList(IPVersion.IPV4);
	}

	@Override
	public boolean supportsInternetGatewayCreation() throws CloudException,
			InternalException {
		return false;
	}

	@Override
	public boolean supportsRawAddressRouting() throws CloudException,
			InternalException {
		return false;
	}

	@Override
	public NamingConstraints getVlanNamingConstraints() throws CloudException,
			InternalException {
		return NamingConstraints.getAlphaNumeric(1, 255);
	}

}
