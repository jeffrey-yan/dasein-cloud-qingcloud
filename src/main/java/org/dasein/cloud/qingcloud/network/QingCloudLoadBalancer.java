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
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.network.AbstractLoadBalancerSupport;
import org.dasein.cloud.network.HealthCheckFilterOptions;
import org.dasein.cloud.network.HealthCheckOptions;
import org.dasein.cloud.network.LbAlgorithm;
import org.dasein.cloud.network.LbAttributesOptions;
import org.dasein.cloud.network.LbListener;
import org.dasein.cloud.network.LbPersistence;
import org.dasein.cloud.network.LbProtocol;
import org.dasein.cloud.network.LbType;
import org.dasein.cloud.network.LoadBalancer;
import org.dasein.cloud.network.LoadBalancerAddressType;
import org.dasein.cloud.network.LoadBalancerCapabilities;
import org.dasein.cloud.network.LoadBalancerCreateOptions;
import org.dasein.cloud.network.LoadBalancerHealthCheck;
import org.dasein.cloud.network.LoadBalancerHealthCheck.HCProtocol;
import org.dasein.cloud.network.LoadBalancerState;
import org.dasein.cloud.network.LoadBalancerSupport;
import org.dasein.cloud.network.SSLCertificate;
import org.dasein.cloud.network.SSLCertificateCreateOptions;
import org.dasein.cloud.network.SetLoadBalancerSSLCertificateOptions;
import org.dasein.cloud.qingcloud.QingCloud;
import org.dasein.cloud.qingcloud.model.ResponseModel;
import org.dasein.cloud.qingcloud.model.SimpleJobResponseModel;
import org.dasein.cloud.qingcloud.network.model.AddLoadBalancerBackendsResponseModel;
import org.dasein.cloud.qingcloud.network.model.AddLoadBalancerListenersResponseModel;
import org.dasein.cloud.qingcloud.network.model.CreateLoadBalancerResponseModel;
import org.dasein.cloud.qingcloud.network.model.DeleteLoadBalancerBackendsResponseModel;
import org.dasein.cloud.qingcloud.network.model.DeleteLoadBalancerListenersResponseModel;
import org.dasein.cloud.qingcloud.network.model.DescribeLoadBalancerListenersResponseModel;
import org.dasein.cloud.qingcloud.network.model.DescribeLoadBalancerListenersResponseModel.DescribeLoadBalancerListenersResponseItemModel;
import org.dasein.cloud.qingcloud.network.model.DescribeLoadBalancerListenersResponseModel.DescribeLoadBalancerListenersResponseItemModel.DescribeLoadBalancerListenerBackends;
import org.dasein.cloud.qingcloud.network.model.DescribeLoadBalancersResponseModel;
import org.dasein.cloud.qingcloud.network.model.DescribeLoadBalancersResponseModel.DescribeLoadBalancersResponseItemModel;
import org.dasein.cloud.qingcloud.util.requester.QingCloudDriverToCoreMapper;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequestBuilder;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequester;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.requester.fluent.Requester;

/**
 * Created by Jane Wang on 12/17/2015.
 *
 * @author Jane Wang
 * @since 2016.02.1
 */
public class QingCloudLoadBalancer extends
		AbstractLoadBalancerSupport<QingCloud> implements LoadBalancerSupport {

	private static final Integer DefaultResponseDataLimit = 999;
	private static final String SessionStickyInsert = "insert|3600";
	private static final String SessionStickyRewrite = "prefix|%s";
	private static final String HealthCheckMethodHttp = "http|%s|%s";
	private static final String HealthCheckOption = "%d|%d|%d|%d";
	private static final Integer DefaultBackendServerWeight = 5;
	
	protected QingCloudLoadBalancer(QingCloud provider) {
		super(provider);
	}

	@Override
	public void addListeners(String toLoadBalancerId, LbListener[] listeners)
			throws CloudException, InternalException {
		APITrace.begin(getProvider(), "QingCloudLoadBalancer.addListeners");
		try {
			
			if (toLoadBalancerId == null) {
				throw new InternalException("Invalid load balancer id or listeners!");
			}
			
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("AddLoadBalancerListeners");
			requestBuilder.parameter("loadbalancer", toLoadBalancerId);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			for (int i = 0; i < listeners.length; i++) {
				
				LbListener listener = listeners[i];
				requestBuilder.parameter("listeners." + (i + 1) + ".listener_port", listener.getPublicPort());
				if (listener.getNetworkProtocol() != null) {
					if (listener.getNetworkProtocol().equals(LbProtocol.RAW_TCP)) {
						requestBuilder.parameter("listeners." + (i + 1) + ".listener_protocol", "tcp");
						requestBuilder.parameter("listeners." + (i + 1) + ".backend_protocol", "tcp");
					} else if (listener.getNetworkProtocol().equals(LbProtocol.HTTP) || 
							listener.getNetworkProtocol().equals(LbProtocol.HTTPS)) {
						requestBuilder.parameter("listeners." + (i + 1) + ".listener_protocol", 
								listener.getNetworkProtocol().name().toLowerCase());
						requestBuilder.parameter("listeners." + (i + 1) + ".backend_protocol", 
								listener.getNetworkProtocol().name().toLowerCase());
					}
				}
				
				if (listener.getAlgorithm() != null) {
					if (listener.getAlgorithm().equals(LbAlgorithm.ROUND_ROBIN)) {
						requestBuilder.parameter("listeners." + (i + 1) + ".balance_mode", "roundrobin");
					} else if (listener.getAlgorithm().equals(LbAlgorithm.LEAST_CONN)) {
						requestBuilder.parameter("listeners." + (i + 1) + ".balance_mode", "leastconn");
					} else if (listener.getAlgorithm().equals(LbAlgorithm.SOURCE)) {
						requestBuilder.parameter("listeners." + (i + 1) + ".balance_mode", "source ");
					}
				}
				
				if (listener.getPersistence().equals(LbPersistence.COOKIE)) {
					if (listener.getCookie() != null) {
						requestBuilder.parameter("listeners." + (i + 1) + ".session_sticky", String.format(SessionStickyRewrite, listener.getCookie()));
					} else {
						requestBuilder.parameter("listeners." + (i + 1) + ".session_sticky", SessionStickyInsert);
					}
				}
				
				//health check
				if (listener.getProviderLBHealthCheckId() != null) {
					LoadBalancerHealthCheck healthCheck = this.getLoadBalancerHealthCheck(listener.getProviderLBHealthCheckId(), toLoadBalancerId);
					if (healthCheck != null) {
						requestBuilder.parameter("listeners." + (i + 1) + ".healthy_check_method", SessionStickyInsert);
						//TODO not support all hclprotocols, no capability matching
						if (healthCheck.getProtocol().equals(HCProtocol.HTTP)) {
							requestBuilder.parameter("listeners." + (i + 1) + ".healthy_check_method", 
									String.format(HealthCheckMethodHttp, healthCheck.getPath(), healthCheck.getHost()));
						} else if (healthCheck.getProtocol().equals(HCProtocol.TCP)) {
							requestBuilder.parameter("listeners." + (i + 1) + ".healthy_check_method", "tcp");
						}
						requestBuilder.parameter("listeners." + (i + 1) + ".healthy_check_option", 
								String.format(HealthCheckOption, healthCheck.getInterval(), healthCheck.getTimeout(), healthCheck.getUnhealthyCount(), healthCheck.getHealthyCount()));
					}
				}
				
				//ssl
				if (listener.getSslCertificateName() != null) {
					SSLCertificate certificate = this.getSSLCertificate(listener.getSslCertificateName());
					if (certificate != null) {
						requestBuilder.parameter("listeners." + (i + 1) + ".server_certificate_id", certificate.getProviderCertificateId());
					}
				}
			}
			
			Requester<AddLoadBalancerListenersResponseModel> requester = new QingCloudRequester<AddLoadBalancerListenersResponseModel, AddLoadBalancerListenersResponseModel>(
					getProvider(), 
					requestBuilder.build(), 
					AddLoadBalancerListenersResponseModel.class);
			requester.execute();
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void removeListeners(String toLoadBalancerId, LbListener[] listeners)
			throws CloudException, InternalException {
		APITrace.begin(getProvider(), "QingCloudLoadBalancer.removeListeners");
		try {
			
			if (toLoadBalancerId == null) {
				throw new InternalException("Invalid load balancer id!");
			}
			
			//search public port mapping listener
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DeleteLoadBalancerListeners");
			requestBuilder.parameter("zone", getProviderDataCenterId());
			List<DescribeLoadBalancerListenersResponseItemModel> lbListeners = listListeners(toLoadBalancerId);
			if (lbListeners != null && lbListeners.size() > 0) {
				for (int i = 0; i < listeners.length; i++) {
					for (DescribeLoadBalancerListenersResponseItemModel lbListener : lbListeners) {
						Integer lbListenerPort = lbListener.getListenerPort();
						if (lbListenerPort == listeners[i].getPublicPort()) {
							requestBuilder.parameter("loadbalancer_listeners." + (i + 1), lbListeners.get(lbListenerPort));
							break;
						}
					}
				}
			}
			
			Requester<DeleteLoadBalancerListenersResponseModel> requester = new QingCloudRequester<DeleteLoadBalancerListenersResponseModel, DeleteLoadBalancerListenersResponseModel>(
					getProvider(), 
					requestBuilder.build(), 
					DeleteLoadBalancerListenersResponseModel.class);
			requester.execute();
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void addServers(String toLoadBalancerId, String... serverIdsToAdd)
			throws CloudException, InternalException {
		APITrace.begin(getProvider(), "QingCloudLoadBalancer.addServers");
		try {
			
			if (toLoadBalancerId == null) {
				throw new InternalException("Invalid load balancer id!");
			}
			
			List<DescribeLoadBalancerListenersResponseItemModel> listeners = listListeners(toLoadBalancerId);
			for (DescribeLoadBalancerListenersResponseItemModel listener : listeners) {
				QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("AddLoadBalancerBackends");
				requestBuilder.parameter("zone", getProviderDataCenterId());
				requestBuilder.parameter("loadbalancer_listener", listener.getLoadBalancerListenerId());
				for (int i = 0; i < serverIdsToAdd.length; i++) {
					String serverIdToAdd = serverIdsToAdd[i];
					requestBuilder.parameter("backends." + (i + 1) + ".resource_id", serverIdToAdd);
					requestBuilder.parameter("backends." + (i + 1) + ".port", listener.getListenerPort()); //TODO same port as the public one
					requestBuilder.parameter("backends." + (i + 1) + ".weight", DefaultBackendServerWeight);
				}
				Requester<AddLoadBalancerBackendsResponseModel> requester = new QingCloudRequester<AddLoadBalancerBackendsResponseModel, AddLoadBalancerBackendsResponseModel>(
						getProvider(), 
						requestBuilder.build(), 
						AddLoadBalancerBackendsResponseModel.class);
				requester.execute();
			}
			
		} finally {
			APITrace.end();
		}
	}

	@Override
	public String createLoadBalancer(LoadBalancerCreateOptions options)
			throws CloudException, InternalException {
		
		APITrace.begin(getProvider(), "QingCloudLoadBalancer.createLoadBalancer");
		try {
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("CreateLoadBalancer");
			requestBuilder.parameter("loadbalancer_name", options.getName());
			requestBuilder.parameter("zone", getProviderDataCenterId());
			if (options.getFirewallIds() != null && options.getFirewallIds().length > 0) {
				requestBuilder.parameter("security_group", options.getFirewallIds()[0]);
			}
			if (options.getType() != null) {
				if (options.getType().equals(LbType.EXTERNAL)) {
					requestBuilder.parameter("eips.1", options.getProviderIpAddressId());
				} else if (options.getType().equals(LbType.INTERNAL)) {
					requestBuilder.parameter("vxnet", options.getProviderSubnetIds()[0]);
				}
			}
			Requester<CreateLoadBalancerResponseModel> requester = new QingCloudRequester<CreateLoadBalancerResponseModel, CreateLoadBalancerResponseModel>(
                    getProvider(), 
                    requestBuilder.build(), 
                    CreateLoadBalancerResponseModel.class);
			CreateLoadBalancerResponseModel response = requester.execute();
			if (response == null || response.getLoadbalancerId() == null) {
				throw new InternalException("Create load balancer failed!");
			}
			
			if (options.getDescription() != null) {
				requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("ModifyLoadBalancerAttributes");
				requestBuilder.parameter("loadbalancer", options.getDescription());
				requestBuilder.parameter("zone", getProviderDataCenterId());
				Requester<ResponseModel> modifyLoadBalancerAttributeResponse = new QingCloudRequester<ResponseModel, ResponseModel>(
	                    getProvider(), 
	                    requestBuilder.build(), 
	                    ResponseModel.class);
				modifyLoadBalancerAttributeResponse.execute();
			}
			
			if (options.getListeners() != null && options.getListeners().length > 0) {
				addListeners(response.getLoadbalancerId(), options.getListeners());
			}
			
			if (options.getHealthCheckOptions() != null) {
				this.createLoadBalancerHealthCheck(options.getHealthCheckOptions());
			}
			
			return response.getLoadbalancerId();
		} finally {
			APITrace.end();
		}
	}

	@Override
	public LoadBalancerCapabilities getCapabilities() throws CloudException,
			InternalException {
		return new QingCloudLoadBalancerCapabilities(getProvider());
	}

	@Override
	public LoadBalancer getLoadBalancer(String loadBalancerId)
			throws CloudException, InternalException {
		APITrace.begin(getProvider(), "QingCloudLoadBalancer.getLoadBalancer");
		try {
			
			if (loadBalancerId == null) {
				throw new InternalException("Invalid load balancer id!");
			}
			
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeLoadBalancers");
			requestBuilder.parameter("loadbalancers.1", loadBalancerId);
			requestBuilder.parameter("verbose", 1);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			Requester<List<LoadBalancer>> requester = new QingCloudRequester<DescribeLoadBalancersResponseModel, List<LoadBalancer>>(
                    getProvider(), 
                    requestBuilder.build(), 
                    new LoadBalancersMapper(), 
                    DescribeLoadBalancersResponseModel.class);
            List<LoadBalancer> loadBalancers = requester.execute();
			if (loadBalancers == null || loadBalancers.size() == 0) {
				throw new InternalException("Get load balancer " + loadBalancerId + " failed!");
			}
			return loadBalancers.get(0);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public boolean isSubscribed() throws CloudException, InternalException {
		return true;
	}

	@Override
	public Iterable<LoadBalancer> listLoadBalancers() throws CloudException,
			InternalException {
		APITrace.begin(getProvider(), "QingCloudLoadBalancer.listLoadBalancers");
		try {
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeLoadBalancers");
			requestBuilder.parameter("verbose", 1);
			requestBuilder.parameter("limit", DefaultResponseDataLimit);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			Requester<List<LoadBalancer>> requester = new QingCloudRequester<DescribeLoadBalancersResponseModel, List<LoadBalancer>>(
                    getProvider(), 
                    requestBuilder.build(), 
                    new LoadBalancersMapper(), 
                    DescribeLoadBalancersResponseModel.class);
            return requester.execute();
		} finally {
			APITrace.end();
		}
	}

	@Override
	public Iterable<ResourceStatus> listLoadBalancerStatus()
			throws CloudException, InternalException {
		APITrace.begin(getProvider(), "QingCloudLoadBalancer.listLoadBalancerStatus");
		try {
			List<ResourceStatus> statuses = new ArrayList<ResourceStatus>();
			for(LoadBalancer loadBalancer : listLoadBalancers()) {
				statuses.add(new ResourceStatus(loadBalancer.getProviderLoadBalancerId(), loadBalancer.getCurrentState()));
			}
			return statuses;
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void removeLoadBalancer(String loadBalancerId)
			throws CloudException, InternalException {
		APITrace.begin(getProvider(), "QingCloudLoadBalancer.removeLoadBalancer");
		try {
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DeleteLoadBalancers");
			requestBuilder.parameter("loadbalancers.1", loadBalancerId);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			Requester<SimpleJobResponseModel> requester = new QingCloudRequester<SimpleJobResponseModel, SimpleJobResponseModel>(
                    getProvider(), 
                    requestBuilder.build(), 
                    SimpleJobResponseModel.class);
			requester.execute();
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void removeServers(String fromLoadBalancerId,
			String... serverIdsToRemove) throws CloudException,
			InternalException {
		APITrace.begin(getProvider(), "QingCloudLoadBalancer.removeServers");
		try {
			
			if (fromLoadBalancerId == null) {
				throw new InternalException("Invalid load balancer id!");
			}
			
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DeleteLoadBalancerBackends");
			requestBuilder.parameter("zone", getProviderDataCenterId());
			
			List<DescribeLoadBalancerListenersResponseItemModel> listeners = listListeners(fromLoadBalancerId);
			int backendCount = 0;
			for (DescribeLoadBalancerListenersResponseItemModel listener : listeners) {
				if (listener.getBackends() != null && listener.getBackends().size() > 0) {
					for (DescribeLoadBalancerListenerBackends backend : listener.getBackends()) {
						for (String serverIdToRemove : serverIdsToRemove) {
							if (serverIdToRemove.equals(backend.getResourceId())) {
								requestBuilder.parameter("loadbalancer_backends." + (++backendCount), backend.getLoadbalancerBackendId());
								break;
							}
						}
						if (backendCount == serverIdsToRemove.length) {
							break;
						}
					}
				}
				if (backendCount == serverIdsToRemove.length) {
					break;
				}
			}
			Requester<DeleteLoadBalancerBackendsResponseModel> requester = new QingCloudRequester<DeleteLoadBalancerBackendsResponseModel, DeleteLoadBalancerBackendsResponseModel>(
                    getProvider(), 
                    requestBuilder.build(), 
                    DeleteLoadBalancerBackendsResponseModel.class);
			requester.execute();
			
		} finally {
			APITrace.end();
		}
	}

	@Override
	public LoadBalancerHealthCheck createLoadBalancerHealthCheck(String name,
			String description, String host, HCProtocol protocol, int port,
			String path, int interval, int timeout, int healthyCount,
			int unhealthyCount) throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LoadBalancerHealthCheck createLoadBalancerHealthCheck(
			HealthCheckOptions options) throws CloudException,
			InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LoadBalancerHealthCheck getLoadBalancerHealthCheck(
			String providerLBHealthCheckId, String providerLoadBalancerId)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<LoadBalancerHealthCheck> listLBHealthChecks(
			HealthCheckFilterOptions options) throws CloudException,
			InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void attachHealthCheckToLoadBalancer(String providerLoadBalancerId,
			String providerLBHealthCheckId) throws CloudException,
			InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void detachHealthCheckFromLoadBalancer(
			String providerLoadBalancerId, String providerLBHeathCheckId)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public LoadBalancerHealthCheck modifyHealthCheck(
			String providerLBHealthCheckId, HealthCheckOptions options)
			throws InternalException, CloudException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeLoadBalancerHealthCheck(String providerLoadBalancerId)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public SSLCertificate createSSLCertificate(
			SSLCertificateCreateOptions options) throws CloudException,
			InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<SSLCertificate> listSSLCertificates()
			throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeSSLCertificate(String certificateName)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSSLCertificate(SetLoadBalancerSSLCertificateOptions options)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public SSLCertificate getSSLCertificate(String certificateName)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setFirewalls(String providerLoadBalancerId,
			String... firewallIds) throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void attachLoadBalancerToSubnets(String toLoadBalancerId,
			String... subnetIdsToAdd) throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void detachLoadBalancerFromSubnets(String fromLoadBalancerId,
			String... subnetIdsToDelete) throws CloudException,
			InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void modifyLoadBalancerAttributes(String id,
			LbAttributesOptions options) throws CloudException,
			InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public LbAttributesOptions getLoadBalancerAttributes(String id)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}
	
	private String getProviderDataCenterId() throws InternalException, CloudException {
		String regionId = getContext().getRegionId();
        if (regionId == null) {
            throw new InternalException("No region was set for this request");
        }

        Iterable<DataCenter> dataCenters = getProvider().getDataCenterServices().listDataCenters(regionId);
        return dataCenters.iterator().next().getProviderDataCenterId();//each account has one DC in each region
	}
	
	private List<DescribeLoadBalancerListenersResponseItemModel> listListeners(String toLoadBalancerId) 
			throws InternalException, CloudException {
		
		QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeLoadBalancerListeners");
		requestBuilder.parameter("loadbalancer", toLoadBalancerId);
		requestBuilder.parameter("zone", getProviderDataCenterId());
		
		Requester<DescribeLoadBalancerListenersResponseModel> requester = new QingCloudRequester<DescribeLoadBalancerListenersResponseModel, DescribeLoadBalancerListenersResponseModel>(
				getProvider(), 
				requestBuilder.build(), 
				DescribeLoadBalancerListenersResponseModel.class);
		DescribeLoadBalancerListenersResponseModel response = requester.execute();
		return response.getLoadbalancerListenerSet();
	}
	
	private class LoadBalancersMapper extends QingCloudDriverToCoreMapper<DescribeLoadBalancersResponseModel, List<LoadBalancer>> {
		@Override
		protected List<LoadBalancer> doMapFrom(
				DescribeLoadBalancersResponseModel responseModel) {
			try {
				List<LoadBalancer> loadBalancers = new ArrayList<LoadBalancer>();
				if (responseModel != null && responseModel.getLoadbalancerSet() != null && responseModel.getLoadbalancerSet().size() > 0) {
					for (DescribeLoadBalancersResponseItemModel item : responseModel.getLoadbalancerSet()) {
						
						List<DescribeLoadBalancerListenersResponseItemModel> response = listListeners(item.getLoadbalancerId());
						int[] ports = new int[response.size()];
						for (int i = 0; i < response.size(); i++) {
							DescribeLoadBalancerListenersResponseItemModel listenerItem = response.get(i);
							ports[i] = listenerItem.getListenerPort();
						}
						loadBalancers.add(LoadBalancer.getInstance(
								getContext().getAccountNumber(), 
								getContext().getRegionId(), 
								item.getLoadbalancerId(), 
								mapFromStatus(item.getStatus(), item.getTransitionStatus()), 
								item.getLoadbalancerName(), 
								item.getDescription(), 
								item.getEips() != null && item.getEips().size() > 0 ? LbType.EXTERNAL : LbType.INTERNAL, 
								LoadBalancerAddressType.IP, 
								item.getEips() != null && item.getEips().size() > 0 ? item.getEips().get(0).getEipAddr() : null, 
								ports));
					}
				}
				return loadBalancers;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		private LoadBalancerState mapFromStatus(String status, String transitionStatus) {
			return null;
		}
		
	}

}
