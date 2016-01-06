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

package org.dasein.cloud.qingcloud;

import org.apache.log4j.Logger;
import org.dasein.cloud.AbstractCloud;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.ContextRequirements;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.dc.DataCenterServices;
import org.dasein.cloud.dc.Region;
import org.dasein.cloud.identity.IdentityServices;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.qingcloud.compute.QingCloudCompute;
import org.dasein.cloud.qingcloud.dc.QingCloudDataCenter;
import org.dasein.cloud.qingcloud.identity.QingCloudIdentity;
import org.dasein.cloud.qingcloud.network.QingCloudNetwork;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;

/**
 * Created by Jeffrey Yan on 11/9/2015.
 *
 * @author Jeffrey Yan
 * @since 2016.02.1
 */
public class QingCloud extends AbstractCloud {

    static private Logger stdLogger = QingCloud.getStdLogger(QingCloud.class);

    static private final String ISO8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    static public final String DSN_ACCESS_KEY = "accessKey";

    static private @Nonnull String getLastItem(@Nonnull String name) {
        int idx = name.lastIndexOf('.');

        if( idx < 0 ) {
            return name;
        }
        else if( idx == (name.length()-1) ) {
            return "";
        }
        return name.substring(idx+1);
    }

    static private @Nonnull Logger getLogger(@Nonnull Class<?> cls, @Nonnull String type) {
        String pkg = getLastItem(cls.getPackage().getName());

        if( pkg.equals("org") ) {
            pkg = "";
        }
        else {
            pkg = pkg + ".";
        }
        return Logger.getLogger("dasein.cloud.qingcloud." + type + "." + pkg + getLastItem(cls.getName()));
    }

    static public @Nonnull Logger getStdLogger(Class<?> cls) {
        return getLogger(cls, "std");
    }

    static public @Nonnull Logger getWireLogger(Class<?> cls) {
        return getLogger(cls, "wire");
    }

    @Nonnull
    @Override
    public String getCloudName() {
        ProviderContext ctx = getContext();
        String name = ( ctx == null ? null : ctx.getCloud().getCloudName() );

        return ( ( name == null ) ? "QingCloud" : name );
    }

    @Nonnull
    @Override
    public String getProviderName() {
        ProviderContext ctx = getContext();
        String name = ( ctx == null ? null : ctx.getCloud().getProviderName() );

        return ( ( name == null ) ? "Yunify" : name );
    }

    @Override
    public @Nonnull
    ContextRequirements getContextRequirements() {
        return new ContextRequirements(
                new ContextRequirements.Field(DSN_ACCESS_KEY, "QingCloud API access keys", ContextRequirements.FieldType.KEYPAIR, ContextRequirements.Field.ACCESS_KEYS, true),
                new ContextRequirements.Field("proxyHost", "Proxy host", ContextRequirements.FieldType.TEXT, false),
                new ContextRequirements.Field("proxyPort", "Proxy port", ContextRequirements.FieldType.TEXT, false));
    }

    @Override
    public @Nullable
    String testContext() {
        if (stdLogger.isTraceEnabled()) {
            stdLogger.trace("Enter - " + QingCloud.class.getName() + ".textContext()");
        }
        try {
            ProviderContext context = getContext();

            if (context == null) {
                return null;
            }

            DataCenterServices dataCenterServices = getDataCenterServices();
            Iterable<Region> regions = dataCenterServices.listRegions();
            if (regions.iterator().hasNext()) {
                return context.getAccountNumber();
            } else {
                return null;
            }
        } catch (Throwable throwable) {
            stdLogger.warn("Failed to test QingCloud connection context: " + throwable.getMessage(), throwable);
            return null;
        } finally {
            if (stdLogger.isTraceEnabled()) {
                stdLogger.trace("Exit - " + QingCloud.class.getName() + ".testContext()");
            }
        }
    }

    /*
     * This method will return the QingCloud Zone ID, no matter how region-dc is mapped.
     * Zone is always same as DC, but region can be different.
     */
    public String getZoneId() throws CloudException, InternalException {
        String regionId = getContext().getRegionId();
        if (regionId == null) {
            throw new InternalException("No region was set for this request");
        }
        return regionId;
        //Iterable<DataCenter> dataCenters = getDataCenterServices().listDataCenters(regionId);
        //return dataCenters.iterator().next().getProviderDataCenterId();//each account has one DC in each region
    }

    @Nonnull
    @Override
    public DataCenterServices getDataCenterServices() {
        return new QingCloudDataCenter(this);
    }

    @Nullable
    @Override
    public IdentityServices getIdentityServices() {
        return new QingCloudIdentity(this);
    }

    @Override
    @Nullable
    public ComputeServices getComputeServices() {
        return new QingCloudCompute(this);
    }

    @Override
    @Nullable
    public NetworkServices getNetworkServices() {
        return new QingCloudNetwork(this);
    }

    public String formatIso8601Date(Date date) {
        SimpleDateFormat df = new SimpleDateFormat(ISO8601_DATE_FORMAT);
        df.setTimeZone(new SimpleTimeZone(8 * 60 * 60 * 1000, "GMT"));
        return df.format(date);
    }

    public Date parseIso8601Date(@Nonnull String date) throws InternalException {
        if (date == null || date.isEmpty()) {
            return new Date(0);
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat(ISO8601_DATE_FORMAT);

        try {
            return dateFormat.parse(date);
        } catch (ParseException parseException) {
            throw new InternalException("Could not parse date: " + date);
        }
    }
}
