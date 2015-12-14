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

import org.apache.http.client.methods.HttpUriRequest;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.identity.AbstractShellKeySupport;
import org.dasein.cloud.identity.SSHKeypair;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.cloud.identity.ShellKeyCapabilities;
import org.dasein.cloud.identity.ShellKeySupport;
import org.dasein.cloud.qingcloud.QingCloud;
import org.dasein.cloud.qingcloud.identity.model.CreateKeyPairResponse;
import org.dasein.cloud.qingcloud.identity.model.DeleteKeyPairsResponse;
import org.dasein.cloud.qingcloud.identity.model.DescribeKeyPairsResponse;
import org.dasein.cloud.qingcloud.util.requester.QingCloudDriverToCoreMapper;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequestBuilder;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequester;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.requester.fluent.Requester;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jeffrey Yan on 12/14/2015.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
public class QingCloudShellKey extends AbstractShellKeySupport<QingCloud> implements ShellKeySupport {

    protected QingCloudShellKey(QingCloud provider) {
        super(provider);
    }

    @Nonnull
    @Override
    public ShellKeyCapabilities getCapabilities() throws CloudException, InternalException {
        return new QingCloudShellKeyCapabilities(getProvider());
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        return true;
    }

    @Nonnull
    @Override
    public SSHKeypair createKeypair(@Nonnull String name) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "ShellKey.createKeypair");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
                    .action("CreateKeyPair")
                    .parameter("keypair_name", name)
                    .parameter("mode", "system")
                    .parameter("encrypt_method", "ssh-rsa")
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<CreateKeyPairResponse> requester = new QingCloudRequester<CreateKeyPairResponse, CreateKeyPairResponse>(
                    getProvider(), request, CreateKeyPairResponse.class);

            CreateKeyPairResponse response = requester.execute();

            SSHKeypair keypair = getKeypair(response.getKeypairId());
            keypair.setPrivateKey(response.getPrivateKey().getBytes()); //TODO, check if correct

            return keypair;
        } finally {
            APITrace.end();
        }
    }

    @Nonnull
    @Override
    public SSHKeypair importKeypair(@Nonnull String name, @Nonnull String publicKey)
            throws InternalException, CloudException {
        APITrace.begin(getProvider(), "ShellKey.importKeypair");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
                    .action("CreateKeyPair")
                    .parameter("keypair_name", name)
                    .parameter("mode", "user")
                    .parameter("public_key", publicKey)
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<CreateKeyPairResponse> requester = new QingCloudRequester<CreateKeyPairResponse, CreateKeyPairResponse>(
                    getProvider(), request, CreateKeyPairResponse.class);

            CreateKeyPairResponse response = requester.execute();

            SSHKeypair keypair = new SSHKeypair();
            keypair.setProviderKeypairId(response.getKeypairId());
            keypair.setName(name);
            keypair.setPublicKey(publicKey);
            keypair.setProviderRegionId(getContext().getRegionId());
            keypair.setProviderOwnerId(getContext().getAccountNumber());

            return keypair;
        } finally {
            APITrace.end();
        }
    }

    @Override
    public void deleteKeypair(@Nonnull String providerId) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "ShellKey.deleteKeypair");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
                    .action("DeleteKeyPairs")
                    .parameter("keypairs.1", providerId)
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<DeleteKeyPairsResponse> requester = new QingCloudRequester<DeleteKeyPairsResponse, DeleteKeyPairsResponse>(
                    getProvider(), request, DeleteKeyPairsResponse.class);

            requester.execute();
        } finally {
            APITrace.end();
        }
    }

    @Nullable
    @Override
    public String getFingerprint(@Nonnull String providerId) throws InternalException, CloudException {
        return ""; //no API to get fingerprint
    }

    @Nullable
    @Override
    public SSHKeypair getKeypair(@Nonnull String providerId) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "ShellKey.getKeypair");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
                    .action("DescribeKeyPairsResponse")
                    .parameter("keypairs.1", providerId)
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<List<SSHKeypair>> requester = new QingCloudRequester<DescribeKeyPairsResponse, List<SSHKeypair>>(
                    getProvider(), request, new SSHKeypairsMapper(), DescribeKeyPairsResponse.class);

            List<SSHKeypair> result = requester.execute();
            if (result.size() > 0) {
                return result.get(0);
            } else {
                return null;
            }
        } finally {
            APITrace.end();
        }
    }

    @Nonnull
    @Override
    public Iterable<SSHKeypair> list() throws InternalException, CloudException {
        APITrace.begin(getProvider(), "ShellKey.list");
        try {
            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
                    .action("DescribeKeyPairsResponse")
                    .parameter("zone", getProvider().getZoneId())
                    .build();

            Requester<List<SSHKeypair>> requester = new QingCloudRequester<DescribeKeyPairsResponse, List<SSHKeypair>>(
                    getProvider(), request, new SSHKeypairsMapper(), DescribeKeyPairsResponse.class);

            return requester.execute();
        } finally {
            APITrace.end();
        }
    }

    @Nonnull
    @Override
    public String[] mapServiceAction(@Nonnull ServiceAction action) {
        return new String[0];
    }


    private class SSHKeypairsMapper extends QingCloudDriverToCoreMapper<DescribeKeyPairsResponse, List<SSHKeypair>> {
        @Override
        protected List<SSHKeypair> doMapFrom(DescribeKeyPairsResponse responseModel) {
            try {
                List<SSHKeypair> sshKeypairs = new ArrayList<SSHKeypair>();

                for(DescribeKeyPairsResponse.Keypair keypairModel : responseModel.getKeypairs()) {
                    SSHKeypair sshKeypair = new SSHKeypair();
                    sshKeypair.setName(keypairModel.getKeypairName());
                    sshKeypair.setPublicKey(keypairModel.getPubKey());
                    sshKeypair.setProviderKeypairId(keypairModel.getKeypairId());
                    sshKeypair.setProviderOwnerId(getContext().getAccountNumber());
                    sshKeypair.setProviderRegionId(getContext().getRegionId());
                    sshKeypairs.add(sshKeypair);
                }

                return sshKeypairs;
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
    }
}
