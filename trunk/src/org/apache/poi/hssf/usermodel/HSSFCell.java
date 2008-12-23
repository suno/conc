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

package org.apache.poi.hssf.usermodel;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.hssf.model.HSSFFormulaParser;
import org.apache.poi.hssf.model.Sheet;
import org.apache.poi.hssf.model.Workbook;
import org.apache.poi.hssf.record.BlankRecord;
import org.apache.poi.hssf.record.BoolErrRecord;
import org.apache.poi.hssf.record.CellValueRecordInterface;
import org.apache.poi.hssf.record.CommonObjectDataSubRecord;
import org.apache.poi.hssf.record.DrawingRecord;
import org.apache.poi.hssf.record.EOFRecord;
import org.apache.poi.hssf.record.ExtendedFormatRecord;
import org.apache.poi.hssf.record.FormulaRecord;
import org.apache.poi.hssf.record.HyperlinkRecord;
import org.apache.poi.hssf.record.LabelSSTRecord;
import org.apache.poi.hssf.record.NoteRecord;
import org.apache.poi.hssf.record.NumberRecord;
import org.apache.poi.hssf.record.ObjRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.RecordBase;
import org.apache.poi.hssf.record.StringRecord;
import org.apache.poi.hssf.record.SubRecord;
import org.apache.poi.hssf.record.TextObjectRecord;
import org.apache.poi.hssf.record.UnicodeString;
import org.apache.poi.hssf.record.aggregates.FormulaRecordAggregate;
import org.apache.poi.hssf.record.formula.Ptg;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.RichTextString;

/**
 * High level representation of a cell in a row of a spreadsheet.
 * Cells can be numeric, formula-based or string-based (text).  The cell type
 * specifies this.  String cells cannot conatin numbers and numeric cells cannot
 * contain strings (at least according to our model).  Client apps should do the
 * conversions themselves.  Formula cells have the formula string, as well as
 * the formula result, which can be numeric or string.
 * <p>
 * Cells should have their number (0 based) before being added to a row.  Only
 * cells that have values should be added.
 * <p>
 *
 * @author  Andrew C. Oliver (acoliver at apache dot org)
 * @author  Dan Sherman (dsherman at isisph.com)
 * @author  Brian Sanders (kestrel at burdell dot org) Active Cell support
 * @author  Yegor Kozlov cell comments support
 * @version 1.0-pre
 */
public class HSSFCell implements Cell {
    /** Numeric Cell type (0) @see #setCellType(int) @see #getCellType() */
    public final static int CELL_TYPE_NUMERIC = 0;
    /** String  Cell type (1) @see #setCellType(int) @see #getCellType() */
    public final static int CELL_TYPE_STRING  = 1;
    /** Formula Cell type (2) @see #setCellType(int) @see #getCellType() */
    public final static int CELL_TYPE_FORMULA = 2;
    /** Blank   Cell type (3) @see #setCellType(int) @see #getCellType() */
    public final static int CELL_TYPE_BLANK   = 3;
    /** Boolean Cell type (4) @see #setCellType(int) @see #getCellType() */
    public final static int CELL_TYPE_BOOLEAN = 4;
    /** Error   Cell type (5) @see #setCellType(int) @see #getCellType() */
    public final static int CELL_TYPE_ERROR   = 5;

    public final static short        ENCODING_UNCHANGED          = -1;
    public final static short        ENCODING_COMPRESSED_UNICODE = 0;
    public final static short        ENCODING_UTF_16             = 1;

    private final HSSFWorkbook       book;
    private final HSSFSheet          sheet;
    private int                      cellType;
    private HSSFRichTextString       stringValue;
    private CellValueRecordInterface record;
    private HSSFComment              comment;

    /**
     * Creates new Cell - Should only be called by HSSFRow.  This creates a cell
     * from scratch.
     * <p>
     * When the cell is initially created it is set to CELL_TYPE_BLANK. Cell types
     * can be changed/overwritten by calling setCellValue with the appropriate
     * type as a parameter although conversions from one type to another may be
     * prohibited.
     *
     * @param book - Workbook record of the workbook containing this cell
     * @param sheet - Sheet record of the sheet containing this cell
     * @param row   - the row of this cell
     * @param col   - the column for this cell
     *
     * @see org.apache.poi.hssf.usermodel.HSSFRow#createCell(short)
     */
    protected HSSFCell(HSSFWorkbook book, HSSFSheet sheet, int row, short col)
    {
        checkBounds(col);
        stringValue  = null;
        this.book    = book;
        this.sheet   = sheet;

        // Relying on the fact that by default the cellType is set to 0 which
        // is different to CELL_TYPE_BLANK hence the following method call correctly
        // creates a new blank cell.
        short xfindex = sheet.getSheet().getXFIndexForColAt(col);
        setCellType(CELL_TYPE_BLANK, false, row, col,xfindex);
    }
    public HSSFSheet getSheet() {
        return sheet;
    }

    /**
     * Creates new Cell - Should only be called by HSSFRow.  This creates a cell
     * from scratch.
     *
     * @param book - Workbook record of the workbook containing this cell
     * @param sheet - Sheet record of the sheet containing this cell
     * @param row   - the row of this cell
     * @param col   - the column for this cell
     * @param type  - CELL_TYPE_NUMERIC, CELL_TYPE_STRING, CELL_TYPE_FORMULA, CELL_TYPE_BLANK,
     *                CELL_TYPE_BOOLEAN, CELL_TYPE_ERROR
     *                Type of cell
     * @see org.apache.poi.hssf.usermodel.HSSFRow#createCell(short,int)
     */
    protected HSSFCell(HSSFWorkbook book, HSSFSheet sheet, int row, short col,
                       int type)
    {
        checkBounds(col);
        cellType     = -1; // Force 'setCellType' to create a first Record
        stringValue  = null;
        this.book    = book;
        this.sheet   = sheet;

        short xfindex = sheet.getSheet().getXFIndexForColAt(col);
        setCellType(type,false,row,col,xfindex);
    }

    /**
     * Creates an HSSFCell from a CellValueRecordInterface.  HSSFSheet uses this when
     * reading in cells from an existing sheet.
     *
     * @param book - Workbook record of the workbook containing this cell
     * @param sheet - Sheet record of the sheet containing this cell
     * @param cval - the Cell Value Record we wish to represent
     */
    protected HSSFCell(HSSFWorkbook book, HSSFSheet sheet, CellValueRecordInterface cval) {
        record      = cval;
        cellType    = determineType(cval);
        stringValue = null;
        this.book   = book;
        this.sheet  = sheet;
        switch (cellType)
        {
            case CELL_TYPE_STRING :
                stringValue = new HSSFRichTextString(book.getWorkbook(), (LabelSSTRecord ) cval);
                break;

            case CELL_TYPE_BLANK :
                break;

            case CELL_TYPE_FORMULA :
                stringValue=new HSSFRichTextString(((FormulaRecordAggregate) cval).getStringValue());
                break;
        }
        ExtendedFormatRecord xf = book.getWorkbook().getExFormatAt(cval.getXFIndex());

        setCellStyle(new HSSFCellStyle(cval.getXFIndex(), xf, book));
    }


    /**
     * used internally -- given a cell value record, figure out its type
     */
    private static int determineType(CellValueRecordInterface cval) {
        if (cval instanceof FormulaRecordAggregate) {
            return HSSFCell.CELL_TYPE_FORMULA;
        }
        // all others are plain BIFF records
        Record record = ( Record ) cval;
        switch (record.getSid()) {

            case NumberRecord.sid :   return HSSFCell.CELL_TYPE_NUMERIC;
            case BlankRecord.sid :    return HSSFCell.CELL_TYPE_BLANK;
            case LabelSSTRecord.sid : return HSSFCell.CELL_TYPE_STRING;
            case BoolErrRecord.sid :
                BoolErrRecord boolErrRecord = ( BoolErrRecord ) record;

                return boolErrRecord.isBoolean()
                         ? HSSFCell.CELL_TYPE_BOOLEAN
                         : HSSFCell.CELL_TYPE_ERROR;
        }
        throw new RuntimeException("Bad cell value rec (" + cval.getClass().getName() + ")");
    }

    /**
     * Returns the Workbook that this Cell is bound to
     * @return
     */
    protected Workbook getBoundWorkbook() {
        return book.getWorkbook();
    }

    /**
     * @return the (zero based) index of the row containing this cell
     */
    public int getRowIndex() {
        return record.getRow();
    }
    /**
     * Set the cell's number within the row (0 based).
     * @param num  short the cell number
     * @deprecated Doesn't update the row's idea of what cell this is, use {@link HSSFRow#moveCell(HSSFCell, short)} instead
     */
    public void setCellNum(short num)
    {
        record.setColumn(num);
    }

    /**
     * Updates the cell record's idea of what
     *  column it belongs in (0 based)
     * @param num the new cell number
     */
    protected void updateCellNum(short num)
    {
        record.setColumn(num);
    }

    /**
     *  get the cell's number within the row
     * @return short reperesenting the column number (logical!)
     */

    public short getCellNum()
    {
        return record.getColumn();
    }

    /**
     * set the cells type (numeric, formula or string)
     * @see #CELL_TYPE_NUMERIC
     * @see #CELL_TYPE_STRING
     * @see #CELL_TYPE_FORMULA
     * @see #CELL_TYPE_BLANK
     * @see #CELL_TYPE_BOOLEAN
     * @see #CELL_TYPE_ERROR
     */

    public void setCellType(int cellType)
    {
        int row=record.getRow();
        short col=record.getColumn();
        short styleIndex=record.getXFIndex();
        setCellType(cellType, true, row, col, styleIndex);
    }

    /**
     * sets the cell type. The setValue flag indicates whether to bother about
     *  trying to preserve the current value in the new record if one is created.
     *  <p>
     *  The @see #setCellValue method will call this method with false in setValue
     *  since it will overwrite the cell value later
     *
     */

    private void setCellType(int cellType, boolean setValue, int row,short col, short styleIndex)
    {

        if (cellType > CELL_TYPE_ERROR)
        {
            throw new RuntimeException("I have no idea what type that is!");
        }
        switch (cellType)
        {

            case CELL_TYPE_FORMULA :
                FormulaRecordAggregate frec;

                if (cellType != this.cellType) {
                    frec = sheet.getSheet().getRowsAggregate().createFormula(row, col);
                } else {
                    frec = (FormulaRecordAggregate) record;
                    frec.setRow(row);
                    frec.setColumn(col);
                }
                if (setValue)
                {
                    frec.getFormulaRecord().setValue(getNumericCellValue());
                }
                frec.setXFIndex(styleIndex);
                record = frec;
                break;

            case CELL_TYPE_NUMERIC :
                NumberRecord nrec = null;

                if (cellType != this.cellType)
                {
                    nrec = new NumberRecord();
                }
                else
                {
                    nrec = ( NumberRecord ) record;
                }
                nrec.setColumn(col);
                if (setValue)
                {
                    nrec.setValue(getNumericCellValue());
                }
                nrec.setXFIndex(styleIndex);
                nrec.setRow(row);
                record = nrec;
                break;

            case CELL_TYPE_STRING :
                LabelSSTRecord lrec = null;

                if (cellType != this.cellType)
                {
                    lrec = new LabelSSTRecord();
                }
                else
                {
                    lrec = ( LabelSSTRecord ) record;
                }
                lrec.setColumn(col);
                lrec.setRow(row);
                lrec.setXFIndex(styleIndex);
                if (setValue)
                {
                    if ((getStringCellValue() != null)
                            && (!getStringCellValue().equals("")))
                    {
                        int sst = 0;

                        UnicodeString str = getRichStringCellValue().getUnicodeString();
//jmh                        if (encoding == ENCODING_COMPRESSED_UNICODE)
//jmh                        {
//                      jmh                            str.setCompressedUnicode();
//                      jmh                        } else if (encoding == ENCODING_UTF_16)
//                      jmh                        {
//                      jmh                            str.setUncompressedUnicode();
//                      jmh                        }
                        sst = book.getWorkbook().addSSTString(str);
                        lrec.setSSTIndex(sst);
                        getRichStringCellValue().setUnicodeString(book.getWorkbook().getSSTString(sst));
                    }
                }
                record = lrec;
                break;

            case CELL_TYPE_BLANK :
                BlankRecord brec = null;

                if (cellType != this.cellType)
                {
                    brec = new BlankRecord();
                }
                else
                {
                    brec = ( BlankRecord ) record;
                }
                brec.setColumn(col);

                // During construction the cellStyle may be null for a Blank cell.
                brec.setXFIndex(styleIndex);
                brec.setRow(row);
                record = brec;
                break;

            case CELL_TYPE_BOOLEAN :
                BoolErrRecord boolRec = null;

                if (cellType != this.cellType)
                {
                    boolRec = new BoolErrRecord();
                }
                else
                {
                    boolRec = ( BoolErrRecord ) record;
                }
                boolRec.setColumn(col);
                if (setValue)
                {
                    boolRec.setValue(convertCellValueToBoolean());
                }
                boolRec.setXFIndex(styleIndex);
                boolRec.setRow(row);
                record = boolRec;
                break;

            case CELL_TYPE_ERROR :
                BoolErrRecord errRec = null;

                if (cellType != this.cellType)
                {
                    errRec = new BoolErrRecord();
                }
                else
                {
                    errRec = ( BoolErrRecord ) record;
                }
                errRec.setColumn(col);
                if (setValue)
                {
                    errRec.setValue((byte)HSSFErrorConstants.ERROR_VALUE);
                }
                errRec.setXFIndex(styleIndex);
                errRec.setRow(row);
                record = errRec;
                break;
        }
        if (cellType != this.cellType &&
            this.cellType!=-1 )  // Special Value to indicate an uninitialized Cell
        {
            sheet.getSheet().replaceValueRecord(record);
        }
        this.cellType = cellType;
    }

    /**
     * get the cells type (numeric, formula or string)
     * @see #CELL_TYPE_STRING
     * @see #CELL_TYPE_NUMERIC
     * @see #CELL_TYPE_FORMULA
     * @see #CELL_TYPE_BOOLEAN
     * @see #CELL_TYPE_ERROR
     */

    public int getCellType()
    {
        return cellType;
    }

    /**
     * set a numeric value for the cell
     *
     * @param value  the numeric value to set this cell to.  For formulas we'll set the
     *        precalculated value, for numerics we'll set its value. For other types we
     *        will change the cell to a numeric cell and set its value.
     */
    public void setCellValue(double value) {
        int row=record.getRow();
        short col=record.getColumn();
        short styleIndex=record.getXFIndex();

        switch (cellType) {
            default:
                setCellType(CELL_TYPE_NUMERIC, false, row, col, styleIndex);
            case CELL_TYPE_ERROR:
                (( NumberRecord ) record).setValue(value);
                break;
            case CELL_TYPE_FORMULA:
                ((FormulaRecordAggregate)record).getFormulaRecord().setValue(value);
                break;
        }
    }

    /**
     * set a date value for the cell. Excel treats dates as numeric so you will need to format the cell as
     * a date.
     *
     * @param value  the date value to set this cell to.  For formulas we'll set the
     *        precalculated value, for numerics we'll set its value. For other types we
     *        will change the cell to a numeric cell and set its value.
     */
    public void setCellValue(Date value)
    {
        setCellValue(HSSFDateUtil.getExcelDate(value, this.book.getWorkbook().isUsing1904DateWindowing()));
    }

    /**
     * set a date value for the cell. Excel treats dates as numeric so you will need to format the cell as
     * a date.
     *
     * This will set the cell value based on the Calendar's timezone. As Excel
     * does not support timezones this means that both 20:00+03:00 and
     * 20:00-03:00 will be reported as the same value (20:00) even that there
     * are 6 hours difference between the two times. This difference can be
     * preserved by using <code>setCellValue(value.getTime())</code> which will
     * automatically shift the times to the default timezone.
     *
     * @param value  the date value to set this cell to.  For formulas we'll set the
     *        precalculated value, for numerics we'll set its value. For othertypes we
     *        will change the cell to a numeric cell and set its value.
     */
    public void setCellValue(Calendar value)
    {
        setCellValue( HSSFDateUtil.getExcelDate(value, this.book.getWorkbook().isUsing1904DateWindowing()) );
    }

    /**
     * set a string value for the cell. 
     *
     * @param value value to set the cell to.  For formulas we'll set the formula
     * cached string result, for String cells we'll set its value. For other types we will
     * change the cell to a string cell and set its value.
     * If value is null then we will change the cell to a Blank cell.
     */
    public void setCellValue(String value) {
        HSSFRichTextString str = value == null ? null :  new HSSFRichTextString(value);
        setCellValue(str);
    }

    /**
     * set a string value for the cell. Please note that if you are using
     * full 16 bit unicode you should call <code>setEncoding()</code> first.
     *
     * @param value  value to set the cell to.  For formulas we'll set the formula
     * string, for String cells we'll set its value.  For other types we will
     * change the cell to a string cell and set its value.
     * If value is null then we will change the cell to a Blank cell.
     */

    public void setCellValue(RichTextString value)
    {
        HSSFRichTextString hvalue = (HSSFRichTextString) value;
        int row=record.getRow();
        short col=record.getColumn();
        short styleIndex=record.getXFIndex();
        if (hvalue == null)
        {
            setCellType(CELL_TYPE_BLANK, false, row, col, styleIndex);
            return;
        }
        if (cellType == CELL_TYPE_FORMULA) {
            // Set the 'pre-evaluated result' for the formula
            // note - formulas do not preserve text formatting.
            FormulaRecordAggregate fr = (FormulaRecordAggregate) record;
            fr.setCachedStringResult(hvalue.getString());
            // Update our local cache to the un-formatted version
            stringValue = new HSSFRichTextString(value.getString());

            // All done
            return;
        }

        // If we get here, we're not dealing with a formula,
        //  so handle things as a normal rich text cell

        if (cellType != CELL_TYPE_STRING) {
            setCellType(CELL_TYPE_STRING, false, row, col, styleIndex);
        }
        int index = 0;

        UnicodeString str = hvalue.getUnicodeString();
        index = book.getWorkbook().addSSTString(str);
        (( LabelSSTRecord ) record).setSSTIndex(index);
        stringValue = hvalue;
        stringValue.setWorkbookReferences(book.getWorkbook(), (( LabelSSTRecord ) record));
        stringValue.setUnicodeString(book.getWorkbook().getSSTString(index));
    }

    public void setCellFormula(String formula) {
        int row=record.getRow();
        short col=record.getColumn();
        short styleIndex=record.getXFIndex();

        if (formula==null) {
            setCellType(CELL_TYPE_BLANK, false, row, col, styleIndex);
            return;
        }
        setCellType(CELL_TYPE_FORMULA, false, row, col, styleIndex);
        FormulaRecordAggregate rec = (FormulaRecordAggregate) record;
        FormulaRecord frec = rec.getFormulaRecord();
        frec.setOptions((short) 2);
        frec.setValue(0);

        //only set to default if there is no extended format index already set
        if (rec.getXFIndex() == (short)0) {
            rec.setXFIndex((short) 0x0f);
        }
        Ptg[] ptgs = HSSFFormulaParser.parse(formula, book);
        frec.setParsedExpression(ptgs);
    }

    public String getCellFormula() {
        return HSSFFormulaParser.toFormulaString(book, ((FormulaRecordAggregate)record).getFormulaRecord().getParsedExpression());
    }

    /**
     * Used to help format error messages
     */
    private static String getCellTypeName(int cellTypeCode) {
        switch (cellTypeCode) {
            case CELL_TYPE_BLANK:   return "blank";
            case CELL_TYPE_STRING:  return "text";
            case CELL_TYPE_BOOLEAN: return "boolean";
            case CELL_TYPE_ERROR:   return "error";
            case CELL_TYPE_NUMERIC: return "numeric";
            case CELL_TYPE_FORMULA: return "formula";
        }
        return "#unknown cell type (" + cellTypeCode + ")#";
    }

    private static RuntimeException typeMismatch(int expectedTypeCode, int actualTypeCode, boolean isFormulaCell) {
        String msg = "Cannot get a "
            + getCellTypeName(expectedTypeCode) + " value from a "
            + getCellTypeName(actualTypeCode) + " " + (isFormulaCell ? "formula " : "") + "cell";
        return new IllegalStateException(msg);
    }
    private static void checkFormulaCachedValueType(int expectedTypeCode, FormulaRecord fr) {
        int cachedValueType = fr.getCachedResultType();
        if (cachedValueType != expectedTypeCode) {
            throw typeMismatch(expectedTypeCode, cachedValueType, true);
        }
    }

    /**
     * Get the value of the cell as a number.
     * For strings we throw an exception.
     * For blank cells we return a 0.
     * See {@link HSSFDataFormatter} for turning this
     *  number into a string similar to that which
     *  Excel would render this number as.
     */
    public double getNumericCellValue() {

        switch(cellType) {
            case CELL_TYPE_BLANK:
                return 0.0;
            case CELL_TYPE_NUMERIC:
                return ((NumberRecord)record).getValue();
            default:
                throw typeMismatch(CELL_TYPE_NUMERIC, cellType, false);
            case CELL_TYPE_FORMULA:
                break;
        }
        FormulaRecord fr = ((FormulaRecordAggregate)record).getFormulaRecord();
        checkFormulaCachedValueType(CELL_TYPE_NUMERIC, fr);
        return fr.getValue();
    }

    /**
     * Get the value of the cell as a date.
     * For strings we throw an exception.
     * For blank cells we return a null.
     * See {@link HSSFDataFormatter} for formatting
     *  this date into a string similar to how excel does.
     */
    public Date getDateCellValue() {

        if (cellType == CELL_TYPE_BLANK) {
            return null;
        }
        double value = getNumericCellValue();
        if (book.getWorkbook().isUsing1904DateWindowing()) {
            return HSSFDateUtil.getJavaDate(value, true);
        }
        return HSSFDateUtil.getJavaDate(value, false);
    }

    /**
     * get the value of the cell as a string - for numeric cells we throw an exception.
     * For blank cells we return an empty string.
     * For formulaCells that are not string Formulas, we return empty String
     * @deprecated Use the HSSFRichTextString return
     */

    public String getStringCellValue()
    {
      HSSFRichTextString str = getRichStringCellValue();
      return str.getString();
    }

    /**
     * get the value of the cell as a string - for numeric cells we throw an exception.
     * For blank cells we return an empty string.
     * For formulaCells that are not string Formulas, we return empty String
     */
    public HSSFRichTextString getRichStringCellValue() {

        switch(cellType) {
            case CELL_TYPE_BLANK:
                return new HSSFRichTextString("");
            case CELL_TYPE_STRING:
                return stringValue;
            default:
                throw typeMismatch(CELL_TYPE_STRING, cellType, false);
            case CELL_TYPE_FORMULA:
                break;
        }
        FormulaRecordAggregate fra = ((FormulaRecordAggregate)record);
        checkFormulaCachedValueType(CELL_TYPE_STRING, fra.getFormulaRecord());
        String strVal = fra.getStringValue();
        return new HSSFRichTextString(strVal == null ? "" : strVal);
    }

    /**
     * set a boolean value for the cell
     *
     * @param value the boolean value to set this cell to.  For formulas we'll set the
     *        precalculated value, for booleans we'll set its value. For other types we
     *        will change the cell to a boolean cell and set its value.
     */

    public void setCellValue(boolean value) {
        int row=record.getRow();
        short col=record.getColumn();
        short styleIndex=record.getXFIndex();

        switch (cellType) {
            default:
                setCellType(CELL_TYPE_BOOLEAN, false, row, col, styleIndex);
            case CELL_TYPE_ERROR:
                (( BoolErrRecord ) record).setValue(value);
                break;
            case CELL_TYPE_FORMULA:
                ((FormulaRecordAggregate)record).getFormulaRecord().setCachedResultBoolean(value);
                break;
        }
    }

    /**
     * set a error value for the cell
     *
     * @param errorCode the error value to set this cell to.  For formulas we'll set the
     *        precalculated value , for errors we'll set
     *        its value. For other types we will change the cell to an error
     *        cell and set its value.
     */
    public void setCellErrorValue(byte errorCode) {
        int row=record.getRow();
        short col=record.getColumn();
        short styleIndex=record.getXFIndex();
        switch (cellType) {
            default:
                setCellType(CELL_TYPE_ERROR, false, row, col, styleIndex);
            case CELL_TYPE_ERROR:
                (( BoolErrRecord ) record).setValue(errorCode);
                break;
            case CELL_TYPE_FORMULA:
                ((FormulaRecordAggregate)record).getFormulaRecord().setCachedResultErrorCode(errorCode);
                break;
        }
    }
    /**
     * Chooses a new boolean value for the cell when its type is changing.<p/>
     *
     * Usually the caller is calling setCellType() with the intention of calling
     * setCellValue(boolean) straight afterwards.  This method only exists to give
     * the cell a somewhat reasonable value until the setCellValue() call (if at all).
     * TODO - perhaps a method like setCellTypeAndValue(int, Object) should be introduced to avoid this
     */
    private boolean convertCellValueToBoolean() {

        switch (cellType) {
            case CELL_TYPE_BOOLEAN:
                return (( BoolErrRecord ) record).getBooleanValue();
            case CELL_TYPE_STRING:
                return Boolean.valueOf(((StringRecord)record).getString()).booleanValue();
            case CELL_TYPE_NUMERIC:
                return ((NumberRecord)record).getValue() != 0;

            // All other cases convert to false
            // These choices are not well justified.
            case CELL_TYPE_FORMULA:
                // should really evaluate, but HSSFCell can't call HSSFFormulaEvaluator
            case CELL_TYPE_ERROR:
            case CELL_TYPE_BLANK:
                return false;
        }
        throw new RuntimeException("Unexpected cell type (" + cellType + ")");
    }

    /**
     * get the value of the cell as a boolean.  For strings, numbers, and errors, we throw an exception.
     * For blank cells we return a false.
     */
    public boolean getBooleanCellValue() {

        switch(cellType) {
            case CELL_TYPE_BLANK:
                return false;
            case CELL_TYPE_BOOLEAN:
                return (( BoolErrRecord ) record).getBooleanValue();
            default:
                throw typeMismatch(CELL_TYPE_BOOLEAN, cellType, false);
            case CELL_TYPE_FORMULA:
                break;
        }
        FormulaRecord fr = ((FormulaRecordAggregate)record).getFormulaRecord();
        checkFormulaCachedValueType(CELL_TYPE_BOOLEAN, fr);
        return fr.getCachedBooleanValue();
    }

    /**
     * get the value of the cell as an error code.  For strings, numbers, and booleans, we throw an exception.
     * For blank cells we return a 0.
     */
    public byte getErrorCellValue() {
        switch(cellType) {
            case CELL_TYPE_ERROR:
                return (( BoolErrRecord ) record).getErrorValue();
            default:
                throw typeMismatch(CELL_TYPE_ERROR, cellType, false);
            case CELL_TYPE_FORMULA:
                break;
        }
        FormulaRecord fr = ((FormulaRecordAggregate)record).getFormulaRecord();
        checkFormulaCachedValueType(CELL_TYPE_ERROR, fr);
        return (byte) fr.getCachedErrorValue();
    }

    /**
     * set the style for the cell.  The style should be an HSSFCellStyle created/retreived from
     * the HSSFWorkbook.
     *
     * @param style  reference contained in the workbook
     * @see org.apache.poi.hssf.usermodel.HSSFWorkbook#createCellStyle()
     * @see org.apache.poi.hssf.usermodel.HSSFWorkbook#getCellStyleAt(short)
     */
    public void setCellStyle(CellStyle style) {
		setCellStyle( (HSSFCellStyle)style );
    }
    public void setCellStyle(HSSFCellStyle style) {
        // Verify it really does belong to our workbook
        style.verifyBelongsToWorkbook(book);

        // Change our cell record to use this style
        record.setXFIndex(style.getIndex());
    }

    /**
     * get the style for the cell.  This is a reference to a cell style contained in the workbook
     * object.
     * @see org.apache.poi.hssf.usermodel.HSSFWorkbook#getCellStyleAt(short)
     */
    public HSSFCellStyle getCellStyle()
    {
      short styleIndex=record.getXFIndex();
      ExtendedFormatRecord xf = book.getWorkbook().getExFormatAt(styleIndex);
      return new HSSFCellStyle(styleIndex, xf, book);
    }

    /**
     * Should only be used by HSSFSheet and friends.  Returns the low level CellValueRecordInterface record
     *
     * @return CellValueRecordInterface representing the cell via the low level api.
     */

    protected CellValueRecordInterface getCellValueRecord()
    {
        return record;
    }

    /**
     * @throws RuntimeException if the bounds are exceeded.
     */
    private void checkBounds(int cellNum) {
      if (cellNum > 255) {
          throw new RuntimeException("You cannot have more than 255 columns "+
                    "in a given row (IV).  Because Excel can't handle it");
      }
      else if (cellNum < 0) {
          throw new RuntimeException("You cannot reference columns with an index of less then 0.");
      }
    }

    /**
     * Sets this cell as the active cell for the worksheet
     */
    public void setAsActiveCell()
    {
        int row=record.getRow();
        short col=record.getColumn();
        this.sheet.getSheet().setActiveCellRow(row);
        this.sheet.getSheet().setActiveCellCol(col);
    }

    /**
     * Returns a string representation of the cell
     *
     * This method returns a simple representation,
     * anthing more complex should be in user code, with
     * knowledge of the semantics of the sheet being processed.
     *
     * Formula cells return the formula string,
     * rather than the formula result.
     * Dates are displayed in dd-MMM-yyyy format
     * Errors are displayed as #ERR&lt;errIdx&gt;
     */
    public String toString() {
        switch (getCellType()) {
            case CELL_TYPE_BLANK:
                return "";
            case CELL_TYPE_BOOLEAN:
                return getBooleanCellValue()?"TRUE":"FALSE";
            case CELL_TYPE_ERROR:
                return ErrorEval.getText((( BoolErrRecord ) record).getErrorValue());
            case CELL_TYPE_FORMULA:
                return getCellFormula();
            case CELL_TYPE_NUMERIC:
                //TODO apply the dataformat for this cell
                if (HSSFDateUtil.isCellDateFormatted(this)) {
                    DateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
                    return sdf.format(getDateCellValue());
                } else {
                    return  getNumericCellValue() + "";
                }
            case CELL_TYPE_STRING:
                return getStringCellValue();
            default:
                return "Unknown Cell Type: " + getCellType();
        }
    }

    /**
     * Assign a comment to this cell. If the supplied
     *  comment is null, the comment for this cell
     *  will be removed.
     *
     * @param comment comment associated with this cell
     */
    public void setCellComment(Comment comment){
        if(comment == null) {
            removeCellComment();
            return;
        }

        comment.setRow((short)record.getRow());
        comment.setColumn(record.getColumn());
        this.comment = (HSSFComment)comment;
    }

    /**
     * Returns comment associated with this cell
     *
     * @return comment associated with this cell
     */
     public HSSFComment getCellComment(){
        if (comment == null) {
            comment = findCellComment(sheet.getSheet(), record.getRow(), record.getColumn());
        }
        return comment;
    }

    /**
     * Removes the comment for this cell, if
     *  there is one.
     * WARNING - some versions of excel will loose
     *  all comments after performing this action!
     */
    public void removeCellComment() {
        HSSFComment comment = findCellComment(sheet.getSheet(), record.getRow(), record.getColumn());
        this.comment = null;

        if(comment == null) {
            // Nothing to do
            return;
        }

        // Zap the underlying NoteRecord
        List sheetRecords = sheet.getSheet().getRecords();
        sheetRecords.remove(comment.getNoteRecord());

        // If we have a TextObjectRecord, is should
        //  be proceeed by:
        // MSODRAWING with container
        // OBJ
        // MSODRAWING with EscherTextboxRecord
        if(comment.getTextObjectRecord() != null) {
            TextObjectRecord txo = comment.getTextObjectRecord();
            int txoAt = sheetRecords.indexOf(txo);

            if(sheetRecords.get(txoAt-3) instanceof DrawingRecord &&
                sheetRecords.get(txoAt-2) instanceof ObjRecord &&
                sheetRecords.get(txoAt-1) instanceof DrawingRecord) {
                // Zap these, in reverse order
                sheetRecords.remove(txoAt-1);
                sheetRecords.remove(txoAt-2);
                sheetRecords.remove(txoAt-3);
            } else {
                throw new IllegalStateException("Found the wrong records before the TextObjectRecord, can't remove comment");
            }

            // Now remove the text record
            sheetRecords.remove(txo);
        }
    }

    /**
     * Cell comment finder.
     * Returns cell comment for the specified sheet, row and column.
     *
     * @return cell comment or <code>null</code> if not found
     */
    protected static HSSFComment findCellComment(Sheet sheet, int row, int column){
        HSSFComment comment = null;
        HashMap txshapes = new HashMap(); //map shapeId and TextObjectRecord
        for (Iterator it = sheet.getRecords().iterator(); it.hasNext(); ) {
           RecordBase rec = (RecordBase) it.next();
           if (rec instanceof NoteRecord){
               NoteRecord note = (NoteRecord)rec;
               if (note.getRow() == row && note.getColumn() == column){
                   TextObjectRecord txo = (TextObjectRecord)txshapes.get(new Integer(note.getShapeId()));
                   comment = new HSSFComment(note, txo);
                   comment.setRow(note.getRow());
                   comment.setColumn(note.getColumn());
                   comment.setAuthor(note.getAuthor());
                   comment.setVisible(note.getFlags() == NoteRecord.NOTE_VISIBLE);
                   comment.setString(txo.getStr());
                   break;
               }
           } else if (rec instanceof ObjRecord){
               ObjRecord obj = (ObjRecord)rec;
               SubRecord sub = (SubRecord)obj.getSubRecords().get(0);
               if (sub instanceof CommonObjectDataSubRecord){
                   CommonObjectDataSubRecord cmo = (CommonObjectDataSubRecord)sub;
                   if (cmo.getObjectType() == CommonObjectDataSubRecord.OBJECT_TYPE_COMMENT){
                       //find the nearest TextObjectRecord which holds comment's text and map it to its shapeId
                       while(it.hasNext()) {
                           rec = ( Record ) it.next();
                           if (rec instanceof TextObjectRecord) {
                               txshapes.put(new Integer(cmo.getObjectId()), rec);
                               break;
                           }
                       }

                   }
               }
           }
        }
        return comment;
   }

    /**
     * Returns hyperlink associated with this cell
     *
     * @return hyperlink associated with this cell or null if not found
     */
    public HSSFHyperlink getHyperlink(){
        for (Iterator it = sheet.getSheet().getRecords().iterator(); it.hasNext(); ) {
            RecordBase rec = (RecordBase) it.next();
            if (rec instanceof HyperlinkRecord){
                HyperlinkRecord link = (HyperlinkRecord)rec;
                if(link.getFirstColumn() == record.getColumn() && link.getFirstRow() == record.getRow()){
                    return new HSSFHyperlink(link);
                }
            }
        }
        return null;
    }

    /**
     * Assign a hypelrink to this cell
     *
     * @param link hypelrink associated with this cell
     */
    public void setHyperlink(Hyperlink hyperlink){
    	HSSFHyperlink link = (HSSFHyperlink)hyperlink;
    	
        link.setFirstRow(record.getRow());
        link.setLastRow(record.getRow());
        link.setFirstColumn(record.getColumn());
        link.setLastColumn(record.getColumn());

        switch(link.getType()){
            case HSSFHyperlink.LINK_EMAIL:
            case HSSFHyperlink.LINK_URL:
                link.setLabel("url");
                break;
            case HSSFHyperlink.LINK_FILE:
                link.setLabel("file");
                break;
            case HSSFHyperlink.LINK_DOCUMENT:
                link.setLabel("place");
                break;
        }

        int eofLoc = sheet.getSheet().findFirstRecordLocBySid( EOFRecord.sid );
        sheet.getSheet().getRecords().add( eofLoc, link.record );
    }
    /**
     * Only valid for formula cells
     * @return one of ({@link #CELL_TYPE_NUMERIC}, {@link #CELL_TYPE_STRING},
     *     {@link #CELL_TYPE_BOOLEAN}, {@link #CELL_TYPE_ERROR}) depending
     * on the cached value of the formula
     */
    public int getCachedFormulaResultType() {
        if (this.cellType != CELL_TYPE_FORMULA) {
            throw new IllegalStateException("Only formula cells have cached results");
        }
        return ((FormulaRecordAggregate)record).getFormulaRecord().getCachedResultType();
    }
}
