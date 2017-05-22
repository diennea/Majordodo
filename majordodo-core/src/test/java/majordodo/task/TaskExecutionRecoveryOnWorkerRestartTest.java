/*
 Licensed to Diennea S.r.l. under one
 or more contributor license agreements. See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership. Diennea S.r.l. licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.

 */
package majordodo.task;

import majordodo.executors.TaskExecutor;
import majordodo.network.netty.NettyBrokerLocator;
import majordodo.worker.WorkerCore;
import majordodo.worker.WorkerCoreConfiguration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import majordodo.clientfacade.AddTaskRequest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Basic tests for recovery
 *
 * @author enrico.olivelli
 */
@BrokerTestUtils.StartBroker
public class TaskExecutionRecoveryOnWorkerRestartTest extends BrokerTestUtils {

    @Test
    public void taskRecoveryTest() throws Exception {
        
        String workerId = "abc";
        String taskParams = "param";

        long taskId = broker.getClient().submitTask(new AddTaskRequest(0, TASKTYPE_MYTYPE, userId, taskParams, 0, 0, 0, null, 0, null, null)).getTaskId();

        // startAsWritable a worker, it will die
        try (NettyBrokerLocator locator = new NettyBrokerLocator(server.getHost(), server.getPort(), server.isSsl())) {
            CountDownLatch taskStartedLatch = new CountDownLatch(1);
            Map<String, Integer> tags = new HashMap<>();
            tags.put(TASKTYPE_MYTYPE, 1);

            WorkerCoreConfiguration config = new WorkerCoreConfiguration();
            config.setMaxPendingFinishedTaskNotifications(1);
            config.setWorkerId(workerId);
            config.setMaxThreadsByTaskType(tags);
            config.setGroups(Arrays.asList(group));
            config.setTasksRequestTimeout(1000);
            try (WorkerCore core = new WorkerCore(config, "process1", locator, null);) {
                core.start();
                core.setExecutorFactory(
                        (String tasktype, Map<String, Object> parameters) -> new TaskExecutor() {
                    @Override
                    public String executeTask(Map<String, Object> parameters) throws Exception {
                        taskStartedLatch.countDown();
                        System.out.println("executeTask: " + parameters);
                        Integer attempt = (Integer) parameters.get("attempt");
                        if (attempt == null || attempt == 1) {
                            core.die();
                            return null;
                        }
                        return "theresult";
                    }

                }
                );

                assertTrue(taskStartedLatch.await(30, TimeUnit.SECONDS));
            }
        }

        boolean ok = false;
        for (int i = 0; i < 100; i++) {
            Task task = broker.getBrokerStatus().getTask(taskId);
//                    System.out.println("task:" + task);
            if (task.getStatus() == Task.STATUS_WAITING) {
                ok = true;
                break;
            }
            Thread.sleep(1000);
        }
        assertTrue(ok);

        // boot the worker again
        try (NettyBrokerLocator locator = new NettyBrokerLocator(server.getHost(), server.getPort(), server.isSsl())) {
            CountDownLatch taskStartedLatch = new CountDownLatch(1);
            Map<String, Integer> tags = new HashMap<>();
            tags.put(TASKTYPE_MYTYPE, 1);

            WorkerCoreConfiguration config = new WorkerCoreConfiguration();
            config.setMaxPendingFinishedTaskNotifications(1);
            config.setWorkerId(workerId);
            config.setMaxThreadsByTaskType(tags);
            config.setGroups(Arrays.asList(group));
            try (WorkerCore core = new WorkerCore(config, "process2", locator, null);) {
                core.start();
                core.setExecutorFactory(
                        (String tasktype, Map<String, Object> parameters) -> new TaskExecutor() {
                    @Override
                    public String executeTask(Map<String, Object> parameters) throws Exception {
                        taskStartedLatch.countDown();
                        System.out.println("executeTask2: " + parameters + " ,taskStartedLatch:" + taskStartedLatch.getCount());
                        Integer attempt = (Integer) parameters.get("attempt");
                        if (attempt == null || attempt == 1) {
                            throw new RuntimeException("impossible!");
                        }
                        return "theresult";
                    }

                }
                );
                assertTrue(taskStartedLatch.await(10, TimeUnit.SECONDS));
                ok = false;
                for (int i = 0; i < 100; i++) {
                    Task task = broker.getBrokerStatus().getTask(taskId);
                    System.out.println("task2:" + task);
                    if (task.getStatus() == Task.STATUS_FINISHED) {
                        ok = true;
                        assertEquals("theresult", task.getResult());
                        break;
                    }
                    Thread.sleep(1000);
                }
                assertTrue(ok);
            }
        }
    }
}
