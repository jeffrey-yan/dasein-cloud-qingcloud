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

import org.dasein.cloud.AsynchronousTask;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.Tag;
import org.dasein.cloud.compute.AbstractImageSupport;
import org.dasein.cloud.compute.ImageCapabilities;
import org.dasein.cloud.compute.ImageCreateOptions;
import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.MachineImageSupport;
import org.dasein.cloud.qingcloud.QingCloud;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;

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
        throw new OperationNotSupportedException("Image capture is not currently implemented in " + getProvider().getCloudName());
    }

    @Override
    public void remove(@Nonnull String providerImageId, boolean checkState) throws CloudException, InternalException {

    }

    @Nullable
    @Override
    public MachineImage getImage(@Nonnull String providerImageId) throws CloudException, InternalException {
        return null;
    }

    @Nonnull
    @Override
    public Iterable<MachineImage> listImages(@Nullable ImageFilterOptions options)
            throws CloudException, InternalException {
        return null;
    }

    @Override
    public @Nonnull Iterable<MachineImage> searchPublicImages(@Nonnull ImageFilterOptions options) throws CloudException, InternalException {
        return Collections.emptyList();
    }

    @Override
    public void addImageShare(@Nonnull String providerImageId, @Nonnull String accountNumber) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Support for image sharing is not currently implemented in " + getProvider().getCloudName());
    }

    @Override
    public @Nonnull Iterable<String> listShares(@Nonnull String providerImageId) throws CloudException, InternalException {
        return Collections.emptyList();
    }

    @Override
    public void removeAllImageShares(@Nonnull String providerImageId) throws CloudException, InternalException {
        // NO-OP (does not error even when not supported)
    }

    @Override
    public void removeImageShare(@Nonnull String providerImageId, @Nonnull String accountNumber) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Image sharing is not currently implemented in " + getProvider().getCloudName());
    }

    @Override
    public void updateTags(@Nonnull String imageId, @Nonnull Tag... tags) throws CloudException, InternalException {
        //TODO
    }

    @Override
    public void removeTags(@Nonnull String imageId, @Nonnull Tag ... tags) throws CloudException, InternalException {
        //TODO
    }

}
