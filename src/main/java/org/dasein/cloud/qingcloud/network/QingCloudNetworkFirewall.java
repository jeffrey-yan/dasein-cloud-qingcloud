package org.dasein.cloud.qingcloud.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.VisibleScope;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.network.AbstractNetworkFirewallSupport;
import org.dasein.cloud.network.Direction;
import org.dasein.cloud.network.Firewall;
import org.dasein.cloud.network.FirewallCreateOptions;
import org.dasein.cloud.network.FirewallRule;
import org.dasein.cloud.network.FirewallRuleCreateOptions;
import org.dasein.cloud.network.NetworkFirewallCapabilities;
import org.dasein.cloud.network.NetworkFirewallSupport;
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
import org.dasein.cloud.qingcloud.network.model.DescribeSecurityGroupsResponseModel;
import org.dasein.cloud.qingcloud.network.model.DescribeSecurityGroupRulesResponseModel.DescribeSecurityGroupRulesResponseItemModel;
import org.dasein.cloud.qingcloud.network.model.DescribeSecurityGroupsResponseModel.DescribeSecurityGroupsResponseItemModel;
import org.dasein.cloud.qingcloud.util.requester.QingCloudDriverToCoreMapper;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequestBuilder;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequester;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.requester.fluent.Requester;

public class QingCloudNetworkFirewall extends
		AbstractNetworkFirewallSupport<QingCloud> implements NetworkFirewallSupport {

	private static final Integer DefaultResponseDataLimit = 999;
	
	protected QingCloudNetworkFirewall(QingCloud provider) {
		super(provider);
	}

	@Override
	public String authorize(String firewallId, Direction direction,
			Permission permission, RuleTarget sourceEndpoint,
			Protocol protocol, RuleTarget destinationEndpoint, int beginPort,
			int endPort, int precedence) throws CloudException,
			InternalException {
		
		APITrace.begin(getProvider(), "QingCloudNetworkFirewall.authorize");
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
		
			applySecurityGroup(response.getSecurityGroupRules().get(0));
			return response.getSecurityGroupRules().get(0);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public String createFirewall(FirewallCreateOptions options)
			throws InternalException, CloudException {
		
		APITrace.begin(getProvider(), "QingCloudNetworkFirewall.create");
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
			
			//associate network firewall to router
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
				applySecurityGroup(response.getSecurityGroupId());
			}
			
			return response.getSecurityGroupId();
			
		} finally {
			APITrace.end();
		}
	}

	@Override
	public NetworkFirewallCapabilities getCapabilities() throws CloudException,
			InternalException {
		return new QingCloudNetworkFirewallCapabilities(getProvider());
	}

	@Override
	public Firewall getFirewall(String firewallId) throws InternalException,
			CloudException {
		APITrace.begin(getProvider(), "QingCloudNetworkFirewall.getFirewall");
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
	public boolean isSubscribed() throws CloudException, InternalException {
		return true;
	}

	@Override
	public Iterable<ResourceStatus> listFirewallStatus()
			throws InternalException, CloudException {
		APITrace.begin(getProvider(), "QingCloudNetworkFirewall.listFirewallStatus");
		try {
			List<ResourceStatus> statuses = new ArrayList<ResourceStatus>();
			for ( Firewall firewall : listFirewalls()) {
				statuses.add(new ResourceStatus(firewall.getProviderFirewallId(), true));
			}
			return statuses;
		} finally {
			APITrace.end();
		}
	}

	@Override
	public Iterable<Firewall> listFirewalls() throws InternalException,
			CloudException {
		APITrace.begin(getProvider(), "QingCloudNetworkFirewall.listFirewalls");
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
	public Iterable<FirewallRule> listRules(String firewallId)
			throws InternalException, CloudException {
		APITrace.begin(getProvider(), "QingCloudNetworkFirewall.listRules");
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
	public void removeFirewall(String... firewallIds) throws InternalException,
			CloudException {
		APITrace.begin(getProvider(), "QingCloudNetworkFirewall.removeFirewall");
		try {
		
			if (firewallIds == null && firewallIds.length == 0) {
				return;
			}
			
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DeleteSecurityGroups");
			for (int i = 0; i < firewallIds.length; i++) {
				requestBuilder.parameter("security_groups." + (i + 1), firewallIds[i]);
			}
			requestBuilder.parameter("zone", getProviderDataCenterId());
			Requester<DeleteSecurityGroupsResponseModel> requester = new QingCloudRequester<DeleteSecurityGroupsResponseModel, DeleteSecurityGroupsResponseModel>(
	        		getProvider(), 
	        		requestBuilder.build(), 
	        		DeleteSecurityGroupsResponseModel.class);
			requester.execute();
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void revoke(String providerFirewallRuleId) throws InternalException,
			CloudException {
		APITrace.begin(getProvider(), "QingCloudNetworkFirewall.revoke");
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

	@Override
	public String getProviderTermForNetworkFirewall(Locale locale) {
		try {
			return getCapabilities().getProviderTermForNetworkFirewall(locale);
		} catch (Exception e) {
			throw new RuntimeException("Find term for network firewall through capabilities failed!");
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
	
	private void applySecurityGroup(String securityGroupId) 
			throws InternalException, CloudException {
		QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("ApplySecurityGroup");
		requestBuilder.parameter("security_group", securityGroupId);
		requestBuilder.parameter("zone", getProviderDataCenterId());
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
						firewall.setRules(mapFromFirewallRules(listRules(firewall.getProviderFirewallId())));
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
