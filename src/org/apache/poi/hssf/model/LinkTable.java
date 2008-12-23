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

package org.apache.poi.hssf.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.hssf.record.CRNCountRecord;
import org.apache.poi.hssf.record.CRNRecord;
import org.apache.poi.hssf.record.CountryRecord;
import org.apache.poi.hssf.record.ExternSheetRecord;
import org.apache.poi.hssf.record.ExternalNameRecord;
import org.apache.poi.hssf.record.NameRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.SupBookRecord;
import org.apache.poi.hssf.record.formula.NameXPtg;

/**
 * Link Table (OOO pdf reference: 4.10.3 ) <p/>
 *
 * The main data of all types of references is stored in the Link Table inside the Workbook Globals
 * Substream (4.2.5). The Link Table itself is optional and occurs only, if  there are any
 * references in the document.
 *  <p/>
 *
 *  In BIFF8 the Link Table consists of
 *  <ul>
 *  <li>zero or more EXTERNALBOOK Blocks<p/>
 *  	each consisting of
 *  	<ul>
 *  	<li>exactly one EXTERNALBOOK (0x01AE) record</li>
 *  	<li>zero or more EXTERNALNAME (0x0023) records</li>
 *  	<li>zero or more CRN Blocks<p/>
 *			each consisting of
 *  		<ul>
 *  		<li>exactly one XCT (0x0059)record</li>
 *  		<li>zero or more CRN (0x005A) records (documentation says one or more)</li>
 *  		</ul>
 *  	</li>
 *  	</ul>
 *  </li>
 *  <li>zero or one EXTERNSHEET (0x0017) record</li>
 *  <li>zero or more DEFINEDNAME (0x0018) records</li>
 *  </ul>
 *
 *
 * @author Josh Micich
 */
final class LinkTable {
	
	
	// TODO make this class into a record aggregate

	private static final class CRNBlock {

		private final CRNCountRecord _countRecord;
		private final CRNRecord[] _crns;

		public CRNBlock(RecordStream rs) {
			_countRecord = (CRNCountRecord) rs.getNext();
			int nCRNs = _countRecord.getNumberOfCRNs();
			CRNRecord[] crns = new CRNRecord[nCRNs];
			for (int i = 0; i < crns.length; i++) {
				crns[i] = (CRNRecord) rs.getNext();
			}
			_crns = crns;
		}
		public CRNRecord[] getCrns() {
			return (CRNRecord[]) _crns.clone();
		}
	}

	private static final class ExternalBookBlock {
		private final SupBookRecord _externalBookRecord;
		private final ExternalNameRecord[] _externalNameRecords;
		private final CRNBlock[] _crnBlocks;

		public ExternalBookBlock(RecordStream rs) {
			_externalBookRecord = (SupBookRecord) rs.getNext();
			List temp = new ArrayList();
			while(rs.peekNextClass() == ExternalNameRecord.class) {
			   temp.add(rs.getNext());
			}
			_externalNameRecords = new ExternalNameRecord[temp.size()];
			temp.toArray(_externalNameRecords);

			temp.clear();

			while(rs.peekNextClass() == CRNCountRecord.class) {
				temp.add(new CRNBlock(rs));
			}
			_crnBlocks = new CRNBlock[temp.size()];
			temp.toArray(_crnBlocks);
		}

		public ExternalBookBlock(short numberOfSheets) {
			_externalBookRecord = SupBookRecord.createInternalReferences(numberOfSheets);
			_externalNameRecords = new ExternalNameRecord[0];
			_crnBlocks = new CRNBlock[0];
		}

		public SupBookRecord getExternalBookRecord() {
			return _externalBookRecord;
		}

		public String getNameText(int definedNameIndex) {
			return _externalNameRecords[definedNameIndex].getText();
		}

		/**
		 * Performs case-insensitive search
		 * @return -1 if not found
		 */
		public int getIndexOfName(String name) {
			for (int i = 0; i < _externalNameRecords.length; i++) {
				if(_externalNameRecords[i].getText().equalsIgnoreCase(name)) {
					return i;
				}
			}
			return -1;
		}
	}

	private final ExternalBookBlock[] _externalBookBlocks;
	private final ExternSheetRecord _externSheetRecord;
	private final List _definedNames;
	private final int _recordCount;
	private final WorkbookRecordList _workbookRecordList; // TODO - would be nice to remove this

	public LinkTable(List inputList, int startIndex, WorkbookRecordList workbookRecordList) {

		_workbookRecordList = workbookRecordList;
		RecordStream rs = new RecordStream(inputList, startIndex);

		List temp = new ArrayList();
		while(rs.peekNextClass() == SupBookRecord.class) {
		   temp.add(new ExternalBookBlock(rs));
		}
		
		_externalBookBlocks = new ExternalBookBlock[temp.size()];
		temp.toArray(_externalBookBlocks);
		temp.clear();
		
		if (_externalBookBlocks.length > 0) {
			// If any ExternalBookBlock present, there is always 1 of ExternSheetRecord
			_externSheetRecord = readExtSheetRecord(rs);
		} else {
			_externSheetRecord = null;
		}
		
		_definedNames = new ArrayList();
		// collect zero or more DEFINEDNAMEs id=0x18
		while(rs.peekNextClass() == NameRecord.class) {
			NameRecord nr = (NameRecord)rs.getNext();
			_definedNames.add(nr);
		}

		_recordCount = rs.getCountRead();
		_workbookRecordList.getRecords().addAll(inputList.subList(startIndex, startIndex + _recordCount));
	}

	private static ExternSheetRecord readExtSheetRecord(RecordStream rs) {
		List temp = new ArrayList(2);
		while(rs.peekNextClass() == ExternSheetRecord.class) {
			temp.add(rs.getNext());
		}
		
		int nItems = temp.size();
		if (nItems < 1) {
			throw new RuntimeException("Expected an EXTERNSHEET record but got (" 
					+ rs.peekNextClass().getName() + ")");
		}
		if (nItems == 1) {
			// this is the normal case. There should be just one ExternSheetRecord
			return (ExternSheetRecord) temp.get(0);
		}
		// Some apps generate multiple ExternSheetRecords (see bug 45698).
		// It seems like the best thing to do might be to combine these into one
		ExternSheetRecord[] esrs = new ExternSheetRecord[nItems];
		temp.toArray(esrs);
		return ExternSheetRecord.combine(esrs);
	}

	public LinkTable(short numberOfSheets, WorkbookRecordList workbookRecordList) {
		_workbookRecordList = workbookRecordList;
		_definedNames = new ArrayList();
		_externalBookBlocks = new ExternalBookBlock[] {
				new ExternalBookBlock(numberOfSheets),
		};
		_externSheetRecord = new ExternSheetRecord();
		_recordCount = 2;

		// tell _workbookRecordList about the 2 new records

		SupBookRecord supbook = _externalBookBlocks[0].getExternalBookRecord();

		int idx = findFirstRecordLocBySid(CountryRecord.sid);
		if(idx < 0) {
			throw new RuntimeException("CountryRecord not found");
		}
		_workbookRecordList.add(idx+1, _externSheetRecord);
		_workbookRecordList.add(idx+1, supbook);
	}

	/**
	 * TODO - would not be required if calling code used RecordStream or similar
	 */
	public int getRecordCount() {
		return _recordCount;
	}


	/**
	 * @param builtInCode a BUILTIN_~ constant from {@link NameRecord}
	 * @param sheetNumber 1-based sheet number
	 */
	public NameRecord getSpecificBuiltinRecord(byte builtInCode, int sheetNumber) {

		Iterator iterator = _definedNames.iterator();
		while (iterator.hasNext()) {
			NameRecord record = ( NameRecord ) iterator.next();

			//print areas are one based
			if (record.getBuiltInName() == builtInCode && record.getSheetNumber() == sheetNumber) {
				return record;
			}
		}

		return null;
	}

	public void removeBuiltinRecord(byte name, int sheetIndex) {
		//the name array is smaller so searching through it should be faster than
		//using the findFirstXXXX methods
		NameRecord record = getSpecificBuiltinRecord(name, sheetIndex);
		if (record != null) {
			_definedNames.remove(record);
		}
		// TODO - do we need "Workbook.records.remove(...);" similar to that in Workbook.removeName(int namenum) {}?
	}

	public int getNumNames() {
		return _definedNames.size();
	}

	public NameRecord getNameRecord(int index) {
		return (NameRecord) _definedNames.get(index);
	}

	public void addName(NameRecord name) {
		_definedNames.add(name);

		// TODO - this is messy
		// Not the most efficient way but the other way was causing too many bugs
		int idx = findFirstRecordLocBySid(ExternSheetRecord.sid);
		if (idx == -1) idx = findFirstRecordLocBySid(SupBookRecord.sid);
		if (idx == -1) idx = findFirstRecordLocBySid(CountryRecord.sid);
		int countNames = _definedNames.size();
		_workbookRecordList.add(idx+countNames, name);
	}

	public void removeName(int namenum) {
		_definedNames.remove(namenum);
	}

	/**
	 * checks if the given name is already included in the linkTable
	 */
	public boolean nameAlreadyExists(NameRecord name)
	{
		// Check to ensure no other names have the same case-insensitive name
		for ( int i = getNumNames()-1; i >=0; i-- ) {
			NameRecord rec = getNameRecord(i);
			if (rec != name) {
				if (isDuplicatedNames(name, rec))
					return true;
			}
		}
		return false;
	}
	
	private static boolean isDuplicatedNames(NameRecord firstName, NameRecord lastName) {
		return lastName.getNameText().equalsIgnoreCase(firstName.getNameText()) 
			&& isSameSheetNames(firstName, lastName);
	}
	private static boolean isSameSheetNames(NameRecord firstName, NameRecord lastName) {
		return lastName.getSheetNumber() == firstName.getSheetNumber();
	}

	
	public int getIndexToSheet(int extRefIndex) {
		return _externSheetRecord.getFirstSheetIndexFromRefIndex(extRefIndex);
	}

	public int getSheetIndexFromExternSheetIndex(int extRefIndex) {
		if (extRefIndex >= _externSheetRecord.getNumOfRefs()) {
			return -1;
		}
		return _externSheetRecord.getFirstSheetIndexFromRefIndex(extRefIndex);
	}

	public int addSheetIndexToExternSheet(int sheetNumber) {
		// TODO - what about the first parameter (extBookIndex)?
		return _externSheetRecord.addRef(0, sheetNumber, sheetNumber);
	}

	public short checkExternSheet(int sheetIndex) {

		//Trying to find reference to this sheet
		int i = _externSheetRecord.getRefIxForSheet(sheetIndex);
		if (i>=0) {
			return (short)i;
		}
		//We Haven't found reference to this sheet
		return (short)addSheetIndexToExternSheet((short) sheetIndex);
	}


	/**
	 * copied from Workbook
	 */
	private int findFirstRecordLocBySid(short sid) {
		int index = 0;
		for (Iterator iterator = _workbookRecordList.iterator(); iterator.hasNext(); ) {
			Record record = ( Record ) iterator.next();

			if (record.getSid() == sid) {
				return index;
			}
			index ++;
		}
		return -1;
	}

	public String resolveNameXText(int refIndex, int definedNameIndex) {
		int extBookIndex = _externSheetRecord.getExtbookIndexFromRefIndex(refIndex);
		return _externalBookBlocks[extBookIndex].getNameText(definedNameIndex);
	}

	public NameXPtg getNameXPtg(String name) {
		// first find any external book block that contains the name:
		for (int i = 0; i < _externalBookBlocks.length; i++) {
			int definedNameIndex = _externalBookBlocks[i].getIndexOfName(name);
			if (definedNameIndex < 0) {
				continue;
			}
			// found it.
			int sheetRefIndex = findRefIndexFromExtBookIndex(i); 
			if (sheetRefIndex >= 0) {
				return new NameXPtg(sheetRefIndex, definedNameIndex);
			}
		}
		return null;
	}

	private int findRefIndexFromExtBookIndex(int extBookIndex) {
		return _externSheetRecord.findRefIndexFromExtBookIndex(extBookIndex); 
	}
}
