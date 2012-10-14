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

package org.apache.ambari.server.state;

import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.cluster.ClustersImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ServiceTest {

  private Clusters clusters;
  private Cluster cluster;
  private String clusterName;

  @Before
  public void setup() throws AmbariException {
    clusters = new ClustersImpl();
    clusterName = "foo";
    clusters.addCluster(clusterName);
    cluster = clusters.getCluster(clusterName);
    Assert.assertNotNull(cluster);
  }

  @After
  public void teardown() throws AmbariException {
    clusters = null;
    cluster = null;
  }

  @Test
  public void testCreateService() throws AmbariException {
    String serviceName = "s1";
    Service s = new ServiceImpl(cluster, serviceName);
    cluster.addService(s);
    Service service = cluster.getService(serviceName);

    Assert.assertNotNull(service);
    Assert.assertEquals(serviceName, service.getName());
    Assert.assertEquals(cluster.getClusterId(),
        service.getCluster().getClusterId());
    Assert.assertEquals(cluster.getClusterName(),
        service.getCluster().getClusterName());
    Assert.assertEquals(State.INIT, service.getDesiredState());
    Assert.assertTrue(
        service.getDesiredStackVersion().getStackVersion().isEmpty());
  }

  @Test
  public void testGetAndSetServiceInfo() throws AmbariException {
    String serviceName = "s1";
    Service s = new ServiceImpl(cluster, serviceName);
    cluster.addService(s);
    Service service = cluster.getService(serviceName);

    Assert.assertNotNull(service);

    service.setDesiredStackVersion(new StackVersion("1.1.0"));
    Assert.assertEquals("1.1.0",
        service.getDesiredStackVersion().getStackVersion());


    service.setDesiredState(State.INSTALLING);
    Assert.assertEquals(State.INSTALLING, service.getDesiredState());

  }



  /*



  public ServiceComponent getServiceComponent(String componentName)
      throws AmbariException;

  public Map<String, ServiceComponent> getServiceComponents();

  public void addServiceComponents(Map<String, ServiceComponent> components)
      throws AmbariException;

  public void addServiceComponent(ServiceComponent component)
      throws AmbariException;


  public Map<String, Config> getDesiredConfigs();

  public void updateDesiredConfigs(Map<String, Config> configs);


  public Map<String, Config> getConfigs();

  public void updateConfigs(Map<String, Config> configs);

  public StackVersion getStackVersion();

  public void setStackVersion(StackVersion stackVersion);

  public ServiceResponse convertToResponse();


    */
}