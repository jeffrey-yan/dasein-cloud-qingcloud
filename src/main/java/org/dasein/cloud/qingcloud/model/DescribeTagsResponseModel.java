package org.dasein.cloud.qingcloud.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DescribeTagsResponseModel extends DescribeResponseModel {

	@JsonProperty("tag_set")
	private List<DescribeTagsResponseItemModel> tagSet;
	
	@Override
	public String getAction() {
		return "DescribeTags";
	}
	
	public List<DescribeTagsResponseItemModel> getTagSet() {
		return tagSet;
	}

	public void setTagSet(List<DescribeTagsResponseItemModel> tagSet) {
		this.tagSet = tagSet;
	}

	public static class DescribeTagsResponseItemModel {
		
		@JsonProperty("tag_id")
		private String tagId;
		@JsonProperty("tag_name")
		private String tagName;
		@JsonProperty("description")
		private String description;
		@JsonProperty("resource_count")
		private Integer resourceCount;
		@JsonProperty("resource_type_count")
		private List<ResourceTypeCount> resourceTypeCount;
		@JsonProperty("resource_tag_pairs")
		private List<ResourceTagPair> resourceTagPairs;
		
		public String getTagId() {
			return tagId;
		}
		public void setTagId(String tagId) {
			this.tagId = tagId;
		}
		public String getTagName() {
			return tagName;
		}
		public void setTagName(String tagName) {
			this.tagName = tagName;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public Integer getResourceCount() {
			return resourceCount;
		}
		public void setResourceCount(Integer resourceCount) {
			this.resourceCount = resourceCount;
		}
		public List<ResourceTypeCount> getResourceTypeCount() {
			return resourceTypeCount;
		}
		public void setResourceTypeCount(List<ResourceTypeCount> resourceTypeCount) {
			this.resourceTypeCount = resourceTypeCount;
		}
		public List<ResourceTagPair> getResourceTagPairs() {
			return resourceTagPairs;
		}
		public void setResourceTagPairs(List<ResourceTagPair> resourceTagPairs) {
			this.resourceTagPairs = resourceTagPairs;
		}

		public static class ResourceTypeCount {
			
			@JsonProperty("count")
			private Integer count;
			@JsonProperty("resource_type")
			private String resourceType;
			
			public Integer getCount() {
				return count;
			}
			public void setCount(Integer count) {
				this.count = count;
			}
			public String getResourceType() {
				return resourceType;
			}
			public void setResourceType(String resourceType) {
				this.resourceType = resourceType;
			}
		}
		
		public static class ResourceTagPair {
			
			@JsonProperty("tag_id")
			private String tagId;
			@JsonProperty("resource_type")
			private String resourceType;
			@JsonProperty("resource_id")
			private String resourceId;
			
			public String getTagId() {
				return tagId;
			}
			public void setTagId(String tagId) {
				this.tagId = tagId;
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
