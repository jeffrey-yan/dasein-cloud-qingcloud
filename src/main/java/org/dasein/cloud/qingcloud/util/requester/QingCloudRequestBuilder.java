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

import org.apache.commons.codec.binary.Base64;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.log4j.Logger;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.qingcloud.QingCloud;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jeffrey Yan on 11/12/2015.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
public class QingCloudRequestBuilder {
    static private final Logger logger = QingCloud
            .getStdLogger(org.dasein.cloud.qingcloud.util.requester.QingCloudRequestBuilder.class);

    static private final String SIGNATURE_ALGORITHM = "HmacSHA256";
    static private final String ENCODING = "UTF-8";

    protected RequestBuilder requestBuilder;

    protected QingCloud qingCloud;

    private QingCloudRequestBuilder(QingCloud qingCloud, RequestBuilder requestBuilder) {
        this.qingCloud = qingCloud;
        this.requestBuilder = requestBuilder;

        byte[][] accessKey = (byte[][]) qingCloud.getContext().getConfigurationValue(QingCloud.DSN_ACCESS_KEY);

        requestBuilder.setVersion(new ProtocolVersion("HTTP", 1, 1));

        requestBuilder.setUri("https://api.qingcloud.com/iaas/");

        parameter("time_stamp", qingCloud.formatIso8601Date(new Date()));
        parameter("access_key_id", new String(accessKey[0]));
        parameter("version", "1");
        parameter("signature_method", SIGNATURE_ALGORITHM);
        parameter("signature_version", "1");
    }

    public static QingCloudRequestBuilder head(QingCloud qingCloud) {
        return new QingCloudRequestBuilder(qingCloud, RequestBuilder.head());
    }

    public static QingCloudRequestBuilder get(QingCloud qingCloud) {
        return new QingCloudRequestBuilder(qingCloud, RequestBuilder.get());
    }

    public static QingCloudRequestBuilder post(QingCloud qingCloud) {
        return new QingCloudRequestBuilder(qingCloud, RequestBuilder.post());
    }

    public static QingCloudRequestBuilder put(QingCloud qingCloud) {
        return new QingCloudRequestBuilder(qingCloud, RequestBuilder.put());
    }

    public static QingCloudRequestBuilder delete(QingCloud qingCloud) {
        return new QingCloudRequestBuilder(qingCloud, RequestBuilder.delete());
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof Date) {
            return qingCloud.formatIso8601Date((Date) value);
        } else {
            return value.toString();
        }
    }

    public QingCloudRequestBuilder action(String action) {
        return parameter("action", action);
    }

    public QingCloudRequestBuilder parameter(final String name, final Object value) {
        requestBuilder.addParameter(name, asString(value));
        return this;
    }

    public QingCloudRequestBuilder parameterIfNotNull(final String name, final Object value) {
        if (value != null) {
            requestBuilder.addParameter(name, asString(value));
        }
        return this;
    }

    public HttpUriRequest build() throws InternalException {
        parameter("signature", signature());
        return requestBuilder.build();
    }

    protected String signature() throws InternalException {
        Map<String, String> requestParameters = new HashMap<String, String>();
        for (NameValuePair nameValuePair : requestBuilder.getParameters()) {
            requestParameters.put(nameValuePair.getName(), nameValuePair.getValue());
        }

        String[] sortedKeys = requestParameters.keySet().toArray(new String[]{});
        Arrays.sort(sortedKeys);
        StringBuilder canonicalStringBuilder = new StringBuilder();
        for(String key : sortedKeys) {
            canonicalStringBuilder.append("&").append(urlEncode(key)).append("=")
                    .append(urlEncode(requestParameters.get(key)));
        }
        String canonicalString = canonicalStringBuilder.toString().substring(1);

        StringBuilder stringToSign = new StringBuilder();
        stringToSign.append(requestBuilder.getMethod()).append("\n").append(requestBuilder.getUri().getPath()).append("\n");
        stringToSign.append(canonicalString);

        byte[][] accessKey = (byte[][]) qingCloud.getContext().getConfigurationValue(QingCloud.DSN_ACCESS_KEY);

        return doMac(accessKey[1], stringToSign.toString());
    }

    private String doMac(byte[] accessKeySecret, String stringToSign) throws InternalException {
        String signature;
        try {
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            mac.init(new SecretKeySpec(accessKeySecret, SIGNATURE_ALGORITHM));
            byte[] signedData = mac.doFinal(stringToSign.getBytes(ENCODING));
            signature = new String(Base64.encodeBase64(signedData));
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            logger.error("AliyunRequestBuilderStrategy.sign() failed due to algorithm not supported: " + noSuchAlgorithmException
                    .getMessage());
            throw new InternalException(noSuchAlgorithmException);
        } catch (InvalidKeyException invalidKeyException) {
            logger.error("AliyunRequestBuilderStrategy.sign() failed due to key invalid: " + invalidKeyException.getMessage());
            throw new InternalException(invalidKeyException);
        } catch (UnsupportedEncodingException unsupportedEncodingException) {
            logger.error("AliyunMethod.sign() failed due to encoding not supported: " + unsupportedEncodingException
                    .getMessage());
            throw new InternalException(unsupportedEncodingException);
        }
        return signature;
    }

    private String urlEncode(String value) throws InternalException {
        if (value == null) {
            return null;
        }
        try {
            return URLEncoder.encode(value, ENCODING).replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
        } catch (UnsupportedEncodingException unsupportedEncodingException) {
            logger.error(
                    "AliyunRequestBuilderStrategy.urlEncode() failed due to encoding not supported: " + unsupportedEncodingException
                            .getMessage());
            throw new InternalException(unsupportedEncodingException);
        }
    }
}
