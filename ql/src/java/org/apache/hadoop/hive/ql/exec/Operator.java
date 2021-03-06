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

package org.apache.hadoop.hive.ql.exec;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.Map;
import org.apache.hadoop.hive.ql.lib.Node;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.plan.explain;
import org.apache.hadoop.hive.ql.plan.mapredWork;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;

/**
 * Base operator implementation
 **/
public abstract class Operator <T extends Serializable> implements Serializable, Node {

  // Bean methods

  private static final long serialVersionUID = 1L;
  
  protected List<Operator<? extends Serializable>> childOperators;
  protected List<Operator<? extends Serializable>> parentOperators;

  public Operator() {}
  
  /**
   * Create an operator with a reporter.
   * @param reporter Used to report progress of certain operators.
   */
  public Operator(Reporter reporter) {
    this.reporter = reporter;
  }

  public void setChildOperators(List<Operator<? extends Serializable>> childOperators) {
    this.childOperators = childOperators;
  }

  public List<Operator<? extends Serializable>> getChildOperators() {
    return childOperators;
  }

  /**
   * Implements the getChildren function for the Node Interface.
   */
  public Vector<Node> getChildren() {
    
    if (getChildOperators() == null) {
      return null;
    }
    
    Vector<Node> ret_vec = new Vector<Node>();
    for(Operator<? extends Serializable> op: getChildOperators()) {
      ret_vec.add(op);
    }
    
    return ret_vec;
  }
  
  public void setParentOperators(List<Operator<? extends Serializable>> parentOperators) {
    this.parentOperators = parentOperators;
  }

  public List<Operator<? extends Serializable>> getParentOperators() {
    return parentOperators;
  }

  protected String id;
  protected T conf;
  protected boolean done;

  public void setConf(T conf) {
    this.conf = conf;
  }

  @explain
  public T getConf() {
    return conf;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public boolean getDone() {
    return done;
  }

  public void setDone(boolean done) {
    this.done = done;
  }

  // non-bean fields needed during compilation
  transient private RowSchema rowSchema;

  public void setSchema(RowSchema rowSchema) {
    this.rowSchema = rowSchema;
  }

  public RowSchema getSchema() {
    return rowSchema;
  }
  
  // non-bean ..

  transient protected HashMap<Enum<?>, LongWritable> statsMap = new HashMap<Enum<?>, LongWritable> ();
  transient protected OutputCollector out;
  transient protected Log LOG = LogFactory.getLog(this.getClass().getName());;
  transient protected mapredWork gWork;
  transient protected String alias;
  transient protected String joinAlias;
  transient protected Reporter reporter;

  public void setOutputCollector(OutputCollector out) {
    this.out = out;

    // the collector is same across all operators
    if(childOperators == null)
      return;

    for(Operator<? extends Serializable> op: childOperators) {
      op.setOutputCollector(out);
    }
  }

  /**
   * Operators often need access to global variables. This allows
   * us to put global config information in the root configuration
   * object and have that be accessible to all the operators in the
   * tree.
   */
  public void setMapredWork(mapredWork gWork) {
    this.gWork = gWork;

    if(childOperators == null)
      return;

    for(Operator<? extends Serializable> op: childOperators) {
      op.setMapredWork(gWork);
    }
  }

  /**
   * Store the alias this operator is working on behalf of
   */
  public void setAlias(String alias) {
    this.alias = alias;

    if(childOperators == null)
      return;

    for(Operator<? extends Serializable> op: childOperators) {
      op.setAlias(alias);
    }
  }

  /**
   * Store the join alias this operator is working on behalf of
   */
  public void setJoinAlias(String joinAlias) {
    this.joinAlias = joinAlias;

    if(childOperators == null)
      return;

    for(Operator<? extends Serializable> op: childOperators) {
      op.setJoinAlias(joinAlias);
    }
  }



  public Map<Enum<?>, Long> getStats() {
    HashMap<Enum<?>, Long> ret = new HashMap<Enum<?>, Long> ();
    for(Enum<?> one: statsMap.keySet()) {
      ret.put(one, Long.valueOf(statsMap.get(one).get()));
    }
    return(ret);
  }

  public void initialize (Configuration hconf, Reporter reporter) throws HiveException {
    LOG.info("Initializing Self");
    this.reporter = reporter;
    
    if(childOperators == null) {
      return;
    }
    LOG.info("Initializing children:");
    for(Operator<? extends Serializable> op: childOperators) {
      op.initialize(hconf, reporter);
    }    
    LOG.info("Initialization Done");
  }

  public abstract void process(Object row, ObjectInspector rowInspector) throws HiveException;
 
  // If a operator wants to do some work at the beginning of a group
  public void startGroup() throws HiveException {
    LOG.debug("Starting group");
    
    if (childOperators == null)
      return;
    
    LOG.debug("Starting group for children:");
    for (Operator<? extends Serializable> op: childOperators)
      op.startGroup();
    
    LOG.debug("Start group Done");
  }  
  
  // If a operator wants to do some work at the beginning of a group
  public void endGroup() throws HiveException
  {
    LOG.debug("Ending group");
    
    if (childOperators == null)
      return;
    
    LOG.debug("Ending group for children:");
    for (Operator<? extends Serializable> op: childOperators)
      op.endGroup();
    
    LOG.debug("End group Done");
  }

  public void close(boolean abort) throws HiveException {
    try {
      logStats();
      if(childOperators == null)
        return;

      for(Operator<? extends Serializable> op: childOperators) {
        op.close(abort);
      }
    } catch (HiveException e) {
    }
  }

  /**
   * Unlike other operator interfaces which are called from map or reduce task,
   * jobClose is called from the jobclient side once the job has completed
   *
   * @param conf Configuration with with which job was submitted
   * @param succes whether the job was completed successfully or not
   */
  public void jobClose(Configuration conf, boolean success) throws HiveException {
    if(childOperators == null)
      return;
    
    for(Operator<? extends Serializable> op: childOperators) {
      op.jobClose(conf, success);
    }
  }

  protected void forward(Object row, ObjectInspector rowInspector) throws HiveException {
    
    if((childOperators == null) || (getDone())) {
      return;
    }
    
    // if all children are done, this operator is also done
    boolean isDone = true;
    for(Operator<? extends Serializable> o: childOperators) {
      if (!o.getDone()) {
        isDone = false;
        break;
      }
    }

    if (isDone) {
      setDone(isDone);
      return;
    }

    for(Operator<? extends Serializable> o: childOperators) {
      o.process(row, rowInspector);
    }
  }

  public void resetStats() {
    for(Enum<?> e: statsMap.keySet()) {
      statsMap.get(e).set(0L);
    }
  }


  public static interface OperatorFunc {
    public void func(Operator<? extends Serializable> op);
  }

  public void preorderMap (OperatorFunc opFunc) {
    opFunc.func(this);
    if(childOperators != null) {
      for(Operator<? extends Serializable> o: childOperators) {
        o.preorderMap(opFunc);
      }
    }
  }

  public void logStats () {
    for(Enum<?> e: statsMap.keySet()) {
      LOG.info(e.toString() + ":" + statsMap.get(e).toString());
    }    
  }

  /**
   * Implements the getName function for the Node Interface.
   * @return the name of the operator
   */
  public String getName() {
    return new String("OP");
  }

}
