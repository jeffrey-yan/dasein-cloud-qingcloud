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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.Tag;
import org.dasein.cloud.VisibleScope;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.compute.Volume;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.network.AbstractVLANSupport;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.NetworkServices;
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
import org.dasein.cloud.qingcloud.model.DeleteVxnetsResponseModel;
import org.dasein.cloud.qingcloud.model.DescribeRouterVxnetsResponseModel;
import org.dasein.cloud.qingcloud.model.DescribeRoutersResponseModel;
import org.dasein.cloud.qingcloud.model.DescribeRoutersResponseModel.DescribeRoutersResponseItemModel;
import org.dasein.cloud.qingcloud.model.DescribeVxnetsResponseModel;
import org.dasein.cloud.qingcloud.model.DescribeVxnetsResponseModel.DescribeVxnetsResponseItemModel;
import org.dasein.cloud.qingcloud.model.ResponseModel;
import org.dasein.cloud.qingcloud.model.SimpleJobResponseModel;
import org.dasein.cloud.qingcloud.network.QingCloudTags.DescribeTag;
import org.dasein.cloud.qingcloud.network.QingCloudTags.TagResourceType;
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

	private static final Logger stdLogger = QingCloud.getStdLogger(QingCloudVlan.class);
	
	public static class IdentityGenerator {
		
		private String id;
		private String cidr;
		
		public IdentityGenerator(String providerId) {
			if (providerId != null && providerId.split(":").length == 2) {
				this.id = providerId.split(":")[0];
				this.cidr = providerId.split(":")[1];
			}
		}
		
		public IdentityGenerator(String id, String cidr) {
			this.id = id;
			this.cidr = cidr;
		}
		
		public String getId() {
			return id;
		}
		
		public String getCidr() {
			return cidr;
		}
		
		public String toString() {
			return id + ":" + cidr;
		}
	}
	
	protected QingCloudTags qingCloudTags;
	
	protected QingCloudVlan(QingCloud provider) {
		super(provider);
		this.qingCloudTags = new QingCloudTags(provider);
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
				//modify description
				requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("ModifyVxnetAttributes");
				requestBuilder.parameter("vxnet", subnetId);
				requestBuilder.parameter("description", options.getCidr());
				requestBuilder.parameter("zone", getProviderDataCenterId());
				Requester<ResponseModel> modifySubnetRequester = new QingCloudRequester<ResponseModel, ResponseModel>(getProvider(), request, ResponseModel.class);
	            modifySubnetRequester.execute();
				
	            //join router
	            VLAN vlan = getVlan(options.getProviderVlanId());
				if (vlan == null) {
					throw new InternalException("Cannot found vlan with id '" + options.getProviderVlanId() + "'!");
				}
				requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("JoinRouter");
	            requestBuilder.parameter("vxnet", subnetId);
	            requestBuilder.parameter("router", new IdentityGenerator(vlan.getProviderVlanId()).getId());
	            requestBuilder.parameter("ip_network", options.getCidr());
	            requestBuilder.parameter("zone", getProviderDataCenterId());
	            Requester<SimpleJobResponseModel> joinRouterRequester = new QingCloudRequester<SimpleJobResponseModel, SimpleJobResponseModel>(getProvider(), request, SimpleJobResponseModel.class);
	            joinRouterRequester.execute();
			} catch (CloudException e) {
				//join failed, remove created subnet
				stdLogger.error("create vlan failed for modify/join router failed!", e);
				removeSubnet(new IdentityGenerator(
						subnetId, 
						options.getCidr()).toString()); 
			}
			
			return getSubnet(
					new IdentityGenerator(
							subnetId, 
							options.getCidr()).toString()); 
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
			requestBuilder.parameter("verbose", 1);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			Requester<List<Subnet>> subnetRequester = new QingCloudRequester<DescribeVxnetsResponseModel, List<Subnet>>(
                    getProvider(), 
                    requestBuilder.build(), 
                    new SubnetsMapper(), 
        			DescribeVxnetsResponseModel.class);
			List<Subnet> subnets = subnetRequester.execute();
            
			if (subnets != null && subnets.size() > 0) {
            	return subnets.get(0);
            } else {
            	return null;
            }
		} finally {
			APITrace.end();
		}
	}
	
	

	@Override
	public boolean isSubscribed() throws CloudException, InternalException {
		return true; 
	}

	@Override
	public Iterable<Networkable> listResources(String vlanId)
			throws CloudException, InternalException {
		
		List<Networkable> vlanResources = new ArrayList<Networkable>();
		NetworkServices networkServices = getProvider().getNetworkServices();
		ComputeServices computeServices = getProvider().getComputeServices();
		for(Subnet subnet : listSubnets(vlanId)) {
			vlanResources.add(subnet);
			VirtualMachineSupport virtualMachineSupport = computeServices.getVirtualMachineSupport();
			if (virtualMachineSupport != null) {
				for (VirtualMachine virtualMachine : virtualMachineSupport.listVirtualMachines()) {
					if (virtualMachine.getProviderSubnetId().equals(subnet.getProviderSubnetId())) {
						vlanResources.add(virtualMachine);
						if (virtualMachine.getProviderAssignedIpAddressId() != null) {
							vlanResources.add(
									networkServices.getIpAddressSupport().getIpAddress(
											virtualMachine.getProviderAssignedIpAddressId()));
						}
					}
					if (virtualMachine.getVolumes() != null && virtualMachine.getVolumes().length > 0) {
						for (Volume volume : virtualMachine.getVolumes()) {
							vlanResources.add(volume);
						}
					}
				}
			}
		}
		
		//TODO Firewall, LoadBalancer, LoadBalancerHealthCheck
		
		return vlanResources; 
	}
	
	@Override
	public Iterable<Subnet> listSubnets(String vlanId) throws CloudException,
			InternalException {
		APITrace.begin(getProvider(), "QingCloudVlan.listSubnets");
		try {
			
			if (vlanId == null) {
				throw new InternalException("Invalid vlan id!");
			}
			
			final IdentityGenerator vlanIdentityGenerator = new IdentityGenerator(vlanId);
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeRouterVxnets");
			requestBuilder.parameter("zone", getProviderDataCenterId());
			requestBuilder.parameter("verbose", 1);
			requestBuilder.parameter("router", vlanIdentityGenerator.getId());
			Requester<List<Subnet>> describeRouterVxnetsRequester = new QingCloudRequester<DescribeRouterVxnetsResponseModel, List<Subnet>>(
                    getProvider(), 
                    requestBuilder.build(), 
                    new RouterSubnetsMapper(vlanIdentityGenerator.toString()), 
        			DescribeRouterVxnetsResponseModel.class);
			return describeRouterVxnetsRequester.execute();
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
			
			try {
				//modify router/vlan attributes
				requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("ModifyRouterAttributes");
				requestBuilder.parameter("router", routerId);
				requestBuilder.parameter("description", options.getCidr());
				requestBuilder.parameter("zone", getProviderDataCenterId());
				Requester<ResponseModel> modifyRouterAttributesRequester = new QingCloudRequester<ResponseModel, ResponseModel>(
	                    getProvider(), requestBuilder.build(), ResponseModel.class);
				modifyRouterAttributesRequester.execute();
			} catch (Exception e) {
				//remove vlan
				stdLogger.error("create vlan failed for modify vlan attributes failed!", e);
				this.removeVlan(new IdentityGenerator(routerId, options.getCidr()).toString());
			}
			return getVlan(new IdentityGenerator(routerId, options.getCidr()).toString());
		} finally {
			APITrace.end();
		}
	}

	@Override
	public Iterable<ResourceStatus> listVlanStatus() throws CloudException,
			InternalException {
		APITrace.begin(getProvider(), "QingCloudVlan.listVlanStatus");
		try {
			List<ResourceStatus> statuses = new ArrayList<ResourceStatus>();
			for (VLAN vlan : listVlans()) {
				statuses.add(new ResourceStatus(vlan.getProviderVlanId(), vlan.getCurrentState()));
			}
			return statuses;
		} finally {
			APITrace.end();
		}
	}

	
	
	@Override
	public VLAN getVlan(final String vlanId) throws CloudException, InternalException {
		APITrace.begin(getProvider(), "QingCloudVlan.getVlan");
		try {
			
			if (vlanId == null) {
				throw new InternalException("Invalid vlan id!");
			}
			
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeRouters");
			requestBuilder.parameter("routers.1", new IdentityGenerator(vlanId).getId());
			requestBuilder.parameter("zone", getProviderDataCenterId());
			
			Requester<List<VLAN>> requester = new QingCloudRequester<DescribeRoutersResponseModel, List<VLAN>>(
                    getProvider(), 
                    requestBuilder.build(), 
                    new VlansMapper(), 
        			DescribeRoutersResponseModel.class);
			List<VLAN> vlans = requester.execute();
			
			if (vlans != null && vlans.size() > 0) {
				return vlans.get(0);
			} else {
				return null;
			}
		} finally {
			APITrace.end();
		}
	}
	
	@Override
	public Iterable<VLAN> listVlans() throws CloudException, InternalException {
		APITrace.begin(getProvider(), "QingCloudVlan.listVlans");
		try {
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeRouters");
			requestBuilder.parameter("zone", getProviderDataCenterId());
			Requester<List<VLAN>> requester = new QingCloudRequester<DescribeRoutersResponseModel, List<VLAN>>(
                    getProvider(), 
                    requestBuilder.build(), 
                    new VlansMapper(), 
        			DescribeRoutersResponseModel.class);
			return requester.execute();
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void removeSubnet(String providerSubnetId) throws CloudException,
			InternalException {
		APITrace.begin(getProvider(), "QingCloudVlan.removeSubnet");
		try {
			
			if (providerSubnetId == null) {
				throw new InternalException("Invalid subnet id!");
			}
			
			String subnetId = new IdentityGenerator(providerSubnetId).getId();
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeVxnets");
			requestBuilder.parameter("vxnets.1", subnetId);
			requestBuilder.parameter("verbose", 1);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			SubnetsMapper mapper = new SubnetsMapper();
			Requester<List<Subnet>> requester = new QingCloudRequester<DescribeVxnetsResponseModel, List<Subnet>>(
                    getProvider(), 
                    requestBuilder.build(), 
                    mapper, 
        			DescribeVxnetsResponseModel.class);
			requester.execute();
			List<String> instanceIds = mapper.getAssociatedInstances().get(subnetId);
			
			//disassociated instances from subnet
			leaveSubnet(instanceIds, subnetId);
			
			requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DeleteVxnets");
			requestBuilder.parameter("vxnets.1", new IdentityGenerator(providerSubnetId).getId());
			requestBuilder.parameter("zone", getProviderDataCenterId());
			Requester<String> deleteVxnetsRequester = new QingCloudRequester<DeleteVxnetsResponseModel, String>(
                    getProvider(), 
                    requestBuilder.build(), 
                    new QingCloudDriverToCoreMapper<DeleteVxnetsResponseModel, String>(){
        				@Override
        				protected String doMapFrom(DeleteVxnetsResponseModel responseModel) {
        					if (responseModel != null && responseModel.getVxnets() != null && responseModel.getVxnets().size() > 0) {
        						return responseModel.getVxnets().get(0);
        					}
        					return null;
        				}
        			}, 
        			DeleteVxnetsResponseModel.class);
			String successfulDeleteSubnetId = deleteVxnetsRequester.execute();
			
			if (successfulDeleteSubnetId == null || !successfulDeleteSubnetId.equals(new IdentityGenerator(providerSubnetId).getId())) { 
				//delete failed, join back to subnet
				joinSubnet(instanceIds, subnetId);
			}
		} finally {
			APITrace.end();
		}
	}
	
	@Override
	public void removeVlan(String vlanId) throws CloudException,
			InternalException {
		
		if (vlanId == null) {
			throw new InternalException("Invalid vlan id!");
		}
		
		QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DeleteRouters");
		requestBuilder.parameter("routers.1", new IdentityGenerator(vlanId).getId());
		requestBuilder.parameter("zone", getProviderDataCenterId());
		Requester<SimpleJobResponseModel> requester = new QingCloudRequester<SimpleJobResponseModel, SimpleJobResponseModel>(
                getProvider(), 
                requestBuilder.build(), 
                SimpleJobResponseModel.class);
		requester.execute();
	} 
	
	@Override
	public void removeSubnetTags(String subnetId, Tag... tags)
			throws CloudException, InternalException {
		removeSubnetTags((String[]) Arrays.asList(subnetId).toArray(), tags);
	}

	@Override
	public void removeSubnetTags(String[] subnetIds, Tag... tags)
			throws CloudException, InternalException {
		APITrace.begin(getProvider(), "QingCloudIpAddress.removeSubnetTags");
		try {
			qingCloudTags.removeResourcesTags(TagResourceType.VXNET, subnetIds, tags);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void setSubnetTags(String subnetId, Tag... tags)
			throws CloudException, InternalException {
		setSubnetTags((String[]) Arrays.asList(subnetId).toArray(), tags);
	}
	
	@Override
	public void setSubnetTags(String[] subnetIds, Tag... tags)
			throws CloudException, InternalException {
		APITrace.begin(getProvider(), "QingCloudIpAddress.setSubnetTags");
		try {
			qingCloudTags.setResourcesTags(TagResourceType.VXNET, subnetIds, tags);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void updateSubnetTags(String subnetId, Tag... tags)
			throws CloudException, InternalException {
		updateSubnetTags((String[]) Arrays.asList(subnetId).toArray(), tags);
	}

	@Override
	public void updateSubnetTags(String[] subnetIds, Tag... tags)
			throws CloudException, InternalException {
		APITrace.begin(getProvider(), "QingCloudIpAddress.updateSubnetTags");
		try {
			qingCloudTags.updateResourcesTags(TagResourceType.VXNET, subnetIds, tags);
		} finally {
			APITrace.end();
		}
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
	
	private void joinSubnet(List<String> instanceIds, String subnetId) throws InternalException, CloudException {
		if (instanceIds != null && instanceIds.size() > 0) {
			//delete failed, join back vxnet
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("JoinVxnet");
			requestBuilder.parameter("zone", getProviderDataCenterId());
			requestBuilder.parameter("vxnet", subnetId);
			for (int i = 0; i < instanceIds.size(); i++) {
				requestBuilder.parameter("instances." + (i + 1), instanceIds.get(i));
			}
			Requester<SimpleJobResponseModel> joinSubnetRequester = new QingCloudRequester<SimpleJobResponseModel, SimpleJobResponseModel>(
                    getProvider(), 
                    requestBuilder.build(), 
                    SimpleJobResponseModel.class);
			joinSubnetRequester.execute();
		}
	}
	
	private void leaveSubnet(List<String> instanceIds, String subnetId) throws InternalException, CloudException {
		//disassociated instances from subnet
		if (instanceIds != null && instanceIds.size() > 0) {
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("LeaveVxnet");
			requestBuilder.parameter("zone", getProviderDataCenterId());
			requestBuilder.parameter("vxnet", subnetId);
			for (int i = 0; i < instanceIds.size(); i++) {
				requestBuilder.parameter("instances." + (i + 1), instanceIds.get(i));
			}
			Requester<SimpleJobResponseModel> leaveSubnetRequester = new QingCloudRequester<SimpleJobResponseModel, SimpleJobResponseModel>(
                    getProvider(), 
                    requestBuilder.build(), 
                    SimpleJobResponseModel.class);
			leaveSubnetRequester.execute();
		}
	}
	
	private class SubnetsMapper extends QingCloudDriverToCoreMapper<DescribeVxnetsResponseModel, List<Subnet>> {

		private Map<String, List<String>> associatedInstances;
		
		public Map<String, List<String>> getAssociatedInstances() {
			return this.associatedInstances;
		}
		
		@Override
		protected List<Subnet> doMapFrom(
				DescribeVxnetsResponseModel responseModel) {
			try {
				
				List<Subnet> subnets = new ArrayList<Subnet>();
				
				if (responseModel != null && responseModel.getVxnetSet() != null && responseModel.getVxnetSet().size() > 0) {
					for (DescribeVxnetsResponseItemModel vxnet : responseModel.getVxnetSet()) {
						
						String routerId = vxnet.getRouter().get("router_id").toString();
						Subnet subnet = Subnet.getInstance(
		            		getContext().getAccountNumber(), 
		            		getContext().getRegionId(), 
		            		new IdentityGenerator(routerId, getRouterCidrById(routerId)).toString(), 
		            		new IdentityGenerator(vxnet.getVxnetId(), vxnet.getDescription()).toString(), 
		            		SubnetState.AVAILABLE, 
		            		vxnet.getVxnetName(), 
		            		vxnet.getDescription(), 
		            		vxnet.getDescription());
						
						if (associatedInstances == null) {
							associatedInstances = new HashMap<String, List<String>>();
						}
						List<String> instances = null;
						for (String instanceId : vxnet.getInstanceIds()) {
							if (instances == null) {
								instances = new ArrayList<String>();
							}
							instances.add(instanceId);
						}
						if (instances != null) {
							associatedInstances.put(vxnet.getVxnetId(), instances);
						}
						
						List<DescribeTag> subnetTags = qingCloudTags.describeResourceTags(vxnet.getVxnetId());
						for (DescribeTag tag : subnetTags) {
							subnet.setTag(tag.getTagName(), tag.getTagDescription());
						}
						subnets.add(subnet);
					}
				}
				return subnets;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		private String getRouterCidrById(String routerId) throws InternalException, CloudException {
			
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeRouters");
			requestBuilder.parameter("routers.1", routerId);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			
			Requester<String> vlanRequester = new QingCloudRequester<DescribeRoutersResponseModel, String>(
	             getProvider(), 
	             requestBuilder.build(), 
	             new QingCloudDriverToCoreMapper<DescribeRoutersResponseModel, String>(){
	 				@Override
	 				protected String doMapFrom(DescribeRoutersResponseModel responseModel) {
	 					if (responseModel != null && responseModel.getRouterSet() != null && responseModel.getRouterSet().size() > 0) {
	 						return responseModel.getRouterSet().get(0).getDescription();
	 					} else {
	 						throw new RuntimeException("find router by id failed");
	 					}
	 				}
	 			}, 
	 			DescribeRoutersResponseModel.class);
			return vlanRequester.execute();	
		} 
		
	}
	
	private class RouterSubnetsMapper extends QingCloudDriverToCoreMapper<DescribeRouterVxnetsResponseModel, List<Subnet>> {
		
		private String vlanId;
		
		public RouterSubnetsMapper(String vlanId) {
			this.vlanId = vlanId;
		}
		
		@Override
		protected List<Subnet> doMapFrom(
				DescribeRouterVxnetsResponseModel responseModel) {
			try {
				List<Subnet> subnets = new ArrayList<Subnet>();
				if (responseModel != null && responseModel.getRouterVxnetSet() != null && responseModel.getRouterVxnetSet().size() > 0) {
					for (DescribeRouterVxnetsResponseModel.DescribeRouterVxnetsResponseItemModel routerVxnet : responseModel.getRouterVxnetSet()) {
						IdentityGenerator subnetIdentityGenerator = new IdentityGenerator(routerVxnet.getVxnetId(), routerVxnet.getIpNetwork());
						Subnet subnet = Subnet.getInstance(
								getContext().getAccountNumber(), 
								getContext().getRegionId(), 
								vlanId, 
								subnetIdentityGenerator.toString(), 
								SubnetState.AVAILABLE, 
								routerVxnet.getVxnetName(), 
								routerVxnet.getVxnetName(), 
								routerVxnet.getIpNetwork());
						List<DescribeTag> subnetTags;
						try {
							subnetTags = qingCloudTags.describeResourceTags(new IdentityGenerator(subnet.getProviderSubnetId()).getId());
							for (DescribeTag tag : subnetTags) {
								subnet.setTag(tag.getTagName(), tag.getTagDescription());
							}
							subnets.add(subnet);
						} catch (InternalException e) {
							stdLogger.error("retrieve tags for subnet " + subnetIdentityGenerator.getId() + " failed!", e);
							throw new RuntimeException(e);
						} catch (CloudException e) {
							stdLogger.error("retrieve tags for subnet " + subnetIdentityGenerator.getId() + " failed!", e);
							throw new RuntimeException(e);
						}
					}
				}
				return subnets;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private class VlansMapper extends QingCloudDriverToCoreMapper<DescribeRoutersResponseModel, List<VLAN>> {

		@Override
		protected List<VLAN> doMapFrom(
				DescribeRoutersResponseModel responseModel) {
			try {
				List<VLAN> vlans = new ArrayList<VLAN>();
				if (responseModel != null && responseModel.getRouterSet() != null) {
					for (DescribeRoutersResponseItemModel item : responseModel.getRouterSet()) {
						VLAN vlan = new VLAN();
				        vlan.setProviderVlanId(new IdentityGenerator(item.getRouterId(), item.getDescription()).toString());
				        vlan.setCidr(item.getDescription());
				        vlan.setCurrentState(mapVLANState(item.getStatus()));
				        vlan.setName(item.getRouterName());
				        vlan.setDescription(item.getDescription());
				        vlan.setProviderDataCenterId(getProviderDataCenterId());
				        vlan.setProviderOwnerId(getContext().getAccountNumber());
				        vlan.setProviderRegionId(getContext().getRegionId());
				        vlan.setSupportedTraffic(IPVersion.IPV4);
				        vlan.setVisibleScope(VisibleScope.ACCOUNT_DATACENTER);
				        vlans.add(vlan);
					}
				}
				return vlans;
			} catch (Exception e) {
				stdLogger.error("map from response to vlan list failed!", e);
				throw new RuntimeException(e);
			}
		}
		
		private VLANState mapVLANState(String state) {
			if (state.equals("active")) {
				return VLANState.AVAILABLE;
			} else {
				return VLANState.PENDING;
			}
		}
		
	}
}
