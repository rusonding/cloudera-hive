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

package org.apache.hadoop.hive.ql.plan;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hive.ql.typeinfo.TypeInfo;
import org.apache.hadoop.hive.ql.exec.FunctionInfo;
import org.apache.hadoop.hive.ql.exec.FunctionRegistry;
import org.apache.hadoop.hive.ql.exec.UDF;
import org.apache.hadoop.hive.ql.exec.Utilities;

/**
 * The reason that we have to store UDFClass as well as UDFMethod is because
 * UDFMethod might be declared in a parent class of UDFClass. As a result,
 * UDFMethod.getDeclaringClass() may not work.
 */
public class exprNodeFuncDesc extends exprNodeDesc implements Serializable {

  private static final long serialVersionUID = 1L;
  private Class<? extends UDF> UDFClass;
  private Method UDFMethod;
  private ArrayList<exprNodeDesc> children; 
  
  public exprNodeFuncDesc() {}
  public exprNodeFuncDesc(TypeInfo typeInfo, Class<? extends UDF> UDFClass, 
                          Method UDFMethod, ArrayList<exprNodeDesc> children) {
    super(typeInfo);
    assert(UDFClass != null);
    this.UDFClass = UDFClass;
    assert(UDFMethod != null);
    this.UDFMethod = UDFMethod;
    this.children = children;
  }
  
  public Class<? extends UDF> getUDFClass() {
    return UDFClass;
  }
  
  public void setUDFClass(Class<? extends UDF> UDFClass) {
    this.UDFClass = UDFClass;
  }
  public Method getUDFMethod() {
    return this.UDFMethod;
  }
  public void setUDFMethod(Method method) {
    this.UDFMethod = method;
  }
  public ArrayList<exprNodeDesc> getChildren() {
    return this.children;
  }
  public void setChildren(ArrayList<exprNodeDesc> children) {
    this.children = children;
  }
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(UDFClass.toString());
    sb.append(".");
    sb.append(UDFMethod.toString());
    sb.append("(");
    for(int i=0; i<children.size(); i++) {
      if (i>0) sb.append(", ");
      sb.append(children.get(i).toString());
    }
    sb.append("(");
    sb.append(")");
    return sb.toString();
  }
  
  @explain(displayName="expr")
  @Override
  public String getExprString() {
    FunctionInfo fI = FunctionRegistry.getInfo(UDFClass);
    StringBuilder sb = new StringBuilder();
    
    if (fI.getOpType() == FunctionInfo.OperatorType.PREFIX ||
        fI.isAggFunction()) {
      sb.append(fI.getDisplayName());
      if (!fI.isOperator()) {
        sb.append("(");
      }
      else {
        sb.append(" ");
      }
      
      boolean first = true;
      for(exprNodeDesc chld: children) {
        if (!first) {
          sb.append(", ");
        }
        first = false;
        
        sb.append(chld.getExprString());
      }
      
      if(!fI.isOperator()) {
        sb.append(")");
      }
    }
    else if (fI.getOpType() == FunctionInfo.OperatorType.INFIX) {
      // assert that this has only 2 children
      assert(children.size() == 2);
      sb.append("(");
      sb.append(children.get(0).getExprString());
      sb.append(" ");
      sb.append(fI.getDisplayName());
      sb.append(" ");
      sb.append(children.get(1).getExprString());
      sb.append(")");
    }
    else if (fI.getOpType() == FunctionInfo.OperatorType.POSTFIX) {
      // assert for now as there should be no such case
      assert(children.size() == 1);
      sb.append(children.get(0).getExprString());
      sb.append(" ");
      sb.append(fI.getDisplayName());
    }
    
    return sb.toString();
  }

  public List<String> getCols() {
    List<String> colList = new ArrayList<String>();
    if (children != null) {
      int pos = 0;
      while (pos < children.size()) {
        List<String> colCh = children.get(pos).getCols();
        colList = Utilities.mergeUniqElems(colList, colCh);
        pos++;
      }
    }

    return colList;
  }
}
