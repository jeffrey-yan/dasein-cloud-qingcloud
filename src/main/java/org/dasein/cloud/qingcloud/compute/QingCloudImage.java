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
import org.dasein.cloud.AsynchronousTask;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Tag;
import org.dasein.cloud.VisibleScope;
import org.dasein.cloud.compute.AbstractImageSupport;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.ImageCapabilities;
import org.dasein.cloud.compute.ImageClass;
import org.dasein.cloud.compute.ImageCreateOptions;
import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.MachineImageFormat;
import org.dasein.cloud.compute.MachineImageState;
import org.dasein.cloud.compute.MachineImageSupport;
import org.dasein.cloud.compute.MachineImageType;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.qingcloud.QingCloud;
import org.dasein.cloud.qingcloud.compute.model.CaptureInstanceResponseModel;
import org.dasein.cloud.qingcloud.compute.model.DescribeImageUsersResponseModel;
import org.dasein.cloud.qingcloud.compute.model.DescribeImagesResponseModel;
import org.dasein.cloud.qingcloud.model.ResponseModel;
import org.dasein.cloud.qingcloud.model.SimpleJobResponseModel;
import org.dasein.cloud.qingcloud.util.requester.QingCloudDriverToCoreMapper;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequestBuilder;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequester;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.requester.fluent.Requester;
import org.dasein.util.CalendarWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Jeffrey Yan on 12/9/2015.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
public class QingCloudImage extends AbstractImageSupport<QingCloud> implements MachineImageSupport {

    protected QingCloudImage(QingCloud provider) {
        super(provider);
    }

    @Override
    public ImageCapabilities getCapabilities() throws CloudException, InternalException {
        return new QingCloudImageCapabilities(getProvider());
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        return true;
    }

    @Override
    protected MachineImage capture(@Nonnull ImageCreateOptions options, @Nullable AsynchronousTask<MachineImage> task) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "Image.capture");
        try {
            VirtualMachine vm = getProvider().getComputeServices().getVirtualMachineSupport().getVirtualMachine(options.getVirtualMachineId());
            if( vm == null ) {
                throw new CloudException("Virtual machine not found: " + options.getVirtualMachineId());
            }

            if( !getCapabilities().canImage(vm.getCurrentState()) ) {
                throw new CloudException("Server must be stopped before making an image - current state: " + vm.getCurrentState());
            }

            if( task != null ) {
                task.setStartTime(System.currentTimeMillis());
            }

            HttpUriRequest request = QingCloudRequestBuilder.post(getProvider())
                    .action("CaptureInstance")
                    .parameter("instance", options.getVirtualMachineId())
                    .parameter("image_name", options.getName())
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<CaptureInstanceResponseModel> requester = new QingCloudRequester<CaptureInstanceResponseModel, CaptureInstanceResponseModel>(
                    getProvider(), request, CaptureInstanceResponseModel.class);

            CaptureInstanceResponseModel responseModel = requester.execute();

            MachineImage image = getImage(responseModel.getImageId());

            //TODO, handle options.getReboot() option
            if( task != null ) {
                task.completeWithResult(image);
            }
            return image;
        } finally {
            APITrace.end();
        }
    }

    @Override
    public void remove(@Nonnull String providerImageId, boolean checkState) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "Image.remove");
        try {
            if (checkState) {
                long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 30L);

                while (timeout > System.currentTimeMillis()) {
                    try {
                        MachineImage img = getMachineImage(providerImageId);

                        if (img == null || MachineImageState.DELETED.equals(img.getCurrentState())) {
                            return;
                        }
                        if (MachineImageState.ACTIVE.equals(img.getCurrentState())) {
                            break;
                        }
                    } catch (Throwable ignore) {
                        // ignore
                    }
                    try {
                        Thread.sleep(15000L);
                    } catch (InterruptedException ignore) {
                    }
                }
            }

            HttpUriRequest request = QingCloudRequestBuilder.post(getProvider())
                    .action("DeleteImages")
                    .parameter("images.1", providerImageId)
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<SimpleJobResponseModel> requester = new QingCloudRequester<SimpleJobResponseModel, SimpleJobResponseModel>(
                    getProvider(), request, SimpleJobResponseModel.class);

            requester.execute();
        } finally {
            APITrace.end();
        }
    }

    @Nullable
    @Override
    public MachineImage getImage(@Nonnull String providerImageId) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "Image.getImage");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
                    .action("DescribeImages")
                    .parameter("images.1", providerImageId)
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<List<MachineImage>> requester = new QingCloudRequester<DescribeImagesResponseModel, List<MachineImage>>(
                    getProvider(), request, new ImagesMapper(), DescribeImagesResponseModel.class);

            List<MachineImage> result = requester.execute();
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
    public Iterable<MachineImage> listImages(@Nullable ImageFilterOptions options) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "Image.listImages");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
                    .action("DescribeImages")
                    .parameter("visibility", "private")
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<List<MachineImage>> requester = new QingCloudRequester<DescribeImagesResponseModel, List<MachineImage>>(
                    getProvider(), request, new ImagesMapper(), DescribeImagesResponseModel.class);

            List<MachineImage> result = new ArrayList<MachineImage>();

            for (MachineImage machineImage : requester.execute()) {
                if (options.matches(machineImage)) {
                    result.add(machineImage);
                }
            }

            return result;
        } finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull Iterable<MachineImage> searchPublicImages(@Nonnull ImageFilterOptions options) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "Image.searchPublicImages");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
                    .action("DescribeImages")
                    .parameter("visibility", "public")
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<List<MachineImage>> requester = new QingCloudRequester<DescribeImagesResponseModel, List<MachineImage>>(
                    getProvider(), request, new ImagesMapper(), DescribeImagesResponseModel.class);

            List<MachineImage> result = new ArrayList<MachineImage>();

            for (MachineImage machineImage : requester.execute()) {
                if (options.matches(machineImage)) {
                    result.add(machineImage);
                }
            }

            return result;
        } finally {
            APITrace.end();
        }
    }

    @Override
    public void addImageShare(@Nonnull String providerImageId, @Nonnull String accountNumber) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "Image.addImageShare");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.post(getProvider())
                    .action("GrantImageToUsers")
                    .parameter("image", providerImageId)
                    .parameter("users.1", accountNumber)
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<ResponseModel> requester = new QingCloudRequester<ResponseModel, ResponseModel>(getProvider(),
                    request, ResponseModel.class);

            requester.execute();
        } finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull Iterable<String> listShares(@Nonnull String providerImageId) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "Image.listShares");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
                    .action("DescribeImageUsers")
                    .parameter("image_id", providerImageId)
                    .parameter("offset", "0")
                    .parameter("limit", "999")
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<List<String>> requester = new QingCloudRequester<DescribeImageUsersResponseModel, List<String>>(
                    getProvider(), request,
                    new QingCloudDriverToCoreMapper<DescribeImageUsersResponseModel, List<String>>() {
                        @Override
                        protected List<String> doMapFrom(DescribeImageUsersResponseModel responseModel) {
                            List<String> result = new ArrayList<String>();
                            for (DescribeImageUsersResponseModel.ImageUser imageUser : responseModel.getImageUsers()) {
                                result.add(imageUser.getUser().getUserId());
                            }
                            return result;
                        }
                    }, DescribeImageUsersResponseModel.class);


            return requester.execute();
        } finally {
            APITrace.end();
        }
    }

    @Override
    public void removeAllImageShares(@Nonnull String providerImageId) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "Image.removeAllImageShares");
        try {
            Iterable<String> shares = listShares(providerImageId);

            QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.post(getProvider())
                    .action("RevokeImageFromUsers")
                    .parameter("image", providerImageId)
                    .parameter("zone", getProvider().getZoneId());

            Iterator<String> sharesIterator = shares.iterator();
            int index = 1;
            while (sharesIterator.hasNext()) {
                requestBuilder.parameter("users." + (index++), sharesIterator.next());
            }

            Requester<ResponseModel> requester = new QingCloudRequester<ResponseModel, ResponseModel>(getProvider(),
                    requestBuilder.build(), ResponseModel.class);

            requester.execute();
        } finally {
            APITrace.end();
        }
    }

    @Override
    public void removeImageShare(@Nonnull String providerImageId, @Nonnull String accountNumber) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "Image.removeImageShare");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.post(getProvider())
                    .action("RevokeImageFromUsers")
                    .parameter("image", providerImageId)
                    .parameter("users.1", accountNumber)
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<ResponseModel> requester = new QingCloudRequester<ResponseModel, ResponseModel>(getProvider(),
                    request, ResponseModel.class);

            requester.execute();
        } finally {
            APITrace.end();
        }
    }

    @Override
    public void updateTags(@Nonnull String imageId, @Nonnull Tag... tags) throws CloudException, InternalException {
        //TODO
    }

    @Override
    public void removeTags(@Nonnull String imageId, @Nonnull Tag ... tags) throws CloudException, InternalException {
        //TODO
    }

    private class ImagesMapper extends QingCloudDriverToCoreMapper<DescribeImagesResponseModel, List<MachineImage>> {

        @Override
        protected List<MachineImage> doMapFrom(DescribeImagesResponseModel responseModel) {
            try {
                List<MachineImage> images = new ArrayList<MachineImage>();

                for(DescribeImagesResponseModel.Image imageModel : responseModel.getImages()) {
                    String ownerId = mapOwner(imageModel.getProvider(), imageModel.getOwner());
                    String regionId = getContext().getRegionId();
                    MachineImageState imageState = mapImageState(imageModel.getStatus(),
                            imageModel.getTransitionStatus());
                    Architecture architecture = mapArchitecture(imageModel.getProcessorType());
                    Platform platform = Platform.guess(imageModel.getOsFamily());

                    MachineImage machineImage = MachineImage.getInstance(ownerId, regionId, imageModel.getImageId(),
                            ImageClass.MACHINE, imageState, imageModel.getImageName(), imageModel.getDescription(),
                            architecture, platform);
                    machineImage.constrainedTo(getProvider().getZoneId());
                    machineImage.createdAt(getProvider().parseIso8601Date(imageModel.getCreateTime()).getTime());
                    machineImage.setMinimumDiskSizeGb(imageModel.getSize());

                    machineImage.withStorageFormat(MachineImageFormat.VHD);
                    machineImage.withType(MachineImageType.VOLUME);
                    machineImage.withVisibleScope(VisibleScope.ACCOUNT_DATACENTER);
                    //TODO, set tags

                    images.add(machineImage);
                }

                return images;
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

        private String mapOwner(String provider, String owner) {
            if ("system".equals(provider)) {
                return provider;
            } else { //self or may be other
                return owner;
            }
        }

        private MachineImageState mapImageState(String state, String transitionState) {
            if (transitionState != null && !transitionState.equals("")) {
                return MachineImageState.PENDING;
            }

            if ("pending".equals(state)) {
                return MachineImageState.PENDING;
            } else if ("available".equals(state)) {
                return MachineImageState.ACTIVE;
            } else if ("deprecated".equals(state) || "suspended".equals(state)) {
                return MachineImageState.ERROR;
            } else if ("deleted".equals(state) || "ceased".equals(state)) {
                return MachineImageState.DELETED;
            } else {
                return null;
            }
        }
    }
}
