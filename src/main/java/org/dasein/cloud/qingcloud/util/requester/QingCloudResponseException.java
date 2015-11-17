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

/**
 * Created by Jeffrey Yan on 11/17/2015.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
public class QingCloudResponseException extends RuntimeException {
    private int retCode;
    private String errorMessage;

    public QingCloudResponseException(int retCode, String errorMessage) {
        this.retCode = retCode;
        this.errorMessage = errorMessage;
    }

    public int getRetCode() {
        return retCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
