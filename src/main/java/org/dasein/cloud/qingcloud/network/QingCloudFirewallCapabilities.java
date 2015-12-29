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
import org.dasein.cloud.VisibleScope;
import org.dasein.cloud.network.Direction;
import org.dasein.cloud.network.FirewallCapabilities;
import org.dasein.cloud.network.FirewallConstraints;
import org.dasein.cloud.network.Permission;
import org.dasein.cloud.network.Protocol;
import org.dasein.cloud.network.RuleTargetType;
import org.dasein.cloud.qingcloud.QingCloud;
import org.dasein.cloud.util.NamingConstraints;

/**
 * Created by Jane Wang on 12/16/2015.
 *
 * @author Jane Wang
 * @since 2016.02.1
 */
public class QingCloudFirewallCapabilities extends AbstractCapabilities<QingCloud> implements FirewallCapabilities {

	public QingCloudFirewallCapabilities(QingCloud provider) {
		super(provider);
	}

	@Override
	public FirewallConstraints getFirewallConstraintsForCloud()
			throws InternalException, CloudException {
		return null;
	}

	@Override
	public String getProviderTermForFirewall(Locale locale) {
		return "Security Group";
	}

	@Override
	public VisibleScope getFirewallVisibleScope() {
		return VisibleScope.ACCOUNT_DATACENTER;
	}

	@Override
	public Requirement identifyPrecedenceRequirement(boolean inVlan)
			throws InternalException, CloudException {
		return Requirement.REQUIRED;
	}

	@Override
	public boolean isZeroPrecedenceHighest() throws InternalException,
			CloudException {
		return true;
	}

	@Override
	public Iterable<RuleTargetType> listSupportedDestinationTypes(boolean inVlan)
			throws InternalException, CloudException {
		return listSupportedDestinationTypes(inVlan, null);
	}

	@Override
	public Iterable<RuleTargetType> listSupportedDestinationTypes(
			boolean inVlan, Direction direction) throws InternalException,
			CloudException {
		return Arrays.asList(RuleTargetType.CIDR, RuleTargetType.GLOBAL);
	}

	@Override
	public Iterable<Direction> listSupportedDirections(boolean inVlan)
			throws InternalException, CloudException {
		return Arrays.asList(Direction.EGRESS, Direction.INGRESS);
	}

	@Override
	public Iterable<Permission> listSupportedPermissions(boolean inVlan)
			throws InternalException, CloudException {
		return Arrays.asList(Permission.ALLOW, Permission.DENY);
	}

	@Override
	public Iterable<Protocol> listSupportedProtocols(boolean inVlan)
			throws InternalException, CloudException {
		return Arrays.asList(Protocol.UDP, Protocol.TCP, Protocol.ICMP); //TODO gre, esp, ah, ipip
	}

	@Override
	public Iterable<RuleTargetType> listSupportedSourceTypes(boolean inVlan)
			throws InternalException, CloudException {
		return listSupportedSourceTypes(inVlan, null);
	}

	@Override
	public Iterable<RuleTargetType> listSupportedSourceTypes(boolean inVlan,
			Direction direction) throws InternalException, CloudException {
		return Arrays.asList(RuleTargetType.CIDR, RuleTargetType.GLOBAL);
	}

	@Override
	public boolean requiresRulesOnCreation() throws CloudException,
			InternalException {
		return false;
	}

	@Override
	public Requirement requiresVLAN() throws CloudException, InternalException {
		return Requirement.OPTIONAL;
	}

	@Override
	public boolean supportsRules(Direction direction, Permission permission,
			boolean inVlan) throws CloudException, InternalException {
		return true;
	}

	@Override
	public boolean supportsFirewallCreation(boolean inVlan)
			throws CloudException, InternalException {
		if (inVlan) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean supportsFirewallDeletion() throws CloudException,
			InternalException {
		return true;
	}

	@Override
	public NamingConstraints getFirewallNamingConstraints()
			throws CloudException, InternalException {
		return NamingConstraints.getAlphaNumeric(1, 255);
	}
}
