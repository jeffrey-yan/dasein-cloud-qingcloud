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
package org.dasein.cloud.qingcloud.network;

import java.util.ArrayList;
import java.util.List;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Tag;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.qingcloud.QingCloud;
import org.dasein.cloud.qingcloud.model.AttachTagsResponseModel;
import org.dasein.cloud.qingcloud.model.CreateTagResponseModel;
import org.dasein.cloud.qingcloud.model.DeleteTagsResponseModel;
import org.dasein.cloud.qingcloud.model.DescribeTagsResponseModel;
import org.dasein.cloud.qingcloud.model.DetachTagsResponseModel;
import org.dasein.cloud.qingcloud.model.ModifyTagAttributesResponseModel;
import org.dasein.cloud.qingcloud.model.DescribeTagsResponseModel.DescribeTagsResponseItemModel;
import org.dasein.cloud.qingcloud.model.DescribeTagsResponseModel.DescribeTagsResponseItemModel.ResourceTagPair;
import org.dasein.cloud.qingcloud.util.requester.QingCloudDriverToCoreMapper;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequestBuilder;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequester;
import org.dasein.cloud.util.requester.fluent.Requester;

/**
 * Created by Jane Wang on 12/11/2015.
 *
 * @author Jane Wang
 * @since 2016.02.1
 */
public class QingCloudTags {

	public static class DescribeTag {
		
		private String tagId;
		private String tagName;
		private String tagDescription;
		
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
		public String getTagDescription() {
			return tagDescription;
		}
		public void setTagDescription(String tagDescription) {
			this.tagDescription = tagDescription;
		}
	}
	
	public static class TagIdentityGenerator {
		
		private String tagId;
		private String tagName;
		
		public TagIdentityGenerator(String providerTagId) {
			if (providerTagId != null && providerTagId.split(":").length == 3) {
				String[] segments = providerTagId.split(":");
				this.tagId = segments[1];
				this.tagName = segments[2];
			}
		}
		
		public TagIdentityGenerator(String tagId, String tagName) {
			this.tagId = tagId;
			this.tagName = tagName;
		}
		
		public String getTagId() {
			return this.tagId;
		}
		
		public String getTagName() {
			return this.tagName;
		}
		
		public String toString() {
			return "tag:" + this.tagId + ":" + this.tagName; 
		}
	}
	
	public static enum TagResourceType {
		INSTANCE,
		VOLUME,
		KEYPAIR,
		SECURITY_GROUP,
		VXNET,
		EIP,
		ROUTER,
		LOADBALANCER,
		S2_SERVER,
		SNAPSHOT,
		RDB,
		MONGO,
		CACHE,
		ZOOKEEPER,
		QUEUE,
		SPARK
	}
	
	private QingCloud provider;
	
	public QingCloudTags(QingCloud provider) {
		this.provider = provider;
	}
	
	
	
	public void setResourcesTags(TagResourceType resourceType, String[] resourceIds, Tag[] tags) 
			throws InternalException, CloudException {
		
		if (resourceIds == null || resourceIds.length == 0 || tags == null || tags.length == 0) {
			throw new InternalException("Invalid resource ids or tags!");
		}
		
		if (validateSameKeyTags(tags)) {
			throw new InternalException("Found same key tags, make sure no same key tags!");
		}
		
		final String providerDataCenterId = getProviderDataCenterId();
		
		removeResourcesTags(resourceType, resourceIds, tags);
		
		List<String> tagIds = new ArrayList<String>();
		for (Tag tag : tags) {
			
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(provider).action("CreateTag");
			requestBuilder.parameter("tag_name", tag.getKey());
			requestBuilder.parameter("zone", providerDataCenterId);
			Requester<String> createTagRequester = new QingCloudRequester<CreateTagResponseModel, String>(
                    provider, 
                    requestBuilder.build(), 
                    new QingCloudDriverToCoreMapper<CreateTagResponseModel, String>(){
        				@Override
        				protected String doMapFrom(CreateTagResponseModel responseModel) {
        					if (responseModel != null && responseModel.getTagId() != null) {
        						return responseModel.getTagId();
        					}
        					return null;
        				}
        			}, 
        			CreateTagResponseModel.class);
			String tagId = createTagRequester.execute();
			
			requestBuilder = QingCloudRequestBuilder.get(provider).action("ModifyTagAttributes");
			requestBuilder.parameter("tag", tagId);
			requestBuilder.parameter("description", tag.getValue());
			requestBuilder.parameter("zone", providerDataCenterId);
			Requester<ModifyTagAttributesResponseModel> modifyTagAttributesRequester = new QingCloudRequester<ModifyTagAttributesResponseModel, ModifyTagAttributesResponseModel>(
                    provider, 
                    requestBuilder.build(), 
                    ModifyTagAttributesResponseModel.class);
			modifyTagAttributesRequester.execute();
			
			tagIds.add(tagId);
		}
		
		if (tagIds.size() > 0) { //attach tags to subnet
			QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(provider).action("AttachTags");
			requestBuilder.parameter("zone", providerDataCenterId);
			for (int i = 0; i < resourceIds.length; i++) {
				for (int j = 0; j < tagIds.size(); j++) {
					requestBuilder.parameter("resource_tag_pairs." + (i + j + 1) + ".tag_id", tagIds.get(j));
					requestBuilder.parameter("resource_tag_pairs." + (i + j + 1) + ".resource_type", resourceType.name().toLowerCase());
					requestBuilder.parameter("resource_tag_pairs." + (i + j + 1) + ".resource_id", resourceIds[i]);
				}
			}
			Requester<AttachTagsResponseModel> attachTagsRequester = new QingCloudRequester<AttachTagsResponseModel, AttachTagsResponseModel>(
                    provider, 
                    requestBuilder.build(), 
                    AttachTagsResponseModel.class);
			attachTagsRequester.execute();
		}
	}
	
	public void removeResourcesTags (TagResourceType resourceType, String[] resourceIds, Tag[] tags) 
			throws InternalException, CloudException {
		
		if (resourceIds == null || resourceIds.length == 0 || tags == null || tags.length == 0) {
			throw new InternalException("Invalid resource ids or tags!");
		}
		
		List<String> deletedTagIds = new ArrayList<String>();
		QingCloudRequestBuilder requestBuilder = null;
		for (int i = 0; i < resourceIds.length; i++) {
			List<DescribeTag> remoteSubnetTags = describeResourceTags(resourceIds[i]);
			for (int j = 0; j < tags.length; j++) {
				for (DescribeTag remoteSubnetTag : remoteSubnetTags) {
					if (remoteSubnetTag.getTagName().equals(tags[j].getKey())) {
						requestBuilder = QingCloudRequestBuilder.get(provider).action("DetachTags");
						requestBuilder.parameter("resource_tag_pairs." + (i + j + 1) + ".tag_id", remoteSubnetTag.getTagId());
						requestBuilder.parameter("resource_tag_pairs." + (i + j + 1) + ".resource_type", resourceType.name().toLowerCase());
						requestBuilder.parameter("resource_tag_pairs." + (i + j + 1) + ".resource_id", resourceIds[i]);
						deletedTagIds.add(remoteSubnetTag.getTagId());
						break;
					}
				}
			}
		}
		Requester<DetachTagsResponseModel> modifyRequester = new QingCloudRequester<DetachTagsResponseModel, DetachTagsResponseModel>(
                provider, 
                requestBuilder.build(), 
                DetachTagsResponseModel.class);
		modifyRequester.execute();
		
		for (int i = 0; i < deletedTagIds.size(); i++) {
			requestBuilder = QingCloudRequestBuilder.get(provider).action("DeleteTags");
			requestBuilder.parameter("tags." + (i + 1), deletedTagIds.get(i));
			requestBuilder.parameter("zone", getProviderDataCenterId());
		}
		Requester<DeleteTagsResponseModel> deleteRequester = new QingCloudRequester<DeleteTagsResponseModel, DeleteTagsResponseModel>(
                provider, 
                requestBuilder.build(), 
                DeleteTagsResponseModel.class);
		deleteRequester.execute();
	}
	
	public void updateResourcesTags (TagResourceType resourceType, String[] resourceIds, Tag[] tags) 
			throws InternalException, CloudException {
		for (String resourceId : resourceIds) {
			List<DescribeTag> subnetTags = describeResourceTags(resourceId);
			for (Tag tag : tags) {
				for (DescribeTag subnetTag: subnetTags) {
					if (tag.getKey().equals(subnetTag.getTagName())) { //find tag, update it
						QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(provider).action("ModifyTagAttributes");
						requestBuilder.parameter("tag", subnetTag.getTagId());
						requestBuilder.parameter("description", tag.getValue());
						requestBuilder.parameter("zone", getProviderDataCenterId());
						Requester<ModifyTagAttributesResponseModel> requester = new QingCloudRequester<ModifyTagAttributesResponseModel, ModifyTagAttributesResponseModel>(
			                    provider, 
			                    requestBuilder.build(), 
			                    ModifyTagAttributesResponseModel.class);
						requester.execute();
						break;
					}
				}
			}
		}
	}
	
	public List<DescribeTag> describeResourceTags(final String resourceId) throws InternalException, CloudException {
		QingCloudRequestBuilder requestBuilder = QingCloudRequestBuilder.get(provider).action("DescribeTags");
		requestBuilder.parameter("zone", getProviderDataCenterId());
		Requester<List<DescribeTag>> requester = new QingCloudRequester<DescribeTagsResponseModel, List<DescribeTag>>(
                provider, 
                requestBuilder.build(), 
                new QingCloudDriverToCoreMapper<DescribeTagsResponseModel, List<DescribeTag>>(){
    				@Override
    				protected List<DescribeTag> doMapFrom(DescribeTagsResponseModel responseModel) {
    					List<DescribeTag> tags = new ArrayList<DescribeTag>();
    					if (responseModel != null && responseModel.getTagSet() != null && responseModel.getTagSet().size() > 0) {
    						for (DescribeTagsResponseItemModel item : responseModel.getTagSet()) {
    							for (ResourceTagPair pair : item.getResourceTagPairs()) {
    								if (pair.getResourceId().equals(resourceId)) {
    									DescribeTag tag = new DescribeTag();
    									tag.setTagId(item.getTagId());
    									tag.setTagName(item.getTagName());
    									tag.setTagDescription(item.getDescription());
    									tags.add(tag);
    									break;
    								}
    							}
    						}
    					}
    					return tags;
    				}
    			}, 
    			DescribeTagsResponseModel.class);
		return requester.execute();
	}
	
	private boolean validateSameKeyTags(Tag ... tags) {
		for (int i = 0; i < tags.length; i++) {
			for (int j = i + 1; j < tags.length; j++) {
				Tag leftTag = tags[i];
				Tag rightTag = tags[j];
				if (leftTag.getKey().equals(rightTag.getKey())) {
					return true;
				}
			}
		}
		return false;
	}
	
	private String getProviderDataCenterId() throws InternalException, CloudException {
		String regionId = provider.getContext().getRegionId();
        if (regionId == null) {
            throw new InternalException("No region was set for this request");
        }

        Iterable<DataCenter> dataCenters = provider.getDataCenterServices().listDataCenters(regionId);
        return dataCenters.iterator().next().getProviderDataCenterId();//each account has one DC in each region
	}
	
}
