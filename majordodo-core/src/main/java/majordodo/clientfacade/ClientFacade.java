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
package majordodo.clientfacade;

import java.util.ArrayList;
import majordodo.task.AddTaskResult;
import majordodo.task.Broker;
import java.util.List;

/**
 * Client API
 *
 * @author enrico.olivelli
 */
public class ClientFacade {

    private final Broker broker;

    public ClientFacade(Broker broker) {
        this.broker = broker;
    }

    public AuthenticatedUser login(String username, String password) {
        return broker.getAuthenticationManager().login(username, password);
    }

    public BrokerStatusView getBrokerStatus() {
        return this.broker.createBrokerStatusView();
    }

    public SubmitTaskResult submitTask(AddTaskRequest task) throws Exception {
        AddTaskResult res = broker.addTask(task);
        return new SubmitTaskResult(res.taskId, res.error);
    }

    public TransactionStatus getTransaction(long transactionId) throws Exception {
        return broker.getTransactionStatus(transactionId);
    }

    public CreateCodePoolResult createCodePool(CreateCodePoolRequest request) throws Exception {
        return broker.createCodePool(request);
    }

    public void deleteCodePool(String codePoolId) throws Exception {
        broker.deleteCodePool(codePoolId);
    }

    public CodePoolView getCodePool(String codePoolId) throws Exception {
        return broker.getCodePool(codePoolId);
    }

    public List<SubmitTaskResult> submitTasks(List<AddTaskRequest> tasks) throws Exception {
        List<AddTaskResult> addressult = broker.addTasks(tasks);
        List<SubmitTaskResult> res = new ArrayList<>(tasks.size());
        for (AddTaskResult a : addressult) {
            res.add(new SubmitTaskResult(a.taskId, a.error));
        }
        return res;
    }

    public long beginTransaction() throws Exception {
        return broker.beginTransaction();
    }

    public void commitTransaction(long id) throws Exception {
        broker.commitTransaction(id);
    }

    public void rollbackTransaction(long id) throws Exception {
        broker.rollbackTransaction(id);
    }

    public List<TaskStatusView> getAllTasks() {
        return broker.getBrokerStatus().getAllTasks();
    }

    public List<WorkerStatusView> getAllWorkers() {
        return broker.getBrokerStatus().getAllWorkers();
    }

    public List<ResourceStatusView> getAllResources() {
        return broker.getAllResources();
    }
    
    public List<ResourceStatusView> getAllResourcesForWorker(String workerId) {
        return broker.getAllResourcesForWorker(workerId);
    }

    public TaskStatusView getTask(long taskid) {
        return broker.getBrokerStatus().getTaskStatus(taskid);
    }

    public HeapStatusView getHeapStatus() {
        return broker.getHeapStatusView();
    }
    
    public DelayedTasksQueueView getDelayedTasksQueueView() {
        return broker.getDelayedTasksQueueView();
    }
    
    public TransactionsStatusView getTransactionsStatusView() {
        return broker.getTransactionsStatusView();
    }

    public SlotsStatusView getSlotsStatusView() {
        return broker.getSlotsStatusView();
    }

}
