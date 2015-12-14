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

package org.dasein.cloud.qingcloud.dc;

import org.apache.http.client.methods.HttpUriRequest;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.dc.AbstractDataCenterServices;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.dc.DataCenterCapabilities;
import org.dasein.cloud.dc.DataCenterServices;
import org.dasein.cloud.dc.Region;
import org.dasein.cloud.qingcloud.QingCloud;
import org.dasein.cloud.qingcloud.dc.model.DescribeZonesResponseModel;
import org.dasein.cloud.qingcloud.util.requester.QingCloudDriverToCoreMapper;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequestBuilder;
import org.dasein.cloud.qingcloud.util.requester.QingCloudRequester;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.cloud.util.requester.fluent.Requester;
import org.dasein.util.uom.time.TimePeriod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Jeffrey Yan on 11/9/2015.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
public class QingCloudDataCenter extends AbstractDataCenterServices<QingCloud> implements DataCenterServices {

    public QingCloudDataCenter(QingCloud provider) {
        super(provider);
    }

    @Nonnull
    @Override
    public DataCenterCapabilities getCapabilities() throws InternalException, CloudException {
        return new QingCloudDataCenterCapabilities(getProvider());
    }

    @Override
    @Nullable
    public DataCenter getDataCenter(@Nonnull String providerDataCenterId) throws InternalException, CloudException {
        ProviderContext context = getProvider().getContext();
        if (context == null) {
            throw new InternalException("No context exists for this request");
        }

        String regionId = context.getRegionId();
        if (regionId == null) {
            throw new InternalException("No region is established for this request");
        }

        for (DataCenter dataCenter : listDataCenters(regionId)) {
            if (dataCenter.getProviderDataCenterId().equals(providerDataCenterId)) {
                return dataCenter;
            }
        }
        return null;
    }

    @Override
    @Nullable
    public Region getRegion(@Nonnull String providerRegionId) throws InternalException, CloudException {
        for (Region region : listRegions()) {
            if (region.getProviderRegionId().equals(providerRegionId)) {
                return region;
            }
        }
        return null;
    }

    @Override
    public @Nonnull Iterable<DataCenter> listDataCenters(@Nonnull final String providerRegionId) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "DataCenter.listDataCenters");
        try {
            ProviderContext context = getProvider().getContext();
            if (context == null) {
                throw new InternalException("No context was set for this request");
            }

            Cache<DataCenter> cache = null;
            Collection<DataCenter> dataCenters;

            if (providerRegionId.equals(context.getRegionId())) {//only cache context's region
                cache = Cache.getInstance(getProvider(), "dataCenters", DataCenter.class, CacheLevel.REGION_ACCOUNT,
                        TimePeriod.valueOf(1, "day"));
                dataCenters = (Collection<DataCenter>) cache.get(context);
                if (dataCenters != null) {
                    return dataCenters;
                }
            }

            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
                    .action("DescribeZones")
                    .build();

            Requester<List<DataCenter>> requester = new QingCloudRequester<DescribeZonesResponseModel, List<DataCenter>>(
                    getProvider(), request,
                    new QingCloudDriverToCoreMapper<DescribeZonesResponseModel, List<DataCenter>>() {
                        @Override
                        protected List<DataCenter> doMapFrom(DescribeZonesResponseModel responseModel) {
                            List<DataCenter> result = new ArrayList<DataCenter>();
                            for (DescribeZonesResponseModel.Zone zone : responseModel.getZones()) {
                                //if (zone.getZoneId().matches("^" + providerRegionId + "[0-9]+$")) {
                                if (zone.getZoneId().equals(providerRegionId)) {
                                    boolean active = "active".equals(zone.getStatus());
                                    DataCenter dataCenter = new DataCenter(zone.getZoneId(), zone.getZoneId(),
                                            providerRegionId, active, active);
                                    result.add(dataCenter);
                                }
                            }
                            return result;
                        }
                    }, DescribeZonesResponseModel.class);

            dataCenters = requester.execute();

            if (cache != null) {
                cache.put(context, dataCenters);
            }
            return dataCenters;
        } finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull Iterable<Region> listRegions() throws InternalException, CloudException {
        APITrace.begin(getProvider(), "DataCenter.listRegions");

        try {
            ProviderContext context = getProvider().getContext();
            if (context == null) {
                throw new InternalException("No context was set for this request");
            }

            Cache<Region> cache = Cache.getInstance(getProvider(), "regions", Region.class, CacheLevel.CLOUD_ACCOUNT);
            Collection<Region> regions = (Collection<Region>) cache.get(context);
            if (regions != null) {
                return regions;
            }

            HttpUriRequest request = QingCloudRequestBuilder.get(getProvider())
                    .action("DescribeZones")
                    .build();

            Requester<List<Region>> requester = new QingCloudRequester<DescribeZonesResponseModel, List<Region>>(
                    getProvider(), request,
                    new QingCloudDriverToCoreMapper<DescribeZonesResponseModel, List<Region>>() {
                        @Override
                        protected List<Region> doMapFrom(DescribeZonesResponseModel responseModel) {
                            Set<String> regionIds = new HashSet<String>();
                            /*
                            Pattern pattern = Pattern.compile("^([A-Za-z]+)([0-9]+)$");
                            for (DescribeZonesResponseModel.Zone zone : responseModel.getZones()) {
                                Matcher matcher = pattern.matcher(zone.getZoneId());
                                if (matcher.find() && "active".equals(zone.getStatus())) {
                                    regionIds.add(matcher.group(1));
                                }
                            }
                            */

                            for (DescribeZonesResponseModel.Zone zone : responseModel.getZones()) {
                                regionIds.add(zone.getZoneId());
                            }

                            List<Region> result = new ArrayList<Region>();
                            for(String regionId : regionIds){
                                Region region = new Region(regionId, regionId, true, true);
                                //region.setJurisdiction("ap".equals(regionId) ? "HK" : "CN");
                                region.setJurisdiction(regionId.startsWith("ap") ? "HK" : "CN");
                                result.add(region);
                            }
                            return result;
                        }
                    }, DescribeZonesResponseModel.class);

            regions = requester.execute();

            cache.put(context, regions);
            return regions;
        } finally {
            APITrace.end();
        }
    }
}
