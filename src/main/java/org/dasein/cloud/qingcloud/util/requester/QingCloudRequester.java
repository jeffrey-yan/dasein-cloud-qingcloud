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

import org.apache.http.client.methods.HttpUriRequest;
import org.dasein.cloud.CloudErrorType;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.qingcloud.QingCloud;
import org.dasein.cloud.qingcloud.model.ResponseModel;
import org.dasein.cloud.util.requester.DaseinRequestExecutor;
import org.dasein.cloud.util.requester.DaseinResponseHandler;
import org.dasein.cloud.util.requester.DaseinResponseHandlerWithMapper;
import org.dasein.cloud.util.requester.streamprocessors.JsonStreamToObjectProcessor;

/**
 * Created by Jeffrey Yan on 11/17/2015.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
public class QingCloudRequester<C extends ResponseModel, T> extends DaseinRequestExecutor<T> {

    public QingCloudRequester(QingCloud qingCloud, HttpUriRequest httpUriRequest, Class<T> classType) {
        super(qingCloud, QingCloudHttpClientBuilderFactory.newHttpClientBuilder(), httpUriRequest,
                new DaseinResponseHandler(new JsonStreamToObjectProcessor(), classType));
    }

    public QingCloudRequester(QingCloud qingCloud, HttpUriRequest httpUriRequest,
            QingCloudDriverToCoreMapper<C, T> mapper, Class<C> classType) {
        super(qingCloud, QingCloudHttpClientBuilderFactory.newHttpClientBuilder(), httpUriRequest,
                new DaseinResponseHandlerWithMapper(new JsonStreamToObjectProcessor(), mapper, classType));
    }

    protected CloudException translateException(Exception exception) {
        if (exception instanceof QingCloudResponseException) {
            QingCloudResponseException qingCloudResponseException = (QingCloudResponseException) exception;
            int retCode = qingCloudResponseException.getRetCode();
            String errorMessage = qingCloudResponseException.getErrorMessage();
            return new CloudException(mapErrorType(retCode), 200, Integer.toString(retCode), errorMessage);
        } else {
            return super.translateException(exception);
        }
    }

    private CloudErrorType mapErrorType(int retCode) {
        if (retCode == 1100 || retCode == 2100) {
            return CloudErrorType.COMMUNICATION;
        } else if (retCode == 1200 || retCode == 1300 || retCode == 1400) {
            return CloudErrorType.AUTHENTICATION;
        } else if (retCode == 2400) {
            return CloudErrorType.THROTTLING;
        } else if (retCode == 5200) {
            return CloudErrorType.CAPACITY;
        } else if (retCode == 2500) {
            return CloudErrorType.QUOTA;
        } else {
            return CloudErrorType.GENERAL;
        }
    }
}