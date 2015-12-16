package org.dasein.cloud.qingcloud.network;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.network.AbstractFirewallSupport;
import org.dasein.cloud.network.Direction;
import org.dasein.cloud.network.Firewall;
import org.dasein.cloud.network.FirewallCapabilities;
import org.dasein.cloud.network.FirewallCreateOptions;
import org.dasein.cloud.network.FirewallRule;
import org.dasein.cloud.network.FirewallSupport;
import org.dasein.cloud.network.Permission;
import org.dasein.cloud.network.Protocol;
import org.dasein.cloud.network.RuleTarget;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.qingcloud.QingCloud;
import org.dasein.cloud.qingcloud.network.model.AddSecurityGroupRulesResponseModel;
import org.dasein.cloud.qingcloud.network.model.CreateSecurityGroupResponseModel;
import org.dasein.cloud.qingcloud.network.model.DeleteSecurityGroupsResponseModel;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequestBuilder;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequester;
import org.dasein.cloud.util.requester.fluent.Requester;

public class QingCloudFirewallSupport extends
		AbstractFirewallSupport<QingCloud> implements FirewallSupport {

	protected QingCloudFirewallSupport(QingCloud provider) {
		super(provider);
	}

	@Override
	public String authorize(String firewallId, Direction direction,
			Permission permission, RuleTarget sourceEndpoint,
			Protocol protocol, RuleTarget destinationEndpoint, int beginPort,
			int endPort, int precedence) throws CloudException,
			InternalException {
	
		if (firewallId == null || protocol == null || precedence < 0) {
			throw new InternalException("Invalid firewall id or protocol or precedence!");
		}

		QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("AddSecurityGroupRules");
		requestBuilder.parameter("security_group", firewallId);
		requestBuilder.parameter("rules.1.protocol", protocol.name().toLowerCase());
		requestBuilder.parameter("rules.1.priority", precedence);
		if (permission != null) {
			requestBuilder.parameter("rules.1.action", permission.equals(Permission.ALLOW) ? "accept" : "drop");
		}
		if (direction != null) {
			requestBuilder.parameter("rules.1.direction", direction.equals(Direction.INGRESS) ? 0 : 1);
			if (direction.equals(Direction.INGRESS) && destinationEndpoint != null) {
				String source  = null;
				if (destinationEndpoint.getCidr() != null) {
					requestBuilder.parameter("rules.1.val3", destinationEndpoint.getCidr());
				} else if (destinationEndpoint.getProviderVirtualMachineId() != null) {
					VirtualMachine virtualMachine = getProvider().getComputeServices().getVirtualMachineSupport().getVirtualMachine(destinationEndpoint.getProviderVirtualMachineId());
					requestBuilder.parameter("destinationEndpoint.getProviderVlanId()", virtualMachine.getPrivateAddresses()[0].getIpAddress());
				} else if (destinationEndpoint.getProviderVlanId() != null) {
					VLAN vlan = getProvider().getNetworkServices().getVlanSupport().getVlan(destinationEndpoint.getProviderVlanId());
					requestBuilder.parameter("rules.1.val3", vlan.getCidr());
				}
			}
		}
		if (protocol.equals(Protocol.TCP) || protocol.equals(Protocol.UDP)) {
			requestBuilder.parameter("rules.1.val1", beginPort);
			requestBuilder.parameter("rules.1.val2", endPort);
		} else if (protocol.equals(Protocol.ICMP)) {
			//TODO
		}
		requestBuilder.parameter("zone", getProviderDataCenterId());
		Requester<AddSecurityGroupRulesResponseModel> requester = new QingCloudRequester<AddSecurityGroupRulesResponseModel, AddSecurityGroupRulesResponseModel>(
        		getProvider(), 
        		requestBuilder.build(), 
        		AddSecurityGroupRulesResponseModel.class);
		AddSecurityGroupRulesResponseModel response = requester.execute();
		if (response == null || response.getSecurityGroupRules() == null || response.getSecurityGroupRules().size() == 0) {
			throw new InternalException("Authorize firewall rule failed!");
		}
		return response.getSecurityGroupRules().get(0);
	}

	@Override
	public String create(FirewallCreateOptions options)
			throws InternalException, CloudException {

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
		return response.getSecurityGroupId();
	}

	@Override
	public void delete(String firewallId) throws InternalException,
			CloudException {
		
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
	}

	@Override
	public FirewallCapabilities getCapabilities() throws CloudException,
			InternalException {
		return new QingCloudFirewallCapabilities(getProvider());
	}

	@Override
	public Firewall getFirewall(String firewallId) throws InternalException,
			CloudException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<FirewallRule> getRules(String firewallId)
			throws InternalException, CloudException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isSubscribed() throws CloudException, InternalException {
		return true;
	}

	@Override
	public Iterable<Firewall> list() throws InternalException, CloudException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<ResourceStatus> listFirewallStatus()
			throws InternalException, CloudException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void revoke(String providerFirewallRuleId) throws InternalException,
			CloudException {
		// TODO Auto-generated method stub

	}

	private String getProviderDataCenterId() throws InternalException, CloudException {
		String regionId = getContext().getRegionId();
        if (regionId == null) {
            throw new InternalException("No region was set for this request");
        }

        Iterable<DataCenter> dataCenters = getProvider().getDataCenterServices().listDataCenters(regionId);
        return dataCenters.iterator().next().getProviderDataCenterId();//each account has one DC in each region
	}
}
