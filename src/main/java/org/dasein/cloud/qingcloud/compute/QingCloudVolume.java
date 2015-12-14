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

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpUriRequest;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.Tag;
import org.dasein.cloud.compute.AbstractVolumeSupport;
import org.dasein.cloud.compute.Volume;
import org.dasein.cloud.compute.VolumeCapabilities;
import org.dasein.cloud.compute.VolumeCreateOptions;
import org.dasein.cloud.compute.VolumeFormat;
import org.dasein.cloud.compute.VolumeProduct;
import org.dasein.cloud.compute.VolumeState;
import org.dasein.cloud.compute.VolumeSupport;
import org.dasein.cloud.qingcloud.QingCloud;
import org.dasein.cloud.qingcloud.compute.model.CreateVolumeFromSnapshotResponseModel;
import org.dasein.cloud.qingcloud.compute.model.CreateVolumesResponseModel;
import org.dasein.cloud.qingcloud.compute.model.DescribeVolumesResponseModel;
import org.dasein.cloud.qingcloud.model.SimpleJobResponseModel;
import org.dasein.cloud.qingcloud.util.requester.QingCloudDriverToCoreMapper;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequestBuilder;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequester;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.cloud.util.requester.fluent.Requester;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Storage;
import org.dasein.util.uom.time.TimePeriod;
import org.dasein.util.uom.time.Week;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Jeffrey Yan on 12/8/2015.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
public class QingCloudVolume extends AbstractVolumeSupport<QingCloud> implements VolumeSupport {

    protected QingCloudVolume(QingCloud provider) {
        super(provider);
    }

    @Override
    public VolumeCapabilities getCapabilities() throws CloudException, InternalException {
        return new QingCloudVolumeCapabilities(getProvider());
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        return true;
    }

    @Override
    public @Nonnull String createVolume(@Nonnull VolumeCreateOptions options) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "Volume.createVolume");
        try {
            if (options.getSnapshotId() != null && !options.getSnapshotId().isEmpty()) {
                HttpUriRequest request = QingCloudRequestBuilder.put(getProvider())
                        .action("CreateVolumeFromSnapshot")
                        .parameter("snapshot", options.getSnapshotId())
                        .parameter("volume_name", options.getName())
                        .parameter("zone", getProvider().getZoneId())
                        .build();

                Requester<CreateVolumeFromSnapshotResponseModel> requester = new QingCloudRequester<CreateVolumeFromSnapshotResponseModel, CreateVolumeFromSnapshotResponseModel>(
                        getProvider(), request, CreateVolumeFromSnapshotResponseModel.class);
                //TODO, handle tags
                return requester.execute().getVolumeId();
            } else {
                HttpUriRequest request = QingCloudRequestBuilder.put(getProvider())
                        .action("CreateVolumes")
                        .parameter("size", options.getVolumeSize().intValue())
                        .parameter("volume_name", options.getName())
                        .parameter("volume_type", options.getVolumeProductId())
                        .parameter("count", "1")
                        .parameter("zone", getProvider().getZoneId())
                        .build();

                Requester<CreateVolumesResponseModel> requester = new QingCloudRequester<CreateVolumesResponseModel, CreateVolumesResponseModel>(
                        getProvider(), request, CreateVolumesResponseModel.class);
                //TODO, handle tags
                return requester.execute().getVolumeIds().get(0);
            }
        } finally {
            APITrace.end();
        }
    }

    @Override
    public void remove(@Nonnull String volumeId) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "Volume.remove");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.post(getProvider())
                    .action("DeleteVolumes")
                    .parameter("volumes.1", volumeId)
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
    public void attach(@Nonnull String volumeId, @Nonnull String toServer, @Nonnull String deviceId) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "Volume.attach");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.post(getProvider())
                    .action("AttachVolumes")
                    .parameter("volumes.1", volumeId)
                    .parameter("instance", toServer)
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
    public void detach(@Nonnull String volumeId, boolean force) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "Volume.detach");
        try {
            Volume volume = getVolume(volumeId);

            HttpUriRequest request = QingCloudRequestBuilder.post(getProvider())
                    .action("DetachVolumes")
                    .parameter("volumes.1", volumeId)
                    .parameter("instance", volume.getProviderVirtualMachineId()) //Is instance id required?
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
    public Volume getVolume(@Nonnull String volumeId) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "Volume.getVolume");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
                    .action("DescribeVolumes")
                    .parameter("volumes.1", volumeId)
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<List<Volume>> requester = new QingCloudRequester<DescribeVolumesResponseModel, List<Volume>>(
                    getProvider(), request, new VolumesMapper(), DescribeVolumesResponseModel.class);

            List<Volume> result = requester.execute();
            if (result.size() > 0) {
                return result.get(0);
            } else {
                return null;
            }
        } finally {
            APITrace.end();
        }
    }

    @Nonnull
    @Override
    public Iterable<Volume> listVolumes() throws InternalException, CloudException {
        APITrace.begin(getProvider(), "Volume.listVolumes");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
                    .action("DescribeVolumes")
                    .parameter("limit", "999")
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<List<Volume>> requester = new QingCloudRequester<DescribeVolumesResponseModel, List<Volume>>(
                    getProvider(), request, new VolumesMapper(), DescribeVolumesResponseModel.class);

            return requester.execute();
        } finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull Iterable<VolumeProduct> listVolumeProducts() throws InternalException, CloudException {
        ProviderContext context = getProvider().getContext();
        if (context == null) {
            throw new CloudException("No context was set for this request");
        }

        Cache<VolumeProduct> cache = Cache
                .getInstance(getProvider(), "volumeProducts", VolumeProduct.class, CacheLevel.REGION,
                        new TimePeriod<Week>(1, TimePeriod.WEEK));
        Iterable<VolumeProduct> products = cache.get(context);
        if (products == null) {
            products = loadProductsDefinition(getProvider().getZoneId());
            cache.put(context, products);
        }
        return products;
    }

    private List<VolumeProduct> loadProductsDefinition(String zoneId) throws InternalException {
        try {
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("/org/dasein/cloud/aws/volproducts.json");
            JSONArray array = new JSONArray(IOUtils.toString(inputStream));

            Map<String, List<VolumeProduct>> result = new HashMap<String, List<VolumeProduct>>();

            for (int i = 0; i < array.length(); i++) {
                JSONObject definitionOfZone = array.getJSONObject(i);

                JSONArray volumeTypesOfZone = definitionOfZone.getJSONArray("volume_types");
                List<VolumeProduct> productsOfZone = new ArrayList<VolumeProduct>();
                for (int j = 0; j < volumeTypesOfZone.length(); j++) {
                    JSONObject volumeType = volumeTypesOfZone.getJSONObject(j);
                    String id = volumeType.getString("id");
                    String name = volumeType.getString("name");

                    VolumeProduct product = VolumeProduct.getInstance(id, name, name, null);
                    product.withMinVolumeSize(new Storage<Gigabyte>(volumeType.getInt("min_size_in_gb"), Storage.GIGABYTE));
                    product.withMaxVolumeSize(new Storage<Gigabyte>(volumeType.getInt("max_size_in_gb"), Storage.GIGABYTE));
                    productsOfZone.add(product);
                }
                result.put(definitionOfZone.getString("zone"), productsOfZone);
            }

            if (result.containsKey(zoneId)) {
                return result.get(zoneId);
            } else {
                return result.get("pek2");//this is the latest Zone, suppose new Zone will same as this one
            }
        } catch (Exception exception) {
            throw new InternalException("Not able to load vm product definition", exception);
        }
    }

    @Override
    public void removeTags(@Nonnull String volumeId, @Nonnull Tag... tags) throws CloudException, InternalException {
        //TODO
    }

    @Override
    public void updateTags(@Nonnull String volumeId, @Nonnull Tag... tags) throws CloudException, InternalException {
        //TODO
    }

    private class VolumesMapper extends QingCloudDriverToCoreMapper<DescribeVolumesResponseModel, List<Volume>> {
        @Override
        protected List<Volume> doMapFrom(DescribeVolumesResponseModel responseModel) {
            try {
                List<Volume> volumes = new ArrayList<Volume>();

                for(DescribeVolumesResponseModel.Volume volumeModel : responseModel.getVolumes()) {
                    Volume volume = new Volume();
                    volume.setProviderVolumeId(volumeModel.getVolumeId());
                    volume.setName(volumeModel.getVolumeName());
                    volume.setCreationTimestamp(getProvider().parseIso8601Date(volumeModel.getCreateTime()).getTime());
                    volume.setDescription(volumeModel.getDescription());
                    volume.setSize(new Storage<Gigabyte>(volumeModel.getSize(), Storage.GIGABYTE));
                    volume.setProviderVirtualMachineId(volumeModel.getInstance().getInstanceId());
                    volume.setCurrentState(mapVolumeState(volumeModel.getStatus(), volumeModel.getTransitionStatus()));

                    volume.setProviderRegionId(getContext().getRegionId());
                    volume.setProviderDataCenterId(getProvider().getZoneId());
                    volume.setFormat(VolumeFormat.BLOCK);
                    //TODO, set tags
                    volumes.add(volume);
                }

                return volumes;
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }

        private VolumeState mapVolumeState(String state, String transitionState) {
            if (transitionState != null && !transitionState.equals("")) {
                return VolumeState.PENDING;
            }

            if ("pending".equals(state)) {
                return VolumeState.PENDING;
            } else if ("available".equals(state) || "in-use".equals(state)) {
                return VolumeState.AVAILABLE;
            } else if ("deleted".equals(state) || "ceased".equals(state)) {
                return VolumeState.DELETED;
            } else if ("suspended".equals(state)) {
                return VolumeState.ERROR;
            } else {
                return null;
            }
        }
    }
}
