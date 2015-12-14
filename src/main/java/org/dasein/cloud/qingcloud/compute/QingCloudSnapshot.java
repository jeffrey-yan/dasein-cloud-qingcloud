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

import org.apache.http.client.methods.HttpUriRequest;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Tag;
import org.dasein.cloud.VisibleScope;
import org.dasein.cloud.compute.AbstractSnapshotSupport;
import org.dasein.cloud.compute.Snapshot;
import org.dasein.cloud.compute.SnapshotCapabilities;
import org.dasein.cloud.compute.SnapshotCreateOptions;
import org.dasein.cloud.compute.SnapshotState;
import org.dasein.cloud.compute.SnapshotSupport;
import org.dasein.cloud.qingcloud.QingCloud;
import org.dasein.cloud.qingcloud.compute.model.CreateSnapshotsResponseModel;
import org.dasein.cloud.qingcloud.compute.model.DescribeSnapshotsResponseModel;
import org.dasein.cloud.qingcloud.model.SimpleJobResponseModel;
import org.dasein.cloud.qingcloud.util.requester.QingCloudDriverToCoreMapper;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequestBuilder;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequester;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.requester.fluent.Requester;
import org.dasein.util.uom.storage.Megabyte;
import org.dasein.util.uom.storage.Storage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jeffrey Yan on 12/11/2015.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
public class QingCloudSnapshot extends AbstractSnapshotSupport<QingCloud> implements SnapshotSupport {

    protected QingCloudSnapshot(QingCloud provider) {
        super(provider);
    }

    @Nonnull
    @Override
    public SnapshotCapabilities getCapabilities() throws CloudException, InternalException {
        return new QingCloudSnapshotCapabilities(getProvider());
    }

    @Override
    public boolean isSubscribed() throws InternalException, CloudException {
        return true;
    }

    @Override
    public @Nullable String createSnapshot(@Nonnull SnapshotCreateOptions options) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "Snapshot.createSnapshot");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.put(getProvider())
                    .action("CreateSnapshots")
                    .parameter("resources.1", options.getVolumeId())
                    .parameter("snapshot_name", options.getName())
                    .parameter("is_full", "1") //always create full snapshot, incremental snapshot not works as dasein expected as dasein must support delete snapshot independently
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<CreateSnapshotsResponseModel> requester = new QingCloudRequester<CreateSnapshotsResponseModel, CreateSnapshotsResponseModel>(
                    getProvider(), request, CreateSnapshotsResponseModel.class);
            //TODO, handle tags
            return requester.execute().getSnapshots().get(0);
        } finally {
            APITrace.end();
        }
    }

    @Override
    public void remove(@Nonnull String snapshotId) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "Snapshot.createSnapshot");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.post(getProvider())
                    .action("DeleteSnapshots")
                    .parameter("snapshots.1", snapshotId)
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
    public @Nullable Snapshot getSnapshot(@Nonnull String snapshotId) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "Snapshot.getSnapshot");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
                    .action("DescribeSnapshots")
                    .parameter("snapshots.1", snapshotId)
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<List<Snapshot>> requester = new QingCloudRequester<DescribeSnapshotsResponseModel, List<Snapshot>>(
                    getProvider(), request, new SnapshotsMapper(), DescribeSnapshotsResponseModel.class);

            List<Snapshot> result = requester.execute();
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
    public Iterable<Snapshot> listSnapshots() throws InternalException, CloudException {
        APITrace.begin(getProvider(), "Volume.listVolumes");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
                    .action("DescribeSnapshots")
                    .parameter("limit", "999")
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<List<Snapshot>> requester = new QingCloudRequester<DescribeSnapshotsResponseModel, List<Snapshot>>(
                    getProvider(), request, new SnapshotsMapper(), DescribeSnapshotsResponseModel.class);

            return requester.execute();
        } finally {
            APITrace.end();
        }
    }

    @Override
    public void removeTags(@Nonnull String snapshotId, @Nonnull Tag... tags) throws CloudException, InternalException {
        //TODO, set tags
    }

    @Override
    public void updateTags(@Nonnull String snapshotId, @Nonnull Tag... tags) throws CloudException, InternalException {
        //TODO, set tags
    }

    private class SnapshotsMapper extends QingCloudDriverToCoreMapper<DescribeSnapshotsResponseModel, List<Snapshot>> {

        @Override
        protected List<Snapshot> doMapFrom(DescribeSnapshotsResponseModel responseModel) {
            try {
                List<Snapshot> snapshots = new ArrayList<Snapshot>();

                for(DescribeSnapshotsResponseModel.Snapshot snapshotModel: responseModel.getSnapshots()) {
                    Snapshot snapshot = new Snapshot();
                    snapshot.setCurrentState(
                            mapSnapshotState(snapshotModel.getStatus(), snapshotModel.getTransitionStatus()));
                    snapshot.setDescription(snapshotModel.getDescription());
                    snapshot.setName(snapshotModel.getSnapshotName());
                    snapshot.setProviderSnapshotId(snapshotModel.getSnapshotId());
                    snapshot.setRegionId(getContext().getRegionId());
                    snapshot.setSnapshotTimestamp(
                            getProvider().parseIso8601Date(snapshotModel.getSnapshotTime()).getTime());
                    snapshot.setVolumeId(snapshotModel.getResource().getResourceId());
                    snapshot.setOwner(getContext().getAccountNumber());
                    snapshot.setSizeInGb(
                            new Storage<Megabyte>(snapshotModel.getSize(), Storage.MEGABYTE).convertTo(Storage.GIGABYTE)
                                    .intValue());
                    if (snapshotModel.getIsTaken() == 1) {
                        snapshot.setProgress("100%");
                    } else {
                        snapshot.setProgress("0%");
                    }

                    snapshot.setVisibleScope(VisibleScope.ACCOUNT_DATACENTER);
                    //TODO, set tags
                    snapshots.add(snapshot);
                }

                return snapshots;
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }

        private SnapshotState mapSnapshotState(String state, String transitionState) {
            if (transitionState != null && !transitionState.equals("")) {
                return SnapshotState.PENDING;
            }

            if ("pending".equals(state)) {
                return SnapshotState.PENDING;
            } else if ("available".equals(state)) {
                return SnapshotState.AVAILABLE;
            } else if ("deleted".equals(state) || "ceased".equals(state)) {
                return SnapshotState.DELETED;
            } else if ("suspended".equals(state)) {
                return SnapshotState.ERROR;
            } else {
                return null;
            }
        }
    }
}
