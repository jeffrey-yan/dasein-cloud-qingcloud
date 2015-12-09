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
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpUriRequest;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.Tag;
import org.dasein.cloud.VisibleScope;
import org.dasein.cloud.compute.AbstractVMSupport;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VMLaunchOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineCapabilities;
import org.dasein.cloud.compute.VirtualMachineLifecycle;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.VirtualMachineProductFilterOptions;
import org.dasein.cloud.compute.VirtualMachineStatus;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.compute.VmStatus;
import org.dasein.cloud.compute.VmStatusFilterOptions;
import org.dasein.cloud.compute.Volume;
import org.dasein.cloud.compute.VolumeAttachment;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.NICCreateOptions;
import org.dasein.cloud.network.RawAddress;
import org.dasein.cloud.qingcloud.QingCloud;
import org.dasein.cloud.qingcloud.compute.model.DescribeInstancesResponseModel;
import org.dasein.cloud.qingcloud.compute.model.RunInstancesResponseModel;
import org.dasein.cloud.qingcloud.model.SimpleJobResponseModel;
import org.dasein.cloud.qingcloud.util.requester.QingCloudDriverToCoreMapper;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequestBuilder;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequester;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.cloud.util.requester.fluent.Requester;
import org.dasein.util.uom.storage.Megabyte;
import org.dasein.util.uom.storage.Storage;
import org.dasein.util.uom.time.TimePeriod;
import org.dasein.util.uom.time.Week;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
            QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.put(getProvider()).action("RunInstances");

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
    public void reboot( @Nonnull String vmId ) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "VirtualMachine.reboot");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.post(getProvider())
                    .action("RestartInstances")
                    .parameter("instances.1", vmId)
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<SimpleJobResponseModel> requester = new QingCloudRequester<SimpleJobResponseModel, SimpleJobResponseModel>(
                    getProvider(), request, SimpleJobResponseModel.class);

            requester.execute();
        } finally {
            APITrace.end();
        }
    }

    @Override
    public void start( @Nonnull String vmId ) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "VirtualMachine.start");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.post(getProvider())
                    .action("StartInstances")
                    .parameter("instances.1", vmId)
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<SimpleJobResponseModel> requester = new QingCloudRequester<SimpleJobResponseModel, SimpleJobResponseModel>(
                    getProvider(), request, SimpleJobResponseModel.class);

            requester.execute();
        } finally {
            APITrace.end();
        }
    }

    @Override
    public void stop( @Nonnull String vmId, boolean force ) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "VirtualMachine.stop");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.post(getProvider())
                    .action("StopInstances")
                    .parameter("instances.1", vmId)
                    .parameter("force", force ? "1" : "0")
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<SimpleJobResponseModel> requester = new QingCloudRequester<SimpleJobResponseModel, SimpleJobResponseModel>(
                    getProvider(), request, SimpleJobResponseModel.class);

            requester.execute();
        } finally {
            APITrace.end();
        }
    }

    @Override
    public void terminate(@Nonnull String vmId, @Nullable String explanation) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "VirtualMachine.terminate");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.post(getProvider())
                    .action("TerminateInstances")
                    .parameter("instances.1", vmId)
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<SimpleJobResponseModel> requester = new QingCloudRequester<SimpleJobResponseModel, SimpleJobResponseModel>(
                    getProvider(), request, SimpleJobResponseModel.class);

            requester.execute();
        } finally {
            APITrace.end();
        }
    }

    @Override
    public @Nullable VirtualMachineProduct getProduct( @Nonnull String productId ) throws InternalException, CloudException {
        Iterator<VirtualMachineProduct> virtualMachineProductIterator = listAllProducts().iterator();
        while (virtualMachineProductIterator.hasNext()) {
            VirtualMachineProduct virtualMachineProduct = virtualMachineProductIterator.next();
            if (virtualMachineProduct.getProviderProductId().equals(productId)) {
                return virtualMachineProduct;
            }
        }
        return null;
    }

    @Override
    public @Nonnull Iterable<VirtualMachineProduct> listProducts(@Nonnull String machineImageId, @Nonnull VirtualMachineProductFilterOptions options) throws InternalException, CloudException {
        List<VirtualMachineProduct> result = new ArrayList<VirtualMachineProduct>();

        Iterator<VirtualMachineProduct> virtualMachineProductIterator = listAllProducts().iterator();
        while (virtualMachineProductIterator.hasNext()) {
            VirtualMachineProduct virtualMachineProduct = virtualMachineProductIterator.next();
            if (options.matches(virtualMachineProduct)) {
                result.add(virtualMachineProduct);
            }
        }
        return result;
    }

    @Override
    public @Nonnull Iterable<VirtualMachineProduct> listAllProducts() throws InternalException, CloudException {
        ProviderContext context = getProvider().getContext();
        if (context == null) {
            throw new CloudException("No context was set for this request");
        }

        Cache<VirtualMachineProduct> cache = Cache
                .getInstance(getProvider(), "vmProducts", VirtualMachineProduct.class, CacheLevel.REGION,
                        new TimePeriod<Week>(1, TimePeriod.WEEK));
        Iterable<VirtualMachineProduct> products = cache.get(context);
        if (products == null) {
            products = loadProductsDefinition(getProvider().getZoneId());
            cache.put(context, products);
        }
        return products;
    }

    private List<VirtualMachineProduct> loadProductsDefinition(String zoneId) throws InternalException {
        try {
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("/org/dasein/cloud/aws/vmproducts.json");
            JSONArray array = new JSONArray(IOUtils.toString(inputStream));

            Map<String, List<VirtualMachineProduct>> result = new HashMap<String, List<VirtualMachineProduct>>();

            for (int i = 0; i < array.length(); i++) {
                JSONObject definitionOfZone = array.getJSONObject(i);

                JSONArray instanceTypesOfZone = definitionOfZone.getJSONArray("instance_types");
                List<VirtualMachineProduct> productsOfZone = new ArrayList<VirtualMachineProduct>();
                for (int j = 0; j < instanceTypesOfZone.length(); j++) {
                    JSONObject instanceType = instanceTypesOfZone.getJSONObject(j);
                    VirtualMachineProduct product = new VirtualMachineProduct();
                    product.setProviderProductId(instanceType.getString("id"));
                    product.setName(instanceType.getString("id"));
                    product.setCpuCount(instanceType.getInt("cpu_count"));
                    product.setRamSize(
                            (new Storage<Megabyte>(instanceType.getInt("memory_size_in_mb"), Storage.MEGABYTE)));
                    productsOfZone.add(product);
                }
                result.put(definitionOfZone.getString("zone"), productsOfZone);
            }

            if (result.containsKey(zoneId)) {
                return result.get(zoneId);
            } else {
                return result.get("default");
            }
        } catch (Exception exception) {
            throw new InternalException("Not able to load vm product definition", exception);
        }
    }

    @Override
    public @Nonnull VirtualMachine alterVirtualMachineProduct(@Nonnull String virtualMachineId, @Nonnull String productId) throws InternalException, CloudException{
        APITrace.begin(getProvider(), "VirtualMachine.alterVirtualMachineProduct");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.post(getProvider())
                    .action("ResizeInstances")
                    .parameter("instances.1", virtualMachineId)
                    .parameter("zone", getProvider().getZoneId())
                    .parameter("instance_type", productId)
                    .build();

            Requester<SimpleJobResponseModel> requester = new QingCloudRequester<SimpleJobResponseModel, SimpleJobResponseModel>(
                    getProvider(), request, SimpleJobResponseModel.class);

            requester.execute();

            return getVirtualMachine(virtualMachineId);
        } finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull VirtualMachine alterVirtualMachineSize(@Nonnull String virtualMachineId, @Nullable String cpuCount, @Nullable String ramInMB) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "VirtualMachine.alterVirtualMachineSize");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.post(getProvider())
                    .action("ResizeInstances")
                    .parameter("instances.1", virtualMachineId)
                    .parameter("zone", getProvider().getZoneId())
                    .parameter("cpu", cpuCount)
                    .parameter("memory", ramInMB)
                    .build();

            Requester<SimpleJobResponseModel> requester = new QingCloudRequester<SimpleJobResponseModel, SimpleJobResponseModel>(
                    getProvider(), request, SimpleJobResponseModel.class);

            requester.execute();

            return getVirtualMachine(virtualMachineId);
        } finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull Iterable<String> listFirewalls( @Nonnull String vmId ) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "VirtualMachine.listFirewalls");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
                    .action("DescribeInstances")
                    .parameter("instances.1", vmId)
                    .parameter("zone", getProvider().getZoneId())
                    .parameter("verbose", "1").build();

            Requester<String> requester = new QingCloudRequester<DescribeInstancesResponseModel, String>(
                    getProvider(), request,
                    new QingCloudDriverToCoreMapper<DescribeInstancesResponseModel, String>() {
                        @Override
                        protected String doMapFrom(DescribeInstancesResponseModel responseModel) {
                            return responseModel.getInstances().get(0).getSecurityGroup().getSecurityGroupId();
                        }
                    }, DescribeInstancesResponseModel.class);

            String securityGroupId = requester.execute();
            return Arrays.asList(securityGroupId);
        } finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull VirtualMachine alterVirtualMachineFirewalls(@Nonnull String virtualMachineId, @Nonnull String[] firewalls) throws InternalException, CloudException{
        APITrace.begin(getProvider(), "VirtualMachine.alterVirtualMachineFirewalls");
        try {
            if (firewalls == null || firewalls.length != 1) {
                throw new InternalException("QingCloud instance can have only one firewall");
            }

            HttpUriRequest request = QingCloudRequestBuilder.post(getProvider())
                    .action("ApplySecurityGroup")
                    .parameter("security_group", firewalls[0])
                    .parameter("instances.1", virtualMachineId)
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<SimpleJobResponseModel> requester = new QingCloudRequester<SimpleJobResponseModel, SimpleJobResponseModel>(
                    getProvider(), request, SimpleJobResponseModel.class);

            requester.execute();

            return getVirtualMachine(virtualMachineId);
        } finally {
            APITrace.end();
        }
    }

    @Override
    public @Nullable VirtualMachine getVirtualMachine( @Nonnull String vmId ) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "VirtualMachine.getVirtualMachine");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
                    .action("DescribeInstances")
                    .parameter("instances.1", vmId)
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<List<VirtualMachine>> requester = new QingCloudRequester<DescribeInstancesResponseModel, List<VirtualMachine>>(
                    getProvider(), request, new VirtualMachinesMapper(), DescribeInstancesResponseModel.class);

            List<VirtualMachine> result = requester.execute();
            if (result.size() > 0) {
                return result.get(0);
            } else {
                return null;
            }
        } finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull Iterable<VirtualMachine> listVirtualMachines() throws InternalException, CloudException {
        APITrace.begin(getProvider(), "VirtualMachine.listVirtualMachines");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
                    .action("DescribeInstances")
                    .parameter("limit", "999")
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<List<VirtualMachine>> requester = new QingCloudRequester<DescribeInstancesResponseModel, List<VirtualMachine>>(
                    getProvider(), request, new VirtualMachinesMapper(), DescribeInstancesResponseModel.class);

            return requester.execute();
        } finally {
            APITrace.end();
        }
    }

    //TODO VM statistics

    @Override
    public @Nullable Iterable<VirtualMachineStatus> getVMStatus( @Nullable String... vmIds ) throws InternalException, CloudException {
        return getVMStatus(VmStatusFilterOptions.getInstance().withVmIds(vmIds));
    }

    @Override
    public @Nullable Iterable<VirtualMachineStatus> getVMStatus( @Nullable final VmStatusFilterOptions filterOptions ) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "VirtualMachine.getVMStatus");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
                    .action("DescribeInstances")
                    .parameter("limit", "999")
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<List<VirtualMachineStatus>> requester = new QingCloudRequester<DescribeInstancesResponseModel, List<VirtualMachineStatus>>(
                    getProvider(), request,
                    new QingCloudDriverToCoreMapper<DescribeInstancesResponseModel, List<VirtualMachineStatus>>() {
                        @Override
                        protected List<VirtualMachineStatus> doMapFrom(DescribeInstancesResponseModel responseModel) {
                            List<VirtualMachineStatus> result = new ArrayList<VirtualMachineStatus>();
                            for (DescribeInstancesResponseModel.Instance instance : responseModel.getInstances()) {
                                VirtualMachineStatus virtualMachineStatus = new VirtualMachineStatus();
                                virtualMachineStatus.setProviderHostStatus(getHostStatus());
                                virtualMachineStatus.setProviderVmStatus(getVmStatus());
                                virtualMachineStatus.setProviderVirtualMachineId(instance.getInstanceId());

                                if (filterOptions.matches(virtualMachineStatus)) {
                                    result.add(virtualMachineStatus);
                                }
                            }
                            return result;
                        }
                    }, DescribeInstancesResponseModel.class);

            return requester.execute();
        } finally {
            APITrace.end();
        }
    }

    @Override
    public void updateTags( @Nonnull String vmId, @Nonnull Tag... tags ) throws CloudException, InternalException {
        // TODO, tags
    }

    @Override
    public void removeTags( @Nonnull String vmId, @Nonnull Tag... tags ) throws CloudException, InternalException {
        // TODO, tags
    }

    private VmStatus getVmStatus() {
        return VmStatus.OK;
    }

    private VmStatus getHostStatus() {
        return VmStatus.OK;
    }

    private class VirtualMachinesMapper extends QingCloudDriverToCoreMapper<DescribeInstancesResponseModel, List<VirtualMachine>>{
        @Override
        protected List<VirtualMachine> doMapFrom(DescribeInstancesResponseModel responseModel) {
            try {
                List<VirtualMachine> virtualMachines = new ArrayList<VirtualMachine>();

                for(DescribeInstancesResponseModel.Instance instance : responseModel.getInstances()) {
                    VirtualMachine virtualMachine = new VirtualMachine();
                    virtualMachine.setProviderVirtualMachineId(instance.getInstanceId());
                    virtualMachine.setProductId(instance.getInstanceType());
                    virtualMachine.setProviderVolumeIds(instance.getVolumeIds().toArray(new String[0]));
                    virtualMachine.setVolumes(listVolumes(instance.getVolumeIds()));
                    virtualMachine.setProviderNetworkInterfaceIds(collectNetworkInterfaceIds(instance.getVxnets()));
                    virtualMachine.setPrivateAddresses(collectPrivateIpAddresses(instance.getVxnets()));
                    //TODO, private subnet ids not set, because only one allowed for dasein model
                    virtualMachine.setPublicAddresses(new RawAddress(instance.getEip().getEipAddr(), IPVersion.IPV4));
                    virtualMachine.setCurrentState(mapVmState(instance.getStatus(), instance.getTransitionStatus()));
                    virtualMachine.setStateReasonMessage(instance.getTransitionStatus());
                    virtualMachine.setName(instance.getInstanceName());
                    virtualMachine.setCreationTimestamp(
                            getProvider().parseIso8601Date(instance.getCreateTime()).getTime());
                    virtualMachine.setDescription(instance.getDescription());
                    virtualMachine.setProviderFirewallIds(
                            new String[] { instance.getSecurityGroup().getSecurityGroupId() });
                    virtualMachine.setProviderMachineImageId(instance.getImage().getImageId());
                    virtualMachine.setArchitecture(mapArchitecture(instance.getImage().getProcessorType()));
                    virtualMachine.setPlatform(Platform.guess(instance.getImage().getOsFamily()));
                    virtualMachine.setProviderShellKeyIds(instance.getKeypairIds().toArray(new String[0]));

                    virtualMachine.setProviderRegionId(getContext().getRegionId());
                    virtualMachine.setProviderDataCenterId(getProvider().getZoneId());
                    virtualMachine.setProviderOwnerId(getContext().getAccountNumber());
                    //TODO, set tags
                    virtualMachine.setImagable(getCapabilities().canStop(virtualMachine.getCurrentState()));
                    virtualMachine.setRebootable(getCapabilities().canReboot(virtualMachine.getCurrentState()));

                    virtualMachine.setClonable(false);
                    virtualMachine.setPausable(false);
                    virtualMachine.setPersistent(true);
                    virtualMachine.setProviderVmStatus(getVmStatus());
                    virtualMachine.setProviderHostStatus(getHostStatus());
                    virtualMachine.setLifecycle(VirtualMachineLifecycle.NORMAL);
                    virtualMachine.setVisibleScope(VisibleScope.ACCOUNT_DATACENTER);
                }

                return virtualMachines;
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }

        private Architecture mapArchitecture(String architecture) {
            if("64bit".equals(architecture)) {
                return Architecture.I64;
            } else {
                return Architecture.I32;
            }
        }

        private VmState mapVmState(String state, String transitionState) {
            if (transitionState != null && !transitionState.equals("")) {
                return VmState.PENDING;
            }

            if ("pending".equals(state)) {
                return VmState.PENDING;
            } else if ("running".equals(state)) {
                return VmState.RUNNING;
            } else if ("stopped".equals(state)) {
                return VmState.STOPPED;
            } else if ("suspended".equals(state)) {
                return VmState.SUSPENDED;
            } else if ("terminated".equals(state) || "ceased".equals(state)) {
                return VmState.TERMINATED;
            } else {
                return null;
            }
        }

        private RawAddress[] collectPrivateIpAddresses(List<DescribeInstancesResponseModel.Instance.Vxnet> vxnets) {
            RawAddress[] result = new RawAddress[vxnets.size()];
            for (int i = 0; i < vxnets.size(); i++) {
                DescribeInstancesResponseModel.Instance.Vxnet vxnet = vxnets.get(i);
                result[i] = new RawAddress(vxnet.getPrivateIp(), IPVersion.IPV4);
            }
            return result;
        }

        private String[] collectNetworkInterfaceIds(List<DescribeInstancesResponseModel.Instance.Vxnet> vxnets) {
            String[] result = new String[vxnets.size()];
            for (int i = 0; i < vxnets.size(); i++) {
                DescribeInstancesResponseModel.Instance.Vxnet vxnet = vxnets.get(i);
                result[i] = vxnet.getNicId();
            }
            return result;
        }

        private Volume[] listVolumes(List<String> volumeIds) {
            try {
                Volume[] result = new Volume[volumeIds.size()];
                for (int i = 0; i < volumeIds.size(); i++) {
                    String volumeId = volumeIds.get(i);
                    result[i] = getProvider().getComputeServices().getVolumeSupport().getVolume(volumeId);
                }
                return result;
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
    }
}
