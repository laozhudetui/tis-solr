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
package com.qlangtech.tis.coredefine.module.action;

import com.google.common.collect.Lists;
import com.opensymphony.xwork2.ActionProxy;
import com.qlangtech.tis.BasicActionTestCase;
import com.qlangtech.tis.config.module.action.TestCollectionAction;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.valve.AjaxValve;
import com.qlangtech.tis.order.center.IParamContext;
import com.qlangtech.tis.runtime.module.action.TestSchemaAction;
import com.qlangtech.tis.sql.parser.SqlRewriter;
import com.qlangtech.tis.sql.parser.er.ERRules;
import com.qlangtech.tis.sql.parser.er.PrimaryTableMeta;
import com.qlangtech.tis.sql.parser.er.TableMeta;
import com.qlangtech.tis.sql.parser.er.impl.MockERRulesGetter;
import com.qlangtech.tis.sql.parser.meta.TabExtraMeta;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.cloud.DocCollection;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.util.JavaBinCodec;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.response.BinaryResponseWriter;
import org.easymock.EasyMock;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-09-03 16:01
 */
public class TestCoreAction extends BasicActionTestCase {
  private static final SqlRewriter.RewriterDumpTable totalpayinfo
    = SqlRewriter.RewriterDumpTable.create("kkkk", "totalpayinfo");

  public void testGetAllRowsCount() throws Exception {
    MockWebServer mockWebServer = new MockWebServer();
    JavaBinCodec binCodec = new JavaBinCodec(new BinaryResponseWriter.Resolver(null, null));
    final long expectRowcount = 300024;
    SimpleOrderedMap r = new SimpleOrderedMap();
    SolrDocumentList response = new SolrDocumentList();
    response.setNumFound(expectRowcount);
    r.add("response", response);
    Buffer body = new Buffer();
    binCodec.marshal(r, body.outputStream());

    mockWebServer.enqueue(new MockResponse()
      .addHeader("Content-Type", "application/octet-stream; charset=utf-8")
      .setBody(body)
      .setResponseCode(200));
    System.out.println("===============" + mockWebServer.url("/").toString());
    //  String coreURL = "http://192.168.28.200:8080/solr/search4employee2_shard1_replica_n1/";

    String coreURL = mockWebServer.url("/").toString() + "/solr/search4totalpay_shard1_replica_n1/";
    // Config.S4TOTALPAY
    long rowCount;
    assertTrue((rowCount = CoreAction.getAllRowsCount(Config.S4TOTALPAY, coreURL)) > 0);
    // System.out.println(rowCount);
    assertEquals(expectRowcount, rowCount);
  }

  /**
   * 触发全量构建
   */
  public void testTriggerFullbuildTask() throws Exception {
    TableMeta tableMeta = new TableMeta(totalpayinfo.getTableName(), "entity_id");
    this.createCoordinatorMock(false, (zk) -> {
      TestCollectionAction.createAssembleLogCollectPathMock(zk);
    });
    triggerFullbuildTask(tableMeta, (aResult) -> {
      assertTrue(aResult.isSuccess());
      org.json.JSONObject biz = (org.json.JSONObject) aResult.getBizResult();
      assertEquals(1234, biz.getInt(IParamContext.KEY_TASK_ID));
    });
  }

  /**
   * 执行索引全量构建过程中，测试ERRule没有定义主表，会导致final表的分区函数无法正常创建，需要主动抛出一个异常
   */
  public void testTriggerFullbuildTaskByWithoutDefinePrimaryTable() throws Exception {
    TableMeta tableMeta = new TableMeta(totalpayinfo.getTableName(), null);
    this.createCoordinatorMock(false, (zk) -> {
    });
    triggerFullbuildTask(tableMeta, (aResult) -> {
      assertFalse(aResult.isSuccess());
      List<String> errorMsgs = aResult.getErrorMsgs();
      assertNotNull(errorMsgs);
      assertTrue(errorMsgs.size() > 0);
    });
  }

  /**
   * 执行索引全量构建过程中，测试ERRule没有定义主表的<b>shareKey</b>，会导致final表的分区函数无法正常创建，需要主动抛出一个异常
   */
  public void testTriggerFullbuildTaskByWithoutDefinePrimaryTableShareKey() throws Exception {
    this.createCoordinatorMock(false, (zk) -> {
    });
    triggerFullbuildTask(null, (aResult) -> {
      assertFalse(aResult.isSuccess());
      List<String> errorMsgs = aResult.getErrorMsgs();
      assertNotNull(errorMsgs);
      assertTrue(errorMsgs.size() > 0);
    });
  }

  private void triggerFullbuildTask(TableMeta totalpayMeta, Consumer<AjaxValve.ActionExecResult> consumer) throws Exception {
    request.setParameter("emethod", "trigger_fullbuild_task");
    request.setParameter("action", "core_action");
    setCollection(TestSchemaAction.collection);

    createMockErRules(totalpayMeta);

    DocCollection docCollection = this.createMockCollection(TestSchemaAction.collection, false);
    Collection<Slice> slice = Lists.newArrayList();
    slice.add(null);
    EasyMock.expect(docCollection.getSlices()).andReturn(slice);


    this.replay();
    ActionProxy proxy = getActionProxy();
    String result = proxy.execute();
    assertEquals("CoreAction_ajax", result);
    AjaxValve.ActionExecResult aResult = showBizResult();
    assertNotNull(aResult);

    consumer.accept(aResult);
    this.verifyAll();
  }

  public ERRules createMockErRules(TableMeta totalpayMeta) {
    ERRules erRules = mock("erRules", ERRules.class);
    // EasyMock.expect(erRules.getTabFieldProcessorMap()).andReturn(Collections.emptyMap());

    if (totalpayMeta != null) {
      TabExtraMeta extraMeta = new TabExtraMeta();
      extraMeta.setSharedKey(totalpayMeta.getSharedKey());
      PrimaryTableMeta tabMeta = new PrimaryTableMeta(totalpayMeta.getTabName(), extraMeta);
      EasyMock.expect(erRules.getPrimaryTabs()).andReturn(Lists.newArrayList(tabMeta)).times(1);
    } else {
      EasyMock.expect(erRules.getPrimaryTabs()).andReturn(Collections.emptyList()).times(1);
    }

    MockERRulesGetter.erRules = erRules;
    return erRules;
  }


  private ActionProxy getActionProxy() {
    ActionProxy proxy = getActionProxy("/coredefine/coredefine.ajax");
    assertNotNull(proxy);
    CoreAction coreAction = (CoreAction) proxy.getAction();
    assertNotNull(coreAction);
    return proxy;
  }
}
