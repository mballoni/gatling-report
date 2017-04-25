/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Benoit Delbosc
 */

import net.quux00.simplecsv.CsvParser;
import net.quux00.simplecsv.CsvParserBuilder;
import net.quux00.simplecsv.CsvReader;
import net.quux00.simplecsv.CsvReaderBuilder;

import java.io.*;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class SimulationParser {

  private static final int SCENARIO = 1;
  private static final int SIMULATION_NAME = 3;
  private static final int SCENARIO_ACTION = 0;
  private static final int SIMULATION_START_DATE = 4;
  private static final int USER_ACTION = 3;
  private static final int REQUEST_NAME = 4;
  private static final int VERSION = 6;
  private static final int REQUEST_START_DATE = 5;
  private static final int REQUEST_END_DATE = 6;
  private static final int REQUEST_RESULT = 7;

  private static final String OK = "OK";
  private static final String REQUEST = "REQUEST";
  private static final String RUN = "RUN";
  private static final String USER = "USER";
  private static final String START = "START";
  private static final String END = "END";
  private static final String GZ = "gz";
  private final File file;
  private final Float apdexT;

  public SimulationParser(File file, Float apdexT) {
    this.file = file;
    this.apdexT = apdexT;
  }

  public SimulationParser(File file) {
    this.file = file;
    this.apdexT = null;
  }

  public SimulationContext parse() throws IOException {
    SimulationContext ret = new SimulationContext(file.getAbsolutePath(), apdexT);
    CsvParser p = new CsvParserBuilder().trimWhitespace(true).allowUnbalancedQuotes(true).separator('\t').build();
    CsvReader reader = new CsvReaderBuilder(getReaderFor(file)).csvParser(p).build();
    List<String> line;
    String name;
    String scenario;
    long start, end;
    boolean success;
    while ((line = reader.readNext()) != null) {
      if (line.size() <= 2) {
        invalidFile();
      }
      scenario = line.get(SCENARIO);
      switch (line.get(SCENARIO_ACTION)) {
        case RUN:
          String version = line.get(VERSION);
          if (!version.startsWith("2.")) {
            return invalidFile();
          }
          ret.setSimulationName(line.get(SIMULATION_NAME));
          ret.setStart(Long.parseLong(line.get(SIMULATION_START_DATE)));
          break;
        case REQUEST:
          name = line.get(REQUEST_NAME);
          start = Long.parseLong(line.get(REQUEST_START_DATE));
          end = Long.parseLong(line.get(REQUEST_END_DATE));
          success = OK.equals(line.get(REQUEST_RESULT));
          ret.addRequest(scenario, name, start, end, success);
          break;
        case USER:
          switch (line.get(USER_ACTION)) {
            case START:
              ret.addUser(scenario);
              break;
            case END:
              ret.endUser(scenario);
              break;
          }
          break;
      }
    }
    ret.computeStat();
    return ret;
  }

  private SimulationContext invalidFile() {
    throw new IllegalArgumentException(String.format("Invalid simulation file: %s expecting " +
        "Gatling 2.x format", file.getAbsolutePath()));
  }

  private String getFileExtension(File file) {
    String name = file.getName();
    try {
      return name.substring(name.lastIndexOf(".") + 1);
    } catch (Exception e) {
      return "";
    }
  }

  private Reader getReaderFor(File file) throws IOException {
    if (GZ.equals(getFileExtension(file))) {
      InputStream fileStream = new FileInputStream(file);
      InputStream gzipStream = new GZIPInputStream(fileStream);
      return new InputStreamReader(gzipStream, "UTF-8");
    }
    return new FileReader(file);
  }

}
