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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hive.ql.exec.UDF;


public class UDFMonth extends UDF {

  private static Log LOG = LogFactory.getLog(UDFMonth.class.getName());

  private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
  private Calendar calendar = Calendar.getInstance();

  public UDFMonth() {
  }

  /**
   * Get the month from a date string.
   * 
   * @param dateString the dateString in the format of "yyyy-MM-dd HH:mm:ss" or "yyyy-MM-dd".
   * @return an int from 1 to 12. null if the dateString is not a valid date string.
   */
  public Integer evaluate(String dateString)  {
    
    try {
      Date date = formatter.parse(dateString);
      calendar.setTime(date);
      return 1 + calendar.get(Calendar.MONTH);
    } catch (ParseException e) {
      return null;
    }
  }

}
