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
package dodo.scheduler;

import dodo.clustering.LogNotAvailableException;
import dodo.clustering.StatusEdit;
import dodo.task.Broker;
import dodo.task.InvalidActionException;
import dodo.clustering.Task;
import dodo.clustering.TaskQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default simple scheduler
 *
 * @author enrico.olivelli
 */
public class DefaultScheduler extends Scheduler {

    private static final Logger LOGGER = Logger.getLogger(DefaultScheduler.class.getName());

    private final ExecutorService threadpool;
    Broker broker;

    ConcurrentHashMap<String, List<PendingWorker>> pendingWorkersByTag = new ConcurrentHashMap<>();

    private static final class PendingWorker {

        String workerId;
        String tag;

        public PendingWorker(String workerId, String tag) {
            this.workerId = workerId;
            this.tag = tag;
        }

        @Override
        public String toString() {
            return "{" + "workerId=" + workerId + " for tag=" + tag + '}';
        }

    }

    public DefaultScheduler(Broker organizer) {
        this.broker = organizer;
        this.threadpool = Executors.newFixedThreadPool(1, (r) -> new Thread(r, "default-scheduler"));
    }

    public void stop() {
        threadpool.shutdown();
    }

    @Override
    public void wakeUpOnNewTask(long taskId, String tag) {
        threadpool.submit(() -> {
            List<PendingWorker> workers = pendingWorkersByTag.get(tag);
            LOGGER.log(Level.FINE, "taskSubmitted {0}, pending workers: {1}", new Object[]{tag, workers});
            if (workers != null && !workers.isEmpty()) {
                PendingWorker w = workers.remove(0);
                slotAvailable(w.workerId, tag);
            }
        });

    }

    @Override
    void workerDied(String workerId) {

    }

    @Override
    public void workerConnected(String workerId, Map<String, Integer> maximumNumberOfTasksPerTag, Set<Long> actualRunningTasks) {

        Map<String, Integer> remainingTasksPerTag = new HashMap<>(maximumNumberOfTasksPerTag);
        LOGGER.log(Level.INFO, "workerConnected {0}, maximumNumberOfTasksPerTag {1}, actualRunningTasks={2}", new Object[]{workerId, maximumNumberOfTasksPerTag, actualRunningTasks});
        for (long taskId : actualRunningTasks) {
            Task task = broker.getBrokerStatus().getTask(taskId);
            if (task != null) {
                TaskQueue queue = broker.getBrokerStatus().getTaskQueue(task.getQueueName());
                if (queue != null) {
                    Integer count = remainingTasksPerTag.get(queue.getTag());
                    if (count != null) {
                        if (count >= 2) {
                            remainingTasksPerTag.put(queue.getTag(), count - 1);
                        } else {
                            remainingTasksPerTag.remove(queue.getTag());
                        }
                    }
                }
            }
        }
        maximumNumberOfTasksPerTag.forEach((tag, max) -> {
            for (int i = 0; i < max; i++) {
                slotAvailable(workerId, tag);
            }
        });
    }

    @Override
    public void wakeUpOnTaskFinished(String workerId, String tag) {
        slotAvailable(workerId, tag);
    }

    private void slotAvailable(String workerId, String tag) {
        threadpool.submit(() -> {
            LOGGER.log(Level.FINEST, "slotAvailable {0} tag {1}", new Object[]{workerId, tag});
            Task task = getNewTask(tag);
            boolean done = false;
            if (task != null) {
                try {
                    LOGGER.log(Level.INFO, "assign {0}", task.getTaskId());
                    broker.assignTaskToWorker(workerId, task.getTaskId());
                    done = true;
                } catch (LogNotAvailableException error) {
                    LOGGER.log(Level.SEVERE, "fatal error", error);
                }
            }

            if (!done) {
                //LOGGER.log(Level.INFO, "slotAvailable {0} tag {1} -> NO TASK", new Object[]{workerId, tag});
                // put the request in a queue
                List<PendingWorker> workers = pendingWorkersByTag.get(tag);
                if (workers == null) {
                    workers = new ArrayList<>();
                    pendingWorkersByTag.put(tag, workers);
                }
                PendingWorker w = new PendingWorker(workerId, tag);
                //LOGGER.log(Level.INFO, "slotAvailable pending {0}", w);
                workers.add(w);
            }
        });

    }

    private Task getNewTask(String tag) {

        List<TaskQueue> queues = broker.getBrokerStatus().getTaskQueuesByTag(tag);
        for (TaskQueue queue : queues) {
            LOGGER.log(Level.INFO, "getNewTask tag={0}, queue={1}", new Object[]{tag, queue});
            Task task = queue.peekNext();
            LOGGER.log(Level.INFO, "getNewTask tag={0}, queue={1} -> {2}", new Object[]{tag, queue, task});
            if (task != null) {
                return task;
            }
        }
        return null;

    }

}