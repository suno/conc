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

package org.apache.poi.hssf.eventusermodel.dummyrecord;

import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.RecordInputStream;

/**
 * A dummy record for when we're missing a row, but still
 *  want to trigger something
 */
public class MissingRowDummyRecord extends Record {
	private int rowNumber;
	
	public MissingRowDummyRecord(int rowNumber) {
		this.rowNumber = rowNumber;
	}
	
	protected void fillFields(RecordInputStream in) {
	}
	public short getSid() {
		return -1;
	}
	public int serialize(int offset, byte[] data) {
		return -1;
	}
	protected void validateSid(short id) {
	}
	
	public int getRowNumber() {
		return rowNumber;
	}
}
