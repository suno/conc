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

package org.apache.poi.hssf.record;

import org.apache.poi.hssf.record.formula.Ptg;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.util.BitField;
import org.apache.poi.util.BitFieldFactory;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndian;

/**
 * Formula Record (0x0006).
 * REFERENCE:  PG 317/444 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)<P>
 * @author Andrew C. Oliver (acoliver at apache dot org)
 * @author Jason Height (jheight at chariot dot net dot au)
 * @version 2.0-pre
 */
public final class FormulaRecord extends Record implements CellValueRecordInterface {

	public static final short sid = 0x0006;   // docs say 406...because of a bug Microsoft support site article #Q184647)
	private static int FIXED_SIZE = 22;

	private static final BitField alwaysCalc = BitFieldFactory.getInstance(0x0001);
	private static final BitField calcOnLoad = BitFieldFactory.getInstance(0x0002);
	private static final BitField sharedFormula = BitFieldFactory.getInstance(0x0008);

	/**
	 * Manages the cached formula result values of other types besides numeric.
	 * Excel encodes the same 8 bytes that would be field_4_value with various NaN
	 * values that are decoded/encoded by this class. 
	 */
	private static final class SpecialCachedValue {
		/** deliberately chosen by Excel in order to encode other values within Double NaNs */
		private static final long BIT_MARKER = 0xFFFF000000000000L;
		private static final int VARIABLE_DATA_LENGTH = 6;
		private static final int DATA_INDEX = 2;

		public static final int STRING = 0;
		public static final int BOOLEAN = 1;
		public static final int ERROR_CODE = 2;
		public static final int EMPTY = 3;

		private final byte[] _variableData;

		private SpecialCachedValue(byte[] data) {
			_variableData = data;
		}
		public int getTypeCode() {
			return _variableData[0];
		}

		/**
		 * @return <code>null</code> if the double value encoded by <tt>valueLongBits</tt> 
		 * is a normal (non NaN) double value.
		 */
		public static SpecialCachedValue create(long valueLongBits) {
			if ((BIT_MARKER & valueLongBits) != BIT_MARKER) {
				return null;
			}

			byte[] result = new byte[VARIABLE_DATA_LENGTH];
			long x = valueLongBits;
			for (int i=0; i<VARIABLE_DATA_LENGTH; i++) {
				result[i] = (byte) x;
				x >>= 8;
			}
			switch (result[0]) {
				case STRING:
				case BOOLEAN:
				case ERROR_CODE:
				case EMPTY:
					break;
				default:
					throw new RecordFormatException("Bad special value code (" + result[0] + ")");
			}
			return new SpecialCachedValue(result);
		}
		public void serialize(byte[] data, int offset) {
			System.arraycopy(_variableData, 0, data, offset, VARIABLE_DATA_LENGTH);
			LittleEndian.putUShort(data, offset+VARIABLE_DATA_LENGTH, 0xFFFF);
		}
		public String formatDebugString() {
			return formatValue() + ' ' + HexDump.toHex(_variableData);
		}
		private String formatValue() {
			int typeCode = getTypeCode();
			switch (typeCode) {
				case STRING:	 return "<string>";
				case BOOLEAN:	return getDataValue() == 0 ? "FALSE" : "TRUE";
				case ERROR_CODE: return ErrorEval.getText(getDataValue());
				case EMPTY:	  return "<empty>";
			}
			return "#error(type=" + typeCode + ")#";
		}
		private int getDataValue() {
			return _variableData[DATA_INDEX];
		}
		public static SpecialCachedValue createCachedEmptyValue() {
			return create(EMPTY, 0);
		}
		public static SpecialCachedValue createForString() {
			return create(STRING, 0);
		}
		public static SpecialCachedValue createCachedBoolean(boolean b) {
			return create(BOOLEAN, b ? 0 : 1);
		}
		public static SpecialCachedValue createCachedErrorCode(int errorCode) {
			return create(ERROR_CODE, errorCode);
		}
		private static SpecialCachedValue create(int code, int data) {
			byte[] vd = {
					(byte) code,
					0,
					(byte) data,
					0,
					0,
					0,
			};
			return new SpecialCachedValue(vd);
		}
		public String toString() {
			StringBuffer sb = new StringBuffer(64);
			sb.append(getClass().getName());
			sb.append('[').append(formatValue()).append(']');
			return sb.toString();
		}
		public int getValueType() {
			int typeCode = getTypeCode();
			switch (typeCode) {
				case STRING:	 return HSSFCell.CELL_TYPE_STRING;
				case BOOLEAN:	return HSSFCell.CELL_TYPE_BOOLEAN;
				case ERROR_CODE: return HSSFCell.CELL_TYPE_ERROR;
				case EMPTY:	  return HSSFCell.CELL_TYPE_STRING; // is this correct?
			}
			throw new IllegalStateException("Unexpected type id (" + typeCode + ")");
		}
		public boolean getBooleanValue() {
			if (getTypeCode() != BOOLEAN) {
				throw new IllegalStateException("Not a boolean cached value - " + formatValue());
			}
			return getDataValue() != 0;
		}
		public int getErrorValue() {
			if (getTypeCode() != ERROR_CODE) {
				throw new IllegalStateException("Not an error cached value - " + formatValue());
			}
			return getDataValue();
		}
	}



	private int    field_1_row;
	private short  field_2_column;
	private short  field_3_xf;
	private double field_4_value;
	private short  field_5_options;
	private int    field_6_zero;
	private Ptg[]  field_8_parsed_expr;

	/**
	 * Since the NaN support seems sketchy (different constants) we'll store and spit it out directly
	 */
	private SpecialCachedValue specialCachedValue;

	/** Creates new FormulaRecord */

	public FormulaRecord() {
		field_8_parsed_expr = Ptg.EMPTY_PTG_ARRAY;
	}

	/**
	 * Constructs a Formula record and sets its fields appropriately.
	 * Note - id must be 0x06 (NOT 0x406 see MSKB #Q184647 for an
	 * "explanation of this bug in the documentation) or an exception
	 *  will be throw upon validation
	 *
	 * @param in the RecordInputstream to read the record from
	 */

	public FormulaRecord(RecordInputStream in) {
		super(in);
	}

	protected void fillFields(RecordInputStream in) {
		field_1_row	 = in.readUShort();
		field_2_column  = in.readShort();
		field_3_xf	  = in.readShort();
		long valueLongBits  = in.readLong();
		field_5_options = in.readShort();
		specialCachedValue = SpecialCachedValue.create(valueLongBits);
		if (specialCachedValue == null) {
			field_4_value = Double.longBitsToDouble(valueLongBits);
		}

		field_6_zero		   = in.readInt();
		int field_7_expression_len = in.readShort(); // this length does not include any extra array data
		field_8_parsed_expr = Ptg.readTokens(field_7_expression_len, in);
		if (in.remaining() == 10) {
			// TODO - this seems to occur when IntersectionPtg is present
			// 10 extra bytes are just 0x01 and 0x00
			// This causes POI stderr: "WARN. Unread 10 bytes of record 0x6"
		}
	}


	public void setRow(int row) {
		field_1_row = row;
	}

	public void setColumn(short column) {
		field_2_column = column;
	}

	public void setXFIndex(short xf) {
		field_3_xf = xf;
	}

	/**
	 * set the calculated value of the formula
	 *
	 * @param value  calculated value
	 */
	public void setValue(double value) {
		field_4_value = value;
		specialCachedValue = null;
	}

	public void setCachedResultTypeEmptyString() {
		specialCachedValue = SpecialCachedValue.createCachedEmptyValue();
	}
	public void setCachedResultTypeString() {
		specialCachedValue = SpecialCachedValue.createForString();
	}
	public void setCachedResultErrorCode(int errorCode) {
		specialCachedValue = SpecialCachedValue.createCachedErrorCode(errorCode);
	}
	public void setCachedResultBoolean(boolean value) {
		specialCachedValue = SpecialCachedValue.createCachedBoolean(value);
	}
	/**
	 * @return <code>true</code> if this {@link FormulaRecord} is followed by a
	 *  {@link StringRecord} representing the cached text result of the formula
	 *  evaluation.
	 */
	public boolean hasCachedResultString() {
		if (specialCachedValue == null) {
			return false;
		}
		return specialCachedValue.getTypeCode() == SpecialCachedValue.STRING;
	}

	public int getCachedResultType() {
		if (specialCachedValue == null) {
			return HSSFCell.CELL_TYPE_NUMERIC;
		}
		return specialCachedValue.getValueType();
	}

	public boolean getCachedBooleanValue() {
		return specialCachedValue.getBooleanValue();
	}
	public int getCachedErrorValue() {
		return specialCachedValue.getErrorValue();
	}


	/**
	 * set the option flags
	 *
	 * @param options  bitmask
	 */
	public void setOptions(short options) {
		field_5_options = options;
	}

	public int getRow() {
		return field_1_row;
	}

	public short getColumn() {
		return field_2_column;
	}

	public short getXFIndex() {
		return field_3_xf;
	}

	/**
	 * get the calculated value of the formula
	 *
	 * @return calculated value
	 */
	public double getValue() {
		return field_4_value;
	}

	/**
	 * get the option flags
	 *
	 * @return bitmask
	 */
	public short getOptions() {
		return field_5_options;
	}

	public boolean isSharedFormula() {
		return sharedFormula.isSet(field_5_options);
	}
	public void setSharedFormula(boolean flag) {
		field_5_options =
			sharedFormula.setShortBoolean(field_5_options, flag);
	}

	public boolean isAlwaysCalc() {
		return alwaysCalc.isSet(field_5_options);
	}
	public void setAlwaysCalc(boolean flag) {
		field_5_options =
			alwaysCalc.setShortBoolean(field_5_options, flag);
	}

	public boolean isCalcOnLoad() {
		return calcOnLoad.isSet(field_5_options);
	}
	public void setCalcOnLoad(boolean flag) {
		field_5_options =
			calcOnLoad.setShortBoolean(field_5_options, flag);
	}

	/**
	 * @return the formula tokens. never <code>null</code>
	 */
	public Ptg[] getParsedExpression() {
		return (Ptg[]) field_8_parsed_expr.clone();
	}

	public void setParsedExpression(Ptg[] ptgs) {
		field_8_parsed_expr = ptgs;
	}

	/**
	 * called by constructor, should throw runtime exception in the event of a
	 * record passed with a differing ID.
	 *
	 * @param id alleged id for this record
	 */
	protected void validateSid(short id) {
		if (id != sid) {
			throw new RecordFormatException("NOT A FORMULA RECORD");
		}
	}

	public short getSid() {
		return sid;
	}

	private int getDataSize() {
		return FIXED_SIZE + Ptg.getEncodedSize(field_8_parsed_expr);
	}
	public int serialize(int offset, byte [] data) {

		int dataSize = getDataSize();

		LittleEndian.putShort(data, 0 + offset, sid);
		LittleEndian.putUShort(data, 2 + offset, dataSize);
		LittleEndian.putUShort(data, 4 + offset, getRow());
		LittleEndian.putShort(data, 6 + offset, getColumn());
		LittleEndian.putShort(data, 8 + offset, getXFIndex());

		if (specialCachedValue == null) {
			LittleEndian.putDouble(data, 10 + offset, field_4_value);
		} else {
			specialCachedValue.serialize(data, 10+offset);
		}

		LittleEndian.putShort(data, 18 + offset, getOptions());

		//when writing the chn field (offset 20), it's supposed to be 0 but ignored on read
		//Microsoft Excel Developer's Kit Page 318
		LittleEndian.putInt(data, 20 + offset, 0);
		int formulaTokensSize = Ptg.getEncodedSizeWithoutArrayData(field_8_parsed_expr);
		LittleEndian.putUShort(data, 24 + offset, formulaTokensSize);
		Ptg.serializePtgs(field_8_parsed_expr, data, 26+offset);
		return 4 + dataSize;
	}

	public int getRecordSize() {
		return 4 + getDataSize();
	}

	public boolean isInValueSection() {
		return true;
	}

	public boolean isValue() {
		return true;
	}

	public String toString() {

		StringBuffer sb = new StringBuffer();
		sb.append("[FORMULA]\n");
		sb.append("	.row	   = ").append(HexDump.shortToHex(getRow())).append("\n");
		sb.append("	.column	= ").append(HexDump.shortToHex(getColumn())).append("\n");
		sb.append("	.xf		= ").append(HexDump.shortToHex(getXFIndex())).append("\n");
		sb.append("	.value	 = ");
		if (specialCachedValue == null) {
			sb.append(field_4_value).append("\n");
		} else {
			sb.append(specialCachedValue.formatDebugString()).append("\n");
		}
		sb.append("	.options   = ").append(HexDump.shortToHex(getOptions())).append("\n");
		sb.append("	.alwaysCalc= ").append(alwaysCalc.isSet(getOptions())).append("\n");
		sb.append("	.calcOnLoad= ").append(calcOnLoad.isSet(getOptions())).append("\n");
		sb.append("	.shared	= ").append(sharedFormula.isSet(getOptions())).append("\n");
		sb.append("	.zero	  = ").append(HexDump.intToHex(field_6_zero)).append("\n");

		for (int k = 0; k < field_8_parsed_expr.length; k++ ) {
			sb.append("	 Ptg[").append(k).append("]=");
			Ptg ptg = field_8_parsed_expr[k];
			sb.append(ptg.toString()).append(ptg.getRVAType()).append("\n");
		}
		sb.append("[/FORMULA]\n");
		return sb.toString();
	}

	public Object clone() {
		FormulaRecord rec = new FormulaRecord();
		rec.field_1_row = field_1_row;
		rec.field_2_column = field_2_column;
		rec.field_3_xf = field_3_xf;
		rec.field_4_value = field_4_value;
		rec.field_5_options = field_5_options;
		rec.field_6_zero = field_6_zero;
		int nTokens = field_8_parsed_expr.length;
		Ptg[] ptgs = new Ptg[nTokens];
		for (int i = 0; i < nTokens; i++) {
			ptgs[i] = field_8_parsed_expr[i].copy();
		}
		rec.field_8_parsed_expr = ptgs;
		rec.specialCachedValue = specialCachedValue;
		return rec;
	}
}

