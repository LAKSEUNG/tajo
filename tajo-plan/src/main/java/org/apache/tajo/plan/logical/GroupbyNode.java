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

package org.apache.tajo.plan.logical;

import java.util.Arrays;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.Expose;

import org.apache.tajo.catalog.Column;
import org.apache.tajo.plan.PlanString;
import org.apache.tajo.plan.util.PlannerUtil;
import org.apache.tajo.plan.Target;
import org.apache.tajo.plan.expr.AggregationFunctionCallEval;
import org.apache.tajo.util.TUtil;

public class GroupbyNode extends UnaryNode implements Projectable, Cloneable {
  /** Grouping key sets */
  @Expose private Column [] groupingKeys = PlannerUtil.EMPTY_COLUMNS;
  /** Aggregation Functions */
  @Expose private AggregationFunctionCallEval [] aggrFunctions = PlannerUtil.EMPTY_AGG_FUNCS;
  /**
   * It's a list of targets. The grouping columns should be followed by aggregation functions.
   * aggrFunctions keep actual aggregation functions, but it only contains field references.
   * */
  @Expose private Target [] targets;
  @Expose private boolean hasDistinct = false;

  public GroupbyNode(int pid) {
    super(pid, NodeType.GROUP_BY);
  }

  public int groupingKeyNum() {
    return groupingKeys.length;
  }

  public final boolean isEmptyGrouping() {
    return groupingKeys.length == 0;
  }

  public void setGroupingColumns(Column [] groupingKeys) {
    this.groupingKeys = groupingKeys;
  }

	public final Column [] getGroupingColumns() {
	  return this.groupingKeys;
	}

  public final boolean isDistinct() {
    return hasDistinct;
  }

  public void setDistinct(boolean distinct) {
    hasDistinct = distinct;
  }

  public boolean hasAggFunctions() {
    return aggrFunctions.length > 0;
  }

  public int aggregationFunctionNum() {
    return this.aggrFunctions.length;
  }

  public AggregationFunctionCallEval[] getAggFunctions() {
    return this.aggrFunctions;
  }

  public void setAggFunctions(AggregationFunctionCallEval[] evals) {
    Preconditions.checkNotNull(evals);
    this.aggrFunctions = evals;
  }

  @Override
  public boolean hasTargets() {
    return this.targets != null;
  }

  @Override
  public void setTargets(Target[] targets) {
    this.targets = targets;
    setOutSchema(PlannerUtil.targetToSchema(targets));
  }

  @Override
  public Target[] getTargets() {
    return this.targets;
  }
  
  public void setChild(LogicalNode subNode) {
    super.setChild(subNode);
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder("GroupBy (");
    if (groupingKeys != null && groupingKeys.length > 0) {
      sb.append("grouping set=").append(TUtil.arrayToString(groupingKeys));
      sb.append(", ");
    }
    if (hasAggFunctions()) {
      sb.append("funcs=").append(TUtil.arrayToString(aggrFunctions));
    }
    sb.append(")");
    return sb.toString();
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(aggrFunctions);
    result = prime * result + Arrays.hashCode(groupingKeys);
    result = prime * result + (hasDistinct ? 1231 : 1237);
    result = prime * result + Arrays.hashCode(targets);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof GroupbyNode) {
      GroupbyNode other = (GroupbyNode) obj;
      boolean eq = super.equals(other);
      eq = eq && isDistinct() == other.isDistinct();
      eq = eq && TUtil.checkEquals(groupingKeys, other.groupingKeys);
      eq = eq && TUtil.checkEquals(aggrFunctions, other.aggrFunctions);
      eq = eq && TUtil.checkEquals(targets, other.targets);
      return eq;
    } else {
      return false;  
    }
  }
  
  @Override
  public Object clone() throws CloneNotSupportedException {
    GroupbyNode grp = (GroupbyNode) super.clone();
    if (groupingKeys != null) {
      grp.groupingKeys = new Column[groupingKeys.length];
      for (int i = 0; i < groupingKeys.length; i++) {
        grp.groupingKeys[i] = groupingKeys[i];
      }
    }

    if (aggrFunctions != null) {
      grp.aggrFunctions = new AggregationFunctionCallEval[aggrFunctions.length];
      for (int i = 0; i < aggrFunctions.length; i++) {
        grp.aggrFunctions[i] = (AggregationFunctionCallEval) aggrFunctions[i].clone();
      }
    }

    if (targets != null) {
      grp.targets = new Target[targets.length];
      for (int i = 0; i < targets.length; i++) {
        grp.targets[i] = (Target) targets[i].clone();
      }
    }

    return grp;
  }

  public String getShortPlanString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getType().name() + "(" + getPID() + ")").append("(");
    Column [] groupingColumns = this.groupingKeys;
    for (int j = 0; j < groupingColumns.length; j++) {
      sb.append(groupingColumns[j].getSimpleName());
      if(j < groupingColumns.length - 1) {
        sb.append(",");
      }
    }

    sb.append(")");

    // there can be no aggregation functions
    if (hasAggFunctions()) {
      sb.append(", exprs: (");

      for (int j = 0; j < aggrFunctions.length; j++) {
        sb.append(aggrFunctions[j]);
        if(j < aggrFunctions.length - 1) {
          sb.append(",");
        }
      }
      sb.append(")");
    }

    if (targets != null) {
      sb.append(", target list:{");
      for (int i = 0; i < targets.length; i++) {
        sb.append(targets[i]);
        if (i < targets.length - 1) {
          sb.append(", ");
        }
      }
      sb.append("}");
    }
    sb.append(", out schema:").append(getOutSchema().toString());
    sb.append(", in schema:").append(getInSchema().toString());

    return sb.toString();
  }

  @Override
  public PlanString getPlanString() {
    PlanString planStr = new PlanString(this);

    StringBuilder sb = new StringBuilder();
    sb.append("(");
    Column [] groupingColumns = this.groupingKeys;
    for (int j = 0; j < groupingColumns.length; j++) {
      sb.append(groupingColumns[j].getSimpleName());
      if(j < groupingColumns.length - 1) {
        sb.append(",");
      }
    }

    sb.append(")");

    planStr.appendTitle(sb.toString());

    // there can be no aggregation functions
    if (hasAggFunctions()) {
      sb = new StringBuilder();
      sb.append("(");

      for (int j = 0; j < aggrFunctions.length; j++) {
        sb.append(aggrFunctions[j]);
        if(j < aggrFunctions.length - 1) {
          sb.append(",");
        }
      }
      sb.append(")");
      planStr.appendExplain("exprs: ").appendExplain(sb.toString());
    }

    sb = new StringBuilder("target list: ");
    for (int i = 0; i < targets.length; i++) {
      sb.append(targets[i]);
      if( i < targets.length - 1) {
        sb.append(", ");
      }
    }
    planStr.addExplan(sb.toString());

    planStr.addDetail("out schema:").appendDetail(getOutSchema().toString());
    planStr.addDetail("in schema:").appendDetail(getInSchema().toString());

    return planStr;
  }

  /**
   * It checks if an alias name included in the target of this node is for aggregation function.
   * If so, it returns TRUE. Otherwise, it returns FALSE.
   */
  public boolean isAggregationColumn(String simpleName) {
    for (int i = groupingKeys.length; i < targets.length; i++) {
      if (simpleName.equals(targets[i].getNamedColumn().getSimpleName()) ||
          simpleName.equals(targets[i].getAlias())) {
        return true;
      }
    }
    return false;
  }
}
