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

import majordodo.task.Task;

/**
 * Visible status of a task for clients
 *
 * @author enrico.olivelli
 */
public class TaskStatusView {

    private long taskId;
    private int status;
    private String user;
    private String workerId;
    private long createdTimestamp;
    private String data;
    private String result;
    private String type;
    private String slot;
    private int attempts;
    private int maxattempts;
    private long requestedStartTime;
    private long executionDeadline;
    private String mode;
    private String codePoolId;
    private String resources;

    public static String convertTaskStatusForClient(int taskStatus) {
        String status;
        switch (taskStatus) {
            case Task.STATUS_ERROR:
                status = "error";
                break;
            case Task.STATUS_FINISHED:
                status = "finished";
                break;
            case Task.STATUS_RUNNING:
                status = "running";
                break;
            case Task.STATUS_WAITING:
                status = "waiting";
                break;
            case Task.STATUS_DELAYED:
                status = "delayed";
                break;
            default:
                status = "?" + taskStatus;
        }
        return status;
    }

    @Override
    public String toString() {
        return "TaskStatusView{" + "taskId=" + taskId + ", status=" + status + " " + convertTaskStatusForClient(status) + ", user=" + user + ", workerId=" + workerId + ", createdTimestamp=" + createdTimestamp + ", data=" + data + ", result=" + result + ", type=" + type + ", slot=" + slot + ", attempts=" + attempts + ", maxattempts=" + maxattempts + ", resources=" + resources + ", executionDeadline=" + executionDeadline + '}';
    }

    public long getRequestedStartTime() {
        return requestedStartTime;
    }

    public void setRequestedStartTime(long requestedStartTime) {
        this.requestedStartTime = requestedStartTime;
    }

    public String getSlot() {
        return slot;
    }

    public void setSlot(String slot) {
        this.slot = slot;
    }

    public int getMaxattempts() {
        return maxattempts;
    }

    public void setMaxattempts(int maxattempts) {
        this.maxattempts = maxattempts;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public long getExecutionDeadline() {
        return executionDeadline;
    }

    public void setExecutionDeadline(long executionDeadline) {
        this.executionDeadline = executionDeadline;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getCodePoolId() {
        return codePoolId;
    }

    public void setCodePoolId(String codePoolId) {
        this.codePoolId = codePoolId;
    }

    public String getResources() {
        return resources;
    }

    public void setResources(String resources) {
        this.resources = resources;
    }

}
