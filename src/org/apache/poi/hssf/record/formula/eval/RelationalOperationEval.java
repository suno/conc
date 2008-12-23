/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.hssf.record.formula.eval;

/**
 * Base class for all comparison operator evaluators
 * 
 * @author Amol S. Deshmukh &lt; amolweb at ya hoo dot com &gt;
 */
public abstract class RelationalOperationEval implements OperationEval {

	/**
	 * Converts a standard compare result (-1, 0, 1) to <code>true</code> or <code>false</code>
	 * according to subclass' comparison type.
	 */
	protected abstract boolean convertComparisonResult(int cmpResult);

	/**
	 * This is a description of how the relational operators apply in MS Excel.
	 * Use this as a guideline when testing/implementing the evaluate methods
	 * for the relational operators Evals.
	 *
	 * <pre>
	 * Bool.TRUE > any number.
	 * Bool > any string. ALWAYS
	 * Bool.TRUE > Bool.FALSE
	 * Bool.FALSE == Blank
	 *
	 * Strings are never converted to numbers or booleans
	 * String > any number. ALWAYS
	 * Non-empty String > Blank
	 * Empty String == Blank
	 * String are sorted dictionary wise
	 *
	 * Blank > Negative numbers
	 * Blank == 0
	 * Blank < Positive numbers
	 * </pre>
	 */
	public final Eval evaluate(Eval[] operands, int srcRow, short srcCol) {
		if (operands.length != 2) {
			return ErrorEval.VALUE_INVALID;
		}

		ValueEval vA;
		ValueEval vB;
		try {
			vA = OperandResolver.getSingleValue(operands[0], srcRow, srcCol);
			vB = OperandResolver.getSingleValue(operands[1], srcRow, srcCol);
		} catch (EvaluationException e) {
			return e.getErrorEval();
		}
		int cmpResult = doCompare(vA, vB);
		boolean result = convertComparisonResult(cmpResult);
		return BoolEval.valueOf(result);
	}

	private static int doCompare(ValueEval va, ValueEval vb) {
		// special cases when one operand is blank
		if (va == BlankEval.INSTANCE) {
			return compareBlank(vb);
		}
		if (vb == BlankEval.INSTANCE) {
			return -compareBlank(va);
		}

		if (va instanceof BoolEval) {
			if (vb instanceof BoolEval) {
				BoolEval bA = (BoolEval) va;
				BoolEval bB = (BoolEval) vb;
				if (bA.getBooleanValue() == bB.getBooleanValue()) {
					return 0;
				}
				return bA.getBooleanValue() ? 1 : -1;
			}
			return 1;
		}
		if (vb instanceof BoolEval) {
			return -1;
		}
		if (va instanceof StringEval) {
			if (vb instanceof StringEval) {
				StringEval sA = (StringEval) va;
				StringEval sB = (StringEval) vb;
				return sA.getStringValue().compareTo(sB.getStringValue());
			}
			return 1;
		}
		if (vb instanceof StringEval) {
			return -1;
		}
		if (va instanceof NumberEval) {
			if (vb instanceof NumberEval) {
				NumberEval nA = (NumberEval) va;
				NumberEval nB = (NumberEval) vb;
				return Double.compare(nA.getNumberValue(), nB.getNumberValue());
			}
		}
		throw new IllegalArgumentException("Bad operand types (" + va.getClass().getName() + "), ("
				+ vb.getClass().getName() + ")");
	}

	private static int compareBlank(ValueEval v) {
		if (v == BlankEval.INSTANCE) {
			return 0;
		}
		if (v instanceof BoolEval) {
			BoolEval boolEval = (BoolEval) v;
			return boolEval.getBooleanValue() ? -1 : 0;
		}
		if (v instanceof NumberEval) {
			NumberEval ne = (NumberEval) v;
			return Double.compare(0, ne.getNumberValue());
		}
		if (v instanceof StringEval) {
			StringEval se = (StringEval) v;
			return se.getStringValue().length() < 1 ? 0 : -1;
		}
		throw new IllegalArgumentException("bad value class (" + v.getClass().getName() + ")");
	}

	public final int getNumberOfOperands() {
		return 2;
	}

	public final int getType() {
		// TODO - get rid of this method
		throw new RuntimeException("Obsolete code - should not be called");
	}
}
