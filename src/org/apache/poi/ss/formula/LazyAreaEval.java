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

package org.apache.poi.ss.formula;

import org.apache.poi.hssf.record.formula.AreaI;
import org.apache.poi.hssf.record.formula.AreaI.OffsetArea;
import org.apache.poi.hssf.record.formula.eval.AreaEval;
import org.apache.poi.hssf.record.formula.eval.AreaEvalBase;
import org.apache.poi.hssf.record.formula.eval.BlankEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.hssf.util.CellReference;

/**
 *
 * @author Josh Micich 
 */
final class LazyAreaEval extends AreaEvalBase {

	private final Sheet _sheet;
	private final CellEvaluator _evaluator;

	public LazyAreaEval(AreaI ptg, Sheet sheet, CellEvaluator evaluator) {
		super(ptg);
		_sheet = sheet;
		_evaluator = evaluator;
	}

	public ValueEval getRelativeValue(int relativeRowIndex, int relativeColumnIndex) { 
		
		int rowIx = (relativeRowIndex + getFirstRow() ) & 0xFFFF;
		int colIx = (relativeColumnIndex + getFirstColumn() ) & 0x00FF;
		
		Row row = _sheet.getRow(rowIx);
		if (row == null) {
			return BlankEval.INSTANCE;
		}
		Cell cell = row.getCell(colIx);
		if (cell == null) {
			return BlankEval.INSTANCE;
		}
		return _evaluator.getEvalForCell(cell);
	}

	public AreaEval offset(int relFirstRowIx, int relLastRowIx, int relFirstColIx, int relLastColIx) {
		AreaI area = new OffsetArea(getFirstRow(), getFirstColumn(),
				relFirstRowIx, relLastRowIx, relFirstColIx, relLastColIx);

		return new LazyAreaEval(area, _sheet, _evaluator);
	}
	public String toString() {
		CellReference crA = new CellReference(getFirstRow(), getFirstColumn());
		CellReference crB = new CellReference(getLastRow(), getLastColumn());
		StringBuffer sb = new StringBuffer();
		sb.append(getClass().getName()).append("[");
		String sheetName = _evaluator.getSheetName(_sheet);
		sb.append(sheetName);
		sb.append('!');
		sb.append(crA.formatAsString());
		sb.append(':');
		sb.append(crB.formatAsString());
		sb.append("]");
		return sb.toString();
	}
}
