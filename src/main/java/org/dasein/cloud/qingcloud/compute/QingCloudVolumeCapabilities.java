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

import org.dasein.cloud.AbstractCapabilities;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.compute.VolumeCapabilities;
import org.dasein.cloud.compute.VolumeFormat;
import org.dasein.cloud.qingcloud.QingCloud;
import org.dasein.cloud.util.NamingConstraints;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Storage;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

/**
 * Created by Jeffrey Yan on 12/8/2015.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
public class QingCloudVolumeCapabilities extends AbstractCapabilities<QingCloud> implements VolumeCapabilities {

    public QingCloudVolumeCapabilities(@Nonnull QingCloud provider) {
        super(provider);
    }

    @Override
    public boolean canAttach(VmState vmState) throws InternalException, CloudException {
        return vmState == VmState.RUNNING || vmState == VmState.STOPPED;
    }

    @Override
    public boolean canDetach(VmState vmState) throws InternalException, CloudException {
        return true;
    }

    @Override
    public int getMaximumVolumeCount() throws InternalException, CloudException {
        return LIMIT_UNKNOWN;
    }

    @Override
    public int getMaximumVolumeSizeIOPS() throws InternalException, CloudException {
        return 0;
    }

    @Override
    public int getMinimumVolumeSizeIOPS() throws InternalException, CloudException {
        return 0;
    }

    @Nonnull
    @Override
    public Storage<Gigabyte> getMaximumVolumeSize() throws InternalException, CloudException {
        return new Storage<Gigabyte>(500, Storage.GIGABYTE);
    }

    @Nonnull
    @Override
    public Storage<Gigabyte> getMinimumVolumeSize() throws InternalException, CloudException {
        return new Storage<Gigabyte>(10, Storage.GIGABYTE);
    }

    @Nonnull
    @Override
    public NamingConstraints getVolumeNamingConstraints() throws CloudException, InternalException {
        return NamingConstraints.getAlphaNumeric(2, 128);//Document doesn't describe any constraints
    }

    @Nonnull
    @Override
    public String getProviderTermForVolume(@Nonnull Locale locale) {
        return "volume";
    }

    @Nonnull
    @Override
    public Requirement getVolumeProductRequirement() throws InternalException, CloudException {
        return Requirement.OPTIONAL;
    }

    @Nonnull
    @Override
    public Requirement getDeviceIdOnAttachRequirement() throws InternalException, CloudException {
        return Requirement.NONE;
    }

    @Override
    public boolean supportsIOPSVolumes() throws InternalException, CloudException {
        return false;
    }

    @Override
    public boolean isVolumeSizeDeterminedByProduct() throws InternalException, CloudException {
        return false;
    }

    @Nonnull
    @Override
    public Iterable<String> listPossibleDeviceIds(@Nonnull Platform platform) throws InternalException, CloudException {
        return Collections.EMPTY_LIST;
    }

    @Nonnull
    @Override
    public Iterable<VolumeFormat> listSupportedFormats() throws InternalException, CloudException {
        return Arrays.asList(VolumeFormat.BLOCK);
    }

    @Nonnull
    @Override
    public Requirement requiresVMOnCreate() throws InternalException, CloudException {
        return Requirement.NONE;
    }

    @Override
    public boolean supportsAttach() {
        return true;
    }

    @Override
    public boolean supportsDetach() {
        return true;
    }
}
