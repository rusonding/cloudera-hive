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

package org.apache.hadoop.hive.ql.udf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hive.ql.exec.UDF;


public class UDFOPNegative extends UDF {

  private static Log LOG = LogFactory.getLog(UDFOPNegative.class.getName());

  public UDFOPNegative() {
  }

  public Byte evaluate(Byte a)  {
    if (a == null)
      return null;

    Byte r = Byte.valueOf((byte)-a.byteValue());
    return r;
  }

  public Integer evaluate(Integer a)  {
    if (a == null)
      return null;

    Integer r = Integer.valueOf(-a.intValue());
    return r;
  }

  public Long evaluate(Long a)  {
    if (a == null)
      return null;

    Long r = Long.valueOf(-a.longValue());
    return r;
  }

  public Float evaluate(Float a)  {
    if (a == null)
      return null;

    Float r = Float.valueOf(-a.floatValue());
    return r;
  }

  public Double evaluate(Double a)  {
    if (a == null)
      return null;

    Double r = Double.valueOf(-a.doubleValue());
    return r;
  }

}
