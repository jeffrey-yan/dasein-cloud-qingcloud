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
import java.util.List;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.Tag;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.network.AbstractIpAddressSupport;
import org.dasein.cloud.network.AddressType;
import org.dasein.cloud.network.IPAddressCapabilities;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.IpAddress;
import org.dasein.cloud.network.IpAddressSupport;
import org.dasein.cloud.network.Protocol;
import org.dasein.cloud.qingcloud.QingCloud;
import org.dasein.cloud.qingcloud.model.AllocateEipsResponseModel;
import org.dasein.cloud.qingcloud.model.AssociateEipResponseModel;
import org.dasein.cloud.qingcloud.model.DescribeEipsResponseModel;
import org.dasein.cloud.qingcloud.model.DissociateEipsResponseModel;
import org.dasein.cloud.qingcloud.model.ModifyRouterAttributesResponseModel;
import org.dasein.cloud.qingcloud.model.ReleaseEipsResponseModel;
import org.dasein.cloud.qingcloud.util.requester.QingCloudDriverToCoreMapper;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequestBuilder;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequester;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.requester.fluent.Requester;
import org.dasein.cloud.qingcloud.model.DescribeRoutersResponseModel;
import org.dasein.cloud.qingcloud.network.QingCloudTags.TagResourceType;

/**
 * Created by Jane Wang on 12/07/2015.
 *
 * @author Jane Wang
 * @since 2016.02.1
 */
public class QingCloudIpAddress extends AbstractIpAddressSupport<QingCloud>
		implements IpAddressSupport {

	private static final Logger stdLogger = QingCloud.getStdLogger(QingCloudVlan.class);

	private static final String DefaultIpAddressBandwidth = "4Mbps";
	
	protected QingCloudTags qingCloudTags;
	
	protected QingCloudIpAddress(QingCloud provider) {
		super(provider);
		this.qingCloudTags = new QingCloudTags(provider);
	}

	@Override
	public void assign(String addressId, String serverId)
			throws InternalException, CloudException {
		APITrace.begin(getProvider(), "QingCloudIpAddress.assign");
		try {
			
			if (addressId == null || serverId == null) {
				throw new InternalException("Invalid address id or server id!");
			}
			
			IpAddress address = getIpAddress(addressId);
			if (address == null) {
				throw new InternalException("No ip address with id '" + addressId + "' found in qingcloud!");
			}
			if (address.getServerId() != null) {
				throw new InternalException("The address has been already assigned to another virtual machine, should release from it before assign!");
			}
			
			VirtualMachine vm = getProvider().getComputeServices().getVirtualMachineSupport().getVirtualMachine(serverId);
			if (vm == null) {
				throw new InternalException("No virtual machine with id '" + serverId + "' found in qingcloud!");
			}
			if (!getCapabilities().canBeAssigned(vm.getCurrentState())) {
				throw new InternalException("Cannot assign address to the virtual machine in the current state!");
			}
			
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("AssociateEip");
			requestBuilder.parameter("eip", addressId);
			requestBuilder.parameter("instance", serverId);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			HttpUriRequest request = requestBuilder.build();
            Requester<AssociateEipResponseModel> requester = new QingCloudRequester<AssociateEipResponseModel, AssociateEipResponseModel>(getProvider(), request, AssociateEipResponseModel.class);
            requester.execute();
		} finally {
			APITrace.end();
		}
	}

	@Override
	public IPAddressCapabilities getCapabilities() throws CloudException,
			InternalException {
		return new QingCloudIpAddressCapabilities(getProvider());
	}

	@Override
	public IpAddress getIpAddress(String addressId) throws InternalException,
			CloudException {
		APITrace.begin(getProvider(), "QingCloudIpAddress.getIpAddress");
		try {
			
			if (addressId == null) {
				throw new InternalException("Invalid address id!");
			}
			
			final String zone = getProviderDataCenterId();
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeEips");
			requestBuilder.parameter("eips.1", addressId);
			requestBuilder.parameter("zone", zone);
			HttpUriRequest request = requestBuilder.build();
            Requester<IpAddress> requester = new QingCloudRequester<DescribeEipsResponseModel, IpAddress>(
                    getProvider(), 
                    request, 
                    new QingCloudDriverToCoreMapper<DescribeEipsResponseModel, IpAddress>(){
        				@Override
        				protected IpAddress doMapFrom(DescribeEipsResponseModel responseModel) {
        					if (responseModel != null && responseModel.getEipSet() != null && responseModel.getEipSet().size() > 0) {
        						IpAddress ipAddress = new IpAddress();
        						DescribeEipsResponseModel.DescribeEipsResponseItemModel eipResponseItem = responseModel.getEipSet().get(0);
        						ipAddress.setAddress(eipResponseItem.getEipAddress());
        						ipAddress.setAddressType(AddressType.PUBLIC);
        						ipAddress.setIpAddressId(eipResponseItem.getEipId());
        						ipAddress.setRegionId(zone);
        						ipAddress.setVersion(IPVersion.IPV4);
        						if (eipResponseItem.getResource() != null && eipResponseItem.getResource().size() > 0) {
        							if (eipResponseItem.getResource().get("resource_type").equals("instance")) {
        								ipAddress.setServerId(eipResponseItem.getResource().get("resource_id").toString());
        							} else if (eipResponseItem.getResource().get("resource_type").equals("loadbalancer")) {
        								ipAddress.setProviderLoadBalancerId(eipResponseItem.getResource().get("resource_id").toString());
        							} else if (eipResponseItem.getResource().get("resource_type").equals("router")) {
        								try {
											String routerCidr = getVlanCidrByRouterId(eipResponseItem.getResource().get("resource_id").toString());
											ipAddress.setProviderVlanId(new QingCloudVlan.IdentityGenerator(
													eipResponseItem.getResource().get("resource_id").toString(), 
													routerCidr).toString());
        								} catch (InternalException e) {
        									stdLogger.error("retrieve router '" + 
        											eipResponseItem.getResource().get("resource_id").toString() + 
        											"' failed", e);
        									throw new RuntimeException("retrieve router '" + 
        											eipResponseItem.getResource().get("resource_id").toString() + 
        											"' cidr failed!", e);
										} catch (CloudException e) {
											stdLogger.error("retrieve router '" + 
        											eipResponseItem.getResource().get("resource_id").toString() + 
        											"' failed", e);
        									throw new RuntimeException("retrieve router '" + 
        											eipResponseItem.getResource().get("resource_id").toString() + 
        											"' cidr failed!", e);
										}
        							}
        						}
        						return ipAddress;
        					}
        					return null;
        				}
        			}, 
                    DescribeEipsResponseModel.class);
            return requester.execute();
		} finally {
			APITrace.end();
		}
	}

	@Override
	public boolean isSubscribed() throws CloudException, InternalException {
		return true;
	}

	@Override
	public Iterable<IpAddress> listIpPool(IPVersion version,
			final boolean unassignedOnly) throws InternalException, CloudException {
		APITrace.begin(getProvider(), "QingCloudIpAddress.listIpPool");
		try {
			
			if (version == null || !version.equals(IPVersion.IPV4)) {
				return Collections.emptyList();
			}
			
			final String zone = getProviderDataCenterId();
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeEips");
			requestBuilder.parameter("verbose", 1);
			requestBuilder.parameter("zone", zone);
			HttpUriRequest request = requestBuilder.build();
            Requester<List<IpAddress>> requester = new QingCloudRequester<DescribeEipsResponseModel, List<IpAddress>>(
                    getProvider(), 
                    request, 
                    new QingCloudDriverToCoreMapper<DescribeEipsResponseModel, List<IpAddress>>(){
        				@Override
        				protected List<IpAddress> doMapFrom(DescribeEipsResponseModel responseModel) {
        					List<IpAddress> ipAddresses = new ArrayList<IpAddress>();
        					if (responseModel != null && responseModel.getEipSet() != null && responseModel.getEipSet().size() > 0) {
        						for (DescribeEipsResponseModel.DescribeEipsResponseItemModel eipResponseItem : responseModel.getEipSet()) {
	        						IpAddress ipAddress = new IpAddress();
	        						ipAddress.setAddress(eipResponseItem.getEipAddress());
	        						ipAddress.setAddressType(AddressType.PUBLIC);
	        						ipAddress.setIpAddressId(eipResponseItem.getEipId());
	        						ipAddress.setRegionId(zone);
	        						ipAddress.setVersion(IPVersion.IPV4);
	        						if (eipResponseItem.getResource() != null && eipResponseItem.getResource().size() > 0) {
	        							if (eipResponseItem.getResource().get("resource_type").equals("instance")) {
	        								ipAddress.setServerId(eipResponseItem.getResource().get("resource_id").toString());
	        							} else if (eipResponseItem.getResource().get("resource_type").equals("loadbalancer")) {
	        								ipAddress.setProviderLoadBalancerId(eipResponseItem.getResource().get("resource_id").toString());
	        							} else if (eipResponseItem.getResource().get("resource_type").equals("router")) {
	        								try {
												String routerCidr = getVlanCidrByRouterId(eipResponseItem.getResource().get("resource_id").toString());
												ipAddress.setProviderVlanId(new QingCloudVlan.IdentityGenerator(
														eipResponseItem.getResource().get("resource_id").toString(), 
														routerCidr).toString());
	        								} catch (InternalException e) {
												throw new RuntimeException("retrieve router cidr failed!");
											} catch (CloudException e) {
												throw new RuntimeException("retrieve router cidr failed!");
											}
	        							}
	        							if (!unassignedOnly) {
	        								ipAddresses.add(ipAddress);
	        							}
	        						} else {
	        							ipAddresses.add(ipAddress);
	        						}
        						}
        					}
        					return ipAddresses;
        				}
        			}, 
                    DescribeEipsResponseModel.class);
            return requester.execute();
		} finally {
			APITrace.end();
		}
	}

	@Override
	public Iterable<ResourceStatus> listIpPoolStatus(IPVersion version)
			throws InternalException, CloudException {
		APITrace.begin(getProvider(), "QingCloudIpAddress.listIpPoolStatus");
		try {
		
			if (version == null || version.equals(IPVersion.IPV6)) {
				return Collections.emptyList();
			}
			
			final String zone = getProviderDataCenterId();
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeEips");
			requestBuilder.parameter("zone", zone);
			HttpUriRequest request = requestBuilder.build();
            Requester<List<ResourceStatus>> requester = new QingCloudRequester<DescribeEipsResponseModel, List<ResourceStatus>>(
                    getProvider(), 
                    request, 
                    new QingCloudDriverToCoreMapper<DescribeEipsResponseModel, List<ResourceStatus>>(){
        				@Override
        				protected List<ResourceStatus> doMapFrom(DescribeEipsResponseModel responseModel) {
        					List<ResourceStatus> statuses = new ArrayList<ResourceStatus>();
        					if (responseModel != null && responseModel.getEipSet() != null && responseModel.getEipSet().size() > 0) {
        						for (DescribeEipsResponseModel.DescribeEipsResponseItemModel eipResponseItem : responseModel.getEipSet()) {
        							statuses.add(new ResourceStatus(eipResponseItem.getEipId(), eipResponseItem.getStatus()));
        						}
        					}
        					return statuses;
        				}
        			}, 
                    DescribeEipsResponseModel.class);
            return requester.execute();
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void releaseFromPool(String addressId) throws InternalException,
			CloudException {
		APITrace.begin(getProvider(), "QingCloudIpAddress.releaseFromPool");
		try {
			if (addressId == null) {
				throw new InternalException("Invalid address id!");
			}
			
			//release from other resources first
			releaseFromAssociatedResources(addressId);
			
			//release from pool
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("ReleaseEips");
			requestBuilder.parameter("eips.1", addressId);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			HttpUriRequest request = requestBuilder.build();
            Requester<ReleaseEipsResponseModel> requester = new QingCloudRequester<ReleaseEipsResponseModel, ReleaseEipsResponseModel>(getProvider(), request, ReleaseEipsResponseModel.class);
            requester.execute();
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void releaseFromServer(String addressId) throws InternalException,
			CloudException {
		APITrace.begin(getProvider(), "QingCloudIpAddress.releaseFromServer");
		try {
			if (addressId == null) {
				throw new InternalException("Invalid address id!");
			}
			releaseFromAssociatedResources(addressId);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public String request(AddressType typeOfAddress) throws InternalException,
			CloudException {
		APITrace.begin(getProvider(), "QingCloudIpAddress.request");
		try {
			if (AddressType.PRIVATE.equals(typeOfAddress)) {
				throw new InternalException("Qing cloud doesn't support private address management!");
			}
			return request(IPVersion.IPV4);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public String request(IPVersion version) throws InternalException,
			CloudException {
		APITrace.begin(getProvider(), "QingCloudIpAddress.request");
		try {
			
			if (IPVersion.IPV6.equals(version)) {
				throw new InternalException("Qing cloud doesn't support this ip version!");
			}
			
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("AllocateEips");
			requestBuilder.parameter("bandwidth", DefaultIpAddressBandwidth);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			HttpUriRequest request = requestBuilder.build();
            Requester<AllocateEipsResponseModel> requester = new QingCloudRequester<AllocateEipsResponseModel, AllocateEipsResponseModel>(
                    getProvider(), request, AllocateEipsResponseModel.class);
            AllocateEipsResponseModel response = requester.execute();
			if (response != null && response.getEips() != null && response.getEips().size() > 0) {
				return response.getEips().get(0);
			}
			return null;
		} finally {
			APITrace.end();
		}
	}

	@Override
	public String requestForVLAN(IPVersion version) throws InternalException,
			CloudException {
		throw new OperationNotSupportedException("Qing cloud doesn't support assign ip address for vlan without vlan id!");
	}

	@Override
	public String requestForVLAN(IPVersion version, String vlanId)
			throws InternalException, CloudException {
		
		APITrace.begin(getProvider(), "QingCloudIpAddress.requestForVLAN");
		try {
		
			if (!getCapabilities().isRequestable(version)) {
				throw new InternalException("ip version " + version.name() + " doesn't requestable!");
			} 
			
			String ipAddressId =  request(version);
			
			QingCloudVlan.IdentityGenerator identityGenerator = new QingCloudVlan.IdentityGenerator(vlanId);
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("ModifyRouterAttributes");
			requestBuilder.parameter("router", identityGenerator.getId());
			requestBuilder.parameter("eip", ipAddressId);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			Requester<ModifyRouterAttributesResponseModel> requester = new QingCloudRequester<ModifyRouterAttributesResponseModel, ModifyRouterAttributesResponseModel>(
	                getProvider(), requestBuilder.build(), ModifyRouterAttributesResponseModel.class);
			requester.execute();
			
			return ipAddressId;
			
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void assignToNetworkInterface(String addressId, String nicId)
			throws InternalException, CloudException {
		throw new OperationNotSupportedException("Qing cloud doesn't support assign ip address for network interface!");
	}

	@Override
	public String forward(String addressId, int publicPort, Protocol protocol,
			int privatePort, String onServerId) throws InternalException,
			CloudException {
		throw new OperationNotSupportedException("Qing cloud doesn't support ip forward!");
	}
	
	@Override
	public void removeTags(String addressId, Tag... tags)
			throws CloudException, InternalException {
		removeTags((String[]) Arrays.asList(addressId).toArray(), tags);
	}

	@Override
	public void removeTags(String[] addressIds, Tag... tags)
			throws CloudException, InternalException {
		APITrace.begin(getProvider(), "QingCloudIpAddress.removeTags");
		try {
			qingCloudTags.removeResourcesTags(TagResourceType.EIP, addressIds, tags);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void updateTags(String addressId, Tag... tags)
			throws CloudException, InternalException {
		updateTags((String[]) Arrays.asList(addressId).toArray(), tags);
	}

	@Override
	public void updateTags(String[] addressIds, Tag... tags)
			throws CloudException, InternalException {
		APITrace.begin(getProvider(), "QingCloudIpAddress.setTags");
		try {
			qingCloudTags.updateResourcesTags(TagResourceType.EIP, addressIds, tags);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void setTags(String addressId, Tag... tags) throws CloudException,
			InternalException {
		setTags((String[]) Arrays.asList(addressId).toArray(), tags);
	}

	@Override
	public void setTags(String[] addressIds, Tag... tags)
			throws CloudException, InternalException {
		APITrace.begin(getProvider(), "QingCloudIpAddress.setTags");
		try {
			qingCloudTags.setResourcesTags(TagResourceType.EIP, addressIds, tags);
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
	
	private String getVlanCidrByRouterId(String routerId) throws InternalException, CloudException {
		QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeRouters");
		requestBuilder.parameter("routers.1", routerId);
		requestBuilder.parameter("zone", getProviderDataCenterId());
		Requester<String> requester = new QingCloudRequester<DescribeRoutersResponseModel, String>(
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
        return requester.execute();
	}
	
	private void releaseFromAssociatedResources(String addressId) throws InternalException, CloudException {
		IpAddress ipAddress = getIpAddress(addressId);
		if (ipAddress == null) {
			throw new InternalException("No ip address with id '" + addressId + "' found in qingcloud!");
		}
		
		if (ipAddress.getServerId() != null || ipAddress.getProviderLoadBalancerId() != null || ipAddress.getProviderVlanId() != null) { 
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DissociateEips");
			requestBuilder.parameter("eips.1", addressId);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			HttpUriRequest request = requestBuilder.build();
            Requester<DissociateEipsResponseModel> requester = new QingCloudRequester<DissociateEipsResponseModel, DissociateEipsResponseModel>(getProvider(), request, DissociateEipsResponseModel.class);
            requester.execute();
		}
	}

}
