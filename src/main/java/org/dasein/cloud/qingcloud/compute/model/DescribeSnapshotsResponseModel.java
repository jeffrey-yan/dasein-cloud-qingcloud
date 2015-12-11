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
 * Created by Jeffrey Yan on 12/11/2015.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DescribeSnapshotsResponseModel extends DescribeResponseModel {

    @JsonProperty("snapshot_set")
    private List<Snapshot> snapshots;

    public List<Snapshot> getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(List<Snapshot> snapshots) {
        this.snapshots = snapshots;
    }

    public static class Snapshot {
        @JsonProperty("snapshot_id")
        private String snapshotId;

        @JsonProperty("snapshot_name")
        private String snapshotName;

        @JsonProperty("description")
        private String description;

        @JsonProperty("snapshot_type")
        private String snapshotType;

        @JsonProperty("status")
        private String status;

        @JsonProperty("transition_status")
        private String transitionStatus;

        @JsonProperty("create_time")
        private String createTime;

        @JsonProperty("status_time")
        private String statusTime;

        @JsonProperty("snapshot_time")
        private String snapshotTime;

        @JsonProperty("is_taken")
        private int isTaken;

        @JsonProperty("is_head")
        private int isHead;

        @JsonProperty("root_id")
        private String rootId;

        @JsonProperty("parent_id")
        private String parentId;

        @JsonProperty("size")
        private int size;

        @JsonProperty("total_size")
        private int totalSize;

        @JsonProperty("total_count")
        private int totalCount;

        @JsonProperty("lastest_snapshot_time")
        private String lastestSnapshotTime;

        @JsonProperty("sub_code")
        private int subCode;

        @JsonProperty("resource")
        private Resource resource;

        public String getSnapshotId() {
            return snapshotId;
        }

        public void setSnapshotId(String snapshotId) {
            this.snapshotId = snapshotId;
        }

        public String getSnapshotName() {
            return snapshotName;
        }

        public void setSnapshotName(String snapshotName) {
            this.snapshotName = snapshotName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getSnapshotType() {
            return snapshotType;
        }

        public void setSnapshotType(String snapshotType) {
            this.snapshotType = snapshotType;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getTransitionStatus() {
            return transitionStatus;
        }

        public void setTransitionStatus(String transitionStatus) {
            this.transitionStatus = transitionStatus;
        }

        public String getCreateTime() {
            return createTime;
        }

        public void setCreateTime(String createTime) {
            this.createTime = createTime;
        }

        public String getStatusTime() {
            return statusTime;
        }

        public void setStatusTime(String statusTime) {
            this.statusTime = statusTime;
        }

        public String getSnapshotTime() {
            return snapshotTime;
        }

        public void setSnapshotTime(String snapshotTime) {
            this.snapshotTime = snapshotTime;
        }

        public int getIsTaken() {
            return isTaken;
        }

        public void setIsTaken(int isTaken) {
            this.isTaken = isTaken;
        }

        public int getIsHead() {
            return isHead;
        }

        public void setIsHead(int isHead) {
            this.isHead = isHead;
        }

        public String getRootId() {
            return rootId;
        }

        public void setRootId(String rootId) {
            this.rootId = rootId;
        }

        public String getParentId() {
            return parentId;
        }

        public void setParentId(String parentId) {
            this.parentId = parentId;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public int getTotalSize() {
            return totalSize;
        }

        public void setTotalSize(int totalSize) {
            this.totalSize = totalSize;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }

        public String getLastestSnapshotTime() {
            return lastestSnapshotTime;
        }

        public void setLastestSnapshotTime(String lastestSnapshotTime) {
            this.lastestSnapshotTime = lastestSnapshotTime;
        }

        public int getSubCode() {
            return subCode;
        }

        public void setSubCode(int subCode) {
            this.subCode = subCode;
        }

        public Resource getResource() {
            return resource;
        }

        public void setResource(Resource resource) {
            this.resource = resource;
        }

        public static class Resource {
            @JsonProperty("resource_name")
            private String resourceName;

            @JsonProperty("resource_type")
            private String resourceType;

            @JsonProperty("resource_id")
            private String resourceId;

            public String getResourceName() {
                return resourceName;
            }

            public void setResourceName(String resourceName) {
                this.resourceName = resourceName;
            }

            public String getResourceType() {
                return resourceType;
            }

            public void setResourceType(String resourceType) {
                this.resourceType = resourceType;
            }

            public String getResourceId() {
                return resourceId;
            }

            public void setResourceId(String resourceId) {
                this.resourceId = resourceId;
            }
        }
    }
}
