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

package org.dasein.cloud.qingcloud.util.requester;

import org.dasein.cloud.qingcloud.model.ResponseModel;
import org.dasein.cloud.util.requester.DriverToCoreMapper;

/**
 * Created by Jeffrey Yan on 11/17/2015.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
public abstract class QingCloudDriverToCoreMapper<T extends ResponseModel, V> implements DriverToCoreMapper<T, V> {
    @Override
    public V mapFrom(T responseModel) {
        if(responseModel.getRetCode() != 0) {
            throw new QingCloudResponseException(responseModel.getRetCode(), responseModel.getErrorMessage());
        }
        return doMapFrom(responseModel);
    }

    protected abstract V doMapFrom(T responseModel);
}
