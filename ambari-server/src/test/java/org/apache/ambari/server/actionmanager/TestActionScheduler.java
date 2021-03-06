/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.actionmanager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionScheduler.RoleStats;
import org.apache.ambari.server.agent.ActionQueue;
import org.apache.ambari.server.agent.AgentCommand;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.controller.HostsMap;
import org.apache.ambari.server.serveraction.ServerAction;
import org.apache.ambari.server.serveraction.ServerActionManagerImpl;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostUpgradeEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.Test;

public class TestActionScheduler {

  /**
   * This test sends a new action to the action scheduler and verifies that the action
   * shows up in the action queue.
   */
  @Test
  public void testActionSchedule() throws Exception {
    ActionQueue aq = new ActionQueue();
    Clusters fsm = mock(Clusters.class);
    Cluster oneClusterMock = mock(Cluster.class);
    Service serviceObj = mock(Service.class);
    ServiceComponent scomp = mock(ServiceComponent.class);
    ServiceComponentHost sch = mock(ServiceComponentHost.class);
    when(fsm.getCluster(anyString())).thenReturn(oneClusterMock);
    when(oneClusterMock.getService(anyString())).thenReturn(serviceObj);
    when(serviceObj.getServiceComponent(anyString())).thenReturn(scomp);
    when(scomp.getServiceComponentHost(anyString())).thenReturn(sch);
    when(serviceObj.getCluster()).thenReturn(oneClusterMock);

    ActionDBAccessor db = mock(ActionDBAccessorImpl.class);
    List<Stage> stages = new ArrayList<Stage>();
    String hostname = "ahost.ambari.apache.org";
    Stage s = StageUtils.getATestStage(1, 977, hostname);
    stages.add(s);
    when(db.getStagesInProgress()).thenReturn(stages);

    //Keep large number of attempts so that the task is not expired finally
    //Small action timeout to test rescheduling
    ActionScheduler scheduler = new ActionScheduler(100, 100, db, aq, fsm,
        10000, new HostsMap((String) null), null);
    scheduler.setTaskTimeoutAdjustment(false);
    // Start the thread
    scheduler.start();

    List<AgentCommand> ac = waitForQueueSize(hostname, aq, 1);
    assertTrue(ac.get(0) instanceof ExecutionCommand);
    assertEquals("1-977", ((ExecutionCommand) (ac.get(0))).getCommandId());

    //The action status has not changed, it should be queued again.
    ac = waitForQueueSize(hostname, aq, 1);
    assertTrue(ac.get(0) instanceof ExecutionCommand);
    assertEquals("1-977", ((ExecutionCommand) (ac.get(0))).getCommandId());

    //Now change the action status
    s.setHostRoleStatus(hostname, "NAMENODE", HostRoleStatus.COMPLETED);
    ac = aq.dequeueAll(hostname);

    //Wait for sometime, it shouldn't be scheduled this time.
    ac = waitForQueueSize(hostname, aq, 0);
    scheduler.stop();
  }

  private List<AgentCommand> waitForQueueSize(String hostname, ActionQueue aq,
      int expectedQueueSize) throws InterruptedException {
    while (true) {
      List<AgentCommand> ac = aq.dequeueAll(hostname);
      if (ac != null) {
        if (ac.size() == expectedQueueSize) {
          return ac;
        } else if (ac.size() > expectedQueueSize) {
          Assert.fail("Expected size : " + expectedQueueSize + " Actual size="
              + ac.size());
        }
      }
      Thread.sleep(100);
    }
  }

  /**
   * Test whether scheduler times out an action
   */
  @Test
  public void testActionTimeout() throws Exception {
    ActionQueue aq = new ActionQueue();
    Clusters fsm = mock(Clusters.class);
    Cluster oneClusterMock = mock(Cluster.class);
    Service serviceObj = mock(Service.class);
    ServiceComponent scomp = mock(ServiceComponent.class);
    ServiceComponentHost sch = mock(ServiceComponentHost.class);
    when(fsm.getCluster(anyString())).thenReturn(oneClusterMock);
    when(oneClusterMock.getService(anyString())).thenReturn(serviceObj);
    when(serviceObj.getServiceComponent(anyString())).thenReturn(scomp);
    when(scomp.getServiceComponentHost(anyString())).thenReturn(sch);
    when(serviceObj.getCluster()).thenReturn(oneClusterMock);

    ActionDBAccessor db = new ActionDBInMemoryImpl();
    String hostname = "ahost.ambari.apache.org";
    List<Stage> stages = new ArrayList<Stage>();
    Stage s = StageUtils.getATestStage(1, 977, hostname);
    stages.add(s);
    db.persistActions(stages);

    //Small action timeout to test rescheduling
    ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3, 
        new HostsMap((String) null), null);
    scheduler.setTaskTimeoutAdjustment(false);
    // Start the thread
    scheduler.start();

    while (!stages.get(0).getHostRoleStatus(hostname, "NAMENODE")
        .equals(HostRoleStatus.TIMEDOUT)) {
      Thread.sleep(100);
    }
    assertEquals(stages.get(0).getHostRoleStatus(hostname, "NAMENODE"),
        HostRoleStatus.TIMEDOUT);
  }

  /**
   * Test server action
   */
  @Test
  public void testServerAction() throws Exception {
    ActionQueue aq = new ActionQueue();
    Clusters fsm = mock(Clusters.class);
    Cluster oneClusterMock = mock(Cluster.class);
    Service serviceObj = mock(Service.class);
    ServiceComponent scomp = mock(ServiceComponent.class);
    ServiceComponentHost sch = mock(ServiceComponentHost.class);
    when(fsm.getCluster(anyString())).thenReturn(oneClusterMock);
    when(oneClusterMock.getService(anyString())).thenReturn(serviceObj);
    when(serviceObj.getServiceComponent(anyString())).thenReturn(scomp);
    when(scomp.getServiceComponentHost(anyString())).thenReturn(sch);
    when(serviceObj.getCluster()).thenReturn(oneClusterMock);

    ActionDBAccessor db = new ActionDBInMemoryImpl();
    String hostname = "ahost.ambari.apache.org";
    List<Stage> stages = new ArrayList<Stage>();
    Map<String, String> payload = new HashMap<String, String>();
    payload.put(ServerAction.PayloadName.CLUSTER_NAME, "cluster1");
    payload.put(ServerAction.PayloadName.CURRENT_STACK_VERSION, "HDP-0.2");
    Stage s = getStageWithServerAction(1, 977, hostname, payload, "test");
    stages.add(s);
    db.persistActions(stages);

    ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
        new HostsMap((String) null), new ServerActionManagerImpl(fsm));
    scheduler.start();

    while (!stages.get(0).getHostRoleStatus(hostname, "AMBARI_SERVER_ACTION")
        .equals(HostRoleStatus.COMPLETED)) {
      Thread.sleep(100);
    }
    scheduler.stop();
    assertEquals(stages.get(0).getHostRoleStatus(hostname, "AMBARI_SERVER_ACTION"),
        HostRoleStatus.COMPLETED);

    stages = new ArrayList<Stage>();
    payload.remove(ServerAction.PayloadName.CLUSTER_NAME);
    s = getStageWithServerAction(1, 23, hostname, payload, "test");
    stages.add(s);
    db.persistActions(stages);

    scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
        new HostsMap((String) null), new ServerActionManagerImpl(fsm));
    scheduler.start();

    while (!stages.get(0).getHostRoleStatus(hostname, "AMBARI_SERVER_ACTION")
        .equals(HostRoleStatus.FAILED)) {
      Thread.sleep(100);
    }
    scheduler.stop();
    assertEquals(stages.get(0).getHostRoleStatus(hostname, "AMBARI_SERVER_ACTION"),
        HostRoleStatus.FAILED);
    assertEquals("test", stages.get(0).getRequestContext());
  }

  private static Stage getStageWithServerAction(long requestId, long stageId, String hostName,
                                                Map<String, String> payload, String requestContext) {
    Stage stage = new Stage(requestId, "/tmp", "cluster1", requestContext);
    stage.setStageId(stageId);
    long now = System.currentTimeMillis();
    stage.addServerActionCommand(ServerAction.Command.FINALIZE_UPGRADE, Role.AMBARI_SERVER_ACTION,
        RoleCommand.EXECUTE, "cluster1",
        new ServiceComponentHostUpgradeEvent("AMBARI_SERVER_ACTION", hostName, now, "HDP-0.2"),
        hostName);
    ExecutionCommand execCmd = stage.getExecutionCommandWrapper(hostName,
        Role.AMBARI_SERVER_ACTION.toString()).getExecutionCommand();

    execCmd.setCommandParams(payload);
    return stage;
  }

  @Test
  public void testRequestFailureOnStageFailure() throws Exception {
    ActionQueue aq = new ActionQueue();
    Clusters fsm = mock(Clusters.class);
    Cluster oneClusterMock = mock(Cluster.class);
    Service serviceObj = mock(Service.class);
    ServiceComponent scomp = mock(ServiceComponent.class);
    ServiceComponentHost sch = mock(ServiceComponentHost.class);
    when(fsm.getCluster(anyString())).thenReturn(oneClusterMock);
    when(oneClusterMock.getService(anyString())).thenReturn(serviceObj);
    when(serviceObj.getServiceComponent(anyString())).thenReturn(scomp);
    when(scomp.getServiceComponentHost(anyString())).thenReturn(sch);
    when(serviceObj.getCluster()).thenReturn(oneClusterMock);

    ActionDBAccessor db = new ActionDBInMemoryImpl();
    String hostname = "ahost.ambari.apache.org";
    List<Stage> stages = new ArrayList<Stage>();
    stages.add(
        getStageWithSingleTask(
            hostname, "cluster1", Role.NAMENODE, RoleCommand.UPGRADE, Service.Type.HDFS, 1, 1, 1));
    stages.add(
        getStageWithSingleTask(
            hostname, "cluster1", Role.DATANODE, RoleCommand.UPGRADE, Service.Type.HDFS, 2, 2, 1));
    db.persistActions(stages);

    ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
        new HostsMap((String) null), new ServerActionManagerImpl(fsm));
    ActionManager am = new ActionManager(
        2, 2, aq, fsm, db, new HostsMap((String) null), new ServerActionManagerImpl(fsm));

    scheduler.doWork();

    List<CommandReport> reports = new ArrayList<CommandReport>();
    reports.add(getCommandReport(HostRoleStatus.FAILED, Role.NAMENODE, Service.Type.HDFS, "1-1", 1));
    am.processTaskResponse(hostname, reports);

    scheduler.doWork();
    Assert.assertEquals(HostRoleStatus.FAILED, stages.get(0).getHostRoleStatus(hostname, "NAMENODE"));
    Assert.assertEquals(HostRoleStatus.ABORTED, stages.get(1).getHostRoleStatus(hostname, "DATANODE"));
  }

  @Test
  public void testRequestFailureBasedOnSuccessFactor() throws Exception {
    ActionQueue aq = new ActionQueue();
    Clusters fsm = mock(Clusters.class);
    Cluster oneClusterMock = mock(Cluster.class);
    Service serviceObj = mock(Service.class);
    ServiceComponent scomp = mock(ServiceComponent.class);
    ServiceComponentHost sch = mock(ServiceComponentHost.class);
    when(fsm.getCluster(anyString())).thenReturn(oneClusterMock);
    when(oneClusterMock.getService(anyString())).thenReturn(serviceObj);
    when(serviceObj.getServiceComponent(anyString())).thenReturn(scomp);
    when(scomp.getServiceComponentHost(anyString())).thenReturn(sch);
    when(serviceObj.getCluster()).thenReturn(oneClusterMock);

    ActionDBAccessor db = new ActionDBInMemoryImpl();
    List<Stage> stages = new ArrayList<Stage>();

    long now = System.currentTimeMillis();
    Stage stage = new Stage(1, "/tmp", "cluster1", "testRequestFailureBasedOnSuccessFactor");
    stage.setStageId(1);
    stage.addHostRoleExecutionCommand("host1", Role.DATANODE, RoleCommand.UPGRADE,
        new ServiceComponentHostUpgradeEvent(Role.DATANODE.toString(), "host1", now, "HDP-0.2"),
        "cluster1", Service.Type.HDFS.toString());
    stage.getExecutionCommandWrapper("host1",
        Role.DATANODE.toString()).getExecutionCommand();

    stage.addHostRoleExecutionCommand("host2", Role.DATANODE, RoleCommand.UPGRADE,
        new ServiceComponentHostUpgradeEvent(Role.DATANODE.toString(), "host2", now, "HDP-0.2"),
        "cluster1", Service.Type.HDFS.toString());
    stage.getExecutionCommandWrapper("host2",
        Role.DATANODE.toString()).getExecutionCommand();

    stage.addHostRoleExecutionCommand("host3", Role.DATANODE, RoleCommand.UPGRADE,
        new ServiceComponentHostUpgradeEvent(Role.DATANODE.toString(), "host3", now, "HDP-0.2"),
        "cluster1", Service.Type.HDFS.toString());
    stage.getExecutionCommandWrapper("host3",
        Role.DATANODE.toString()).getExecutionCommand();
    stages.add(stage);

    stage.getOrderedHostRoleCommands().get(0).setTaskId(1);
    stage.getOrderedHostRoleCommands().get(1).setTaskId(2);
    stage.getOrderedHostRoleCommands().get(2).setTaskId(3);

    stages.add(
        getStageWithSingleTask(
            "host1", "cluster1", Role.HDFS_CLIENT, RoleCommand.UPGRADE, Service.Type.HDFS, 4, 2, 1));
    db.persistActions(stages);

    ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
        new HostsMap((String) null), new ServerActionManagerImpl(fsm));
    ActionManager am = new ActionManager(
        2, 2, aq, fsm, db, new HostsMap((String) null), new ServerActionManagerImpl(fsm));

    scheduler.doWork();

    List<CommandReport> reports = new ArrayList<CommandReport>();
    reports.add(getCommandReport(HostRoleStatus.FAILED, Role.DATANODE, Service.Type.HDFS, "1-1", 1));
    am.processTaskResponse("host1", reports);

    reports.clear();
    reports.add(getCommandReport(HostRoleStatus.FAILED, Role.DATANODE, Service.Type.HDFS, "1-1", 2));
    am.processTaskResponse("host2", reports);

    reports.clear();
    reports.add(getCommandReport(HostRoleStatus.COMPLETED, Role.DATANODE, Service.Type.HDFS, "1-1", 3));
    am.processTaskResponse("host3", reports);

    scheduler.doWork();
    Assert.assertEquals(HostRoleStatus.ABORTED, stages.get(1).getHostRoleStatus("host1", "HDFS_CLIENT"));
  }

  private CommandReport getCommandReport(HostRoleStatus status, Role role, Service.Type service, String actionId,
                                         int taskId) {
    CommandReport report = new CommandReport();
    report.setExitCode(999);
    report.setStdErr("");
    report.setStdOut("");
    report.setStatus(status.toString());
    report.setRole(role.toString());
    report.setServiceName(service.toString());
    report.setActionId(actionId);
    report.setTaskId(taskId);
    return report;
  }

  private Stage getStageWithSingleTask(String hostname, String clusterName, Role role,
                                       RoleCommand roleCommand, Service.Type service, int taskId,
                                       int stageId, int requestId) {
    Stage stage = new Stage(requestId, "/tmp", clusterName, "getStageWithSingleTask");
    stage.setStageId(stageId);
    stage.addHostRoleExecutionCommand(hostname, role, roleCommand,
        new ServiceComponentHostUpgradeEvent(role.toString(), hostname, System.currentTimeMillis(), "HDP-0.2"),
        clusterName, service.toString());
    stage.getExecutionCommandWrapper(hostname,
        role.toString()).getExecutionCommand();
    stage.getOrderedHostRoleCommands().get(0).setTaskId(taskId);
    return stage;
  }

  @Test
  public void testSuccessFactors() {
    Stage s = StageUtils.getATestStage(1, 1);
    assertEquals(new Float(0.5), new Float(s.getSuccessFactor(Role.DATANODE)));
    assertEquals(new Float(0.5), new Float(s.getSuccessFactor(Role.TASKTRACKER)));
    assertEquals(new Float(0.5), new Float(s.getSuccessFactor(Role.GANGLIA_MONITOR)));
    assertEquals(new Float(0.5), new Float(s.getSuccessFactor(Role.HBASE_REGIONSERVER)));
    assertEquals(new Float(1.0), new Float(s.getSuccessFactor(Role.NAMENODE)));
    assertEquals(new Float(1.0), new Float(s.getSuccessFactor(Role.GANGLIA_SERVER)));
  }
  
  @Test
  public void testSuccessCriteria() {
    RoleStats rs1 = new RoleStats(1, (float)0.5);
    rs1.numSucceeded = 1;
    assertTrue(rs1.isSuccessFactorMet());
    rs1.numSucceeded = 0;
    assertFalse(rs1.isSuccessFactorMet());
    
    RoleStats rs2 = new RoleStats(2, (float)0.5);
    rs2.numSucceeded = 1;
    assertTrue(rs2.isSuccessFactorMet());
    
    RoleStats rs3 = new RoleStats(3, (float)0.5);
    rs3.numSucceeded = 2;
    assertTrue(rs2.isSuccessFactorMet());
    rs3.numSucceeded = 1;
    assertFalse(rs3.isSuccessFactorMet());
    
    RoleStats rs4 = new RoleStats(3, (float)1.0);
    rs4.numSucceeded = 2;
    assertFalse(rs3.isSuccessFactorMet());
  }
}
