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
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.Tag;
import org.dasein.cloud.compute.AbstractVMSupport;
import org.dasein.cloud.compute.VMFilterOptions;
import org.dasein.cloud.compute.VMLaunchOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineCapabilities;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.VirtualMachineProductFilterOptions;
import org.dasein.cloud.compute.VirtualMachineStatus;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.compute.VmStatistics;
import org.dasein.cloud.compute.VmStatusFilterOptions;
import org.dasein.cloud.compute.VolumeAttachment;
import org.dasein.cloud.network.NICCreateOptions;
import org.dasein.cloud.qingcloud.QingCloud;
import org.dasein.cloud.qingcloud.compute.model.RunInstancesResponseModel;
import org.dasein.cloud.qingcloud.model.SimpleJobResponseModel;
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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
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
    public void reboot( @Nonnull String vmId ) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "VirtualMachine.reboot");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
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
            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
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
            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
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
            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
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
    public @Nonnull VirtualMachine alterVirtualMachineProduct(@Nonnull String virtualMachineId, @Nonnull String productId) throws InternalException, CloudException{
        APITrace.begin(getProvider(), "VirtualMachine.alterVirtualMachineProduct");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
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
            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
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
    public @Nonnull VirtualMachine alterVirtualMachineFirewalls(@Nonnull String virtualMachineId, @Nonnull String[] firewalls) throws InternalException, CloudException{
        throw new OperationNotSupportedException("Instance firewall modifications are not currently supported for " + getProvider().getCloudName());
    }

    @Override
    public @Nullable VirtualMachine getVirtualMachine( @Nonnull String vmId ) throws InternalException, CloudException {
        for( VirtualMachine vm : listVirtualMachines(null) ) {
            if( vm.getProviderVirtualMachineId().equals(vmId) ) {
                return vm;
            }
        }
        return null;
    }

    @Override
    public @Nonnull VmStatistics getVMStatistics( @Nonnull String vmId, @Nonnegative long from, @Nonnegative long to ) throws InternalException, CloudException {
        return new VmStatistics();
    }

    @Override
    public @Nonnull Iterable<VmStatistics> getVMStatisticsForPeriod( @Nonnull String vmId, @Nonnegative long from, @Nonnegative long to ) throws InternalException, CloudException {
        return Collections.emptyList();
    }


    @Override
    public @Nonnull Iterable<String> listFirewalls( @Nonnull String vmId ) throws InternalException, CloudException {
        return Collections.emptyList();
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
                        new TimePeriod<Week>(999, TimePeriod.WEEK));
        Iterable<VirtualMachineProduct> products = cache.get(context);
        if (products == null) {
            products = loadProductsDefintion(getProvider().getZoneId());
            cache.put(context, products);
        }
        return products;
    }

    private List<VirtualMachineProduct> loadProductsDefintion(String zoneId) throws InternalException {
        try {
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("/org/dasein/cloud/aws/vmproducts.json");
            JSONArray array = new JSONArray(IOUtils.toString(inputStream));

            Map<String, List<VirtualMachineProduct>> productsOfZonesMap = new HashMap<String, List<VirtualMachineProduct>>();

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
                productsOfZonesMap.put(definitionOfZone.getString("zone"), productsOfZone);
            }

            if (productsOfZonesMap.containsKey(zoneId)) {
                return productsOfZonesMap.get(zoneId);
            } else {
                return productsOfZonesMap.get("default");
            }
        } catch (Exception exception) {
            throw new InternalException("Not able to load vm product definition", exception);
        }
    }


    @Override
    public @Nonnull Iterable<ResourceStatus> listVirtualMachineStatus() throws InternalException, CloudException {
        List<ResourceStatus> status = new ArrayList<ResourceStatus>();

        for( VirtualMachine vm : listVirtualMachines() ) {
            status.add(new ResourceStatus(vm.getProviderVirtualMachineId(), vm.getCurrentState()));
        }
        return status;
    }

    @Override
    public @Nonnull Iterable<VirtualMachine> listVirtualMachines() throws InternalException, CloudException {
        return Collections.<VirtualMachine>emptyList();
    }

    @Override
    public @Nonnull Iterable<VirtualMachine> listVirtualMachines( @Nullable VMFilterOptions options ) throws InternalException, CloudException {
        if( options == null ) {
            return listVirtualMachines();
        }
        List<VirtualMachine> vms = new ArrayList<VirtualMachine>();

        for( VirtualMachine vm : listVirtualMachines() ) {
            if( options.matches(vm) ) {
                vms.add(vm);
            }
        }
        return vms;
    }

    @Override
    public void updateTags( @Nonnull String vmId, @Nonnull Tag... tags ) throws CloudException, InternalException {
        // NO-OP
    }

    @Override
    public void removeTags( @Nonnull String vmId, @Nonnull Tag... tags ) throws CloudException, InternalException {
        // NO-OP
    }

    @Override
    public @Nullable Iterable<VirtualMachineStatus> getVMStatus( @Nullable String... vmIds ) throws InternalException, CloudException {
        throw new OperationNotSupportedException("Virtual Machine Status is not currently implemented for " + getProvider().getCloudName());
    }

    @Override
    public @Nullable Iterable<VirtualMachineStatus> getVMStatus( @Nullable VmStatusFilterOptions filterOptions ) throws InternalException, CloudException {
        throw new OperationNotSupportedException("Virtual Machine Status is not currently implemented for " + getProvider().getCloudName());
    }
}
