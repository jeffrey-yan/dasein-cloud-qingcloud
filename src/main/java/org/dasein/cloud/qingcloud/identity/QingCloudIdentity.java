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

package org.dasein.cloud.qingcloud.identity;

import org.dasein.cloud.identity.AbstractIdentityServices;
import org.dasein.cloud.identity.IdentityServices;
import org.dasein.cloud.identity.ShellKeySupport;
import org.dasein.cloud.qingcloud.QingCloud;

import javax.annotation.Nullable;

/**
 * Created by Jeffrey Yan on 12/14/2015.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
public class QingCloudIdentity extends AbstractIdentityServices<QingCloud> implements IdentityServices {

    public QingCloudIdentity(QingCloud provider) {
        super(provider);
    }

    @Override
    @Nullable
    public ShellKeySupport getShellKeySupport() {
        return new QingCloudShellKey(getProvider());
    }
}
