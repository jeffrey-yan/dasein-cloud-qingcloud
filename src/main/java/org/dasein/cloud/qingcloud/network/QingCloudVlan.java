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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.http.client.methods.HttpUriRequest;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.Tag;
import org.dasein.cloud.VisibleScope;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.network.AbstractVLANSupport;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.InternetGateway;
import org.dasein.cloud.network.Networkable;
import org.dasein.cloud.network.Subnet;
import org.dasein.cloud.network.SubnetCreateOptions;
import org.dasein.cloud.network.SubnetState;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.network.VLANCapabilities;
import org.dasein.cloud.network.VLANState;
import org.dasein.cloud.network.VLANSupport;
import org.dasein.cloud.network.VlanCreateOptions;
import org.dasein.cloud.qingcloud.QingCloud;
import org.dasein.cloud.qingcloud.model.CreateRoutersResponseModel;
import org.dasein.cloud.qingcloud.model.CreateVxnetsResponseModel;
import org.dasein.cloud.qingcloud.model.DescribeRouterVxnetsResponseItemModel;
import org.dasein.cloud.qingcloud.model.DescribeRouterVxnetsResponseModel;
import org.dasein.cloud.qingcloud.model.DescribeRoutersResponseModel;
import org.dasein.cloud.qingcloud.model.DescribeVxnetsResponseItemModel;
import org.dasein.cloud.qingcloud.model.DescribeVxnetsResponseModel;
import org.dasein.cloud.qingcloud.model.IpAddressResponseModel;
import org.dasein.cloud.qingcloud.util.requester.QingCloudDriverToCoreMapper;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequestBuilder;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequester;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.requester.fluent.Requester;

/**
 * Created by Jane Wang on 12/08/2015.
 *
 * @author Jane Wang
 * @since 2016.02.1
 */
public class QingCloudVlan extends AbstractVLANSupport<QingCloud> implements
		VLANSupport {

	protected class VlanIdentityGenerator {
		
		private String routerId;
		private String routerCidr;
		
		public VlanIdentityGenerator(String vlanId) {
			if (vlanId != null && vlanId.split(":").length == 3) {
				this.routerId = vlanId.split(":")[1];
				this.routerCidr = vlanId.split(":")[2];
			}
		}
		
		public VlanIdentityGenerator(String routerId, String routerCidr) {
			this.routerCidr = routerCidr;
			this.routerId = routerId;
		}
		
		public String getRouterId() {
			return routerId;
		}
		
		public String getRouterCidr() {
			return routerCidr;
		}
		
		public String toString() {
			return "vlan:" + routerId + ":" + routerCidr;
		}
	}
	
	protected class SubnetIdentityGenerator {
		
		private String subnetId;
		private String subnetCidr;
		private String vlanCidr;
		
		public SubnetIdentityGenerator(String subnetId) {
			if (subnetId != null && subnetId.split(":").length == 4) {
				this.subnetId = subnetId.split(":")[1];
				this.subnetCidr = subnetId.split(":")[2];
				this.vlanCidr = subnetId.split(":")[3];
			}
		}
		
		public SubnetIdentityGenerator(String subnetId, String subnetCidr, String vlanCidr) {
			this.subnetId = subnetId;
			this.subnetCidr = subnetCidr;
			this.vlanCidr = vlanCidr;
		}
		
		public String getSubnetId() {
			return subnetId;
		}
		
		public String getSubnetCidr() {
			return subnetCidr;
		}
		
		public String getVlanCidr() {
			return vlanCidr;
		}
		
		public String toString() {
			return "subnet:" + subnetId + ":" + subnetCidr;
		}
	}
	
	protected QingCloudVlan(QingCloud provider) {
		super(provider);
	}

	@Override
	public String createInternetGateway(String vlanId) throws CloudException,
			InternalException {
		APITrace.begin(getProvider(), "QingCloudVlan.createInternetGateway");
		try {
			//TODO
			return null;
		} finally {
			APITrace.end();
		}
	}

	@Override
	public Subnet createSubnet(SubnetCreateOptions options)
			throws CloudException, InternalException {
		
		APITrace.begin(getProvider(), "QingCloudVlan.createSubnet");
		try {
			if (options == null || options.getName() == null || 
					options.getProviderVlanId() == null || options.getCidr() == null) {
				throw new InternalException("Invalid subnet create options!");
			}
			
			//create subnet
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("CreateVxnets");
			requestBuilder.parameter("vxnet_type", 1);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			if (options != null && options.getName() != null) {
				requestBuilder.parameter("vxnet_name", options.getName());
			}
			HttpUriRequest request = requestBuilder.build();
            Requester<String> requester = new QingCloudRequester<CreateVxnetsResponseModel, String>(
                    getProvider(), 
                    request, 
                    new QingCloudDriverToCoreMapper<CreateVxnetsResponseModel, String>(){
        				@Override
        				protected String doMapFrom(CreateVxnetsResponseModel responseModel) {
        					if (responseModel != null && responseModel.getVxnets() != null && responseModel.getVxnets().size() > 0) {
        						return responseModel.getVxnets().get(0);
        					}
        					return null;
        				}
        			}, 
        			CreateVxnetsResponseModel.class);
            String subnetId = requester.execute();
			if (subnetId == null) {
				throw new InternalException("Create subnet failed!");
			}
            
			try {
	            //join router
	            VLAN vlan = getVlan(options.getProviderVlanId());
				if (vlan == null) {
					throw new InternalException("Cannot found vlan with id '" + options.getProviderVlanId() + "'!");
				}
				requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("JoinRouter");
	            requestBuilder.parameter("vxnet", subnetId);
	            requestBuilder.parameter("router", new VlanIdentityGenerator(vlan.getProviderVlanId()).getRouterId());
	            requestBuilder.parameter("ip_network", options.getCidr());
	            requestBuilder.parameter("zone", getProviderDataCenterId());
	            Requester<IpAddressResponseModel> joinRouterRequester = new QingCloudRequester<IpAddressResponseModel, IpAddressResponseModel>(
	                    getProvider(), request, IpAddressResponseModel.class);
	            joinRouterRequester.execute();
			} catch (CloudException e) {
				//join failed, remove created subnet
				this.removeSubnet(subnetId);
			}
			
			VlanIdentityGenerator vlanIdentityGenerator = new VlanIdentityGenerator(options.getProviderVlanId());
			return Subnet.getInstance(getContext().getAccountNumber(), getContext().getRegionId(), options.getProviderVlanId(), 
					new SubnetIdentityGenerator(subnetId, options.getCidr(), vlanIdentityGenerator.getRouterCidr()).toString(), 
					SubnetState.PENDING, options.getName(), options.getName(), options.getCidr());
		} finally {
			APITrace.end();
		}
	}

	@Override
	public VLAN createVlan(String cidr, String name, String description,
			String domainName, String[] dnsServers, String[] ntpServers)
			throws CloudException, InternalException {
		return createVlan(VlanCreateOptions.getInstance(name, null, cidr, null, null, null));
	}

	@Override
	public VLAN createVlan(VlanCreateOptions options) throws InternalException,
			CloudException {
		APITrace.begin(getProvider(), "QingCloudVlan.createVlan");
		try {
			
			if (options.getCidr() == null || options.getName() == null) {
				throw new InternalException("Invalid cidr or name!");
			}
			
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("CreateRouters");
			requestBuilder.parameter("router_name", options.getName());
			requestBuilder.parameter("zone", getProviderDataCenterId());
			Requester<String> requester = new QingCloudRequester<CreateRoutersResponseModel, String>(
                    getProvider(), 
                    requestBuilder.build(), 
                    new QingCloudDriverToCoreMapper<CreateRoutersResponseModel, String>(){
        				@Override
        				protected String doMapFrom(CreateRoutersResponseModel responseModel) {
        					if (responseModel != null && responseModel.getRouters() != null && responseModel.getRouters().size() > 0) {
        						return responseModel.getRouters().get(0);
        					}
        					return null;
        				}
        			}, 
        			CreateRoutersResponseModel.class);
            String routerId = requester.execute();
			if (routerId == null) {
				throw new InternalException("Create vlan failed!");
			}
            
			VLAN vlan = new VLAN();
            vlan.setProviderVlanId(new VlanIdentityGenerator(routerId, options.getCidr()).toString());
            vlan.setCidr(options.getCidr());
            vlan.setCurrentState(VLANState.PENDING);
            vlan.setName(options.getName());
            vlan.setDescription(vlan.getName());
            vlan.setProviderDataCenterId(getProviderDataCenterId());
            vlan.setProviderOwnerId(getContext().getAccountNumber());
            vlan.setProviderRegionId(getContext().getRegionId());
            vlan.setSupportedTraffic(IPVersion.IPV4);
            vlan.setVisibleScope(VisibleScope.ACCOUNT_DATACENTER);
            return vlan;
		} finally {
			APITrace.end();
		}
	}

	@Override
	public VLANCapabilities getCapabilities() throws CloudException,
			InternalException {
		return new QingCloudVlanCapabilities(getProvider());
	}

	@Override
	public Subnet getSubnet(String subnetId) throws CloudException,
			InternalException {
		APITrace.begin(getProvider(), "QingCloudVlan.getSubnet");
		try {
			
			if (subnetId == null) {
				throw new InternalException("Invalid subnet id");
			}
			
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeVxnets");
			requestBuilder.parameter("vxnets.1", subnetId);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			final Map<String, String> describeSubnetResponseMap = new HashMap<String, String>();
			Requester<Void> requester = new QingCloudRequester<DescribeVxnetsResponseModel, Void>(
                    getProvider(), 
                    requestBuilder.build(), 
                    new QingCloudDriverToCoreMapper<DescribeVxnetsResponseModel, Void>(){
        				@Override
        				protected Void doMapFrom(DescribeVxnetsResponseModel responseModel) {
        					if (responseModel != null && responseModel.getVxnetSet() != null && responseModel.getVxnetSet().size() > 0) {
        						DescribeVxnetsResponseItemModel item = responseModel.getVxnetSet().get(0);
        						describeSubnetResponseMap.put("vxnet_name", item.getVxnetName());
        						if (item.getRouter().get("router_id") != null) {
        							describeSubnetResponseMap.put("router_id", item.getRouter().get("router_id").toString());
        						}
        					}
        					return null;
        				}
        			}, 
        			DescribeVxnetsResponseModel.class);
            requester.execute();
            
            SubnetIdentityGenerator subnetIdentityGenerator = new SubnetIdentityGenerator(subnetId);
            return Subnet.getInstance(
            		getContext().getAccountNumber(), 
            		getContext().getRegionId(), 
            		new VlanIdentityGenerator(describeSubnetResponseMap.get("router_id"), subnetIdentityGenerator.getVlanCidr()).toString(), 
            		subnetId, 
            		SubnetState.AVAILABLE, 
            		describeSubnetResponseMap.get("vxnet_name"), 
            		describeSubnetResponseMap.get("vxnet_name"), 
            		subnetIdentityGenerator.getSubnetCidr());
		} finally {
			APITrace.end();
		}
	}

	@Override
	public VLAN getVlan(String vlanId) throws CloudException, InternalException {
		APITrace.begin(getProvider(), "QingCloudVlan.getVlan");
		try {
			
			if (vlanId == null) {
				throw new InternalException("Invalid vlan id!");
			}
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeRouters");
			requestBuilder.parameter("routers.1", new VlanIdentityGenerator(vlanId).getRouterId());
			requestBuilder.parameter("zone", getProviderDataCenterId());
			Requester<String> requester = new QingCloudRequester<DescribeRoutersResponseModel, String>(
                    getProvider(), 
                    requestBuilder.build(), 
                    new QingCloudDriverToCoreMapper<DescribeRoutersResponseModel, String>(){
        				@Override
        				protected String doMapFrom(DescribeRoutersResponseModel responseModel) {
        					if (responseModel != null && responseModel.getRouterSet() != null && responseModel.getRouterSet().size() > 0) {
        						return responseModel.getRouterSet().get(0).getRouterName();
        					}
        					return null;
        				}
        			}, 
        			DescribeRoutersResponseModel.class);
			String vlanName = requester.execute();	
			if (vlanName == null) {
				throw new InternalException("Retrieve vlan failed from cloud!");
			}
			
			VlanIdentityGenerator vlanIdentityGenerator = new VlanIdentityGenerator(vlanId);
			VLAN vlan = new VLAN();
            vlan.setProviderVlanId(vlanId);
            vlan.setCidr(vlanIdentityGenerator.getRouterCidr());
            vlan.setCurrentState(VLANState.PENDING);
            vlan.setName(vlanName);
            vlan.setDescription(vlan.getName());
            vlan.setProviderDataCenterId(getProviderDataCenterId());
            vlan.setProviderOwnerId(getContext().getAccountNumber());
            vlan.setProviderRegionId(getContext().getRegionId());
            vlan.setSupportedTraffic(IPVersion.IPV4);
            vlan.setVisibleScope(VisibleScope.ACCOUNT_DATACENTER);
			return vlan;
		} finally {
			APITrace.end();
		}
	}

	@Override
	public boolean isConnectedViaInternetGateway(String vlanId)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getAttachedInternetGatewayId(String vlanId)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InternetGateway getInternetGatewayById(String gatewayId)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isSubscribed() throws CloudException, InternalException {
		return true; //TODO
	}

	@Override
	public Iterable<InternetGateway> listInternetGateways(String vlanId)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<Networkable> listResources(String vlanId)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<Subnet> listSubnets(String vlanId) throws CloudException,
			InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<ResourceStatus> listVlanStatus() throws CloudException,
			InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<VLAN> listVlans() throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeInternetGateway(String forVlanId) throws CloudException,
			InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeInternetGatewayById(String id) throws CloudException,
			InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeInternetGatewayTags(String internetGatewayId, Tag... tags)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeInternetGatewayTags(String[] internetGatewayIds,
			Tag... tags) throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeNetworkInterface(String nicId) throws CloudException,
			InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeSubnet(String providerSubnetId) throws CloudException,
			InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeSubnetTags(String subnetId, Tag... tags)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeSubnetTags(String[] subnetIds, Tag... tags)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSubnetTags(String subnetId, Tag... tags)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSubnetTags(String[] subnetIds, Tag... tags)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeVlan(String vlanId) throws CloudException,
			InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeVLANTags(String vlanId, Tag... tags)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeVLANTags(String[] vlanIds, Tag... tags)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateSubnetTags(String subnetId, Tag... tags)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateSubnetTags(String[] subnetIds, Tag... tags)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setVLANTags(String vlanId, Tag... tags) throws CloudException,
			InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setVLANTags(String[] vlanIds, Tag... tags)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateVLANTags(String vlanId, Tag... tags)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateVLANTags(String[] vlanIds, Tag... tags)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateInternetGatewayTags(String internetGatewayId, Tag... tags)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateInternetGatewayTags(String[] internetGatewayIds,
			Tag... tags) throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setInternetGatewayTags(String internetGatewayId, Tag... tags)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setInternetGatewayTags(String[] internetGatewayIds, Tag... tags)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getProviderTermForNetworkInterface(Locale locale) {
		try {
			return getCapabilities().getProviderTermForNetworkInterface(locale);
		} catch (CloudException e) {
			throw new RuntimeException("get term for network interface failed!");
		} catch (InternalException e) {
			throw new RuntimeException("get term for network interface failed!");
		}
	}

	@Override
	public String getProviderTermForSubnet(Locale locale) {
		try {
			return getCapabilities().getProviderTermForSubnet(locale);
		} catch (CloudException e) {
			throw new RuntimeException("get term for subnet failed!");
		} catch (InternalException e) {
			throw new RuntimeException("get term for subnet failed!");
		}
	}

	@Override
	public String getProviderTermForVlan(Locale locale) {
		try {
			return getCapabilities().getProviderTermForVlan(locale);
		} catch (CloudException e) {
			throw new RuntimeException("get term for vlan failed!");
		} catch (InternalException e) {
			throw new RuntimeException("get term for vlan failed!");
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
}
