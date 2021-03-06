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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.Void;

import org.apache.hadoop.hive.ql.exec.FunctionInfo.OperatorType;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.plan.groupByDesc;
import org.apache.hadoop.hive.ql.udf.*;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorUtils;

public class FunctionRegistry {

  private static Log LOG = LogFactory.getLog("org.apache.hadoop.hive.ql.exec.FunctionRegistry");

  /**
   * The mapping from expression function names to expression classes.
   */
  static HashMap<String, FunctionInfo> mFunctions;
  static {
    mFunctions = new HashMap<String, FunctionInfo>();
    registerUDF("default_sample_hashfn", UDFDefaultSampleHashFn.class, 
                OperatorType.PREFIX, false);
    registerUDF("concat", UDFConcat.class, OperatorType.PREFIX, false);
    registerUDF("substr", UDFSubstr.class, OperatorType.PREFIX, false);
    
    registerUDF("size", UDFSize.class, OperatorType.PREFIX, false);

    registerUDF("round", UDFRound.class, OperatorType.PREFIX, false);
    registerUDF("floor", UDFFloor.class, OperatorType.PREFIX, false);
    registerUDF("sqrt", UDFSqrt.class, OperatorType.PREFIX, false);
    registerUDF("ceil", UDFCeil.class, OperatorType.PREFIX, false);
    registerUDF("ceiling", UDFCeil.class, OperatorType.PREFIX, false);
    registerUDF("rand", UDFRand.class, OperatorType.PREFIX, false);

    registerUDF("ln", UDFLn.class, OperatorType.PREFIX, false);
    registerUDF("log2", UDFLog2.class, OperatorType.PREFIX, false);
    registerUDF("log10", UDFLog10.class, OperatorType.PREFIX, false);
    registerUDF("log", UDFLog.class, OperatorType.PREFIX, false);
    registerUDF("exp", UDFExp.class, OperatorType.PREFIX, false);
    registerUDF("power", UDFPower.class, OperatorType.PREFIX, false);
    registerUDF("pow", UDFPower.class, OperatorType.PREFIX, false);
    
    registerUDF("upper", UDFUpper.class, OperatorType.PREFIX, false);
    registerUDF("lower", UDFLower.class, OperatorType.PREFIX, false);
    registerUDF("ucase", UDFUpper.class, OperatorType.PREFIX, false);
    registerUDF("lcase", UDFLower.class, OperatorType.PREFIX, false);
    registerUDF("trim", UDFTrim.class, OperatorType.PREFIX, false);
    registerUDF("ltrim", UDFLTrim.class, OperatorType.PREFIX, false);
    registerUDF("rtrim", UDFRTrim.class, OperatorType.PREFIX, false);

    registerUDF("like", UDFLike.class, OperatorType.INFIX, true);
    registerUDF("rlike", UDFRegExp.class, OperatorType.INFIX, true);
    registerUDF("regexp", UDFRegExp.class, OperatorType.INFIX, true);
    registerUDF("regexp_replace", UDFRegExpReplace.class, OperatorType.PREFIX, false);

    registerUDF("positive", UDFOPPositive.class, OperatorType.PREFIX, true, "+");
    registerUDF("negative", UDFOPNegative.class, OperatorType.PREFIX, true, "-");

    registerUDF("day", UDFDayOfMonth.class, OperatorType.PREFIX, false);
    registerUDF("dayofmonth", UDFDayOfMonth.class, OperatorType.PREFIX, false);
    registerUDF("month", UDFMonth.class, OperatorType.PREFIX, false);
    registerUDF("year", UDFYear.class, OperatorType.PREFIX, false);
    registerUDF("from_unixtime", UDFFromUnixTime.class, OperatorType.PREFIX, false);
    registerUDF("to_date", UDFDate.class, OperatorType.PREFIX, false);

    registerUDF("get_json_object", UDFJson.class, OperatorType.PREFIX, false);

    registerUDF("+", UDFOPPlus.class, OperatorType.INFIX, true);
    registerUDF("-", UDFOPMinus.class, OperatorType.INFIX, true);
    registerUDF("*", UDFOPMultiply.class, OperatorType.INFIX, true);
    registerUDF("/", UDFOPDivide.class, OperatorType.INFIX, true);
    registerUDF("%", UDFOPMod.class, OperatorType.INFIX, true);

    registerUDF("&", UDFOPBitAnd.class, OperatorType.INFIX, true);
    registerUDF("|", UDFOPBitOr.class, OperatorType.INFIX, true);
    registerUDF("^", UDFOPBitXor.class, OperatorType.INFIX, true);
    registerUDF("~", UDFOPBitNot.class, OperatorType.PREFIX, true);

    registerUDF("=", UDFOPEqual.class, OperatorType.INFIX, true);
    registerUDF("==", UDFOPEqual.class, OperatorType.INFIX, true, "=");
    registerUDF("<>", UDFOPNotEqual.class, OperatorType.INFIX, true);
    registerUDF("<", UDFOPLessThan.class, OperatorType.INFIX, true);
    registerUDF("<=", UDFOPEqualOrLessThan.class, OperatorType.INFIX, true);
    registerUDF(">", UDFOPGreaterThan.class, OperatorType.INFIX, true);
    registerUDF(">=", UDFOPEqualOrGreaterThan.class, OperatorType.INFIX, true);

    registerUDF("and", UDFOPAnd.class, OperatorType.INFIX, true);
    registerUDF("&&", UDFOPAnd.class, OperatorType.INFIX, true, "and");
    registerUDF("or", UDFOPOr.class, OperatorType.INFIX, true);
    registerUDF("||", UDFOPOr.class, OperatorType.INFIX, true, "or");
    registerUDF("not", UDFOPNot.class, OperatorType.PREFIX, true);
    registerUDF("!", UDFOPNot.class, OperatorType.PREFIX, true, "not");

    registerUDF("isnull", UDFOPNull.class, OperatorType.POSTFIX, true, "is null");
    registerUDF("isnotnull", UDFOPNotNull.class, OperatorType.POSTFIX, true, "is not null");

    registerUDF("if", UDFIf.class, OperatorType.PREFIX, true);
    
    // Aliases for Java Class Names
    // These are used in getImplicitConvertUDFMethod
    registerUDF(Boolean.class.getName(), UDFToBoolean.class, OperatorType.PREFIX, false,
                UDFToBoolean.class.getSimpleName());
    registerUDF(Byte.class.getName(), UDFToByte.class, OperatorType.PREFIX, false,
                UDFToByte.class.getSimpleName());
    registerUDF(Short.class.getName(), UDFToShort.class, OperatorType.PREFIX, false,
                UDFToShort.class.getSimpleName());
    registerUDF(Integer.class.getName(), UDFToInteger.class, OperatorType.PREFIX, false,
                UDFToInteger.class.getSimpleName());
    registerUDF(Long.class.getName(), UDFToLong.class, OperatorType.PREFIX, false,
                UDFToLong.class.getSimpleName());
    registerUDF(Float.class.getName(), UDFToFloat.class, OperatorType.PREFIX, false,
                UDFToFloat.class.getSimpleName());
    registerUDF(Double.class.getName(), UDFToDouble.class, OperatorType.PREFIX, false,
                UDFToDouble.class.getSimpleName());
    registerUDF(String.class.getName(), UDFToString.class, OperatorType.PREFIX, false,
                UDFToString.class.getSimpleName());
    registerUDF(java.sql.Date.class.getName(), UDFToDate.class, OperatorType.PREFIX, false,
                UDFToDate.class.getSimpleName());

    // Aggregate functions
    registerUDAF("sum", UDAFSum.class);
    registerUDAF("count", UDAFCount.class);
    registerUDAF("max", UDAFMax.class);
    registerUDAF("min", UDAFMin.class);
    registerUDAF("avg", UDAFAvg.class);    
  }

  public static FunctionInfo getInfo(Class<?> fClass) {
    for(Map.Entry<String, FunctionInfo> ent: mFunctions.entrySet()) {
      FunctionInfo val = ent.getValue();
      if (val.getUDFClass() == fClass) {
        return val;
      }
      // Otherwise this is potentially an aggregate evaluator
      if (val.getUDAFClass() == fClass) {
        return val;
      }
      // Otherwise check if the aggregator is one of the classes within the UDAF
      if (val.getUDAFClass() != null) {
        for(Class<?> c: val.getUDAFClass().getClasses()) {
          if (c == fClass) {
            return val;
          }
        }
      }
    }

    return null;
  }

  public static void registerUDF(String functionName, Class<? extends UDF> UDFClass,
                                 FunctionInfo.OperatorType opt, boolean isOperator) {
    if (UDF.class.isAssignableFrom(UDFClass)) { 
      FunctionInfo fI = new FunctionInfo(functionName.toLowerCase(), UDFClass, null);
      fI.setIsOperator(isOperator);
      fI.setOpType(opt);
      mFunctions.put(functionName.toLowerCase(), fI);
    } else {
      throw new RuntimeException("Registering UDF Class " + UDFClass + " which does not extends " + UDF.class);
    }
  }
  
  public static void registerUDF(String functionName, Class<? extends UDF> UDFClass,
                                 FunctionInfo.OperatorType opt, boolean isOperator,
                                 String displayName) {
    if (UDF.class.isAssignableFrom(UDFClass)) { 
      FunctionInfo fI = new FunctionInfo(displayName, UDFClass, null);
      fI.setIsOperator(isOperator);
      fI.setOpType(opt);
      mFunctions.put(functionName.toLowerCase(), fI);
    } else {
      throw new RuntimeException("Registering UDF Class " + UDFClass + " which does not extends " + UDF.class);
    }
  }

  public static Class<? extends UDF> getUDFClass(String functionName) {
    LOG.debug("Looking up: " + functionName);
    FunctionInfo finfo = mFunctions.get(functionName.toLowerCase());
    if (finfo == null) {
      return null;
    }
    Class<? extends UDF> result = finfo.getUDFClass();
    return result;
  }

  static Map<Class<?>, Integer> numericTypes;
  static {
    numericTypes = new HashMap<Class<?>, Integer>();
    numericTypes.put(Byte.class, 1);
    numericTypes.put(Short.class, 2);
    numericTypes.put(Integer.class, 3);
    numericTypes.put(Long.class, 4);
    numericTypes.put(Float.class, 5);
    numericTypes.put(Double.class, 6);
    numericTypes.put(String.class, 7);
  } 

  /**
   * Find a common class that objects of both Class a and Class b can convert to.
   * @return null if no common class could be found.
   */
  public static Class<?> getCommonClass(Class<?> a, Class<?> b) {
    // Equal
    if (a.equals(b)) return a;
    // Java class inheritance hierarchy
    if (a.isAssignableFrom(b)) return a;
    if (b.isAssignableFrom(a)) return b;
    // Prefer String to Number conversion before implicit conversions
    if (Number.class.isAssignableFrom(a) && b.equals(String.class)) return Double.class;
    if (Number.class.isAssignableFrom(b) && a.equals(String.class)) return Double.class;
    // implicit conversions
    if (FunctionRegistry.implicitConvertable(a, b)) return b;
    if (FunctionRegistry.implicitConvertable(b, a)) return a;
    return null;
  }

  /** Returns whether it is possible to implicitly convert an object of Class from to Class to.
   */
  public static boolean implicitConvertable(Class<?> from, Class<?> to) {
    assert(!from.equals(to));
    // Allow implicit String to Double conversion
    if (from.equals(String.class) && to.equals(Double.class)) {
      return true;
    }
    if (from.equals(String.class) && to.equals(java.sql.Date.class)) {
      return true;
    }
    if (from.equals(java.sql.Date.class) && to.equals(String.class)) {
      return true;
    }
    // Void can be converted to any type
    if (from.equals(Void.class)) {
      return true;
    }
    
    // Allow implicit conversion from Byte -> Integer -> Long -> Float -> Double -> String
    Integer f = numericTypes.get(from);
    Integer t = numericTypes.get(to);
    if (f == null || t == null) return false;
    if (f.intValue() > t.intValue()) return false;
    return true;
  }

  /**
   * Get the UDF method for the name and argumentClasses. 
   * @param name the name of the UDF
   * @param argumentClasses
   * @return
   */
  public static Method getUDFMethod(String name, List<Class<?>> argumentClasses) {
    Class<? extends UDF> udf = getUDFClass(name);
    if (udf == null) return null;
    Method udfMethod = null;
    try {
      udfMethod = udf.newInstance().getResolver().getEvalMethod(argumentClasses);
    }
    catch (AmbiguousMethodException e) {
    }
    catch (Exception e) {
      throw new RuntimeException("getUDFMethod exception: " + e.getMessage());
    }
    return udfMethod;    
  }

  /**
   * Get the UDAF evaluator for the name and argumentClasses. 
   * @param name the name of the UDAF
   * @param argumentClasses
   * @return
   */
  public static Class<? extends UDAFEvaluator> getUDAFEvaluator(String name, List<Class<?>> argumentClasses) {
    Class<? extends UDAF> udf = getUDAF(name);
    if (udf == null) return null;
    
    Class<? extends UDAFEvaluator> evalClass = null;
    try {
      evalClass = udf.newInstance().getResolver().getEvaluatorClass(argumentClasses);
    }
    catch (AmbiguousMethodException e) {
    }
    catch (Exception e) {
      throw new RuntimeException("getUADFEvaluator exception: " + e.getMessage());
    }
    return evalClass;    
  }

  /**
   * This method is shared between UDFRegistry and UDAFRegistry.
   * methodName will be "evaluate" for UDFRegistry, and "aggregate"/"evaluate"/"evaluatePartial" for UDAFRegistry. 
   */
  public static <T> Method getMethodInternal(Class<? extends T> udfClass, String methodName, boolean exact, List<Class<?>> argumentClasses) {

    ArrayList<Method> mlist = new ArrayList<Method>();
    
    for(Method m: Arrays.asList(udfClass.getMethods())) {
      if (m.getName().equals(methodName)) {
        mlist.add(m);
      }
    }
    
    return getMethodInternal(mlist, exact, argumentClasses);
  }

  public static Method getUDFMethod(String name, Class<?> ... argumentClasses) {
    return getUDFMethod(name, Arrays.asList(argumentClasses));
  }

  public static void registerUDAF(String functionName, Class<? extends UDAF> UDAFClass) {

    if (UDAF.class.isAssignableFrom(UDAFClass)) {
      mFunctions.put(functionName.toLowerCase(), new FunctionInfo(functionName
                                                                  .toLowerCase(), null, UDAFClass));
    } else {
      throw new RuntimeException("Registering UDAF Class " + UDAFClass
                                 + " which does not extends " + UDAF.class);
    }
    mFunctions.put(functionName.toLowerCase(), new FunctionInfo(functionName
                                                                .toLowerCase(), null, UDAFClass));
  }

  public static Class<? extends UDAF> getUDAF(String functionName) {
    LOG.debug("Looking up UDAF: " + functionName);
    FunctionInfo finfo = mFunctions.get(functionName.toLowerCase());
    if (finfo == null) {
      return null;
    }
    Class<? extends UDAF> result = finfo.getUDAFClass();
    return result;
  }

  /**
   * Returns the "aggregate" method of the UDAF.
   */
  public static Method getUDAFMethod(String name, List<Class<?>> argumentClasses) {
    Class<? extends UDAF> udaf = getUDAF(name);
    if (udaf == null)
      return null;
    return FunctionRegistry.getMethodInternal(udaf, "iterate", false,
                                         argumentClasses);
  }

  /**
   * Returns the evaluate method for the UDAF based on the aggregation mode.
   * See groupByDesc.Mode for details.
   * 
   * @param name  name of the UDAF
   * @param mode  the mode of the aggregation
   * @return      null if no such UDAF is found
   */
  public static Method getUDAFEvaluateMethod(String name, groupByDesc.Mode mode) {
    Class<? extends UDAF> udaf = getUDAF(name);
    if (udaf == null)
      return null;
    return FunctionRegistry.getMethodInternal(udaf, 
        (mode == groupByDesc.Mode.COMPLETE || mode == groupByDesc.Mode.FINAL) 
        ? "terminate" : "terminatePartial", true,
        new ArrayList<Class<?>>() );
  }

  /**
   * Returns the "aggregate" method of the UDAF.
   */
  public static Method getUDAFMethod(String name, Class<?>... argumentClasses) {
    return getUDAFMethod(name, Arrays.asList(argumentClasses));
  }
  
  public static Object invoke(Method m, Object thisObject, Object[] arguments) throws HiveException {
    Object o;
    try {
      o = m.invoke(thisObject, arguments);
    } catch (Exception e) {
      String thisObjectString = "" + thisObject + " of class " + 
        (thisObject == null? "null" : thisObject.getClass().getName());

      StringBuilder argumentString = new StringBuilder();
      if (arguments == null) {
        argumentString.append("null");
      } else {
        argumentString.append("{");
        for (int i=0; i<arguments.length; i++) {
          if (i>0) {
            argumentString.append(", ");
          }
          if (arguments[i] == null) {
            argumentString.append("null");
          } else {
            argumentString.append("" + arguments[i] + ":" + arguments[i].getClass().getName());
          }
        }
        argumentString.append("} of size " + arguments.length);
      }
     
      e.printStackTrace();
      throw new HiveException("Unable to execute method " + m + " " 
          + " on object " + thisObjectString
          + " with arguments " + argumentString.toString() 
          + ":" + e.getMessage());
    }
    return o;
  }

  /**
   * Gets the closest matching method corresponding to the argument list from a list of methods.
   * 
   * @param mlist The list of methods to inspect.
   * @param exact Boolean to indicate whether this is an exact match or not.
   * @param argumentClasses The classes for the argument.
   * @return The matching method.
   */
  public static Method getMethodInternal(ArrayList<Method> mlist, boolean exact,
      List<Class<?>> argumentClasses) {
    int leastImplicitConversions = Integer.MAX_VALUE;
    Method udfMethod = null;

    for(Method m: mlist) {
      Class<?>[] argumentTypeInfos = m.getParameterTypes();

      boolean match = (argumentTypeInfos.length == argumentClasses.size());
      int implicitConversions = 0;

      for(int i=0; i<argumentClasses.size() && match; i++) {
        if (argumentClasses.get(i) == Void.class) continue;
        Class<?> accepted = ObjectInspectorUtils.generalizePrimitive(argumentTypeInfos[i]);
        if (accepted.isAssignableFrom(argumentClasses.get(i))) {
          // do nothing if match
        } else if (!exact && implicitConvertable(argumentClasses.get(i), accepted)) {
          implicitConversions ++;
        } else {
          match = false;
        }
      }

      if (match) {
        // Always choose the function with least implicit conversions.
        if (implicitConversions < leastImplicitConversions) {
          udfMethod = m;
          leastImplicitConversions = implicitConversions;
          // Found an exact match
          if (leastImplicitConversions == 0) break;
        } else if (implicitConversions == leastImplicitConversions){
          // Ambiguous call: two methods with the same number of implicit conversions 
          udfMethod = null;
        } else {
          // do nothing if implicitConversions > leastImplicitConversions
        }
      }
    }
    return udfMethod;
  }
}
