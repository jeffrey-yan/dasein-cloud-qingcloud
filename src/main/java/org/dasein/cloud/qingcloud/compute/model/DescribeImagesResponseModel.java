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
 * Created by Jeffrey Yan on 12/10/2015.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DescribeImagesResponseModel extends DescribeResponseModel {

    @JsonProperty("image_set")
    private List<Image> images;

    public List<Image> getImages() {
        return images;
    }

    public void setImages(List<Image> images) {
        this.images = images;
    }

    public static class Image {
        @JsonProperty("image_set")
        private String status;

        @JsonProperty("image_set")
        private String processorType;

        @JsonProperty("image_set")
        private String imageId;

        @JsonProperty("image_set")
        private String subCode;

        @JsonProperty("image_set")
        private String transitionStatus;

        @JsonProperty("image_set")
        private String recommendedType;

        @JsonProperty("image_set")
        private String imageName;

        @JsonProperty("image_set")
        private String visibility;

        @JsonProperty("image_set")
        private String platform;

        @JsonProperty("image_set")
        private String createTime;

        @JsonProperty("image_set")
        private String osFamily;

        @JsonProperty("image_set")
        private String provider;

        @JsonProperty("image_set")
        private String owner;

        @JsonProperty("image_set")
        private String statusTime;

        @JsonProperty("image_set")
        private int size;

        @JsonProperty("image_set")
        private String description;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getProcessorType() {
            return processorType;
        }

        public void setProcessorType(String processorType) {
            this.processorType = processorType;
        }

        public String getImageId() {
            return imageId;
        }

        public void setImageId(String imageId) {
            this.imageId = imageId;
        }

        public String getSubCode() {
            return subCode;
        }

        public void setSubCode(String subCode) {
            this.subCode = subCode;
        }

        public String getTransitionStatus() {
            return transitionStatus;
        }

        public void setTransitionStatus(String transitionStatus) {
            this.transitionStatus = transitionStatus;
        }

        public String getRecommendedType() {
            return recommendedType;
        }

        public void setRecommendedType(String recommendedType) {
            this.recommendedType = recommendedType;
        }

        public String getImageName() {
            return imageName;
        }

        public void setImageName(String imageName) {
            this.imageName = imageName;
        }

        public String getVisibility() {
            return visibility;
        }

        public void setVisibility(String visibility) {
            this.visibility = visibility;
        }

        public String getPlatform() {
            return platform;
        }

        public void setPlatform(String platform) {
            this.platform = platform;
        }

        public String getCreateTime() {
            return createTime;
        }

        public void setCreateTime(String createTime) {
            this.createTime = createTime;
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

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
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

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
