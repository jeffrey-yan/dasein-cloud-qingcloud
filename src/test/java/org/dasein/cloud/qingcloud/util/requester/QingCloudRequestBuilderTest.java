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

import mockit.Mocked;
import mockit.NonStrictExpectations;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.qingcloud.QingCloud;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by Jeffrey Yan on 11/13/2015.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
public class QingCloudRequestBuilderTest {

    protected final String ACCESS_KEY_ID = "QYACCESSKEYIDEXAMPLE";
    protected final String ACCESS_KEY_SECRET = "SECRETACCESSKEY";


    @Mocked protected QingCloud qingCloud;

    @Before
    public void setUp() throws CloudException, InternalException {
        new NonStrictExpectations() {{
            byte[][] accessKey = new byte[][]{ACCESS_KEY_ID.getBytes(), ACCESS_KEY_SECRET.getBytes()};
            qingCloud.getContext().getConfigurationValue(QingCloud.DSN_ACCESS_KEY); result = accessKey;
        }};
    }

    @Test
    public void computeSignatureShouldBeCorrect() throws InternalException {
        QingCloudRequestBuilder cloudRequestBuilder = QingCloudRequestBuilder.get(qingCloud);
        cloudRequestBuilder.action("RunInstances");
        cloudRequestBuilder.parameter("count", 1);
        cloudRequestBuilder.parameter("image_id", "centos64x86a");
        cloudRequestBuilder.parameter("instance_name", "demo");
        cloudRequestBuilder.parameter("instance_type", "small_b");
        cloudRequestBuilder.parameter("login_mode", "passwd");
        cloudRequestBuilder.parameter("login_passwd", "QingCloud20130712");
        cloudRequestBuilder.parameter("vxnets.1", "vxnet-0");
        cloudRequestBuilder.parameter("zone", "pek1");

        //overwrite
        cloudRequestBuilder.parameter("time_stamp", "2013-08-27T14:30:10Z");

        String signature = cloudRequestBuilder.signature();

        assertEquals("32bseYy39DOlatuewpeuW5vpmW51sD1A/JdGynqSpP8=", signature);

    }
}
