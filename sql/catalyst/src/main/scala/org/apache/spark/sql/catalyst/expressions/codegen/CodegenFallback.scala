/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.catalyst.expressions.codegen

import org.apache.spark.sql.catalyst.expressions.{Expression, LeafExpression, Nondeterministic}
import org.apache.spark.sql.catalyst.util.toCommentSafeString

/**
 * A trait that can be used to provide a fallback mode for expression code generation.
 */
trait CodegenFallback extends Expression {

  protected def genCode(ctx: CodegenContext, ev: ExprCode): String = {
    foreach {
      case n: Nondeterministic => n.setInitialValues()
      case _ =>
    }

    // LeafNode does not need `input`
    val input = if (this.isInstanceOf[LeafExpression]) "null" else ctx.INPUT_ROW
    val idx = ctx.references.length
    ctx.references += this
    val objectTerm = ctx.freshName("obj")
    if (nullable) {
      s"""
        /* expression: ${toCommentSafeString(this.toString)} */
        Object $objectTerm = ((Expression) references[$idx]).eval($input);
        boolean ${ev.isNull} = $objectTerm == null;
        ${ctx.javaType(this.dataType)} ${ev.value} = ${ctx.defaultValue(this.dataType)};
        if (!${ev.isNull}) {
          ${ev.value} = (${ctx.boxedType(this.dataType)}) $objectTerm;
        }
      """
    } else {
      ev.isNull = "false"
      s"""
        /* expression: ${toCommentSafeString(this.toString)} */
        Object $objectTerm = ((Expression) references[$idx]).eval($input);
        ${ctx.javaType(this.dataType)} ${ev.value} = (${ctx.boxedType(this.dataType)}) $objectTerm;
      """
    }
  }
}