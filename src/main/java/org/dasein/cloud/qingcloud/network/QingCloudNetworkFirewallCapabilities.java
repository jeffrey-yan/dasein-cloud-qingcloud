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
import java.util.Locale;

import org.dasein.cloud.AbstractCapabilities;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.network.Direction;
import org.dasein.cloud.network.FirewallConstraints;
import org.dasein.cloud.network.NetworkFirewallCapabilities;
import org.dasein.cloud.network.Permission;
import org.dasein.cloud.network.RuleTargetType;
import org.dasein.cloud.qingcloud.QingCloud;

/**
 * Created by Jane Wang on 01/05/2016.
 *
 * @author Jane Wang
 * @since 2016.02.1
 */
public class QingCloudNetworkFirewallCapabilities extends
		AbstractCapabilities<QingCloud> implements NetworkFirewallCapabilities {

	public QingCloudNetworkFirewallCapabilities(QingCloud provider) {
		super(provider);
	}

	@Override
	public FirewallConstraints getFirewallConstraintsForCloud()
			throws InternalException, CloudException {
		return null;
	}

	@Override
	public String getProviderTermForNetworkFirewall(Locale locale) {
		return "Security Group";
	}

	@Override
	public Requirement identifyPrecedenceRequirement()
			throws InternalException, CloudException {
		return Requirement.REQUIRED;
	}

	@Override
	public boolean isZeroPrecedenceHighest() throws InternalException,
			CloudException {
		return true;
	}

	@Override
	public Iterable<RuleTargetType> listSupportedDestinationTypes()
			throws InternalException, CloudException {
		return Arrays.asList(RuleTargetType.CIDR, RuleTargetType.GLOBAL);
	}

	@Override
	public Iterable<Direction> listSupportedDirections()
			throws InternalException, CloudException {
		return Arrays.asList(Direction.EGRESS, Direction.INGRESS);
	}

	@Override
	public Iterable<Permission> listSupportedPermissions()
			throws InternalException, CloudException {
		return Arrays.asList(Permission.ALLOW, Permission.DENY);
	}

	@Override
	public Iterable<RuleTargetType> listSupportedSourceTypes()
			throws InternalException, CloudException {
		return Arrays.asList(RuleTargetType.CIDR, RuleTargetType.GLOBAL);
	}

	@Override
	public boolean supportsNetworkFirewallCreation() throws CloudException,
			InternalException {
		return true;
	}

}
