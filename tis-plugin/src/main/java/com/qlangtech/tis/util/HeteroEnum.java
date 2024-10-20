/**
 * Copyright (c) 2020 QingLang, Inc. <baisui@qlangtech.com>
 * <p>
 * This program is free software: you can use, redistribute, and/or modify
 * it under the terms of the GNU Affero General Public License, version 3
 * or later ("AGPL"), as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.qlangtech.tis.util;

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.async.message.client.consumer.impl.MQListenerFactory;
import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.datax.job.DataXJobWorker;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.manage.IAppSource;
import com.qlangtech.tis.offline.*;
import com.qlangtech.tis.plugin.IdentityName;
import com.qlangtech.tis.plugin.PluginStore;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugin.ds.PostedDSProp;
import com.qlangtech.tis.plugin.incr.IncrStreamFactory;
import com.qlangtech.tis.plugin.k8s.K8sImage;
import com.qlangtech.tis.plugin.solr.config.QueryParserFactory;
import com.qlangtech.tis.plugin.solr.config.SearchComponentFactory;
import com.qlangtech.tis.plugin.solr.config.TISTransformerFactory;
import com.qlangtech.tis.plugin.solr.schema.FieldTypeFactory;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 表明一种插件的类型
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public enum HeteroEnum {

    FLAT_TABLE_BUILDER(// 
            FlatTableBuilder.class, //
            "flat_table_builder", "宽表构建", Selectable.Single),
    // ////////////////////////////////////////////////////////
    INDEX_BUILD_CONTAINER(// 
            IndexBuilderTriggerFactory.class, //
            "index_build_container", // },
            "索引构建容器", Selectable.Single),
    // ////////////////////////////////////////////////////////
    DS_DUMP(// 
            TableDumpFactory.class, //
            "ds_dump", // },
            "数据导出", Selectable.Single),
    // ////////////////////////////////////////////////////////
    FS(// 
            FileSystemFactory.class, //
            "fs", "存储"),
    // ////////////////////////////////////////////////////////
    MQ(// 
            MQListenerFactory.class, //
            "mq", "MQ消息监听"),
    // ////////////////////////////////////////////////////////
    PARAMS_CONFIG(// 
            ParamsConfig.class, //
            "params-cfg", // },//
            "基础配置", Selectable.Multi),
    // ////////////////////////////////////////////////////////
    K8S_IMAGES(//
            K8sImage.class, //
            "k8s-images", // },//
            "K8S-Images", Selectable.Multi)
    // ////////////////////////////////////////////////////////
    ,
    DATAX_WORKER(//
            DataXJobWorker.class, //
            "datax-worker", // },//
            "DataX Worker", Selectable.Single),
    // ////////////////////////////////////////////////////////

    INCR_K8S_CONFIG(//
            IncrStreamFactory.class, //
            "incr-config", // },
            "增量配置", Selectable.Single),
    DATASOURCE(//
            DataSourceFactory.class, //
            "datasource", //
            "数据源", //
            Selectable.Single),
    SOLR_FIELD_TYPE(//
            FieldTypeFactory.class, //
            "field-type", //
            "字段类型", //
            Selectable.Multi),
    SOLR_QP(//
            QueryParserFactory.class, //
            "qp", //
            "QueryParser", //
            Selectable.Multi),
    SOLR_SEARCH_COMPONENT(//
            SearchComponentFactory.class, //
            "searchComponent", //
            "SearchComponent", //
            Selectable.Multi),
    SOLR_TRANSFORMER(//
            TISTransformerFactory.class, //
            "transformer", //
            "Transformer", //
            Selectable.Multi),
    DATAX_READER(//
            DataxReader.class, //
            "dataxReader", //
            "DataX Reader", //
            Selectable.Multi),
    DATAX_WRITER(//
            DataxWriter.class, //
            "dataxWriter", //
            "DataX Writer", //
            Selectable.Multi),
    APP_SOURCE(//
            IAppSource.class, //
            "appSource", //
            "App Source", //
            Selectable.Multi);

    public final String caption;

    public final String identity;

    public final Class<? extends Describable> extensionPoint;

    // public final IDescriptorsGetter descriptorsGetter;
    // private final IItemGetter itemGetter;
    public final Selectable selectable;

    <T extends Describable<T>> HeteroEnum(
            Class<T> extensionPoint,
            String identity, String caption, Selectable selectable) {
        this.extensionPoint = extensionPoint;
        this.caption = caption;
        // this.descriptorsGetter = descriptorsGetter;
        // this.itemGetter = itemGetter;
        this.identity = identity;
        this.selectable = selectable;
    }

    /**
     * 判断实例是否是应该名称唯一的
     *
     * @return
     */
    public boolean isIdentityUnique() {
        return IdentityName.class.isAssignableFrom(this.extensionPoint);
    }

    <// , IDescriptorsGetter descriptorsGetter//, ISaveable saveable
            T extends Describable<T>> HeteroEnum(// , IDescriptorsGetter descriptorsGetter//, ISaveable saveable
                                                 Class<T> extensionPoint, // , IDescriptorsGetter descriptorsGetter//, ISaveable saveable
                                                 String identity, String caption) {
        this(extensionPoint, identity, caption, Selectable.Multi);
    }

    public <T> T getPlugin() {
        if (this.selectable != Selectable.Single) {
            throw new IllegalStateException(this.extensionPoint + " selectable is:" + this.selectable);
        }
        PluginStore store = TIS.getPluginStore(this.extensionPoint);
        return (T) store.getPlugin();
    }

    /**
     * ref: PluginItems.save()
     *
     * @param pluginContext
     * @param pluginMeta
     * @param <T>
     * @return
     */
    public <T> List<T> getPlugins(IPluginContext pluginContext, UploadPluginMeta pluginMeta) {
        PluginStore store = getPluginStore(pluginContext, pluginMeta);
        if (store == null) {
            return Collections.emptyList();
        }
//        if (this == HeteroEnum.APP_SOURCE) {
//            final String dataxName = (pluginMeta.getExtraParam(DataxUtils.DATAX_NAME));
//            if (StringUtils.isEmpty(dataxName)) {
//                throw new IllegalArgumentException("plugin extra param 'DataxUtils.DATAX_NAME'" + DataxUtils.DATAX_NAME + " can not be null");
//            }
//            store = com.qlangtech.tis.manage.IAppSource.getPluginStore(pluginContext, dataxName);
//        } else if (this == HeteroEnum.DATAX_WRITER || this == HeteroEnum.DATAX_READER) {
//            final String dataxName = pluginMeta.getExtraParam(DataxUtils.DATAX_NAME);
//            if (StringUtils.isEmpty(dataxName)) {
//                throw new IllegalArgumentException("plugin extra param 'DataxUtils.DATAX_NAME': '" + DataxUtils.DATAX_NAME + "' can not be null");
//            }
//            store = (this == HeteroEnum.DATAX_READER) ? DataxReader.getPluginStore(pluginContext, dataxName) : DataxWriter.getPluginStore(pluginContext, dataxName);
//        } else if (pluginContext.isCollectionAware()) {
//            store = TIS.getPluginStore(pluginContext.getCollectionName(), this.extensionPoint);
//        } else if (pluginContext.isDataSourceAware()) {
//            PostedDSProp dsProp = PostedDSProp.parse(pluginMeta);
//            if (StringUtils.isEmpty(dsProp.getDbname())) {
//                return Collections.emptyList();
//            }
//            store = TIS.getDataBasePluginStore(dsProp);
//        } else {
//            store = TIS.getPluginStore(this.extensionPoint);
//        }
        //Objects.requireNonNull(store, "plugin store can not be null");
        return store.getPlugins();
    }

    public PluginStore getPluginStore(IPluginContext pluginContext, UploadPluginMeta pluginMeta) {
        PluginStore store = null;
        if (this == HeteroEnum.APP_SOURCE) {
            final String dataxName = (pluginMeta.getExtraParam(DataxUtils.DATAX_NAME));
            if (StringUtils.isEmpty(dataxName)) {
                throw new IllegalArgumentException("plugin extra param 'DataxUtils.DATAX_NAME'" + DataxUtils.DATAX_NAME + " can not be null");
            }
            store = com.qlangtech.tis.manage.IAppSource.getPluginStore(pluginContext, dataxName);
        } else if (this == HeteroEnum.DATAX_WRITER || this == HeteroEnum.DATAX_READER) {
            final String dataxName = pluginMeta.getExtraParam(DataxUtils.DATAX_NAME);
            if (StringUtils.isEmpty(dataxName)) {
                throw new IllegalArgumentException("plugin extra param 'DataxUtils.DATAX_NAME': '" + DataxUtils.DATAX_NAME + "' can not be null");
            }
            store = (this == HeteroEnum.DATAX_READER) ? DataxReader.getPluginStore(pluginContext, dataxName) : DataxWriter.getPluginStore(pluginContext, dataxName);
        } else if (pluginContext.isCollectionAware()) {
            store = TIS.getPluginStore(pluginContext.getCollectionName(), this.extensionPoint);
        } else if (pluginContext.isDataSourceAware()) {
            PostedDSProp dsProp = PostedDSProp.parse(pluginMeta);
            if (StringUtils.isEmpty(dsProp.getDbname())) {
                return null; //Collections.emptyList();
            }
            store = TIS.getDataBasePluginStore(dsProp);
        } else {
            store = TIS.getPluginStore(this.extensionPoint);
        }
        Objects.requireNonNull(store, "plugin store can not be null");
        return store;
    }

    public <T extends Describable<T>> List<Descriptor<T>> descriptors() {
        PluginStore pluginStore = TIS.getPluginStore(this.extensionPoint);
        return pluginStore.allDescriptor();
    }

    public static HeteroEnum of(String identity) {
        for (HeteroEnum he : HeteroEnum.values()) {
            if (StringUtils.equals(he.identity, identity)) {
                return he;
            }
        }
        throw new IllegalStateException("identity:" + identity + " is illegal,exist:"
                + Arrays.stream(HeteroEnum.values()).map((h) -> "'" + h.identity + "'").collect(Collectors.joining(",")));
    }
}
