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

package org.dasein.cloud.qingcloud.identity.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dasein.cloud.qingcloud.model.DescribeResponseModel;

import java.util.List;

/**
 * Created by Jeffrey Yan on 12/14/2015.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DescribeKeyPairsResponse extends DescribeResponseModel {

    @JsonProperty("keypair_set")
    private List<Keypair> keypairs;

    public List<Keypair> getKeypairs() {
        return keypairs;
    }

    public void setKeypairs(List<Keypair> keypairs) {
        this.keypairs = keypairs;
    }

    public static class Keypair {
        @JsonProperty("description")
        private String description;

        @JsonProperty("encrypt_method")
        private String encryptMethod;

        @JsonProperty("keypair_name")
        private String keypairName;

        @JsonProperty("instance_ids")
        private List<String> instanceIds;

        @JsonProperty("create_time")
        private String createTime;

        @JsonProperty("keypair_id")
        private String keypairId;

        @JsonProperty("pub_key")
        private String pubKey;

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getEncryptMethod() {
            return encryptMethod;
        }

        public void setEncryptMethod(String encryptMethod) {
            this.encryptMethod = encryptMethod;
        }

        public String getKeypairName() {
            return keypairName;
        }

        public void setKeypairName(String keypairName) {
            this.keypairName = keypairName;
        }

        public List<String> getInstanceIds() {
            return instanceIds;
        }

        public void setInstanceIds(List<String> instanceIds) {
            this.instanceIds = instanceIds;
        }

        public String getCreateTime() {
            return createTime;
        }

        public void setCreateTime(String createTime) {
            this.createTime = createTime;
        }

        public String getKeypairId() {
            return keypairId;
        }

        public void setKeypairId(String keypairId) {
            this.keypairId = keypairId;
        }

        public String getPubKey() {
            return pubKey;
        }

        public void setPubKey(String pubKey) {
            this.pubKey = pubKey;
        }
    }
}
