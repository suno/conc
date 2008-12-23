package org.apache.poi.ss.formula;

import org.apache.poi.hssf.record.formula.eval.BlankEval;
import org.apache.poi.hssf.record.formula.eval.BoolEval;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;

final class CellEvaluator {

	private final WorkbookEvaluator _bookEvaluator;
	private final EvaluationTracker _tracker;

	public CellEvaluator(WorkbookEvaluator bookEvaluator, EvaluationTracker tracker) {
		_bookEvaluator = bookEvaluator;
		_tracker = tracker;
	}

	   /**
     * Given a cell, find its type and from that create an appropriate ValueEval
     * impl instance and return that. Since the cell could be an external
     * reference, we need the sheet that this belongs to.
     * Non existent cells are treated as empty.
     */
	public ValueEval getEvalForCell(Cell cell) {

        if (cell == null) {
            return BlankEval.INSTANCE;
        }
        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_NUMERIC:
                return new NumberEval(cell.getNumericCellValue());
            case Cell.CELL_TYPE_STRING:
                return new StringEval(cell.getRichStringCellValue().getString());
            case Cell.CELL_TYPE_FORMULA:
                return _bookEvaluator.internalEvaluate(cell, _tracker);
            case Cell.CELL_TYPE_BOOLEAN:
                return BoolEval.valueOf(cell.getBooleanCellValue());
            case Cell.CELL_TYPE_BLANK:
                return BlankEval.INSTANCE;
            case Cell.CELL_TYPE_ERROR:
                return ErrorEval.valueOf(cell.getErrorCellValue());
        }
        throw new RuntimeException("Unexpected cell type (" + cell.getCellType() + ")");
    }

	public String getSheetName(Sheet sheet) {
        return _bookEvaluator.getSheetName(sheet);
	}

}
