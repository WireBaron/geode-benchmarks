/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.perftest.yardstick;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class YardstickThroughputSensorParser {
  static final String sensorOutputFile = "ThroughputLatencyProbe.csv";

  private class SensorDatapoint {
    public int second;
    public float opsPerSec;
    public float avgLatency;

    SensorDatapoint(String dataLine) throws IOException {
      String[] data = dataLine.split(",");
      if (data.length != 3) {
        throw new IOException("Invalid data line: " + dataLine);
      }
      try {
        second = Integer.parseInt(data[0]);
        opsPerSec = Float.parseFloat(data[1]);
        avgLatency = Float.parseFloat(data[2]);
      } catch (NumberFormatException e) {
        throw new IOException("Invalid data line: " + dataLine);
      }
    }
  }

  private List<SensorDatapoint> datapoints = new ArrayList<>();

  public void parseResults(File resultDir) throws IOException {
    File sensorData = new File(resultDir, sensorOutputFile);
    BufferedReader dataStream = new BufferedReader(new FileReader(sensorData));
    String nextLine;

    while ((nextLine = dataStream.readLine()) != null) {
      if (nextLine.startsWith("--") ||
          nextLine.startsWith("@@") ||
          nextLine.startsWith("**")) {
        continue;
      }
      datapoints.add(new SensorDatapoint(nextLine));
    }
  }

  public float getAverageThroughput() {
    float accumulator = 0;
    for (SensorDatapoint datapoint : datapoints) {
      accumulator += datapoint.opsPerSec;
    }
    return  accumulator / datapoints.size();
  }
}
