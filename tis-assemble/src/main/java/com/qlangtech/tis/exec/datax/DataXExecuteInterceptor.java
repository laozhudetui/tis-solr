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

package com.qlangtech.tis.exec.datax;

import com.google.common.collect.Lists;
import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.datax.DataXJobSubmit;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.datax.job.DataXJobWorker;
import com.qlangtech.tis.exec.ExecuteResult;
import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.exec.impl.TrackableExecuteInterceptor;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteJobTrigger;
import com.qlangtech.tis.fullbuild.indexbuild.RunningStatus;
import com.qlangtech.tis.fullbuild.phasestatus.impl.DumpPhaseStatus;
import com.qlangtech.tis.realtime.yarn.rpc.IncrStatusUmbilicalProtocol;
import com.qlangtech.tis.realtime.yarn.rpc.impl.AdapterStatusUmbilicalProtocol;
import com.qlangtech.tis.rpc.server.IncrStatusUmbilicalProtocolImpl;
import com.tis.hadoop.rpc.ITISRpcService;
import com.tis.hadoop.rpc.RpcServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * DataX 执行器
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-04-27 15:42
 **/
public class DataXExecuteInterceptor extends TrackableExecuteInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(DataXExecuteInterceptor.class);

    @Override
    protected ExecuteResult execute(IExecChainContext execChainContext) throws Exception {

        RpcServiceReference statusRpc = getDataXExecReporter();

        DataxProcessor appSource = execChainContext.getAppSource();
        IRemoteJobTrigger jobTrigger = null;
        RunningStatus runningStatus = null;

        List<IRemoteJobTrigger> triggers = Lists.newArrayList();

        List<String> cfgFileNames = appSource.getDataxCfgFileNames(null);

        for (String fileName : cfgFileNames) {
            jobTrigger = createDataXJob(execChainContext, statusRpc, appSource, fileName);

            triggers.add(jobTrigger);
        }

        logger.info("trigger dataX jobs by mode:{},with:{}", this.getDataXTriggerType()
                , cfgFileNames.stream().collect(Collectors.joining(",")));
        for (IRemoteJobTrigger t : triggers) {
            t.submitJob();
        }
        boolean faild = false;
        boolean allComplete = false;
        waitting:
        while (!allComplete) {

            try {
                faild = false;
                for (IRemoteJobTrigger t : triggers) {
                    runningStatus = t.getRunningStatus();
                    if (runningStatus.isComplete() && !runningStatus.isSuccess()) {
                        // faild
                        faild = true;
                        allComplete = true;
                        break waitting;
                    }

                    if (!runningStatus.isComplete()) {
                        continue waitting;
                    }
                }
                allComplete = true;
            } finally {
                Thread.sleep(2000);
            }
        }

        ExecuteResult result = new ExecuteResult(!faild);
        for (IRemoteJobTrigger trigger : triggers) {
            if (trigger.isAsyn()) {
                execChainContext.addAsynSubJob(new IExecChainContext.AsynSubJob(trigger.getAsynJobName()));
            }
        }

        return result;
    }

    protected IRemoteJobTrigger createDataXJob(IExecChainContext execChainContext, RpcServiceReference statusRpc
            , DataxProcessor appSource, String fileName) {
        DataXJobSubmit.InstanceType expectDataXJobSumit = getDataXTriggerType();
        Optional<DataXJobSubmit> jobSubmit = DataXJobSubmit.getDataXJobSubmit(expectDataXJobSumit);

        // 如果分布式worker ready的话
        if (!jobSubmit.isPresent()) {
            throw new IllegalStateException("can not find expect jobSubmit by type:" + expectDataXJobSumit);
        }

        return jobSubmit.get().createDataXJob(execChainContext, statusRpc, appSource, fileName);
    }

    protected DataXJobSubmit.InstanceType getDataXTriggerType() {
        boolean dataXWorkerServiceOnDuty = DataXJobWorker.isDataXWorkerServiceOnDuty();
        return dataXWorkerServiceOnDuty ? DataXJobSubmit.InstanceType.DISTRIBUTE : DataXJobSubmit.InstanceType.LOCAL;
    }

    protected RpcServiceReference getDataXExecReporter() {
        IncrStatusUmbilicalProtocolImpl statusServer = IncrStatusUmbilicalProtocolImpl.getInstance();
        IncrStatusUmbilicalProtocol statReceiveSvc = new AdapterStatusUmbilicalProtocol() {
            @Override
            public void reportDumpTableStatus(DumpPhaseStatus.TableDumpStatus tableDumpStatus) {
                statusServer.reportDumpTableStatus(tableDumpStatus.getTaskid(), tableDumpStatus.isFaild(), tableDumpStatus.getName());
            }
        };
        AtomicReference<ITISRpcService> ref = new AtomicReference<>();
        ref.set(new DataXAssembleSvcCompsite(statReceiveSvc));
        return new RpcServiceReference(ref);
    }

    @Override
    public Set<FullbuildPhase> getPhase() {
        return Collections.singleton(FullbuildPhase.FullDump);
    }
}
