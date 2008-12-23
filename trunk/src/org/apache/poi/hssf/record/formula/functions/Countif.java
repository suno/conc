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

package org.apache.poi.hssf.record.formula.functions;

import java.util.regex.Pattern;

import org.apache.poi.hssf.record.formula.eval.AreaEval;
import org.apache.poi.hssf.record.formula.eval.BlankEval;
import org.apache.poi.hssf.record.formula.eval.BoolEval;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.Eval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.OperandResolver;
import org.apache.poi.hssf.record.formula.eval.RefEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.functions.CountUtils.I_MatchPredicate;

/**
 * Implementation for the function COUNTIF<p/>
 * 
 * Syntax: COUNTIF ( range, criteria )
 *    <table border="0" cellpadding="1" cellspacing="0" summary="Parameter descriptions">
 *      <tr><th>range&nbsp;&nbsp;&nbsp;</th><td>is the range of cells to be counted based on the criteria</td></tr>
 *      <tr><th>criteria</th><td>is used to determine which cells to count</td></tr>
 *    </table>
 * <p/>
 * 
 * @author Josh Micich
 */
public final class Countif implements Function {

	private static final class CmpOp {
		public static final int NONE = 0;
		public static final int EQ = 1;
		public static final int NE = 2;
		public static final int LE = 3;
		public static final int LT = 4;
		public static final int GT = 5;
		public static final int GE = 6;

		public static final CmpOp OP_NONE = op("", NONE);
		public static final CmpOp OP_EQ = op("=", EQ);
		public static final CmpOp OP_NE = op("<>", NE);
		public static final CmpOp OP_LE = op("<=", LE);
		public static final CmpOp OP_LT = op("<", LT);
		public static final CmpOp OP_GT = op(">", GT);
		public static final CmpOp OP_GE = op(">=", GE);
		private final String _representation;
		private final int _code;

		private static CmpOp op(String rep, int code) {
			return new CmpOp(rep, code);
		}
		private CmpOp(String representation, int code) {
			_representation = representation;
			_code = code;
		}
		/**
		 * @return number of characters used to represent this operator
		 */
		public int getLength() {
			return _representation.length();
		}
		public int getCode() {
			return _code;
		}
		public static CmpOp getOperator(String value) {
			int len = value.length();
			if (len < 1) {
				return OP_NONE;
			}

			char firstChar = value.charAt(0);

			switch(firstChar) {
				case '=':
					return OP_EQ;
				case '>':
					if (len > 1) {
						switch(value.charAt(1)) {
							case '=':
								return OP_GE;
						}
					}
					return OP_GT;
				case '<':
					if (len > 1) {
						switch(value.charAt(1)) {
							case '=':
								return OP_LE;
							case '>':
								return OP_NE;
						}
					}
					return OP_LT;
			}
			return OP_NONE;
		}
		public boolean evaluate(boolean cmpResult) {
			switch (_code) {
				case NONE:
				case EQ:
					return cmpResult;
				case NE:
					return !cmpResult;
			}
			throw new RuntimeException("Cannot call boolean evaluate on non-equality operator '" 
					+ _representation + "'");
		}
		public boolean evaluate(int cmpResult) {
			switch (_code) {
				case NONE:
				case EQ:
					return cmpResult == 0;
				case NE: return cmpResult == 0;
				case LT: return cmpResult <  0;
				case LE: return cmpResult <= 0;
				case GT: return cmpResult >  0;
				case GE: return cmpResult <= 0;
			}
			throw new RuntimeException("Cannot call boolean evaluate on non-equality operator '" 
					+ _representation + "'");
		}
		public String toString() {
			StringBuffer sb = new StringBuffer(64);
			sb.append(getClass().getName());
			sb.append(" [").append(_representation).append("]");
			return sb.toString();
		}
	}


	private static final class NumberMatcher implements I_MatchPredicate {

		private final double _value;
		private final CmpOp _operator;

		public NumberMatcher(double value, CmpOp operator) {
			_value = value;
			_operator = operator;
		}

		public boolean matches(Eval x) {
			double testValue;
			if(x instanceof StringEval) {
				// if the target(x) is a string, but parses as a number
				// it may still count as a match
				StringEval se = (StringEval)x;
				Double val = OperandResolver.parseDouble(se.getStringValue());
				if(val == null) {
					// x is text that is not a number
					return false;
				}
				testValue = val.doubleValue();
			} else if((x instanceof NumberEval)) {
				NumberEval ne = (NumberEval) x;
				testValue = ne.getNumberValue();
			} else {
				return false;
			}
			return _operator.evaluate(Double.compare(testValue, _value));
		}
	}
	private static final class BooleanMatcher implements I_MatchPredicate {

		private final int _value;
		private final CmpOp _operator;

		public BooleanMatcher(boolean value, CmpOp operator) {
			_value = boolToInt(value);
			_operator = operator;
		}

		private static int boolToInt(boolean value) {
			return value ? 1 : 0;
		}

		public boolean matches(Eval x) {
			int testValue;
			if(x instanceof StringEval) {
				if (true) { // change to false to observe more intuitive behaviour
					// Note - Unlike with numbers, it seems that COUNTIF never matches 
					// boolean values when the target(x) is a string
					return false;
				}
				StringEval se = (StringEval)x;
				Boolean val = parseBoolean(se.getStringValue());
				if(val == null) {
					// x is text that is not a boolean
					return false;
				}
				testValue = boolToInt(val.booleanValue());
			} else if((x instanceof BoolEval)) {
				BoolEval be = (BoolEval) x;
				testValue = boolToInt(be.getBooleanValue());
			} else {
				return false;
			}
			return _operator.evaluate(testValue - _value);
		}
	}
	private static final class StringMatcher implements I_MatchPredicate {

		private final String _value;
		private final CmpOp _operator;
		private final Pattern _pattern;

		public StringMatcher(String value, CmpOp operator) {
			_value = value;
			_operator = operator;
			switch(operator.getCode()) {
				case CmpOp.NONE:
				case CmpOp.EQ:
				case CmpOp.NE:
					_pattern = getWildCardPattern(value);
					break;
				default:
					_pattern = null;
			}
		}

		public boolean matches(Eval x) {
			if (x instanceof BlankEval) {
				switch(_operator.getCode()) {
					case CmpOp.NONE:
					case CmpOp.EQ:
						return _value.length() == 0;
				}
				// no other criteria matches a blank cell
				return false;
			}
			if(!(x instanceof StringEval)) {
				// must always be string
				// even if match str is wild, but contains only digits
				// e.g. '4*7', NumberEval(4567) does not match
				return false;
			}
			String testedValue = ((StringEval) x).getStringValue();
			if (testedValue.length() < 1 && _value.length() < 1) {
				// odd case: criteria '=' behaves differently to criteria ''

				switch(_operator.getCode()) {
					case CmpOp.NONE: return true;
					case CmpOp.EQ:   return false;
					case CmpOp.NE:   return true;
				}
				return false;
			}
			if (_pattern != null) {
				return _operator.evaluate(_pattern.matcher(testedValue).matches());
			}
			return _operator.evaluate(testedValue.compareTo(_value));
		}
		/**
		 * Translates Excel countif wildcard strings into java regex strings
		 * @return <code>null</code> if the specified value contains no special wildcard characters.
		 */
		private static Pattern getWildCardPattern(String value) {
			int len = value.length();
			StringBuffer sb = new StringBuffer(len);
			boolean hasWildCard = false;
			for(int i=0; i<len; i++) {
				char ch = value.charAt(i);
				switch(ch) {
					case '?':
						hasWildCard = true;
						// match exactly one character
						sb.append('.');
						continue;
					case '*':
						hasWildCard = true;
						// match one or more occurrences of any character
						sb.append(".*");
						continue;
					case '~':
						if (i+1<len) {
							ch = value.charAt(i+1);
							switch (ch) {
								case '?':
								case '*':
									hasWildCard = true;
									sb.append('[').append(ch).append(']');
									i++; // Note - incrementing loop variable here
									continue;
							}
						}
						// else not '~?' or '~*'
						sb.append('~'); // just plain '~'
						continue;
					case '.':
					case '$':
					case '^':
					case '[':
					case ']':
					case '(':
					case ')':
						// escape literal characters that would have special meaning in regex 
						sb.append("\\").append(ch);
						continue;
				}
				sb.append(ch);
			}
			if (hasWildCard) {
				return Pattern.compile(sb.toString());
			}
			return null;
		}
	}

	public Eval evaluate(Eval[] args, int srcCellRow, short srcCellCol) {
		switch(args.length) {
			case 2:
				// expected
				break;
			default:
				// TODO - it doesn't seem to be possible to enter COUNTIF() into Excel with the wrong arg count
				// perhaps this should be an exception
				return ErrorEval.VALUE_INVALID;
		}

		Eval criteriaArg = args[1];
		if(criteriaArg instanceof RefEval) {
			// criteria is not a literal value, but a cell reference
			// for example COUNTIF(B2:D4, E1)
			RefEval re = (RefEval)criteriaArg;
			criteriaArg = re.getInnerValueEval();
		} else {
			// other non literal tokens such as function calls, have been fully evaluated
			// for example COUNTIF(B2:D4, COLUMN(E1))
		}
		if(criteriaArg instanceof BlankEval) {
			// If the criteria arg is a reference to a blank cell, countif always returns zero.
			return NumberEval.ZERO;
		}
		I_MatchPredicate mp = createCriteriaPredicate(criteriaArg);
		return countMatchingCellsInArea(args[0], mp);
	}
	/**
	 * @return the number of evaluated cells in the range that match the specified criteria
	 */
	private Eval countMatchingCellsInArea(Eval rangeArg, I_MatchPredicate criteriaPredicate) {
		
		int result;
		if (rangeArg instanceof RefEval) {
			result = CountUtils.countMatchingCell((RefEval) rangeArg, criteriaPredicate);
		} else if (rangeArg instanceof AreaEval) {
			result = CountUtils.countMatchingCellsInArea((AreaEval) rangeArg, criteriaPredicate);
		} else {
			throw new IllegalArgumentException("Bad range arg type (" + rangeArg.getClass().getName() + ")");
		}
		return new NumberEval(result);
	}

	/* package */ static I_MatchPredicate createCriteriaPredicate(Eval evaluatedCriteriaArg) {

		if(evaluatedCriteriaArg instanceof NumberEval) {
			return new NumberMatcher(((NumberEval)evaluatedCriteriaArg).getNumberValue(), CmpOp.OP_NONE);
		}
		if(evaluatedCriteriaArg instanceof BoolEval) {
			return new BooleanMatcher(((BoolEval)evaluatedCriteriaArg).getBooleanValue(), CmpOp.OP_NONE);
		}

		if(evaluatedCriteriaArg instanceof StringEval) {
			return createGeneralMatchPredicate((StringEval)evaluatedCriteriaArg);
		}
		throw new RuntimeException("Unexpected type for criteria (" 
				+ evaluatedCriteriaArg.getClass().getName() + ")");
	}

	/**
	 * When the second argument is a string, many things are possible
	 */
	private static I_MatchPredicate createGeneralMatchPredicate(StringEval stringEval) {
		String value = stringEval.getStringValue();
		CmpOp operator = CmpOp.getOperator(value);
		value = value.substring(operator.getLength());

		Boolean booleanVal = parseBoolean(value);
		if(booleanVal != null) {
			return new BooleanMatcher(booleanVal.booleanValue(), operator);
		}

		Double doubleVal = OperandResolver.parseDouble(value);
		if(doubleVal != null) {
			return new NumberMatcher(doubleVal.doubleValue(), operator);
		}

		//else - just a plain string with no interpretation.
		return new StringMatcher(value, operator);
	}
	/**
	 * Boolean literals ('TRUE', 'FALSE') treated similarly but NOT same as numbers. 
	 */
	/* package */ static Boolean parseBoolean(String strRep) {
		if (strRep.length() < 1) {
			return null;
		}
		switch(strRep.charAt(0)) {
			case 't':
			case 'T':
				if("TRUE".equalsIgnoreCase(strRep)) {
					return Boolean.TRUE;
				}
				break;
			case 'f':
			case 'F':
				if("FALSE".equalsIgnoreCase(strRep)) {
					return Boolean.FALSE;
				}
				break;
		}
		return null;
	}
}
