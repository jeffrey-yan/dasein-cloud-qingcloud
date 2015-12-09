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

import org.dasein.cloud.compute.AbstractComputeServices;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.MachineImageSupport;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.compute.VolumeSupport;
import org.dasein.cloud.qingcloud.QingCloud;

import javax.annotation.Nullable;

/**
 * Created by Jeffrey Yan on 11/24/2015.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
public class QingCloudCompute extends AbstractComputeServices<QingCloud> implements ComputeServices {

    public QingCloudCompute(QingCloud provider) {
        super(provider);
    }

    @Override
    public @Nullable VirtualMachineSupport getVirtualMachineSupport() {
        return new QingCloudVirtualMachine(getProvider());
    }

    @Override
    public @Nullable VolumeSupport getVolumeSupport() {
        return new QingCloudVolume(getProvider());
    }

    @Override
    public @Nullable MachineImageSupport getImageSupport() {
        return new QingCloudImage(getProvider());
    }
}
