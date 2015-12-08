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
 * Created by Jeffrey Yan on 12/7/2015.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DescribeInstancesResponseModel extends DescribeResponseModel {

    @JsonProperty("instance_set")
    private List<Instance> instances;

    public List<Instance> getInstances() {
        return instances;
    }

    public void setInstances(List<Instance> instances) {
        this.instances = instances;
    }

    public static class Instance {
        @JsonProperty("instance_id")
        private String instanceId;

        @JsonProperty("vcpus_current")
        private int vcpusCurrent;

        @JsonProperty("volume_ids")
        private List<String> volumeIds;

        @JsonProperty("vxnets")
        private List<Vxnet> vxnets;

        @JsonProperty("eip")
        private Eip eip;

        @JsonProperty("memory_current")
        private int memoryCurrent;

        @JsonProperty("sub_code")
        private int subCode;

        @JsonProperty("transition_status")
        private String transitionStatus;

        @JsonProperty("instance_name")
        private String instanceName;

        @JsonProperty("instance_type")
        private String instanceType;

        @JsonProperty("create_time")
        private String createTime;

        @JsonProperty("status")
        private String status;

        @JsonProperty("description")
        private String description;

        @JsonProperty("security_group")
        private SecurityGroup securityGroup;

        @JsonProperty("status_time")
        private String statusTime;

        @JsonProperty("image")
        private Image image;

        @JsonProperty("keypair_ids")
        private List<String> keypairIds;

        public String getInstanceId() {
            return instanceId;
        }

        public void setInstanceId(String instanceId) {
            this.instanceId = instanceId;
        }

        public int getVcpusCurrent() {
            return vcpusCurrent;
        }

        public void setVcpusCurrent(int vcpusCurrent) {
            this.vcpusCurrent = vcpusCurrent;
        }

        public List<String> getVolumeIds() {
            return volumeIds;
        }

        public void setVolumeIds(List<String> volumeIds) {
            this.volumeIds = volumeIds;
        }

        public List<Vxnet> getVxnets() {
            return vxnets;
        }

        public void setVxnets(List<Vxnet> vxnets) {
            this.vxnets = vxnets;
        }

        public Eip getEip() {
            return eip;
        }

        public void setEip(Eip eip) {
            this.eip = eip;
        }

        public int getMemoryCurrent() {
            return memoryCurrent;
        }

        public void setMemoryCurrent(int memoryCurrent) {
            this.memoryCurrent = memoryCurrent;
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

        public String getInstanceName() {
            return instanceName;
        }

        public void setInstanceName(String instanceName) {
            this.instanceName = instanceName;
        }

        public String getInstanceType() {
            return instanceType;
        }

        public void setInstanceType(String instanceType) {
            this.instanceType = instanceType;
        }

        public String getCreateTime() {
            return createTime;
        }

        public void setCreateTime(String createTime) {
            this.createTime = createTime;
        }

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

        public SecurityGroup getSecurityGroup() {
            return securityGroup;
        }

        public void setSecurityGroup(SecurityGroup securityGroup) {
            this.securityGroup = securityGroup;
        }

        public String getStatusTime() {
            return statusTime;
        }

        public void setStatusTime(String statusTime) {
            this.statusTime = statusTime;
        }

        public Image getImage() {
            return image;
        }

        public void setImage(Image image) {
            this.image = image;
        }

        public List<String> getKeypairIds() {
            return keypairIds;
        }

        public void setKeypairIds(List<String> keypairIds) {
            this.keypairIds = keypairIds;
        }

        public static class Vxnet {
            @JsonProperty("vxnet_name")
            private String vxnetName;

            @JsonProperty("vxnet_type")
            private int vxnetType;

            @JsonProperty("vxnet_id")
            private String vxnetId;

            @JsonProperty("nic_id")
            private String nicId;

            @JsonProperty("private_ip")
            private String privateIp;

            public String getVxnetName() {
                return vxnetName;
            }

            public void setVxnetName(String vxnetName) {
                this.vxnetName = vxnetName;
            }

            public int getVxnetType() {
                return vxnetType;
            }

            public void setVxnetType(int vxnetType) {
                this.vxnetType = vxnetType;
            }

            public String getVxnetId() {
                return vxnetId;
            }

            public void setVxnetId(String vxnetId) {
                this.vxnetId = vxnetId;
            }

            public String getNicId() {
                return nicId;
            }

            public void setNicId(String nicId) {
                this.nicId = nicId;
            }

            public String getPrivateIp() {
                return privateIp;
            }

            public void setPrivateIp(String privateIp) {
                this.privateIp = privateIp;
            }
        }

        public static class Eip {
            @JsonProperty("eip_id")
            private String eipId;

            @JsonProperty("eip_addr")
            private String eipAddr;

            @JsonProperty("bandwidth")
            private String bandwidth;

            public String getEipId() {
                return eipId;
            }

            public void setEipId(String eipId) {
                this.eipId = eipId;
            }

            public String getEipAddr() {
                return eipAddr;
            }

            public void setEipAddr(String eipAddr) {
                this.eipAddr = eipAddr;
            }

            public String getBandwidth() {
                return bandwidth;
            }

            public void setBandwidth(String bandwidth) {
                this.bandwidth = bandwidth;
            }
        }

        public static class SecurityGroup {
            @JsonProperty("is_default")
            private int isDefault;

            @JsonProperty("security_group_id")
            private String securityGroupId;

            public int getIsDefault() {
                return isDefault;
            }

            public void setIsDefault(int isDefault) {
                this.isDefault = isDefault;
            }

            public String getSecurityGroupId() {
                return securityGroupId;
            }

            public void setSecurityGroupId(String securityGroupId) {
                this.securityGroupId = securityGroupId;
            }
        }

        public static class Image {
            @JsonProperty("processor_type")
            private String processorType;

            @JsonProperty("platform")
            private String platform;

            @JsonProperty("image_size")
            private int imageSize;

            @JsonProperty("image_name")
            private String imageName;

            @JsonProperty("image_id")
            private String imageId;

            @JsonProperty("os_family")
            private String osFamily;

            @JsonProperty("provider")
            private String provider;

            public String getProcessorType() {
                return processorType;
            }

            public void setProcessorType(String processorType) {
                this.processorType = processorType;
            }

            public String getPlatform() {
                return platform;
            }

            public void setPlatform(String platform) {
                this.platform = platform;
            }

            public int getImageSize() {
                return imageSize;
            }

            public void setImageSize(int imageSize) {
                this.imageSize = imageSize;
            }

            public String getImageName() {
                return imageName;
            }

            public void setImageName(String imageName) {
                this.imageName = imageName;
            }

            public String getImageId() {
                return imageId;
            }

            public void setImageId(String imageId) {
                this.imageId = imageId;
            }

            public String getOsFamily() {
                return osFamily;
            }

            public void setOsFamily(String osFamily) {
                this.osFamily = osFamily;
            }

            public String getProvider() {
                return provider;
            }

            public void setProvider(String provider) {
                this.provider = provider;
            }
        }
    }


}
