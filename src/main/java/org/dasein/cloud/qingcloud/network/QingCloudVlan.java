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
import java.util.Collections;
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
import org.dasein.cloud.qingcloud.model.DeleteRoutersResponseModel;
import org.dasein.cloud.qingcloud.model.DeleteVxnetsResponseModel;
import org.dasein.cloud.qingcloud.model.DescribeRouterVxnetsResponseModel;
import org.dasein.cloud.qingcloud.model.DescribeRoutersResponseItemModel;
import org.dasein.cloud.qingcloud.model.DescribeRoutersResponseModel;
import org.dasein.cloud.qingcloud.model.DescribeVxnetsResponseModel;
import org.dasein.cloud.qingcloud.model.JoinRouterResponseModel;
import org.dasein.cloud.qingcloud.model.JoinVxnetResponseModel;
import org.dasein.cloud.qingcloud.model.LeaveVxnetResponseModel;
import org.dasein.cloud.qingcloud.model.ModifyRouterAttributesResponseModel;
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
	            Requester<JoinRouterResponseModel> joinRouterRequester = new QingCloudRequester<JoinRouterResponseModel, JoinRouterResponseModel>(getProvider(), request, JoinRouterResponseModel.class);
	            joinRouterRequester.execute();
			} catch (CloudException e) {
				//join failed, remove created subnet
				this.removeSubnet(new IdentityGenerator(
						subnetId, 
						options.getCidr()).toString()); 
			}
			
			return this.getSubnet(
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
			final Map<String, String> describeSubnetResponseMap = new HashMap<String, String>();
			Requester<Void> subnetRequester = new QingCloudRequester<DescribeVxnetsResponseModel, Void>(
                    getProvider(), 
                    requestBuilder.build(), 
                    new QingCloudDriverToCoreMapper<DescribeVxnetsResponseModel, Void>(){
        				@Override
        				protected Void doMapFrom(DescribeVxnetsResponseModel responseModel) {
        					if (responseModel != null && responseModel.getVxnetSet() != null && responseModel.getVxnetSet().size() > 0) {
        						DescribeVxnetsResponseModel.DescribeVxnetsResponseItemModel item = responseModel.getVxnetSet().get(0);
        						describeSubnetResponseMap.put("vxnet_name", item.getVxnetName());
        						if (item.getRouter().get("router_id") != null) {
        							describeSubnetResponseMap.put("router_id", item.getRouter().get("router_id").toString());
        						}
        					}
        					return null;
        				}
        			}, 
        			DescribeVxnetsResponseModel.class);
			subnetRequester.execute();
            
            requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeRouters");
			requestBuilder.parameter("routers.1", new IdentityGenerator(describeSubnetResponseMap.get("router_id")).getId());
			requestBuilder.parameter("zone", getProviderDataCenterId());
			
			Requester<String> vlanRequester = new QingCloudRequester<DescribeRoutersResponseModel, String>(
                    getProvider(), 
                    requestBuilder.build(), 
                    new QingCloudDriverToCoreMapper<DescribeRoutersResponseModel, String>(){
        				@Override
        				protected String doMapFrom(DescribeRoutersResponseModel responseModel) {
        					if (responseModel != null && responseModel.getRouterSet() != null && responseModel.getRouterSet().size() > 0) {
        						return responseModel.getRouterSet().get(0).getDescription();
        					}
        					return null;
        				}
        			}, 
        			DescribeRoutersResponseModel.class);
			String routerCidr = vlanRequester.execute();	
            
			IdentityGenerator subnetIdentityGenerator = new IdentityGenerator(subnetId);
            Subnet subnet = Subnet.getInstance(
            		getContext().getAccountNumber(), 
            		getContext().getRegionId(), 
            		new IdentityGenerator(describeSubnetResponseMap.get("router_id"), routerCidr).toString(), 
            		subnetId, 
            		SubnetState.AVAILABLE, 
            		describeSubnetResponseMap.get("vxnet_name"), 
            		describeSubnetResponseMap.get("vxnet_name"), 
            		subnetIdentityGenerator.getCidr());
            
            List<DescribeTag> subnetTags = qingCloudTags.describeResourceTags(new IdentityGenerator(subnetId).getId());
			for (DescribeTag tag : subnetTags) {
				subnet.setTag(tag.getTagName(), tag.getTagDescription());
			}
			
            return subnet;
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
			final String ownerId = getContext().getAccountNumber();
			final String regionId = getContext().getRegionId();
			Requester<List<Subnet>> describeRouterVxnetsRequester = new QingCloudRequester<DescribeRouterVxnetsResponseModel, List<Subnet>>(
                    getProvider(), 
                    requestBuilder.build(), 
                    new QingCloudDriverToCoreMapper<DescribeRouterVxnetsResponseModel, List<Subnet>>(){
        				@Override
        				protected List<Subnet> doMapFrom(DescribeRouterVxnetsResponseModel responseModel) {
        					List<Subnet> subnets = new ArrayList<Subnet>();
        					if (responseModel != null && responseModel.getRouterVxnetSet() != null && responseModel.getRouterVxnetSet().size() > 0) {
        						for (DescribeRouterVxnetsResponseModel.DescribeRouterVxnetsResponseItemModel routerVxnet : responseModel.getRouterVxnetSet()) {
        							IdentityGenerator subnetIdentityGenerator = new IdentityGenerator(routerVxnet.getVxnetId(), routerVxnet.getIpNetwork());
        							Subnet subnet = Subnet.getInstance(
        									ownerId, 
        									regionId, 
        									vlanIdentityGenerator.toString(), 
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
        				}
        			}, 
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
			
			requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("ModifyRouterAttributes");
			requestBuilder.parameter("router", routerId);
			requestBuilder.parameter("description", options.getCidr());
			requestBuilder.parameter("zone", getProviderDataCenterId());
			Requester<ModifyRouterAttributesResponseModel> modifyRouterAttributesRequester = new QingCloudRequester<ModifyRouterAttributesResponseModel, ModifyRouterAttributesResponseModel>(
                    getProvider(), requestBuilder.build(), ModifyRouterAttributesResponseModel.class);
			modifyRouterAttributesRequester.execute();
			
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
			
			Requester<VLAN> requester = new QingCloudRequester<DescribeRoutersResponseModel, VLAN>(
                    getProvider(), 
                    requestBuilder.build(), 
                    new QingCloudDriverToCoreMapper<DescribeRoutersResponseModel, VLAN>(){
        				@Override
        				protected VLAN doMapFrom(DescribeRoutersResponseModel responseModel) {
        					if (responseModel != null && responseModel.getRouterSet() != null && responseModel.getRouterSet().size() > 0) {
        						try {
									return toVlan(responseModel.getRouterSet().get(0));
								} catch (InternalException e) {
									stdLogger.error(e.getMessage());
									throw new RuntimeException(e.getMessage());
								} catch (CloudException e) {
									stdLogger.error(e.getMessage());
									throw new RuntimeException(e.getMessage());
								}
        					}
        					return null;
        				}
        			}, 
        			DescribeRoutersResponseModel.class);
			return requester.execute();	
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
                    new QingCloudDriverToCoreMapper<DescribeRoutersResponseModel, List<VLAN>>(){
        				@Override
        				protected List<VLAN> doMapFrom(DescribeRoutersResponseModel responseModel) {
        					List<VLAN> vlans = new ArrayList<VLAN>();
        					if (responseModel != null && responseModel.getRouterSet() != null && responseModel.getRouterSet().size() > 0) {
        						for (DescribeRoutersResponseItemModel item : responseModel.getRouterSet()) {
	        						try {
										vlans.add(toVlan(item));
									} catch (InternalException e) {
										stdLogger.error(e);
										throw new RuntimeException(e);
									} catch (CloudException e) {
										stdLogger.error(e);
										throw new RuntimeException(e);
									}
        						}
        					}
        					return vlans;
        				}
        			}, 
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
			
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeVxnets");
			requestBuilder.parameter("vxnets.1", new IdentityGenerator(providerSubnetId).getId());
			requestBuilder.parameter("verbose", 1);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			Requester<List<String>> requester = new QingCloudRequester<DescribeVxnetsResponseModel, List<String>>(
                    getProvider(), 
                    requestBuilder.build(), 
                    new QingCloudDriverToCoreMapper<DescribeVxnetsResponseModel, List<String>>(){
        				@Override
        				protected List<String> doMapFrom(DescribeVxnetsResponseModel responseModel) {
        					if (responseModel != null && responseModel.getVxnetSet() != null && responseModel.getVxnetSet().size() > 0) {
        						return responseModel.getVxnetSet().get(0).getInstanceIds();
        					}
        					return Collections.emptyList();
        				}
        			}, 
        			DescribeVxnetsResponseModel.class);
			List<String> instanceIds = requester.execute();
			
			if (instanceIds != null && instanceIds.size() > 0) {
				requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("LeaveVxnet");
				requestBuilder.parameter("zone", getProviderDataCenterId());
				for (int i = 0; i < instanceIds.size(); i++) {
					requestBuilder.parameter("instances." + (i + 1), instanceIds.get(i));
				}
				Requester<LeaveVxnetResponseModel> leaveSubnetRequester = new QingCloudRequester<LeaveVxnetResponseModel, LeaveVxnetResponseModel>(
	                    getProvider(), 
	                    requestBuilder.build(), 
	                    LeaveVxnetResponseModel.class);
				leaveSubnetRequester.execute();
			}
			
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
				//delete failed, join back vxnet
				requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("JoinVxnet");
				requestBuilder.parameter("zone", getProviderDataCenterId());
				requestBuilder.parameter("vxnet", new IdentityGenerator(providerSubnetId).getId());
				for (int i = 0; i < instanceIds.size(); i++) {
					requestBuilder.parameter("instances." + (i + 1), instanceIds.get(i));
				}
				Requester<JoinVxnetResponseModel> joinSubnetRequester = new QingCloudRequester<JoinVxnetResponseModel, JoinVxnetResponseModel>(
	                    getProvider(), 
	                    requestBuilder.build(), 
	                    JoinVxnetResponseModel.class);
				joinSubnetRequester.execute();
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
		Requester<DeleteRoutersResponseModel> requester = new QingCloudRequester<DeleteRoutersResponseModel, DeleteRoutersResponseModel>(
                getProvider(), 
                requestBuilder.build(), 
                DeleteRoutersResponseModel.class);
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
	
	private VLAN toVlan(DescribeRoutersResponseItemModel item) throws InternalException, CloudException {
		VLAN vlan = new VLAN();
        vlan.setProviderVlanId(new IdentityGenerator(item.getRouterId(), item.getDescription()).toString());
        vlan.setCidr(item.getDescription());
        if (item.getStatus().equals("active")) {
        	vlan.setCurrentState(VLANState.AVAILABLE);
        } else {
        	vlan.setCurrentState(VLANState.PENDING);
        }
        vlan.setName(item.getRouterName());
        vlan.setDescription(item.getDescription());
        vlan.setProviderDataCenterId(getProviderDataCenterId());
        vlan.setProviderOwnerId(getContext().getAccountNumber());
        vlan.setProviderRegionId(getContext().getRegionId());
        vlan.setSupportedTraffic(IPVersion.IPV4);
        vlan.setVisibleScope(VisibleScope.ACCOUNT_DATACENTER);
		return vlan;
	}
}
