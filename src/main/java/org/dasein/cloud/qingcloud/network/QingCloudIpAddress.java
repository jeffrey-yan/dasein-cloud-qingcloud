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
import java.util.List;

import org.apache.http.client.methods.HttpUriRequest;
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
import org.dasein.cloud.network.Protocol;
import org.dasein.cloud.qingcloud.QingCloud;
import org.dasein.cloud.qingcloud.model.AllocateEipsResponseModel;
import org.dasein.cloud.qingcloud.model.IpAddressResponseModel;
import org.dasein.cloud.qingcloud.model.DescribeEipsResponseItemModel;
import org.dasein.cloud.qingcloud.model.DescribeEipsResponseModel;
import org.dasein.cloud.qingcloud.util.requester.QingCloudDriverToCoreMapper;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequestBuilder;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequester;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.requester.fluent.Requester;

/**
 * Created by Jane Wang on 12/07/2015.
 *
 * @author Jane Wang
 * @since 2016.02.1
 */
public class QingCloudIpAddress extends AbstractIpAddressSupport<QingCloud>
		implements IpAddressSupport {

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
            Requester<IpAddressResponseModel> requester = new QingCloudRequester<IpAddressResponseModel, IpAddressResponseModel>(
                    getProvider(), request, IpAddressResponseModel.class);
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
        						DescribeEipsResponseItemModel eipResponseItem = responseModel.getEipSet().get(0);
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
			boolean unassignedOnly) throws InternalException, CloudException {
		APITrace.begin(getProvider(), "QingCloudIpAddress.listIpPool");
		try {
			
			if (version == null || !version.equals(IPVersion.IPV4) || unassignedOnly) {
				return Collections.emptyList();
			}
			
			final String zone = getProviderDataCenterId();
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeEips");
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
        						for (DescribeEipsResponseItemModel eipResponseItem : responseModel.getEipSet()) {
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
	        							} 
	        						}
	        						ipAddresses.add(ipAddress);
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
        						for (DescribeEipsResponseItemModel eipResponseItem : responseModel.getEipSet()) {
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
			
			IpAddress ipAddress = getIpAddress(addressId);
			if (ipAddress == null) {
				throw new InternalException("No ip address with id '" + addressId + "' found in qingcloud!");
			}
			
			if (ipAddress.getServerId() != null || ipAddress.getProviderLoadBalancerId() != null) {
				throw new InternalException("Cannot release a ip address which is associated with an other resource!");
			}
			
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("ReleaseEips");
			requestBuilder.parameter("eips.1", addressId);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			HttpUriRequest request = requestBuilder.build();
            Requester<IpAddressResponseModel> requester = new QingCloudRequester<IpAddressResponseModel, IpAddressResponseModel>(
                    getProvider(), request, IpAddressResponseModel.class);
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
			
			IpAddress ipAddress = getIpAddress(addressId);
			if (ipAddress == null) {
				throw new InternalException("No ip address with id '" + addressId + "' found in qingcloud!");
			}
			
			if (ipAddress.getServerId() != null) { 
				QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DissociateEips");
				requestBuilder.parameter("eips.1", addressId);
				requestBuilder.parameter("zone", getProviderDataCenterId());
				HttpUriRequest request = requestBuilder.build();
	            Requester<IpAddressResponseModel> requester = new QingCloudRequester<IpAddressResponseModel, IpAddressResponseModel>(
	                    getProvider(), request, IpAddressResponseModel.class);
	            requester.execute();
			}
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
			
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("AllocateEips");
			requestBuilder.parameter("bandwidth", QingCloudNetworkCommon.DefaultIpAddressBandwidth);
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
	public String request(IPVersion version) throws InternalException,
			CloudException {
		APITrace.begin(getProvider(), "QingCloudIpAddress.request");
		try {
			
			if (IPVersion.IPV6.equals(version)) {
				throw new InternalException("Qing cloud doesn't support this ip version!");
			}
			
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("AllocateEips");
			requestBuilder.parameter("bandwidth", QingCloudNetworkCommon.DefaultIpAddressBandwidth);
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
		throw new OperationNotSupportedException("Qing cloud doesn't support assign ip address for vlan!");
	}

	@Override
	public String requestForVLAN(IPVersion version, String vlanId)
			throws InternalException, CloudException {
		throw new OperationNotSupportedException("Qing cloud doesn't support assign ip address for vlan!");
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
	
	private String getProviderDataCenterId() throws InternalException, CloudException {
		String regionId = getContext().getRegionId();
        if (regionId == null) {
            throw new InternalException("No region was set for this request");
        }

        Iterable<DataCenter> dataCenters = getProvider().getDataCenterServices().listDataCenters(regionId);
        return dataCenters.iterator().next().getProviderDataCenterId();//each account has one DC in each region
	}

}
