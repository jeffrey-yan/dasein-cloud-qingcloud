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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.network.AbstractLoadBalancerSupport;
import org.dasein.cloud.network.HealthCheckFilterOptions;
import org.dasein.cloud.network.HealthCheckOptions;
import org.dasein.cloud.network.LbAlgorithm;
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
import org.dasein.cloud.qingcloud.network.model.CreateServerCertificateResponseModel;
import org.dasein.cloud.qingcloud.network.model.DeleteLoadBalancerBackendsResponseModel;
import org.dasein.cloud.qingcloud.network.model.DeleteLoadBalancerListenersResponseModel;
import org.dasein.cloud.qingcloud.network.model.DescribeLoadBalancerListenersResponseModel;
import org.dasein.cloud.qingcloud.network.model.DescribeLoadBalancerListenersResponseModel.DescribeLoadBalancerListenersResponseItemModel;
import org.dasein.cloud.qingcloud.network.model.DescribeLoadBalancerListenersResponseModel.DescribeLoadBalancerListenersResponseItemModel.DescribeLoadBalancerListenerBackends;
import org.dasein.cloud.qingcloud.network.model.DescribeLoadBalancersResponseModel;
import org.dasein.cloud.qingcloud.network.model.DescribeLoadBalancersResponseModel.DescribeLoadBalancersResponseItemModel;
import org.dasein.cloud.qingcloud.network.model.DescribeServerCertificatesResponseModel;
import org.dasein.cloud.qingcloud.network.model.DescribeServerCertificatesResponseModel.DescribeServerCertificatesResponseItemModel;
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
	
	private static final String DefaultDateFormat = "yyyy-MM-dd'T'hh:mm:ss'Z'";	
	private static final Integer DefaultResponseDataLimit = 999;
	
	private static final String DefaultSessionStickyInsert = "insert|3600";
	private static final String SessionStickyRewrite = "prefix|%s";
	
	private static final Integer DefaultBackendServerWeight = 5;
	
	private static final String HealthCheckMethodHttp = "http|%s|%s";
	private static final String HealthCheckOption = "%d|%d|%d|%d";
	
	private static final Integer DefaultLoadBalancerHealthCheckInterval = 10;
	private static final Integer DefaultLoadBalancerHealthCheckTimeout = 5;
	private static final Integer DefaultLoadBalancerHealthCheckUnhealthyCount = 2;
	private static final Integer DefaultLoadBalancerHealthCheckHealthyCount = 5;
	private static final String DefaultHealthCheckProtocol = "tcp";
	
	protected QingCloudLoadBalancer(QingCloud provider) {
		super(provider);
	}

	@Override
	public void addListeners(String toLoadBalancerId, LbListener[] listeners)
			throws CloudException, InternalException {
		APITrace.begin(getProvider(), "QingCloudLoadBalancer.addListeners");
		try {
			
			if (toLoadBalancerId == null) {
				throw new InternalException("Load balancer id cannot be null!");
			}
			
			if (listeners == null || listeners.length == 0) {
				return;
			}
			
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("AddLoadBalancerListeners");
			requestBuilder.parameter("loadbalancer", toLoadBalancerId);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			for (int i = 0; i < listeners.length; i++) {
				
				LbListener listener = listeners[i];
				requestBuilder.parameter("listeners." + (i + 1) + ".listener_port", listener.getPublicPort());

				if (listener.getNetworkProtocol().equals(LbProtocol.RAW_TCP)) {
						
					requestBuilder.parameter("listeners." + (i + 1) + ".listener_protocol", "tcp");
					requestBuilder.parameter("listeners." + (i + 1) + ".backend_protocol", "tcp");
				
				} else if (listener.getNetworkProtocol().equals(LbProtocol.HTTP)) {
					
					requestBuilder.parameter("listeners." + (i + 1) + ".listener_protocol", "http");
					requestBuilder.parameter("listeners." + (i + 1) + ".backend_protocol", "http");
				} else if (listener.getNetworkProtocol().equals(LbProtocol.HTTPS)) {
					
					requestBuilder.parameter("listeners." + (i + 1) + ".listener_protocol", "https");
					requestBuilder.parameter("listeners." + (i + 1) + ".backend_protocol", "https");
					
					//add ssl certificate for https listener
					SSLCertificate certificate = getSSLCertificate(listener.getSslCertificateName());
					requestBuilder.parameter("listeners." + (i + 1) + ".server_certificate_id", certificate.getProviderCertificateId());
				} else {
					throw new OperationNotSupportedException("Qingcloud not support " + listener.getNetworkProtocol().name() + " listener protocol");
				}
				
				requestBuilder.parameter("listeners." + (i + 1) + ".loadbalancer_listener_name", listener.getNetworkProtocol().name() + "_listener_" + toLoadBalancerId);
				
				if (listener.getAlgorithm().equals(LbAlgorithm.ROUND_ROBIN)) {
					requestBuilder.parameter("listeners." + (i + 1) + ".balance_mode", "roundrobin");
				} else if (listener.getAlgorithm().equals(LbAlgorithm.LEAST_CONN)) {
					requestBuilder.parameter("listeners." + (i + 1) + ".balance_mode", "leastconn");
				} else if (listener.getAlgorithm().equals(LbAlgorithm.SOURCE)) {
					requestBuilder.parameter("listeners." + (i + 1) + ".balance_mode", "source ");
				} else {
					throw new OperationNotSupportedException("Qingcloud not support " + listener.getAlgorithm().name() + " algorithm");
				}
				
				if (listener.getPersistence().equals(LbPersistence.COOKIE)) {
					if (listener.getCookie() != null) {
						requestBuilder.parameter("listeners." + (i + 1) + ".session_sticky", String.format(SessionStickyRewrite, listener.getCookie()));
					} else {
						requestBuilder.parameter("listeners." + (i + 1) + ".session_sticky", DefaultSessionStickyInsert);
					}
				}
				
				//TODO keep default health check settings
			}
			
			Requester<AddLoadBalancerListenersResponseModel> requester = new QingCloudRequester<AddLoadBalancerListenersResponseModel, AddLoadBalancerListenersResponseModel>(
					getProvider(), 
					requestBuilder.build(), 
					AddLoadBalancerListenersResponseModel.class);
			requester.execute();
			updateLoadBalancers(toLoadBalancerId);
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
				throw new InternalException("Load balancer id cannot be null!");
			}
			if (listeners == null || listeners.length == 0) {
				return;
			}
			
			//search public port mapping listener, one listener for one public port
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DeleteLoadBalancerListeners");
			requestBuilder.parameter("zone", getProviderDataCenterId());
			List<QingCloudLbListener> lbListeners = listListeners(toLoadBalancerId);
			if (lbListeners != null && lbListeners.size() > 0) {
				for (int i = 0; i < listeners.length; i++) {
					for (QingCloudLbListener lbListener : lbListeners) {
						if (lbListener.getLbListener().getPublicPort() == listeners[i].getPublicPort()) {
							requestBuilder.parameter("loadbalancer_listeners." + (i + 1), lbListener.getListenerId());
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
			updateLoadBalancers(toLoadBalancerId);
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
				throw new InternalException("Load balancer id cannot be null!");
			}
			if (serverIdsToAdd == null || serverIdsToAdd.length == 0) {
				return;
			}
			
			List<QingCloudLbListener> listeners = listListeners(toLoadBalancerId);
			for (QingCloudLbListener listener : listeners) {
				QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("AddLoadBalancerBackends");
				requestBuilder.parameter("zone", getProviderDataCenterId());
				requestBuilder.parameter("loadbalancer_listener", listener.getListenerId());
				for (int i = 0; i < serverIdsToAdd.length; i++) {
					String serverIdToAdd = serverIdsToAdd[i];
					requestBuilder.parameter("backends." + (i + 1) + ".resource_id", serverIdToAdd);
					requestBuilder.parameter("backends." + (i + 1) + ".port", listener.getLbListener().getPublicPort()); //TODO check same port as the public one
					requestBuilder.parameter("backends." + (i + 1) + ".weight", DefaultBackendServerWeight);
				}
				Requester<AddLoadBalancerBackendsResponseModel> requester = new QingCloudRequester<AddLoadBalancerBackendsResponseModel, AddLoadBalancerBackendsResponseModel>(
						getProvider(), 
						requestBuilder.build(), 
						AddLoadBalancerBackendsResponseModel.class);
				requester.execute();
			}
			updateLoadBalancers(toLoadBalancerId);
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
				requestBuilder.parameter("loadbalancer", response.getLoadbalancerId());
				requestBuilder.parameter("description", options.getDescription());
				requestBuilder.parameter("zone", getProviderDataCenterId());
				Requester<ResponseModel> modifyLoadBalancerAttributeResponse = new QingCloudRequester<ResponseModel, ResponseModel>(
	                    getProvider(), 
	                    requestBuilder.build(), 
	                    ResponseModel.class);
				modifyLoadBalancerAttributeResponse.execute();
				updateLoadBalancers(response.getLoadbalancerId());
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
				throw new InternalException("Load balancer id cannot be null!");
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
			
			if (loadBalancerId == null) {
				throw new InternalException("Load balancer id cannot be null!");
			}
			
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
				throw new InternalException("Load balancer id cannot be null!");
			}
			if (serverIdsToRemove == null || serverIdsToRemove.length == 0) {
				return;
			}
			
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DeleteLoadBalancerBackends");
			requestBuilder.parameter("zone", getProviderDataCenterId());
			
			List<QingCloudLbListener> listeners = listListeners(fromLoadBalancerId);
			int backendCount = 0;
			for (int i = 0; i < serverIdsToRemove.length; i++) {
				boolean isFind = false;
				for (QingCloudLbListener listener : listeners) {
					if (listener.getBackends() != null || listener.getBackends().size() > 0) {
						for (QingCloudLbListenerBackend backend : listener.getBackends()) {
							if (backend.getBackendId().equals(serverIdsToRemove[i])) {
								requestBuilder.parameter("loadbalancer_backends." + (++backendCount), backend.getBackendId());
								isFind = true;
								break;
							}
						}
						if (isFind) {
							break;
						}
					}
				}
			}
			Requester<DeleteLoadBalancerBackendsResponseModel> requester = new QingCloudRequester<DeleteLoadBalancerBackendsResponseModel, DeleteLoadBalancerBackendsResponseModel>(
                    getProvider(), 
                    requestBuilder.build(), 
                    DeleteLoadBalancerBackendsResponseModel.class);
			requester.execute();
			updateLoadBalancers(fromLoadBalancerId);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public LoadBalancerHealthCheck createLoadBalancerHealthCheck(
			HealthCheckOptions options) throws CloudException,
			InternalException {
		
		APITrace.begin(getProvider(), "QingCloudLoadBalancer.createLoadBalancerHealthCheck");
		try {
			
			if (options.getProviderLoadBalancerId() == null) {
				throw new InternalException("Qingcloud not support create load balancer health check without load balancer id!");
			}
			if (options.getListener() == null) {
				throw new InternalException("Qingcloud not support create load balancer health check without listener!");
			}
			
			int publicPort = options.getListener().getPublicPort();
			QingCloudLbListener listenerResponse = getListener(options.getProviderLoadBalancerId(), publicPort);
			if (listenerResponse == null) {
				throw new InternalException("Find listener listener on port " + publicPort + " for load balancer " + options.getProviderLoadBalancerId() + " failed!");
			}
			
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("ModifyLoadBalancerListenerAttributes");
			requestBuilder.parameter("loadbalancer_listener", listenerResponse.getListenerId());
			if (options.getProtocol().equals(HCProtocol.TCP)) {
				requestBuilder.parameter("healthy_check_method", "tcp");
			} else if (options.getProtocol().equals(HCProtocol.HTTP)) {
				requestBuilder.parameter("healthy_check_method", String.format(HealthCheckMethodHttp, options.getPath(), options.getHost()));
			}
			
			int interval = DefaultLoadBalancerHealthCheckInterval;
			if (options.getInterval() > 0) {
				interval = options.getInterval();
			}
			int timeout = DefaultLoadBalancerHealthCheckTimeout;
			if (options.getTimeout() > 0) {
				timeout = options.getTimeout();
			}
			int unhealthyCount = DefaultLoadBalancerHealthCheckUnhealthyCount;
			if (options.getUnhealthyCount() > 0) {
				unhealthyCount = options.getUnhealthyCount();
			}
			int healthyCount = DefaultLoadBalancerHealthCheckHealthyCount;
			if (options.getHealthyCount() > 0) {
				healthyCount = options.getHealthyCount();
			}
			requestBuilder.parameter("healthy_check_option", String.format(HealthCheckOption, interval, timeout, unhealthyCount, healthyCount));
			requestBuilder.parameter("zone", getProviderDataCenterId());
			Requester<ResponseModel> requester = new QingCloudRequester<ResponseModel, ResponseModel>(
                    getProvider(), 
                    requestBuilder.build(), 
                    ResponseModel.class);
			requester.execute();
			updateLoadBalancers(options.getProviderLoadBalancerId());
			
			return LoadBalancerHealthCheck.getInstance(listenerResponse.getListenerId(), 
					null, 
					null, 
					options.getHost(), 
					options.getProtocol(), 
					publicPort,
					options.getPath(), 
					interval, 
					timeout, 
					healthyCount, 
					unhealthyCount);
		} finally {
			APITrace.end();
		}
		
	}

	@Override
	public LoadBalancerHealthCheck getLoadBalancerHealthCheck(
			String providerLBHealthCheckId, String providerLoadBalancerId)
			throws CloudException, InternalException {
		APITrace.begin(getProvider(), "QingCloudLoadBalancer.getLoadBalancerHealthCheck");
		try {
			if (providerLBHealthCheckId == null) {
				throw new InternalException("Healthcheck Id cannot be null!");
			}
			if (providerLoadBalancerId == null) {
				throw new InternalException("Load balancer id cannot be null!");
			}
			
			List<LoadBalancerHealthCheck> healthChecks = listHealthChecks(providerLoadBalancerId, providerLBHealthCheckId);
			if (healthChecks == null || healthChecks.size() == 0) {
				throw new InternalException("find health check by load balancer id " + providerLoadBalancerId + " and health check id " + providerLBHealthCheckId + " failed!");
			}
			return healthChecks.get(0);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public Iterable<LoadBalancerHealthCheck> listLBHealthChecks(
			HealthCheckFilterOptions options) throws CloudException,
			InternalException {
		APITrace.begin(getProvider(), "QingCloudLoadBalancer.listLBHealthChecks");
		try {
			List<LoadBalancerHealthCheck> results = new ArrayList<LoadBalancerHealthCheck>();
			for (LoadBalancerHealthCheck healthCheck : listHealthChecks(null, null)) {
				if (options == null || options.matches(healthCheck)) {
					results.add(healthCheck);
				}
			}
			return results;
		} finally {
			APITrace.end();
		}
	}
	
	@Override
	public LoadBalancerHealthCheck modifyHealthCheck(
			String providerLBHealthCheckId, HealthCheckOptions options)
			throws InternalException, CloudException {
		APITrace.begin(getProvider(), "QingCloudLoadBalancer.modifyHealthCheck");
		try {
			
			if (providerLBHealthCheckId == null) {
				throw new InternalException("Healcheck id cannot be null!");
			}
			
			List<LoadBalancerHealthCheck> healthChecks = listHealthChecks(null, providerLBHealthCheckId);
			if (healthChecks == null || healthChecks.size() == 0) {
				throw new InternalException("Find health check " + providerLBHealthCheckId + " failed!");
			}
			LoadBalancerHealthCheck healthCheck = healthChecks.get(0);
			if (options == null) {
				return healthCheck;
			}
			
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("ModifyLoadBalancerListenerAttributes");
			requestBuilder.parameter("loadbalancer_listener", healthCheck.getProviderLBHealthCheckId());
			
			if (options.getProtocol() != null) {
				String path = healthCheck.getPath();
				String host = healthCheck.getHost();
				if (options.getProtocol().equals(HCProtocol.TCP)) {
					requestBuilder.parameter("healthy_check_method", "tcp");
				} else if (options.getProtocol().equals(HCProtocol.HTTP)) {
					if (options.getPath() != null) {
						path = options.getPath();
					}
					if (options.getHost() != null) {
						host = options.getHost();
					} 
					requestBuilder.parameter("healthy_check_method", 
							String.format(HealthCheckMethodHttp, path, host));
				}
			}
			
			int interval = healthCheck.getInterval();
			if (options.getInterval() > 0) {
				interval = options.getInterval();
			} 
			int timeout = healthCheck.getTimeout();
			if (options.getTimeout() > 0) {
				timeout = options.getTimeout();
			}
			int unhealthyCount = healthCheck.getUnhealthyCount();
			if (options.getUnhealthyCount() > 0) {
				unhealthyCount = options.getUnhealthyCount();
			}
			int healthyCount = healthCheck.getHealthyCount();
			if (options.getHealthyCount() > 0) {
				healthyCount = options.getHealthyCount();
			}
			requestBuilder.parameter("healthy_check_option", 
					String.format(HealthCheckOption, interval, timeout, unhealthyCount, healthyCount));			
			requestBuilder.parameter("zone", getProviderDataCenterId());
			
			Requester<ResponseModel> requester = new QingCloudRequester<ResponseModel, ResponseModel>(
                    getProvider(), 
                    requestBuilder.build(), 
                    ResponseModel.class);
			requester.execute();
			updateLoadBalancers(healthCheck.getProviderLoadBalancerIds().get(0));
			
			return getLoadBalancerHealthCheck(
					providerLBHealthCheckId, 
					healthCheck.getProviderLoadBalancerIds().get(0));
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void removeLoadBalancerHealthCheck(String providerLoadBalancerId)
			throws CloudException, InternalException {
		APITrace.begin(getProvider(), "QingCloudLoadBalancer.removeLoadBalancerHealthCheck");
		try {
			
			if (providerLoadBalancerId == null) {
				throw new InternalException("Load balancer id cannot be null!");
			}
			
			List<LoadBalancerHealthCheck> healthChecks = listHealthChecks(providerLoadBalancerId, null);
			for (LoadBalancerHealthCheck healthCheck : healthChecks) {
				QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("ModifyLoadBalancerListenerAttributes");
				requestBuilder.parameter("loadbalancer_listener", healthCheck.getProviderLBHealthCheckId());
				requestBuilder.parameter("healthy_check_method", DefaultHealthCheckProtocol);
				requestBuilder.parameter("healthy_check_option", 
						String.format(HealthCheckOption, 
								DefaultLoadBalancerHealthCheckInterval,
								DefaultLoadBalancerHealthCheckTimeout, 
								DefaultLoadBalancerHealthCheckUnhealthyCount, 
								DefaultLoadBalancerHealthCheckHealthyCount));
				requestBuilder.parameter("zone", getProviderDataCenterId());
				Requester<ResponseModel> requester = new QingCloudRequester<ResponseModel, ResponseModel>(
	                    getProvider(), 
	                    requestBuilder.build(), 
	                    ResponseModel.class);
				requester.execute();
			}
			updateLoadBalancers(providerLoadBalancerId);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public SSLCertificate createSSLCertificate(
			SSLCertificateCreateOptions options) throws CloudException,
			InternalException {
		APITrace.begin(getProvider(), "QingCloudLoadBalancer.createSSLCertificate");
		try {
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("CreateServerCertificate");
			requestBuilder.parameter("server_certificate_name", options.getCertificateName());
			requestBuilder.parameter("certificate_content", options.getCertificateBody());
			requestBuilder.parameter("private_key", options.getPrivateKey());
			requestBuilder.parameter("zone", getProviderDataCenterId());
			Requester<CreateServerCertificateResponseModel> requester = new QingCloudRequester<CreateServerCertificateResponseModel, CreateServerCertificateResponseModel>(
                    getProvider(), 
                    requestBuilder.build(), 
                    CreateServerCertificateResponseModel.class);
			CreateServerCertificateResponseModel response = requester.execute();
			if (response == null) {
				throw new InternalException("Create SSL Certificate failed!");
			}
			
			return getSSLCertificate(response.getServerCertificateId());
		} finally {
			APITrace.end();
		}
	}

	@Override
	public Iterable<SSLCertificate> listSSLCertificates()
			throws CloudException, InternalException {
		APITrace.begin(getProvider(), "QingCloudLoadBalancer.listSSLCertificates");
		try {
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeServerCertificates");
			requestBuilder.parameter("verbose", 1);
			requestBuilder.parameter("limit", DefaultResponseDataLimit);
			requestBuilder.parameter("zone", getProviderDataCenterId());
			Requester<List<SSLCertificate>> requester = new QingCloudRequester<DescribeServerCertificatesResponseModel, List<SSLCertificate>>(
                    getProvider(), 
                    requestBuilder.build(), 
                    new SSLCertificatesMapper(), 
                    DescribeServerCertificatesResponseModel.class);
            return requester.execute();
		} finally {
			APITrace.end();
		}
	}
	
	@Override
	public void removeSSLCertificate(String certificateName)
			throws CloudException, InternalException {
		APITrace.begin(getProvider(), "QingCloudLoadBalancer.removeSSLCertificate");
		try {
			
			if (certificateName == null) {
				throw new InternalException("Certificate name cannot be null!");
			}
			
			for(SSLCertificate sslCertificate : listSSLCertificates()) {
				if (sslCertificate.getCertificateName().equals(certificateName)) {
					QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DeleteServerCertificates");
					requestBuilder.parameter("server_certificates.1", sslCertificate.getProviderCertificateId());
					requestBuilder.parameter("zone", getProviderDataCenterId());
					Requester<SimpleJobResponseModel> requester = new QingCloudRequester<SimpleJobResponseModel, SimpleJobResponseModel>(
		                    getProvider(), 
		                    requestBuilder.build(), 
		                    SimpleJobResponseModel.class);
					requester.execute();
		            break;
				}
			}
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void setSSLCertificate(SetLoadBalancerSSLCertificateOptions options)
			throws CloudException, InternalException {
		APITrace.begin(getProvider(), "QingCloudLoadBalancer.setSSLCertificate");
		try {
			
			SSLCertificate sslCertificate = getSSLCertificate(options.getSslCertificateName());
			if (sslCertificate == null) {
				throw new InternalException("Find ssl certificate " + options.getSslCertificateName() + " failed!");
			}
			LoadBalancer loadBalancer = getLoadBalancerByName(options.getLoadBalancerName());
			if (loadBalancer == null) {
				throw new InternalException("Find load balancer " + options.getLoadBalancerName() + " failed!");
			}
			
			QingCloudLbListener listener = getListener(
					loadBalancer.getProviderLoadBalancerId(), 
					options.getSslCertificateAssignToPort());
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("ModifyLoadBalancerListenerAttributes");
			requestBuilder.parameter("loadbalancer_listener", listener.getListenerId());
			requestBuilder.parameter("server_certificate_id", sslCertificate.getProviderCertificateId());
			requestBuilder.parameter("zone", getProviderDataCenterId());
			Requester<ResponseModel> requester = new QingCloudRequester<ResponseModel, ResponseModel>(
                    getProvider(), 
                    requestBuilder.build(), 
                    ResponseModel.class);
			requester.execute();
			updateLoadBalancers(loadBalancer.getProviderLoadBalancerId());
		} finally {
			APITrace.end();
		}
	}

	@Override
	public SSLCertificate getSSLCertificate(String certificateName)
			throws CloudException, InternalException {
		APITrace.begin(getProvider(), "QingCloudLoadBalancer.getSSLCertificate");
		try {
			for(SSLCertificate sslCertificate : listSSLCertificates()) {
				if (sslCertificate.getCertificateName().equals(certificateName)) {
					return sslCertificate;
				}
			}
			return null;
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void setFirewalls(String providerLoadBalancerId,
			String... firewallIds) throws CloudException, InternalException {
		APITrace.begin(getProvider(), "QingCloudLoadBalancer.setFirewalls");
		try {
			
			if (providerLoadBalancerId == null) {
				throw new InternalException("Load balancer id cannot be null!");
			}
			if (firewallIds == null || firewallIds.length == 0) {
				return;
			}
			
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("ModifyLoadBalancerAttributes");
			requestBuilder.parameter("loadbalancer", providerLoadBalancerId);
			requestBuilder.parameter("security_group", firewallIds[0]);
			Requester<ResponseModel> requester = new QingCloudRequester<ResponseModel, ResponseModel>(
                    getProvider(), 
                    requestBuilder.build(), 
                    ResponseModel.class);
			requester.execute();
			updateLoadBalancers(providerLoadBalancerId);
		} finally {
			APITrace.end();
		}
	}
	
	private List<LoadBalancerHealthCheck> listHealthChecks(String loadBalancerId, String listenerId) throws InternalException, CloudException {
		QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeLoadBalancerListeners");
		if (loadBalancerId != null) {
			requestBuilder.parameter("loadbalancer", loadBalancerId);
		}
		if (listenerId != null) {
			requestBuilder.parameter("loadbalancer_listeners.1", listenerId);
		}
		requestBuilder.parameter("verbose", 1);
		requestBuilder.parameter("limit", DefaultResponseDataLimit);
		requestBuilder.parameter("zone", getProviderDataCenterId());
		
		Requester<List<LoadBalancerHealthCheck>> requester = new QingCloudRequester<DescribeLoadBalancerListenersResponseModel, List<LoadBalancerHealthCheck>>(
                getProvider(), 
                requestBuilder.build(), 
                new LoadBalancerHealthChecksMapper(), 
                DescribeLoadBalancerListenersResponseModel.class);
        return requester.execute();
	}
	
	private void updateLoadBalancers(String ... ids) throws InternalException, CloudException {
		QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("UpdateLoadBalancers");
		for (int i = 0; i < ids.length; i++) {
			requestBuilder.parameter("loadbalancers." + (i + 1), ids[i]);
		}
		requestBuilder.parameter("zone", getProviderDataCenterId());
		Requester<SimpleJobResponseModel> requester = new QingCloudRequester<SimpleJobResponseModel, SimpleJobResponseModel>(
                getProvider(), 
                requestBuilder.build(), 
                SimpleJobResponseModel.class);
		requester.execute();
	}
	
	private LoadBalancer getLoadBalancerByName(String name) throws CloudException, InternalException {
		for(LoadBalancer loadBalancer : listLoadBalancers()) {
			if (loadBalancer.getName().equals(name)) {
				return loadBalancer;
			}
		}
		return null;
	}
	
	private QingCloudLbListener getListener(String toLoadBalancerId, int publicPort) 
			throws InternalException, CloudException {
		List<QingCloudLbListener> listeners = listListeners(toLoadBalancerId);
		if (listeners != null) {
			for (QingCloudLbListener item : listeners) {
				if (item.getLbListener().getPublicPort() == publicPort) {
					return item;
				}
			}
		}
		return null;
	}
	
	private List<QingCloudLbListener> listListeners(String toLoadBalancerId) 
			throws InternalException, CloudException {
		QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("DescribeLoadBalancerListeners");
		if (toLoadBalancerId != null) {
			requestBuilder.parameter("loadbalancer", toLoadBalancerId);
		}
		requestBuilder.parameter("verbose", 1);
		requestBuilder.parameter("limit", DefaultResponseDataLimit);
		requestBuilder.parameter("zone", getProviderDataCenterId());
		
		Requester<List<QingCloudLbListener>> requester = new QingCloudRequester<DescribeLoadBalancerListenersResponseModel, List<QingCloudLbListener>>(
                getProvider(), 
                requestBuilder.build(), 
                new LoadBalancerListenersMapper(), 
                DescribeLoadBalancerListenersResponseModel.class);
        return requester.execute();
	}
	
	private String getProviderDataCenterId() throws InternalException, CloudException {
		String regionId = getContext().getRegionId();
        if (regionId == null) {
            throw new InternalException("No region was set for this request");
        }

        Iterable<DataCenter> dataCenters = getProvider().getDataCenterServices().listDataCenters(regionId);
        return dataCenters.iterator().next().getProviderDataCenterId();//each account has one DC in each region
	}
	
	private class SSLCertificatesMapper extends QingCloudDriverToCoreMapper<DescribeServerCertificatesResponseModel, List<SSLCertificate>> {
		@Override
		protected List<SSLCertificate> doMapFrom(
				DescribeServerCertificatesResponseModel responseModel) {
			try {
				List<SSLCertificate> sslCertificates = new ArrayList<SSLCertificate>();
				if (responseModel != null && responseModel.getServerCertificateSet() != null) {
					for (DescribeServerCertificatesResponseItemModel sslCertificate : responseModel.getServerCertificateSet()) {
						sslCertificates.add(SSLCertificate.getInstance(
								sslCertificate.getServerCertificateName(), 
								sslCertificate.getServerCertificateId(), 
								mapFromCreateTime(sslCertificate.getCreate_time()),
								sslCertificate.getCertificateContent(), 
								null, 
								null));
					}
				}
				return sslCertificates;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		private Long mapFromCreateTime(String createTime) throws ParseException {
			return new SimpleDateFormat(DefaultDateFormat).parse(createTime).getTime();
		}
	}
	
	private class QingCloudLbListenerBackend {
		private String backendId;
		private String backendName;
		private int port;
		private int weight;
		private String resourceId;
		private String createTime;
		public QingCloudLbListenerBackend(String backendId, String backendName, int port, int weight, String resourceId, String createTime) {
			this.backendId = backendId;
			this.backendName = backendName;
			this.port = port;
			this.weight = weight;
			this.resourceId = resourceId;
			this.createTime = createTime;
		}
		public String getBackendId() {
			return backendId;
		}
		public String getBackendName() {
			return backendName;
		}
		public int getPort() {
			return port;
		}
		public int getWeight() {
			return weight;
		}
		public String getResourceId() {
			return resourceId;
		}
		public String getCreateTime() {
			return createTime;
		}
	}
	
	private class QingCloudLbListener {
		
		private String listenerId;
		private String loadBalancerId;
		private LbListener lbListener;
		private List<QingCloudLbListenerBackend> backends;
		public String getListenerId() {
			return listenerId;
		}
		public LbListener getLbListener() {
			return lbListener;
		}
		public String getLoadBalancerId() {
			return this.loadBalancerId;
		}
		public List<QingCloudLbListenerBackend> getBackends() {
			return this.backends;
		}
		private QingCloudLbListener(String listenerId, LbListener lbListener) {
			this.listenerId = listenerId;
			this.lbListener = lbListener;
		}
		public QingCloudLbListener(String listenerId, LbAlgorithm algorithm, String persistenceCookie, LbProtocol protocol, int publicPort, int privatePort) {
			this(listenerId, LbListener.getInstance(algorithm, persistenceCookie, protocol, publicPort, privatePort));
		}
		public void addBackends(QingCloudLbListenerBackend backend) {
			if (backends == null) {
				backends = new ArrayList<QingCloudLbListenerBackend>();
			}
			backends.add(backend);
		}
		public void withLoadBalancer(String loadBalancerId) {
			this.loadBalancerId = loadBalancerId;
		}
	}
	
	private class LoadBalancerHealthChecksMapper extends QingCloudDriverToCoreMapper<DescribeLoadBalancerListenersResponseModel, List<LoadBalancerHealthCheck>> {
		@Override
		protected List<LoadBalancerHealthCheck> doMapFrom(
				DescribeLoadBalancerListenersResponseModel responseModel) {
			try {
				List<LoadBalancerHealthCheck> healthChecks = new ArrayList<LoadBalancerHealthCheck>();
				if (responseModel != null && responseModel.getLoadbalancerListenerSet() != null) {
					for (DescribeLoadBalancerListenersResponseItemModel listener : responseModel.getLoadbalancerListenerSet()) {
						String[] healthcheckOptionSegments = listener.getHealthyCheckOption().split("|");
						LoadBalancerHealthCheck healthCheck = LoadBalancerHealthCheck.getInstance(listener.getLoadBalancerListenerId(), 
								null, 
								null, 
								mapHostFromMethod(listener.getHealthyCheckMethod()), 
								mapProtocolFromMethod(listener.getHealthyCheckMethod()), 
								listener.getListenerPort(),
								mapPathFromMethod(listener.getHealthyCheckMethod()),
								Integer.valueOf(healthcheckOptionSegments[0]),
								Integer.valueOf(healthcheckOptionSegments[1]),
								Integer.valueOf(healthcheckOptionSegments[3]),
								Integer.valueOf(healthcheckOptionSegments[2]));
						
						//Should have a listener with the healthcheck
						DescribeLoadBalancerListenersResponseModel model = new DescribeLoadBalancerListenersResponseModel();
						model.setLoadbalancerListenerSet(Arrays.asList(listener));
						List<QingCloudLbListener> listeners = new LoadBalancerListenersMapper().doMapFrom(model);
						if (listeners != null && listeners.size() > 0) {
							healthCheck.addListener(listeners.get(0).getLbListener());
							healthCheck.addProviderLoadBalancerId(listeners.get(0).getLoadBalancerId());
						}
						
						healthChecks.add(healthCheck);
					}
				}
				return healthChecks;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		private String mapHostFromMethod(String method) {
			if (method.split("|")[0].equals("http")) {
				return method.split("|")[2];
			}
			return null;
		}
		
		private String mapPathFromMethod(String method) {
			if (method.split("|")[0].equals("http")) {
				return method.split("|")[1];
			}
			return null;
		}
		
		private HCProtocol mapProtocolFromMethod(String method) {
			if (method != null) {
				if (method.split("|")[0].equals("tcp")) {
					return HCProtocol.TCP;
				} else if (method.split("|")[0].equals("http")) {
					return HCProtocol.HTTP;
				} 
			}
			return null;
		}
	}
	
	private class LoadBalancerListenersMapper extends QingCloudDriverToCoreMapper<DescribeLoadBalancerListenersResponseModel, List<QingCloudLbListener>> {
		
		@Override
		protected List<QingCloudLbListener> doMapFrom(
				DescribeLoadBalancerListenersResponseModel responseModel) {
			try {
				List<QingCloudLbListener> listeners = new ArrayList<QingCloudLbListener>();
				if (responseModel != null && responseModel.getLoadbalancerListenerSet() != null) {
					for (DescribeLoadBalancerListenersResponseItemModel item : responseModel.getLoadbalancerListenerSet()) {
						QingCloudLbListener listener = new QingCloudLbListener(
							item.getLoadBalancerListenerId(),
							mapAlgorithmFromBalanceMode(item.getBalanceMode()), 
							mapCookieFromSessionSticky(item.getSessionSticky()), 
							mapProtocolFromListenerProtocol(item.getListenerProtocol()), 
							item.getListenerPort(), 
							item.getListenerPort());
						String loadBalancerId = null;
						if (item.getBackends() != null && item.getBackends().size() > 0) {
							for (DescribeLoadBalancerListenerBackends backend : item.getBackends()) {
								listener.addBackends(new QingCloudLbListenerBackend(
										backend.getLoadbalancerBackendId(), 
										backend.getLoadbalancerBackendName(), 
										backend.getPort(),
										backend.getWeight(),
										backend.getResourceId(), 
										backend.getCreate_time()));
								if (loadBalancerId == null) {
									loadBalancerId = backend.getLoadbalancerId();
								}
							}
						}
						listener.withLoadBalancer(loadBalancerId);
					}
				}
				return listeners;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		private LbAlgorithm mapAlgorithmFromBalanceMode(String mode) {
			if (mode != null) {
				if (mode.equals("roundrobin")) {
					return LbAlgorithm.ROUND_ROBIN;
				} else if (mode.equals("leastconn ")) {
					return LbAlgorithm.LEAST_CONN;
				} else if (mode.equals("source")) {
					return LbAlgorithm.SOURCE;
				}
			}
			return null;
		}
		
		private String mapCookieFromSessionSticky(String sessionSticky) {
			if (sessionSticky != null) {
				if (sessionSticky.split("|")[0].equals("prefix")) {
					return sessionSticky.split("|")[1];
				}
			}
			return null;
		}
		
		private LbProtocol mapProtocolFromListenerProtocol(String protocol) {
			if (protocol != null) {
				if (protocol.equals("http")) {
					return LbProtocol.HTTP;
				} else if (protocol.equals("https")) {
					return LbProtocol.HTTPS;
				} else if (protocol.equals("tcp")) {
					return LbProtocol.RAW_TCP;
				}
			}
			return null;
		}
	}
	
	private class LoadBalancersMapper extends QingCloudDriverToCoreMapper<DescribeLoadBalancersResponseModel, List<LoadBalancer>> {
		@Override
		protected List<LoadBalancer> doMapFrom(
				DescribeLoadBalancersResponseModel responseModel) {
			try {
				List<LoadBalancer> loadBalancers = new ArrayList<LoadBalancer>();
				if (responseModel != null && responseModel.getLoadbalancerSet() != null && responseModel.getLoadbalancerSet().size() > 0) {
					for (DescribeLoadBalancersResponseItemModel item : responseModel.getLoadbalancerSet()) {
						
						List<QingCloudLbListener> response = listListeners(item.getLoadbalancerId());
						int[] ports = new int[response.size()];
						for (int i = 0; i < response.size(); i++) {
							QingCloudLbListener listenerItem = response.get(i);
							ports[i] = listenerItem.getLbListener().getPublicPort();
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
			if (status.equals("pending")) {
				return LoadBalancerState.PENDING;
			} else if (status.equals("active")) {
				return LoadBalancerState.ACTIVE;
			} else if (status.equals("stopped") || status.equals("suspended") || status.equals("deleted") || status.equals("ceased")) {
				return LoadBalancerState.TERMINATED;
			}
			return null;
		}
		
	}

}
