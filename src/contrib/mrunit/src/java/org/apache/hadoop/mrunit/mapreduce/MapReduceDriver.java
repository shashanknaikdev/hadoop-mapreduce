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
package org.apache.hadoop.mrunit.mapreduce;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mrunit.MapReduceDriverBase;
import org.apache.hadoop.mrunit.types.Pair;

/**
 * Harness that allows you to test a Mapper and a Reducer instance together
 * You provide the input key and value that should be sent to the Mapper, and
 * outputs you expect to be sent by the Reducer to the collector for those
 * inputs. By calling runTest(), the harness will deliver the input to the
 * Mapper, feed the intermediate results to the Reducer (without checking
 * them), and will check the Reducer's outputs against the expected results.
 * This is designed to handle a single (k, v)* -> (k, v)* case from the
 * Mapper/Reducer pair, representing a single unit test.
 */
public class MapReduceDriver<K1, V1, K2, V2, K3, V3>
    extends MapReduceDriverBase<K1, V1, K2, V2, K3, V3> {

  public static final Log LOG = LogFactory.getLog(MapReduceDriver.class);

  private Mapper<K1, V1, K2, V2> myMapper;
  private Reducer<K2, V2, K3, V3> myReducer;
  private Counters counters;

  public MapReduceDriver(final Mapper<K1, V1, K2, V2> m,
                         final Reducer<K2, V2, K3, V3> r) {
    myMapper = m;
    myReducer = r;
    counters = new Counters();
  }

  public MapReduceDriver() {
    counters = new Counters();
  }

  /** Set the Mapper instance to use with this test driver
   * @param m the Mapper instance to use */
  public void setMapper(Mapper<K1, V1, K2, V2> m) {
    myMapper = m;
  }

  /** Sets the Mapper instance to use and returns self for fluent style */
  public MapReduceDriver<K1, V1, K2, V2, K3, V3> withMapper(
          Mapper<K1, V1, K2, V2> m) {
    setMapper(m);
    return this;
  }

  /**
   * @return the Mapper object being used by this test
   */
  public Mapper<K1, V1, K2, V2> getMapper() {
    return myMapper;
  }

  /**
   * Sets the reducer object to use for this test
   * @param r The reducer object to use
   */
  public void setReducer(Reducer<K2, V2, K3, V3> r) {
    myReducer = r;
  }

  /**
   * Identical to setReducer(), but with fluent programming style
   * @param r The Reducer to use
   * @return this
   */
  public MapReduceDriver<K1, V1, K2, V2, K3, V3> withReducer(
          Reducer<K2, V2, K3, V3> r) {
    setReducer(r);
    return this;
  }

  /**
   * @return the Reducer object being used for this test
   */
  public Reducer<K2, V2, K3, V3> getReducer() {
    return myReducer;
  }

  /** @return the counters used in this test */
  public Counters getCounters() {
    return counters;
  }

  /** Sets the counters object to use for this test.
   * @param ctrs The counters object to use.
   */
  public void setCounters(final Counters ctrs) {
    this.counters = ctrs;
  }

  /** Sets the counters to use and returns self for fluent style */
  public MapReduceDriver<K1, V1, K2, V2, K3, V3> withCounters(final Counters ctrs) {
    setCounters(ctrs);
    return this;
  }

  /**
   * Identical to addInput() but returns self for fluent programming style
   * @param key
   * @param val
   * @return this
   */
  public MapReduceDriver<K1, V1, K2, V2, K3, V3> withInput(K1 key, V1 val) {
    addInput(key, val);
    return this;
  }

  /**
   * Identical to addInput() but returns self for fluent programming style
   * @param input The (k, v) pair to add
   * @return this
   */
  public MapReduceDriver<K1, V1, K2, V2, K3, V3> withInput(
      Pair<K1, V1> input) {
    addInput(input);
    return this;
  }

  /**
   * Works like addOutput(), but returns self for fluent style
   * @param outputRecord
   * @return this
   */
  public MapReduceDriver<K1, V1, K2, V2, K3, V3> withOutput(
          Pair<K3, V3> outputRecord) {
    addOutput(outputRecord);
    return this;
  }

  /**
   * Functions like addOutput() but returns self for fluent programming style
   * @param key
   * @param val
   * @return this
   */
  public MapReduceDriver<K1, V1, K2, V2, K3, V3> withOutput(K3 key, V3 val) {
    addOutput(key, val);
    return this;
  }

  /**
   * Identical to addInputFromString, but with a fluent programming style
   * @param input A string of the form "key \t val". Trims any whitespace.
   * @return this
   */
  public MapReduceDriver<K1, V1, K2, V2, K3, V3> withInputFromString(String input) {
    addInputFromString(input);
    return this;
  }

  /**
   * Identical to addOutputFromString, but with a fluent programming style
   * @param output A string of the form "key \t val". Trims any whitespace.
   * @return this
   */
  public MapReduceDriver<K1, V1, K2, V2, K3, V3> withOutputFromString(String output) {
    addOutputFromString(output);
    return this;
  }

  public List<Pair<K3, V3>> run() throws IOException {

    List<Pair<K2, V2>> mapOutputs = new ArrayList<Pair<K2, V2>>();

    // run map component
    for (Pair<K1, V1> input : inputList) {
      LOG.debug("Mapping input " + input.toString() + ")");

      mapOutputs.addAll(new MapDriver<K1, V1, K2, V2>(myMapper).withInput(
              input).withCounters(getCounters()).run());
    }

    List<Pair<K2, List<V2>>> reduceInputs = shuffle(mapOutputs);
    List<Pair<K3, V3>> reduceOutputs = new ArrayList<Pair<K3, V3>>();

    for (Pair<K2, List<V2>> input : reduceInputs) {
      K2 inputKey = input.getFirst();
      List<V2> inputValues = input.getSecond();
      StringBuilder sb = new StringBuilder();
      formatValueList(inputValues, sb);
      LOG.debug("Reducing input (" + inputKey.toString() + ", "
          + sb.toString() + ")");

      reduceOutputs.addAll(new ReduceDriver<K2, V2, K3, V3>(myReducer)
              .withCounters(getCounters())
              .withInputKey(inputKey).withInputValues(inputValues).run());
    }

    return reduceOutputs;
  }

  @Override
  public String toString() {
    return "MapReduceDriver (0.20+) (" + myMapper + ", " + myReducer + ")";
  }
}
