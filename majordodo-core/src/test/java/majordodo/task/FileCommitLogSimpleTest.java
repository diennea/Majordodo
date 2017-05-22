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

import majordodo.task.BrokerStatusSnapshot;
import majordodo.task.FileCommitLog;
import majordodo.task.LogSequenceNumber;
import majordodo.task.Task;
import majordodo.task.StatusEdit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Simple tests for FileCommit Log
 *
 * @author enrico.olivelli
 */
public class FileCommitLogSimpleTest {

    @Rule
    public TemporaryFolder folderSnapshots = new TemporaryFolder();
    @Rule
    public TemporaryFolder folderLogs = new TemporaryFolder();

    @Test
    public void test() throws Exception {
        try (FileCommitLog log = new FileCommitLog(folderSnapshots.getRoot().toPath(), folderLogs.getRoot().toPath(), 1024 * 1024);) {
            BrokerStatusSnapshot snapshot = log.loadBrokerStatusSnapshot();
            log.recovery(snapshot.getActualLogSequenceNumber(), (a, b) -> {
                fail();
            }, false);
            log.startWriting();
            assertEquals(snapshot.getActualLogSequenceNumber().ledgerId, -1);
            assertEquals(snapshot.getActualLogSequenceNumber().sequenceNumber, -1);
            assertTrue(snapshot.getTasks().isEmpty());
            StatusEdit edit1 = StatusEdit.ADD_TASK(1, "mytype", "param1", "myuser", 0, 0, 0, null, 0, null, null);
            StatusEdit edit2 = StatusEdit.WORKER_CONNECTED("node1", "psasa", "localhost", new HashSet<>(), System.currentTimeMillis());
            StatusEdit edit3 = StatusEdit.ASSIGN_TASK_TO_WORKER(1, "worker1", 1, "db1,db2");
            StatusEdit edit4 = StatusEdit.TASK_STATUS_CHANGE(1, "node1", Task.STATUS_FINISHED, "theresult");
            LogSequenceNumber logStatusEdit1 = log.logStatusEdit(edit1);
            LogSequenceNumber logStatusEdit2 = log.logStatusEdit(edit2);
            LogSequenceNumber logStatusEdit3 = log.logStatusEdit(edit3);
            LogSequenceNumber logStatusEdit4 = log.logStatusEdit(edit4);
        }
        try (FileCommitLog log = new FileCommitLog(folderSnapshots.getRoot().toPath(), folderLogs.getRoot().toPath(), 1024 * 1024);) {
            BrokerStatusSnapshot snapshot = log.loadBrokerStatusSnapshot();
            System.out.println("snapshot:" + snapshot);
            // no snapshot was taken...
            assertEquals(snapshot.getActualLogSequenceNumber().ledgerId, -1);
            assertEquals(snapshot.getActualLogSequenceNumber().sequenceNumber, -1);
            List<StatusEdit> edits = new ArrayList<>();
            AtomicLong last = new AtomicLong(-1);
            log.recovery(snapshot.getActualLogSequenceNumber(), (a, b) -> {
                System.out.println("entry:" + a + ", " + b);
                assertEquals(1, a.ledgerId);
                assertTrue(a.sequenceNumber > last.get());
                edits.add(b);
                last.set(a.sequenceNumber);
            }, false);
            log.startWriting();
            assertEquals(StatusEdit.TYPE_ADD_TASK, edits.get(0).editType);
            assertEquals(StatusEdit.TYPE_WORKER_CONNECTED, edits.get(1).editType);
            assertEquals(StatusEdit.TYPE_ASSIGN_TASK_TO_WORKER, edits.get(2).editType);
            assertEquals("db1,db2", edits.get(2).resources);
            assertEquals(StatusEdit.TYPE_TASK_STATUS_CHANGE, edits.get(3).editType);

        }

    }

}
