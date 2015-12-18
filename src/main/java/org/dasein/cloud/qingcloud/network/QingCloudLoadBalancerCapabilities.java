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

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import org.dasein.cloud.AbstractCapabilities;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.VisibleScope;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.LbAlgorithm;
import org.dasein.cloud.network.LbEndpointType;
import org.dasein.cloud.network.LbPersistence;
import org.dasein.cloud.network.LbProtocol;
import org.dasein.cloud.network.LoadBalancerAddressType;
import org.dasein.cloud.network.LoadBalancerCapabilities;
import org.dasein.cloud.qingcloud.QingCloud;
import org.dasein.cloud.util.NamingConstraints;

/**
 * Created by Jane Wang on 12/17/2015.
 *
 * @author Jane Wang
 * @since 2016.02.1
 */
public class QingCloudLoadBalancerCapabilities extends
		AbstractCapabilities<QingCloud> implements LoadBalancerCapabilities {

	public QingCloudLoadBalancerCapabilities(QingCloud provider) {
		super(provider);
	}

	@Override
	public LoadBalancerAddressType getAddressType() throws CloudException,
			InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getMaxPublicPorts() throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxHealthCheckTimeout() throws CloudException,
			InternalException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMinHealthCheckTimeout() throws CloudException,
			InternalException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxHealthCheckInterval() throws CloudException,
			InternalException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMinHealthCheckInterval() throws CloudException,
			InternalException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getProviderTermForLoadBalancer(Locale locale) {
		return "Load Balancer";
	}

	@Override
	public VisibleScope getLoadBalancerVisibleScope() {
		return VisibleScope.ACCOUNT_DATACENTER;
	}

	@Override
	public boolean healthCheckRequiresLoadBalancer() throws CloudException,
			InternalException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean healthCheckRequiresListener() throws CloudException,
			InternalException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Requirement healthCheckRequiresName() throws CloudException,
			InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Requirement healthCheckRequiresPort() throws CloudException,
			InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Requirement identifyEndpointsOnCreateRequirement()
			throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Requirement identifyListenersOnCreateRequirement()
			throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Requirement identifyVlanOnCreateRequirement() throws CloudException,
			InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Requirement identifyHealthCheckOnCreateRequirement()
			throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAddressAssignedByProvider() throws CloudException,
			InternalException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isDataCenterLimited() throws CloudException,
			InternalException {
		return true;
	}

	@Override
	public Iterable<LbAlgorithm> listSupportedAlgorithms()
			throws CloudException, InternalException {
		return Arrays.asList(LbAlgorithm.ROUND_ROBIN, LbAlgorithm.LEAST_CONN, LbAlgorithm.SOURCE);
	}

	@Override
	public Iterable<LbEndpointType> listSupportedEndpointTypes()
			throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<IPVersion> listSupportedIPVersions() throws CloudException,
			InternalException {
		return Collections.singletonList(IPVersion.IPV4);
	}

	@Override
	public Iterable<LbPersistence> listSupportedPersistenceOptions()
			throws CloudException, InternalException {
		return Collections.singletonList(LbPersistence.COOKIE);
	}

	@Override
	public Iterable<LbProtocol> listSupportedProtocols() throws CloudException,
			InternalException {
		return Arrays.asList(LbProtocol.RAW_TCP, LbProtocol.HTTP, LbProtocol.HTTPS);
	}

	@Override
	public boolean supportsAddingEndpoints() throws CloudException,
			InternalException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsMonitoring() throws CloudException,
			InternalException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsMultipleTrafficTypes() throws CloudException,
			InternalException {
		return false;
	}

	@Override
	public boolean supportsSslCertificateStore() throws CloudException,
			InternalException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public NamingConstraints getLoadBalancerNamingConstraints()
			throws CloudException, InternalException {
		return NamingConstraints.getAlphaNumeric(1, 255);
	}

}
