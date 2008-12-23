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

import org.apache.poi.hssf.record.cf.CellRangeUtil;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.util.LittleEndian;

/**
 * Conditional Formatting Header record CFHEADER (0x1B0)
 * 
 * @author Dmitriy Kumshayev
 */
public final class CFHeaderRecord extends Record {
	public static final short sid = 0x1B0;

	private int field_1_numcf;
	private int field_2_need_recalculation;
	private CellRangeAddress field_3_enclosing_cell_range;
	private CellRangeAddressList field_4_cell_ranges;

	/** Creates new CFHeaderRecord */
	public CFHeaderRecord()
	{
		field_4_cell_ranges = new CellRangeAddressList();
	}
	public CFHeaderRecord(CellRangeAddress[] regions, int nRules) {
		CellRangeAddress[] unmergedRanges = regions;
		CellRangeAddress[] mergeCellRanges = CellRangeUtil.mergeCellRanges(unmergedRanges);
		setCellRanges(mergeCellRanges);
		field_1_numcf = nRules;
	}

	public CFHeaderRecord(RecordInputStream in)
	{
		super(in);
	}

	protected void fillFields(RecordInputStream in)
	{
		field_1_numcf = in.readShort();
		field_2_need_recalculation = in.readShort();
		field_3_enclosing_cell_range = new org.apache.poi.hssf.util.CellRangeAddress(in);
		field_4_cell_ranges = new org.apache.poi.hssf.util.CellRangeAddressList(in);
	}
	
	public int getNumberOfConditionalFormats()
	{
		return field_1_numcf;
	}
	public void setNumberOfConditionalFormats(int n)
	{
		field_1_numcf=n;
	}
	
	public boolean getNeedRecalculation()
	{
		return field_2_need_recalculation==1?true:false;
	}

	public void setNeedRecalculation(boolean b)
	{
		field_2_need_recalculation=b?1:0;
	}
	
	public CellRangeAddress getEnclosingCellRange()
	{
		return field_3_enclosing_cell_range;
	}

	public void setEnclosingCellRange(CellRangeAddress cr)
	{
		field_3_enclosing_cell_range = cr;
	}

	/**
	 * Set cell ranges list to a single cell range and 
	 * modify the enclosing cell range accordingly.
	 * @param List cellRanges - list of CellRange objects
	 */
	public void setCellRanges(CellRangeAddress[] cellRanges)
	{
		if(cellRanges == null)
		{
			throw new IllegalArgumentException("cellRanges must not be null");
		}
		CellRangeAddressList cral = new CellRangeAddressList();
		CellRangeAddress enclosingRange = null;
		for (int i = 0; i < cellRanges.length; i++)
		{
			CellRangeAddress cr = cellRanges[i];
			enclosingRange = CellRangeUtil.createEnclosingCellRange(cr, enclosingRange);
			cral.addCellRangeAddress(cr);
		}
		field_3_enclosing_cell_range = enclosingRange;
		field_4_cell_ranges = cral;
	}
	
	public CellRangeAddress[] getCellRanges() {
		return field_4_cell_ranges.getCellRangeAddresses();
	}

	public String toString()
	{
		StringBuffer buffer = new StringBuffer();

		buffer.append("[CFHEADER]\n");
		buffer.append("	.id		= ").append(Integer.toHexString(sid)).append("\n");
		buffer.append("	.numCF			= ").append(getNumberOfConditionalFormats()).append("\n");
		buffer.append("	.needRecalc	   = ").append(getNeedRecalculation()).append("\n");
		buffer.append("	.enclosingCellRange= ").append(getEnclosingCellRange()).append("\n");
		buffer.append("	.cfranges=[");
		for( int i=0; i<field_4_cell_ranges.countRanges(); i++)
		{
			buffer.append(i==0?"":",").append(field_4_cell_ranges.getCellRangeAddress(i).toString());
		}
		buffer.append("]\n");
		buffer.append("[/CFHEADER]\n");
		return buffer.toString();
	}

	private int getDataSize() {
		return 4 // 2 short fields
			+ CellRangeAddress.ENCODED_SIZE
			+ field_4_cell_ranges.getSize();
	}
	
	/**
	 * @return byte array containing instance data
	 */
	public int serialize(int offset, byte[] data) {
		int dataSize = getDataSize();
		
		LittleEndian.putUShort(data, 0 + offset, sid);
		LittleEndian.putUShort(data, 2 + offset, dataSize);
		LittleEndian.putUShort(data, 4 + offset, field_1_numcf);
		LittleEndian.putUShort(data, 6 + offset, field_2_need_recalculation);
		field_3_enclosing_cell_range.serialize(8 + offset, data);
		field_4_cell_ranges.serialize(16 + offset, data);
		return 4 + dataSize;
	}

	public int getRecordSize() {
		return 4 + getDataSize();
	}

	/**
	 * called by constructor, should throw runtime exception in the event of a
	 * record passed with a differing ID.
	 *
	 * @param id alleged id for this record
	 */

	protected void validateSid(short id)
	{
		if (id != sid)
		{
			throw new RecordFormatException(
					"NOT A ConditionalFormattingHeaderRecord RECORD");
		}
	}

	public short getSid()
	{
		return sid;
	}

	public Object clone() 
	{
		CFHeaderRecord result = new CFHeaderRecord();
		result.field_1_numcf = field_1_numcf;
		result.field_2_need_recalculation = field_2_need_recalculation;
		result.field_3_enclosing_cell_range = field_3_enclosing_cell_range;
		result.field_4_cell_ranges = field_4_cell_ranges.copy();
		return result;
	}
}
