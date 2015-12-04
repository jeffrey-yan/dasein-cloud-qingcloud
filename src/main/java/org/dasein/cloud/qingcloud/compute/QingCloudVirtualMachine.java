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

package org.dasein.cloud.qingcloud.compute;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.HttpUriRequest;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.AbstractVMSupport;
import org.dasein.cloud.compute.VMLaunchOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineCapabilities;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.compute.VolumeAttachment;
import org.dasein.cloud.compute.VolumeCreateOptions;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.network.NICCreateOptions;
import org.dasein.cloud.qingcloud.QingCloud;
import org.dasein.cloud.qingcloud.compute.model.RunInstancesResponseModel;
import org.dasein.cloud.qingcloud.compute.model.TerminateInstancesResponse;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequestBuilder;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequester;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.requester.fluent.Requester;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jeffrey Yan on 11/24/2015.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
public class QingCloudVirtualMachine extends AbstractVMSupport<QingCloud> implements VirtualMachineSupport {

    protected QingCloudVirtualMachine(QingCloud provider) {
        super(provider);
    }

    @Nonnull
    @Override
    public VirtualMachineCapabilities getCapabilities() throws InternalException, CloudException {
        return new QingCloudVirtualMachineCapabilities(getProvider());
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        return true;
    }

    @Nonnull
    @Override
    public VirtualMachine launch(@Nonnull VMLaunchOptions withLaunchOptions) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "VirtualMachine.launch");
        try {
            QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(getProvider()).action("RunInstances");

            requestBuilder.parameter("zone", withLaunchOptions.getDataCenterId());
            requestBuilder.parameter("image_id", withLaunchOptions.getMachineImageId());
            requestBuilder.parameter("instance_type", withLaunchOptions.getStandardProductId());
            requestBuilder.parameter("count", "1");
            requestBuilder.parameterIfNotNull("instance_name", withLaunchOptions.getHostName());

            if (withLaunchOptions.getBootstrapKey() != null) {
                requestBuilder.parameter("login_mode", "login_keypair");
                requestBuilder.parameter("login_keypair", withLaunchOptions.getBootstrapKey());
            } else if (withLaunchOptions.getBootstrapPassword() != null) {
                requestBuilder.parameter("login_mode", "login_passwd");
                requestBuilder.parameter("login_passwd", withLaunchOptions.getBootstrapPassword());
            }

            if (withLaunchOptions.getSubnetId() != null) {
                if (withLaunchOptions.getPrivateIp() != null) {
                    requestBuilder.parameter("vxnets.1", withLaunchOptions.getSubnetId() + "|" + withLaunchOptions.getPrivateIp());
                } else {
                    requestBuilder.parameter("vxnets.1", withLaunchOptions.getSubnetId());
                }
            } else if(withLaunchOptions.getNetworkInterfaces() != null && withLaunchOptions.getNetworkInterfaces().length > 0 ) {
                int index = 1;
                for (VMLaunchOptions.NICConfig nicConfig : withLaunchOptions.getNetworkInterfaces()) {
                    NICCreateOptions nicCreateOptions = nicConfig.nicToCreate;
                    if(nicCreateOptions.getIpAddress() != null) {
                        requestBuilder.parameter("vxnets." + index, nicCreateOptions.getSubnetId() + "|" + nicCreateOptions.getIpAddress());
                    } else {
                        requestBuilder.parameter("vxnets." + index, nicCreateOptions.getSubnetId());
                    }

                    index++;
                }
            }

            //actually, security is needed only when subnet is vxnet-0
            requestBuilder.parameterIfNotNull("security_group", withLaunchOptions.getFirewallIds()[0]);

            int volumeIndex = 1;
            for (VolumeAttachment volumeAttachment : withLaunchOptions.getVolumes()) {
                requestBuilder.parameter("volumes." + volumeIndex, volumeAttachment.getExistingVolumeId());
                volumeIndex++;
            }

            if (withLaunchOptions.getUserData() != null) {
                requestBuilder.parameter("need_userdata", 1);
                requestBuilder.parameter("userdata_type", "plain");
                requestBuilder.parameter("userdata_value", Base64.encodeBase64String(withLaunchOptions.getUserData().getBytes()));
            } else {
                requestBuilder.parameter("need_userdata", 0);
            }

            requestBuilder.parameter("instance_class", 1);//use high performance type, although no doc found

            HttpUriRequest request = requestBuilder.build();

            Requester<RunInstancesResponseModel> requester = new QingCloudRequester<RunInstancesResponseModel, RunInstancesResponseModel>(
                    getProvider(), request, RunInstancesResponseModel.class);

            RunInstancesResponseModel runInstancesResponseModel = requester.execute();

            return getVirtualMachine(runInstancesResponseModel.getInstances().get(0));
        } finally {
            APITrace.end();
        }
    }

    @Override
    public void terminate(@Nonnull String vmId, @Nullable String explanation) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "VirtualMachine.terminate");
        try {

            String regionId = getContext().getRegionId();
            if (regionId == null) {
                throw new InternalException("No region was set for this request");
            }

            Iterable<DataCenter> dataCenters = getProvider().getDataCenterServices().listDataCenters(regionId);
            String dataCenterId = dataCenters.iterator().next().getProviderDataCenterId();//each account has one DC in each region

            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
                    .action("TerminateInstances")
                    .parameter("instances.1", vmId)
                    .parameter("zone", dataCenterId)
                    .build();

            Requester<TerminateInstancesResponse> requester = new QingCloudRequester<TerminateInstancesResponse, TerminateInstancesResponse>(
                    getProvider(), request, TerminateInstancesResponse.class);

            requester.execute();
        } finally {
            APITrace.end();
        }
    }
}
