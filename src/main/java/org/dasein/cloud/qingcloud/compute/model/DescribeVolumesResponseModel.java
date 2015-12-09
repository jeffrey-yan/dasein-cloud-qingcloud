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

package org.dasein.cloud.qingcloud.compute.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dasein.cloud.qingcloud.model.DescribeResponseModel;

import java.util.List;

/**
 * Created by Jeffrey Yan on 12/9/2015.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DescribeVolumesResponseModel extends DescribeResponseModel {

    @JsonProperty("volume_set")
    private List<Volume> volumes;

    public List<Volume> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<Volume> volumes) {
        this.volumes = volumes;
    }

    public static class Volume {
        @JsonProperty("status")
        private String status;

        @JsonProperty("description")
        private String description;

        @JsonProperty("volume_name")
        private String volumeName;

        @JsonProperty("sub_code")
        private int subCode;

        @JsonProperty("transition_status")
        private String transitionStatus;

        @JsonProperty("instance")
        private Instance instance;

        @JsonProperty("create_time")
        private String createTime;

        @JsonProperty("volume_id")
        private String volumeId;

        @JsonProperty("status_time")
        private String statusTime;

        @JsonProperty("size")
        private int size;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getVolumeName() {
            return volumeName;
        }

        public void setVolumeName(String volumeName) {
            this.volumeName = volumeName;
        }

        public int getSubCode() {
            return subCode;
        }

        public void setSubCode(int subCode) {
            this.subCode = subCode;
        }

        public String getTransitionStatus() {
            return transitionStatus;
        }

        public void setTransitionStatus(String transitionStatus) {
            this.transitionStatus = transitionStatus;
        }

        public Instance getInstance() {
            return instance;
        }

        public void setInstance(Instance instance) {
            this.instance = instance;
        }

        public String getCreateTime() {
            return createTime;
        }

        public void setCreateTime(String createTime) {
            this.createTime = createTime;
        }

        public String getVolumeId() {
            return volumeId;
        }

        public void setVolumeId(String volumeId) {
            this.volumeId = volumeId;
        }

        public String getStatusTime() {
            return statusTime;
        }

        public void setStatusTime(String statusTime) {
            this.statusTime = statusTime;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public static class Instance {
            @JsonProperty("instance_id")
            private String instanceId;

            @JsonProperty("instance_name")
            private String instanceName;

            public String getInstanceId() {
                return instanceId;
            }

            public void setInstanceId(String instanceId) {
                this.instanceId = instanceId;
            }

            public String getInstanceName() {
                return instanceName;
            }

            public void setInstanceName(String instanceName) {
                this.instanceName = instanceName;
            }
        }
    }
}
