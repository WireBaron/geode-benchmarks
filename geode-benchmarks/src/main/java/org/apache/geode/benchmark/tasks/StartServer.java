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

package org.apache.geode.benchmark.tasks;

import java.net.InetAddress;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.cache.server.CacheServer;
import org.apache.geode.distributed.ConfigurationProperties;
import org.apache.geode.perftest.Task;
import org.apache.geode.perftest.TestContext;

public class StartServer implements Task {

  private int locatorPort;

  public StartServer(int locatorPort) {
    this.locatorPort = locatorPort;
  }

  @Override
  public void run(TestContext context) throws Exception {

    String locatorString = LocatorUtil.getLocatorString(context, locatorPort);

    Cache cache = new CacheFactory()
        .set(ConfigurationProperties.LOCATORS,locatorString)
        .set(ConfigurationProperties.NAME,"server-"+ InetAddress.getLocalHost())
        .set(ConfigurationProperties.STATISTIC_SAMPLING_ENABLED,"true")
        .set(ConfigurationProperties.STATISTIC_ARCHIVE_FILE,"output/stats.gfs")
        .create();

    CacheServer cacheServer = ((Cache) cache).addCacheServer();
    cacheServer.setPort(0);
    cacheServer.start();

    cache.createRegionFactory(RegionShortcut.PARTITION).create("region");
  }

}
