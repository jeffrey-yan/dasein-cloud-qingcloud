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

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.AbstractVMSupport;
import org.dasein.cloud.compute.VMLaunchOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineCapabilities;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.qingcloud.QingCloud;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
        return null;
    }

    @Override
    public void terminate(@Nonnull String vmId, @Nullable String explanation) throws InternalException, CloudException {

    }
}
