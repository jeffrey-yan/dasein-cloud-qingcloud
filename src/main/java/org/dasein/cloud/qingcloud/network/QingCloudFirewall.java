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

import java.util.ArrayList;
import java.util.List;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.VisibleScope;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.network.AbstractFirewallSupport;
import org.dasein.cloud.network.Direction;
import org.dasein.cloud.network.Firewall;
import org.dasein.cloud.network.FirewallCapabilities;
import org.dasein.cloud.network.FirewallCreateOptions;
import org.dasein.cloud.network.FirewallRule;
import org.dasein.cloud.network.FirewallRuleCreateOptions;
import org.dasein.cloud.network.FirewallSupport;
import org.dasein.cloud.network.Permission;
import org.dasein.cloud.network.Protocol;
import org.dasein.cloud.network.RuleTarget;
import org.dasein.cloud.network.Subnet;
import org.dasein.cloud.qingcloud.QingCloud;
import org.dasein.cloud.qingcloud.model.ResponseModel;
import org.dasein.cloud.qingcloud.model.SimpleJobResponseModel;
import org.dasein.cloud.qingcloud.network.model.AddSecurityGroupRulesResponseModel;
import org.dasein.cloud.qingcloud.network.model.CreateSecurityGroupResponseModel;
import org.dasein.cloud.qingcloud.network.model.DeleteSecurityGroupRulesResponseModel;
import org.dasein.cloud.qingcloud.network.model.DeleteSecurityGroupsResponseModel;
import org.dasein.cloud.qingcloud.network.model.DescribeSecurityGroupRulesResponseModel;
import org.dasein.cloud.qingcloud.network.model.DescribeSecurityGroupRulesResponseModel.DescribeSecurityGroupRulesResponseItemModel;
import org.dasein.cloud.qingcloud.network.model.DescribeSecurityGroupsResponseModel;
import org.dasein.cloud.qingcloud.network.model.DescribeSecurityGroupsResponseModel.DescribeSecurityGroupsResponseItemModel;
import org.dasein.cloud.qingcloud.util.requester.QingCloudDriverToCoreMapper;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequestBuilder;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequester;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.requester.fluent.Requester;

/**
 * Created by Jane Wang on 12/16/2015.
 *
 * @author Jane Wang
 * @since 2016.02.1
 */
public class QingCloudFirewall extends
		AbstractFirewallSupport<QingCloud> implements FirewallSupport {

	private static final Integer DefaultResponseDataLimit = 999;
	
	protected QingCloudFirewall(QingCloud provider) {
		super(provider);
	}

	@Override
	public String authorize(String firewallId, Direction direction,
			Permission permission, RuleTarget sourceEndpoint,
			Protocol protocol, RuleTarget destinationEndpoint, int beginPort,
			int endPort, int precedence) throws CloudException,
			InternalException {
	
		APITrace.begin(getProvider(), "QingCloudFirewall.authorize");
		try {
			
			if (firewallId == null) {
				throw new InternalException("Invalid firewall id!");
			}
	
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("AddSecurityGroupRules");
			requestBuilder.parameter("security_group", firewallId);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			requestBuilder.parameter("rules.1.protocol", protocol.name().toLowerCase());
			requestBuilder.parameter("rules.1.priority", precedence);
			requestBuilder.parameter("rules.1.action", permission.equals(Permission.ALLOW) ? "accept" : "drop");
			requestBuilder.parameter("rules.1.direction", direction.equals(Direction.INGRESS) ? 0 : 1);
			if (sourceEndpoint.getCidr() != null) {
				requestBuilder.parameter("rules.1.val3", sourceEndpoint.getCidr());
			}
			if (protocol.equals(Protocol.TCP) || protocol.equals(Protocol.UDP)) {
				requestBuilder.parameter("rules.1.val1", beginPort);
				requestBuilder.parameter("rules.1.val2", endPort);
			} 
			Requester<AddSecurityGroupRulesResponseModel> requester = new QingCloudRequester<AddSecurityGroupRulesResponseModel, AddSecurityGroupRulesResponseModel>(
	        		getProvider(), 
	        		requestBuilder.build(), 
	        		AddSecurityGroupRulesResponseModel.class);
			AddSecurityGroupRulesResponseModel response = requester.execute();
			if (response == null || response.getSecurityGroupRules() == null || response.getSecurityGroupRules().size() == 0) {
				throw new InternalException("Authorize firewall rule failed!");
			}
		
			applySecurityGroup(response.getSecurityGroupRules().get(0), null);
			return response.getSecurityGroupRules().get(0);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public String create(FirewallCreateOptions options)
			throws InternalException, CloudException {

		APITrace.begin(getProvider(), "QingCloudFirewall.create");
		try {
		
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("CreateSecurityGroup");
			if (options.getName() != null) {
				requestBuilder.parameter("security_group_name", options.getName());
			}
			requestBuilder.parameter("zone", getProviderDataCenterId());
	        Requester<CreateSecurityGroupResponseModel> requester = new QingCloudRequester<CreateSecurityGroupResponseModel, CreateSecurityGroupResponseModel>(
	        		getProvider(), 
	        		requestBuilder.build(), 
	        		CreateSecurityGroupResponseModel.class);
	        CreateSecurityGroupResponseModel response = requester.execute();
			if (response == null || response.getSecurityGroupId() == null) {
				throw new InternalException("Create firewall failed!");
			}
			
			if (options.getInitialRules() != null && options.getInitialRules().length > 0) {
				for (FirewallRuleCreateOptions rule : options.getInitialRules()) {
					authorize(response.getSecurityGroupId(), rule);
				}
			}
			
			if (options.getProviderVlanId() != null) {
				
				requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("ModifyRouterAttributes");
				requestBuilder.parameter("router", options.getProviderVlanId());
				requestBuilder.parameter("security_group", response.getSecurityGroupId());
				requestBuilder.parameter("zone", getProviderDataCenterId());
				Requester<ResponseModel> modifyRouterAttributesRequester = new QingCloudRequester<ResponseModel, ResponseModel>(
		        		getProvider(), 
		        		requestBuilder.build(), 
		        		ResponseModel.class);
				modifyRouterAttributesRequester.execute();
		        
				applySecurityGroup(response.getSecurityGroupId(), null);
			}
			
			return response.getSecurityGroupId();
			
		} finally {
			APITrace.end();
		}
	}
	
	@Override
	public void delete(String firewallId) throws InternalException,
			CloudException {
		
		APITrace.begin(getProvider(), "QingCloudFirewall.delete");
		try {
		
			if (firewallId == null) {
				throw new InternalException("Invalid firewall id!");
			}
			
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DeleteSecurityGroups");
			requestBuilder.parameter("security_groups.1", firewallId);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			Requester<DeleteSecurityGroupsResponseModel> requester = new QingCloudRequester<DeleteSecurityGroupsResponseModel, DeleteSecurityGroupsResponseModel>(
	        		getProvider(), 
	        		requestBuilder.build(), 
	        		DeleteSecurityGroupsResponseModel.class);
			DeleteSecurityGroupsResponseModel response = requester.execute();
			if (response == null || response.getSecurityGroups() == null || 
					response.getSecurityGroups().size() == 0 || !response.getSecurityGroups().get(0).equals(firewallId)) {
				throw new InternalException("Delete firewall " + firewallId + " failed!");
			}
			
		} finally {
			APITrace.end();
		}
	}

	@Override
	public FirewallCapabilities getCapabilities() throws CloudException,
			InternalException {
		return new QingCloudFirewallCapabilities(getProvider());
	}

	@Override
	public Firewall getFirewall(String firewallId) throws InternalException,
			CloudException {
		APITrace.begin(getProvider(), "QingCloudFirewall.getFirewall");
		try {
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeSecurityGroups");
			requestBuilder.parameter("verbose", 1);
			requestBuilder.parameter("security_groups.1", firewallId);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			Requester<List<Firewall>> subnetRequester = new QingCloudRequester<DescribeSecurityGroupsResponseModel, List<Firewall>>(
	                getProvider(), 
	                requestBuilder.build(), 
	                new FirewallsMapper(), 
	                DescribeSecurityGroupsResponseModel.class);
			List<Firewall> firewalls = subnetRequester.execute();
			if (firewalls == null || firewalls.size() == 0) {
				throw new InternalException("Get firewall " + firewallId + " failed!");
			}
			return firewalls.get(0);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public Iterable<FirewallRule> getRules(String firewallId)
			throws InternalException, CloudException {
		APITrace.begin(getProvider(), "QingCloudFirewall.getRules");
		try {
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeSecurityGroupRules");
			requestBuilder.parameter("security_group", firewallId);
			requestBuilder.parameter("limit", DefaultResponseDataLimit);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			Requester<List<FirewallRule>> subnetRequester = new QingCloudRequester<DescribeSecurityGroupRulesResponseModel, List<FirewallRule>>(
	                getProvider(), 
	                requestBuilder.build(), 
	                new FirewallRulesMapper(), 
	                DescribeSecurityGroupRulesResponseModel.class);
			return subnetRequester.execute();
		} finally {
			APITrace.end();
		}
	}

	@Override
	public boolean isSubscribed() throws CloudException, InternalException {
		return true;
	}
 
	@Override
	public Iterable<Firewall> list() throws InternalException, CloudException {
		APITrace.begin(getProvider(), "QingCloudFirewall.list");
		try {
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeSecurityGroups");
			requestBuilder.parameter("verbose", 1);
			requestBuilder.parameter("limit", DefaultResponseDataLimit);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			Requester<List<Firewall>> subnetRequester = new QingCloudRequester<DescribeSecurityGroupsResponseModel, List<Firewall>>(
	                getProvider(), 
	                requestBuilder.build(), 
	                new FirewallsMapper(), 
	                DescribeSecurityGroupsResponseModel.class);
			return subnetRequester.execute();
		} finally {
			APITrace.end();
		}
	}

	@Override
	public Iterable<ResourceStatus> listFirewallStatus()
			throws InternalException, CloudException {
		APITrace.begin(getProvider(), "QingCloudFirewall.listFirewallStatus");
		try {
			List<ResourceStatus> statuses = new ArrayList<ResourceStatus>();
			for ( Firewall firewall : list()) {
				statuses.add(new ResourceStatus(firewall.getProviderFirewallId(), true));
			}
			return statuses;
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void revoke(String providerFirewallRuleId) throws InternalException,
			CloudException {
		APITrace.begin(getProvider(), "QingCloudFirewall.revoke");
		try {
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DeleteSecurityGroupRules");
			requestBuilder.parameter("security_group_rules.1", providerFirewallRuleId);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			Requester<DeleteSecurityGroupRulesResponseModel> requester = new QingCloudRequester<DeleteSecurityGroupRulesResponseModel, DeleteSecurityGroupRulesResponseModel>(
	        		getProvider(), 
	        		requestBuilder.build(), 
	        		DeleteSecurityGroupRulesResponseModel.class);
			DeleteSecurityGroupRulesResponseModel response = requester.execute();
			if (response == null || response.getSecurityGroupRules() == null || 
					response.getSecurityGroupRules().size() == 0 || !response.getSecurityGroupRules().get(0).equals(providerFirewallRuleId)) {
				throw new InternalException("Revoke firewall rule " + providerFirewallRuleId + " failed!");
			}
		} finally {
			APITrace.end();
		}
	}

	private String getProviderDataCenterId() throws InternalException, CloudException {
		String regionId = getContext().getRegionId();
        if (regionId == null) {
            throw new InternalException("No region was set for this request");
        }
        Iterable<DataCenter> dataCenters = getProvider().getDataCenterServices().listDataCenters(regionId);
        return dataCenters.iterator().next().getProviderDataCenterId();//each account has one DC in each region
	}
	
	private void applySecurityGroup(String securityGroupId, List<String> instanceIds) 
			throws InternalException, CloudException {
		QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("ApplySecurityGroup");
		requestBuilder.parameter("security_group", securityGroupId);
		requestBuilder.parameter("zone", getProviderDataCenterId());
		if (instanceIds != null && instanceIds.size() > 0) {
			for (int i = 0; i < instanceIds.size(); i++) {
				requestBuilder.parameter("instances." + (i + 1), instanceIds.get(i));
			}
		}
		Requester<SimpleJobResponseModel> requester = new QingCloudRequester<SimpleJobResponseModel, SimpleJobResponseModel>(
        		getProvider(), 
        		requestBuilder.build(), 
        		SimpleJobResponseModel.class);
		requester.execute();
	}
	
	private class FirewallsMapper extends QingCloudDriverToCoreMapper<DescribeSecurityGroupsResponseModel, List<Firewall>> {
		
		@Override
		protected List<Firewall> doMapFrom(
				DescribeSecurityGroupsResponseModel responseModel) {
			try {
				List<Firewall> firewalls = new ArrayList<Firewall>();
				if (responseModel != null && responseModel.getSecurityGroupSet() != null && responseModel.getSecurityGroupSet().size() > 0) {
					for (DescribeSecurityGroupsResponseItemModel item : responseModel.getSecurityGroupSet()) {
						Firewall firewall = new Firewall();
						firewall.setActive(true);
						firewall.setAvailable(true);
						firewall.setDescription(item.getDescription());
						firewall.setName(item.getSecurityGroupName());
						firewall.setProviderFirewallId(item.getSecurityGroupId());
						if (item.getResources() != null) {
							if (item.getResources().get("resource_type").equals("router")) {
								firewall.setProviderVlanId(item.getResources().get("resource_id").toString());
								firewall.setSubnetAssociations((String[]) mapFromSubnets(getProvider().getNetworkServices().getVlanSupport().listSubnets(
										item.getResources().get("resource_id").toString())).toArray());
							}
						}
						firewall.setRegionId(getContext().getRegionId());
						firewall.setVisibleScope(VisibleScope.ACCOUNT_DATACENTER);
						firewall.setRules(mapFromFirewallRules(getRules(firewall.getProviderFirewallId())));
						firewalls.add(firewall);
					}
				}
				return firewalls;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		private List<FirewallRule> mapFromFirewallRules(Iterable<FirewallRule> iterable) {
			List<FirewallRule> rules = null;
			for (FirewallRule rule : iterable) {
				if (rules == null) {
					rules = new ArrayList<FirewallRule>();
				}
				rules.add(rule);
			}
			return rules;
		}
		
		private List<String> mapFromSubnets(Iterable<Subnet> iterable) {
			List<String> subnets = null;
			for (Subnet subnet : iterable) {
				if (subnets == null) {
					subnets = new ArrayList<String>();
				}
				subnets.add(subnet.getProviderSubnetId());
			}
			return subnets;
		}
	}
	
	private class FirewallRulesMapper extends QingCloudDriverToCoreMapper<DescribeSecurityGroupRulesResponseModel, List<FirewallRule>> {
		@Override
		protected List<FirewallRule> doMapFrom(
				DescribeSecurityGroupRulesResponseModel responseModel) {
			try {
				List<FirewallRule> firewallRules = new ArrayList<FirewallRule>();
				if (responseModel != null && responseModel.getSecurityGroupRuleSet() != null && responseModel.getSecurityGroupRuleSet().size() > 0) {
					for (DescribeSecurityGroupRulesResponseItemModel item : responseModel.getSecurityGroupRuleSet()) {
						Protocol protocol = mapFromProtocol(item.getProtocol());
						FirewallRule rule = FirewallRule.getInstance(item.getSecurityGroupRuleId(), 
								item.getSecurityGroupId(),  
								mapFromVal3OrSecurityGroupId(item.getVal3(), item.getSecurityGroupId()), 
								mapFromDirection(item.getDirection()), 
								protocol, 
								mapFromAction(item.getAction()), 
								RuleTarget.getGlobal(item.getSecurityGroupId()), 
								mapFromValAndProtocol(item.getVal1(), protocol), 
								mapFromValAndProtocol(item.getVal2(), protocol));
						firewallRules.add(rule);
					}
				}
				return firewallRules;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		private int mapFromValAndProtocol(String val, Protocol protocol) {
			return protocol.equals(Protocol.TCP) || protocol.equals(Protocol.UDP) ? Integer.valueOf(val) : 0;
		}
		
		private Protocol mapFromProtocol(String protocol) {
			if (protocol != null && (protocol.equals("tcp") || protocol.equals("udp") || protocol.equals("icmp"))) {
				return Protocol.valueOf(protocol.toUpperCase());
			}
			return null;
		}
		
		private Direction mapFromDirection (String direction) {
			if (direction != null && direction.equals("0")) {
				return Direction.INGRESS;
			}
			if (direction != null && direction.equals("1")) {
				return Direction.EGRESS;
			}
			return null;
		}
		
		private RuleTarget mapFromVal3OrSecurityGroupId (String val3, String securityGroupId) {
			if (val3 == null) {
				return RuleTarget.getGlobal(securityGroupId);
			} else {
				return RuleTarget.getCIDR(val3);
			}
		}
		
		private Permission mapFromAction (String action) {
			if ( action != null && action.equals("accept")) {
				return Permission.ALLOW;
			}
			if ( action != null && action.equals("drop")) {
				return Permission.DENY;
			}
			return null;
		}
	}
}
