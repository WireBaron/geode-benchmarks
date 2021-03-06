/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.geode.perftest.jvms;

import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.geode.perftest.infrastructure.Infrastructure;
import org.apache.geode.perftest.jdk.RMI;
import org.apache.geode.perftest.jvms.classpath.ClassPathCopier;
import org.apache.geode.perftest.jvms.rmi.Controller;
import org.apache.geode.perftest.jvms.rmi.ControllerFactory;

/**
 * Factory for launching JVMs and a given infrastructure and setting up RMI
 * access to all JVMs.
 */
public class RemoteJVMFactory {
  private static final Logger logger = LoggerFactory.getLogger(RemoteJVMFactory.class);

  public static final String RMI_HOST = "RMI_HOST";
  public static final String RMI_PORT_PROPERTY = "RMI_PORT";
  public static final String CONTROLLER = "CONTROLLER";
  public static final String JVM_ID = "JVM_ID";
  public static final int RMI_PORT = 33333;
  public static final String CLASSPATH = System.getProperty("java.class.path");
  public static final String JAVA_HOME = System.getProperty("java.home");
  private final JVMLauncher jvmLauncher;
  private final ClassPathCopier classPathCopier;
  private final ControllerFactory controllerFactory;

  public RemoteJVMFactory(JVMLauncher jvmLauncher, RMI rmi,
                          ClassPathCopier classPathCopier,
                          ControllerFactory controllerFactory) {
    this.jvmLauncher = jvmLauncher;
    this.classPathCopier = classPathCopier;
    this.controllerFactory = controllerFactory;
  }
  public RemoteJVMFactory() {
    this(new JVMLauncher(), new RMI(), new ClassPathCopier(CLASSPATH, JAVA_HOME), new ControllerFactory());
  }

  /**
   * Start all requested JVMs on the infrastructure
   * @param infra The infrastructure to use
   * @param roles The JVMs to start. Keys a roles and values are the number
   * of JVMs in that role.
   *
   * @return a {@link RemoteJVMs} object used to access the JVMs through RMI
   */
  public RemoteJVMs launch(Infrastructure infra,
                           Map<String, Integer> roles) throws Exception {

    Set<Infrastructure.Node> nodes = infra.getNodes();
    int numWorkers = roles.values().stream().mapToInt(Integer::intValue).sum();

    if(nodes.size() < numWorkers) {
      throw new IllegalStateException("Too few nodes for test. Need " + numWorkers + ", have " + nodes.size());
    }

    Controller controller = controllerFactory.createController(numWorkers);

    classPathCopier.copyToNodes(infra);

    List<JVMMapping> mapping = mapRolesToNodes(roles, nodes);
    CompletableFuture<Void> processesExited = jvmLauncher.launchProcesses(infra, RMI_PORT, mapping);

    if(!controller.waitForWorkers(5, TimeUnit.MINUTES)) {
      throw new IllegalStateException("Workers failed to start in 1 minute");
    }

    return new RemoteJVMs(mapping, controller, processesExited);
  }

  private List<JVMMapping> mapRolesToNodes(Map<String, Integer> roles,
                                           Set<Infrastructure.Node> nodes) {


    List<JVMMapping> mapping = new ArrayList<>();
    Iterator<Infrastructure.Node> nodeItr = nodes.iterator();

    int id = 0;
    for(Map.Entry<String, Integer> roleEntry : roles.entrySet()) {
      for(int i = 0; i < roleEntry.getValue(); i++) {
        Infrastructure.Node node = nodeItr.next();
        mapping.add(new JVMMapping(node, roleEntry.getKey(), id++));
      }

    }
    return mapping;
  }

}
