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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.network.AbstractIpAddressSupport;
import org.dasein.cloud.network.AddressType;
import org.dasein.cloud.network.IPAddressCapabilities;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.IpAddress;
import org.dasein.cloud.network.IpAddressSupport;
import org.dasein.cloud.network.IpForwardingRule;
import org.dasein.cloud.network.Protocol;
import org.dasein.cloud.network.RawAddress;
import org.dasein.cloud.qingcloud.QingCloud;
import org.dasein.cloud.qingcloud.model.AddRouterStaticsResponseModel;
import org.dasein.cloud.qingcloud.model.AllocateEipsResponseModel;
import org.dasein.cloud.qingcloud.model.DeleteRouterStaticsResponseModel;
import org.dasein.cloud.qingcloud.model.DescribeEipsResponseModel;
import org.dasein.cloud.qingcloud.model.DescribeEipsResponseModel.DescribeEipsResponseItemModel;
import org.dasein.cloud.qingcloud.model.DescribeRouterStaticsResponseModel.DescribeRouterStaticsResponseItemModel;
import org.dasein.cloud.qingcloud.model.DescribeRouterStaticsResponseModel;
import org.dasein.cloud.qingcloud.model.SimpleJobResponseModel;
import org.dasein.cloud.qingcloud.util.requester.QingCloudDriverToCoreMapper;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequestBuilder;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequester;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.requester.fluent.Requester;
import org.dasein.cloud.qingcloud.model.DescribeRoutersResponseModel;

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
	private static final Integer DefaultResponseDataLimit = 999;
	
	protected QingCloudIpAddress(QingCloud provider) {
		super(provider);
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
            Requester<SimpleJobResponseModel> requester = new QingCloudRequester<SimpleJobResponseModel, SimpleJobResponseModel>(getProvider(), request, SimpleJobResponseModel.class);
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
			requestBuilder.parameter("verbose", 1);
			requestBuilder.parameter("zone", zone);
			HttpUriRequest request = requestBuilder.build();
            Requester<List<IpAddress>> requester = new QingCloudRequester<DescribeEipsResponseModel, List<IpAddress>>(
                    getProvider(), 
                    request, 
                    new IpAddressesMapper(), 
                    DescribeEipsResponseModel.class);
            List<IpAddress> ipAddresses = requester.execute();
            
            if (ipAddresses != null && ipAddresses.size() > 0) {
            	return ipAddresses.get(0);
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
			requestBuilder.parameter("limit", DefaultResponseDataLimit);
			requestBuilder.parameter("zone", zone);
			HttpUriRequest request = requestBuilder.build();
            Requester<List<IpAddress>> requester = new QingCloudRequester<DescribeEipsResponseModel, List<IpAddress>>(
                    getProvider(), 
                    request, 
                    new IpAddressesMapper(), 
                    DescribeEipsResponseModel.class);
            
            if (unassignedOnly) {
	            List<IpAddress> ipAddresses = new ArrayList<IpAddress>();
	            for (IpAddress ipAddress : requester.execute()) {
	            	if (ipAddress.getServerId() == null && 
	            			ipAddress.getProviderLoadBalancerId() == null && 
	            			ipAddress.getProviderVlanId() == null) {
	            		ipAddresses.add(ipAddress);
	            	}
	            }
	            return ipAddresses;
            } else {
            	return requester.execute();
            }
            
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
			requestBuilder.parameter("limit", DefaultResponseDataLimit);
			HttpUriRequest request = requestBuilder.build();
			IpAddressesMapper mapper = new IpAddressesMapper();
			Requester<List<IpAddress>> requester = new QingCloudRequester<DescribeEipsResponseModel, List<IpAddress>>(
                    getProvider(), 
                    request, 
                    mapper, 
                    DescribeEipsResponseModel.class);
			
			List<ResourceStatus> statuses = new ArrayList<ResourceStatus>();
            for(IpAddress ipAddress : requester.execute()) {
            	statuses.add(new ResourceStatus(ipAddress.getProviderIpAddressId(), 
            			mapper.getIpAddressStatusMap().get(ipAddress.getProviderIpAddressId())));
            }
            return statuses;
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
            Requester<SimpleJobResponseModel> requester = new QingCloudRequester<SimpleJobResponseModel, SimpleJobResponseModel>(getProvider(), request, SimpleJobResponseModel.class);
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
		throw new OperationNotSupportedException("Qing cloud doesn't support assign ip address for vlan use!");
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
		APITrace.begin(getProvider(), "QingCloudIpAddress.forward");
		try {
			if (addressId == null || protocol == null || onServerId == null) {
				throw new InternalException("Invalid addresss id or protocol or server id!");
			}
			
			VirtualMachine vm = getProvider().getComputeServices().getVirtualMachineSupport().getVirtualMachine(onServerId);
			RawAddress[] addresses = vm.getPrivateAddresses();
			if (addresses == null || addresses.length == 0) {
				throw new InternalException("get target server ip address failed!");
			}
			String onServerIpAddress = addresses[0].getIpAddress();
			
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeEips");
			requestBuilder.parameter("eips.1", addressId);
			requestBuilder.parameter("verbose", 1);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			HttpUriRequest request = requestBuilder.build();
			IpAddressesMapper mapper = new IpAddressesMapper();
			Requester<List<IpAddress>> requester = new QingCloudRequester<DescribeEipsResponseModel, List<IpAddress>>(
	                getProvider(), 
	                request, 
	                new IpAddressesMapper(), 
	                DescribeEipsResponseModel.class);
			requester.execute();
			
			Map<String, IpAddressesMapper.IpAddressResource> resources = mapper.getIpAddressResourceMap();
			if (resources != null && resources.keySet() != null && resources.keySet().size() > 0) {
				IpAddressesMapper.IpAddressResource resource = resources.get(addressId);
				if (resource != null && resource.getType().equals("router")) {
					String routerId = resource.getId();
					requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("AddRouterStatics");
					requestBuilder.parameter("router", routerId);
					requestBuilder.parameter("statics.1.static_type", 1);
					requestBuilder.parameter("statics.1.val1", publicPort);
					requestBuilder.parameter("statics.1.val2", onServerIpAddress);
					requestBuilder.parameter("statics.1.val3", privatePort);
					requestBuilder.parameter("zone", getProviderDataCenterId());
					Requester<AddRouterStaticsResponseModel> addRouterStaticsRequester = new QingCloudRequester<AddRouterStaticsResponseModel, AddRouterStaticsResponseModel>(
			                getProvider(), requestBuilder.build(), AddRouterStaticsResponseModel.class);
					AddRouterStaticsResponseModel model = addRouterStaticsRequester.execute();
					if (model != null && model.getRouterStatics() != null && model.getRouterStatics().size() > 0) {
						return model.getRouterStatics().get(0);
					}
				}
			}
			return null;
		} finally {
			APITrace.end();
		}
	}

	@Override
	public Iterable<IpForwardingRule> listRules(String addressId)
			throws InternalException, CloudException {
		APITrace.begin(getProvider(), "QingCloudIpAddress.listRules");
		try {
			if (addressId == null) {
				throw new InternalException("Invalid eip address id!");
			}
			
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeEips");
			requestBuilder.parameter("verbose", 1);
			requestBuilder.parameter("eips.1", addressId);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			HttpUriRequest request = requestBuilder.build();
			IpAddressesMapper mapper = new IpAddressesMapper();
			Requester<List<IpAddress>> requester = new QingCloudRequester<DescribeEipsResponseModel, List<IpAddress>>(
	                getProvider(), 
	                request, 
	                new IpAddressesMapper(), 
	                DescribeEipsResponseModel.class);
			List<IpAddress> ipAddresses = requester.execute();
			if (ipAddresses == null || ipAddresses.size() == 0 || 
					mapper.getIpAddressResourceMap() == null || mapper.getIpAddressResourceMap().keySet() == null ||
					mapper.getIpAddressResourceMap().keySet().size() == 0) {
				return Collections.emptyList();
			}
			
			IpAddress ipAddress = ipAddresses.get(0);
			IpAddressesMapper.IpAddressResource resource = mapper.getIpAddressResourceMap().get(addressId);
			if (!resource.getType().equals("router")) {
				throw new InternalException("Eip not associate with a router!");
			}
			
			requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeRouterStatics");
			requestBuilder.parameter("zone", getProviderDataCenterId());
			requestBuilder.parameter("router", resource.getId());
			requestBuilder.parameter("static_type", 1);
			requestBuilder.parameter("limit", DefaultResponseDataLimit);
			Requester<List<IpForwardingRule>> describeRouterStaticsRequester = new QingCloudRequester<DescribeRouterStaticsResponseModel, List<IpForwardingRule>>(
	                getProvider(), 
	                request, 
	                new RouterStaticsIpForwardingMapper(ipAddress), 
	                DescribeRouterStaticsResponseModel.class);
			return describeRouterStaticsRequester.execute();
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void stopForward(String ruleId) throws InternalException,
			CloudException {
		APITrace.begin(getProvider(), "QingCloudIpAddress.stopForward");
		try {
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DeleteRouterStatics");
			requestBuilder.parameter("router_statics.1", ruleId);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			Requester<DeleteRouterStaticsResponseModel> addRouterStaticsRequester = new QingCloudRequester<DeleteRouterStaticsResponseModel, DeleteRouterStaticsResponseModel>(
	                getProvider(), requestBuilder.build(), DeleteRouterStaticsResponseModel.class);
			DeleteRouterStaticsResponseModel model = addRouterStaticsRequester.execute();
			if (model == null || 
					model.getRouterStatics() == null || model.getRouterStatics().size() == 0 ||
					!model.getRouterStatics().get(0).equals("ruleId")) {
				throw new InternalException("Stop forward for rule id " + ruleId + " failed!");
			}
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void stopForwardToServer(String ruleId, String serverId)
			throws InternalException, CloudException {
		stopForward(ruleId);
	}

	private String getProviderDataCenterId() throws InternalException, CloudException {
		String regionId = getContext().getRegionId();
        if (regionId == null) {
            throw new InternalException("No region was set for this request");
        }

        Iterable<DataCenter> dataCenters = getProvider().getDataCenterServices().listDataCenters(regionId);
        return dataCenters.iterator().next().getProviderDataCenterId();//each account has one DC in each region
	}
	
	private IpAddress getIpAddressByAddress(String address) throws InternalException, CloudException {
		for(IpAddress ipAddress : listIpPool(IPVersion.IPV4, false)) {
			if (ipAddress.getRawAddress().getIpAddress().equals(address)) {
				return ipAddress;
			}
		}
		return null;
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
            Requester<SimpleJobResponseModel> requester = new QingCloudRequester<SimpleJobResponseModel, SimpleJobResponseModel>(getProvider(), request, SimpleJobResponseModel.class);
            requester.execute();
		}
	}
	
	private class RouterStaticsIpForwardingMapper extends QingCloudDriverToCoreMapper<DescribeRouterStaticsResponseModel, List<IpForwardingRule>> {
		
		private IpAddress publicIpAddress;
		
		public RouterStaticsIpForwardingMapper(IpAddress publicIpAddress) {
			this.publicIpAddress = publicIpAddress;
		}
		
		@Override
		protected List<IpForwardingRule> doMapFrom(
				DescribeRouterStaticsResponseModel responseModel) {
			try {
				List<IpForwardingRule> rules = new ArrayList<IpForwardingRule>();
				if (responseModel != null && responseModel.getRouterStaticSet() != null && responseModel.getRouterStaticSet().size() > 0) {
					for (DescribeRouterStaticsResponseItemModel item : responseModel.getRouterStaticSet()) {
						IpAddress ipAddress = getIpAddressByAddress(item.getVal2());
						if (ipAddress == null) {
							throw new RuntimeException("failed address id for " + item.getVal2() + " failed!");
						}
						IpForwardingRule rule = new IpForwardingRule();
						rule.setAddressId(publicIpAddress.getProviderIpAddressId());
						rule.setProtocol(Protocol.valueOf(item.getVal4().toUpperCase()));
						rule.setPrivatePort(Integer.valueOf(item.getVal3()));
						rule.setPublicPort(Integer.valueOf(item.getVal1()));
						rule.setProviderRuleId(item.getRouterStaticId());
						rule.setServerId(ipAddress.getServerId());
						rules.add(rule);
					}
				}
				return rules;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private class IpAddressesMapper extends QingCloudDriverToCoreMapper<DescribeEipsResponseModel, List<IpAddress>> {
		
		public class IpAddressResource {
			private String name;
			private String type;
			private String id;
			public IpAddressResource(String id, String type, String name) {
				this.id = id;
				this.type = type;
				this.name = name;
			}
			public String getName() {
				return name;
			}
			public String getType() {
				return type;
			}
			public String getId() {
				return id;
			}
		}
		
		private Map<String, String> ipAddressStatusMap;
		private Map<String, IpAddressResource> ipAddressResourceMap;
		
		public Map<String, String> getIpAddressStatusMap() {
			return ipAddressStatusMap;
		}

		public Map<String, IpAddressResource> getIpAddressResourceMap() {
			return ipAddressResourceMap;
		}

		@Override
		protected List<IpAddress> doMapFrom(
				DescribeEipsResponseModel responseModel) {
			try {
				List<IpAddress> ipAddresses = new ArrayList<IpAddress>();
				if (responseModel != null && responseModel.getEipSet() != null) {
					for (DescribeEipsResponseItemModel item : responseModel.getEipSet()) {
						IpAddress ipAddress = new IpAddress();
						ipAddress.setAddress(item.getEipAddress());
						ipAddress.setAddressType(AddressType.PUBLIC);
						ipAddress.setIpAddressId(item.getEipId());
						ipAddress.setRegionId(getContext().getRegionId());
						ipAddress.setVersion(IPVersion.IPV4);
						if (item.getResource() != null) {
							if (item.getResource().get("resource_type").equals("instance")) {
								ipAddress.setServerId(item.getResource().get("resource_id").toString());
							} else if (item.getResource().get("resource_type").equals("loadbalancer")) {
								ipAddress.setProviderLoadBalancerId(item.getResource().get("resource_id").toString());
							} else if (item.getResource().get("resource_type").equals("router")) {
								String routerCidr = getVlanCidrByRouterId(item.getResource().get("resource_id").toString());
								ipAddress.setProviderVlanId(new QingCloudVlan.IdentityGenerator(
										item.getResource().get("resource_id").toString(), 
										routerCidr).toString());
							}
							if (ipAddressResourceMap == null) {
								ipAddressResourceMap = new HashMap<String, IpAddressResource>();
							}
							ipAddressResourceMap.put(ipAddress.getProviderIpAddressId(), 
									new IpAddressResource(item.getResource().get("resource_id").toString(), 
											item.getResource().get("resource_type").toString(),
											item.getResource().get("resource_name").toString()));
						}
						
						if (item.getStatus() != null) {
							if (ipAddressStatusMap == null) {
								ipAddressStatusMap = new HashMap<String, String>();
							}
							ipAddressStatusMap.put(ipAddress.getProviderIpAddressId(), item.getStatus());
						}
						
						ipAddresses.add(ipAddress);
					}
				}
				return ipAddresses;
			} catch (Exception e) {
				stdLogger.error("map from response to ip address list failed!", e);
				throw new RuntimeException(e);
			}
		}
	}

}
