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

package org.apache.ambari.server.security;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import junit.framework.TestCase;

public class CertGenerationTest extends TestCase {
	
  private static Log LOG = LogFactory.getLog(CertGenerationTest.class);
  public TemporaryFolder temp = new TemporaryFolder();

  Injector injector;

  private static CertificateManager certMan;

  @Inject
  static void init(CertificateManager instance) {
    certMan = instance;
  }


  private class SecurityModule extends AbstractModule {
    @Override
    protected void configure() {
      requestStaticInjection(CertGenerationTest.class);
    }
  }
	
  @Before
  public void setUp() throws IOException {
    temp.create();
    System.setProperty(Configuration.AMBARI_CONF_VAR, temp.getRoot().getAbsolutePath());
    Properties props = new Properties();
    props.setProperty(Configuration.SRVR_KSTR_DIR_KEY, temp.getRoot().getAbsolutePath());
    FileOutputStream out = new FileOutputStream(temp.getRoot().getAbsolutePath() + File.separator + Configuration.CONFIG_FILE);
    props.store(out, "");
    out.close();

    injector = Guice.createInjector(new SecurityModule());
    certMan = injector.getInstance(CertificateManager.class);

    certMan.initRootCert();
  }
	
  @After
  public void tearDown() throws IOException {
	  temp.delete();
  }
	
  @Test
  public void testServerCertGen() throws Exception {

    File serverCrt = new File(temp.getRoot().getAbsoluteFile() +
    						  File.separator + Configuration.SRVR_CRT_NAME_DEFAULT);
    assertTrue(serverCrt.exists());
  }

  @Test
  public void testServerKeyGen() throws Exception {

    File serverKey = new File(temp.getRoot().getAbsoluteFile() +
    						  File.separator + Configuration.SRVR_KEY_NAME_DEFAULT);
    assertTrue(serverKey.exists());
  }

  @Test
  public void testServerKeystoreGen() throws Exception {

    File serverKeyStrore = new File(temp.getRoot().getAbsoluteFile() +
    						  File.separator + Configuration.KSTR_NAME_DEFAULT);
    assertTrue(serverKeyStrore.exists());
  }
}