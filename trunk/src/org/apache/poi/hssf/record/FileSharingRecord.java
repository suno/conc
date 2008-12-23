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

import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.StringUtil;

/**
 * Title:        FILESHARING<P>
 * Description:  stores the encrypted readonly for a workbook (write protect) 
 * This functionality is accessed from the options dialog box available when performing 'Save As'.<p/>
 * REFERENCE:  PG 314 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)<p/>
 * @author Andrew C. Oliver (acoliver at apache dot org)
 */
public final class FileSharingRecord extends Record {

    public final static short sid = 0x5b;
    private short             field_1_readonly;
    private short             field_2_password;
    private byte              field_3_username_unicode_options;
    private String            field_3_username_value;

    public FileSharingRecord() {}
    

    /**
     * Constructs a FileSharing record and sets its fields appropriately.
     * @param in the RecordInputstream to read the record from
     */

    public FileSharingRecord(RecordInputStream in) {
        super(in);
    }

    protected void validateSid(short id) {
        if (id != sid) {
            throw new RecordFormatException("NOT A FILESHARING RECORD");
        }
    }

    protected void fillFields(RecordInputStream in) {
        field_1_readonly = in.readShort();
        field_2_password = in.readShort();
        
        int nameLen = in.readShort();
        
        if(nameLen > 0) {
            // TODO - Current examples(3) from junits only have zero length username. 
            field_3_username_unicode_options = in.readByte();
            field_3_username_value = in.readCompressedUnicode(nameLen);
        } else {
            field_3_username_value = "";
        }
    }

    //this is the world's lamest "security".  thanks to Wouter van Vugt for making me
    //not have to try real hard.  -ACO
    public static short hashPassword(String password) {
        byte[] passwordCharacters = password.getBytes();
        int hash = 0;
        if (passwordCharacters.length > 0) {
            int charIndex = passwordCharacters.length;
            while (charIndex-- > 0) {
                hash = ((hash >> 14) & 0x01) | ((hash << 1) & 0x7fff);
                hash ^= passwordCharacters[charIndex];
            }
            // also hash with charcount
            hash = ((hash >> 14) & 0x01) | ((hash << 1) & 0x7fff);
            hash ^= passwordCharacters.length;
            hash ^= (0x8000 | ('N' << 8) | 'K');
        }
        return (short)hash;
    } 

    /**
     * set the readonly flag
     *
     * @param readonly 1 for true, not 1 for false
     */
    public void setReadOnly(short readonly) {
        field_1_readonly = readonly;
    }

    /**
     * get the readonly
     *
     * @return short  representing if this is read only (1 = true)
     */
    public short getReadOnly() {
        return field_1_readonly;
    }

    /**
     * @param hashed password
     */
    public void setPassword(short password) {
        field_2_password = password;
    }

    /**
     * @returns password hashed with hashPassword() (very lame)
     */
    public short getPassword() {
        return field_2_password;
    }

    /**
     * @returns byte representing the length of the username field
     */
    public short getUsernameLength() {
        return (short) field_3_username_value.length();
    }

    /**
     * @returns username of the user that created the file
     */
    public String getUsername() {
        return field_3_username_value;
    }

    /**
     * @param username of the user that created the file
     */
    public void setUsername(String username) {
        field_3_username_value = username;
    }


    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[FILESHARING]\n");
        buffer.append("    .readonly       = ")
            .append(getReadOnly() == 1 ? "true" : "false").append("\n");
        buffer.append("    .password       = ")
            .append(Integer.toHexString(getPassword())).append("\n");
        buffer.append("    .username       = ")
            .append(getUsername()).append("\n");
        buffer.append("[/FILESHARING]\n");
        return buffer.toString();
    }

    public int serialize(int offset, byte [] data) {
        // TODO - junit
        LittleEndian.putShort(data, 0 + offset, sid);
        LittleEndian.putShort(data, 2 + offset, (short)(getRecordSize()-4));
        LittleEndian.putShort(data, 4 + offset, getReadOnly());
        LittleEndian.putShort(data, 6 + offset, getPassword());
        LittleEndian.putShort(data, 8 + offset, getUsernameLength());
        if(getUsernameLength() > 0) {
            LittleEndian.putByte(data, 10 + offset, field_3_username_unicode_options);
            StringUtil.putCompressedUnicode( getUsername(), data, 11 + offset );
        }
        return getRecordSize();
    }

    public int getRecordSize() {
        short nameLen = getUsernameLength();
        if (nameLen < 1) {
            return 10;
        }
        return 11+nameLen;
    }

    public short getSid() {
        return sid;
    }

    /**
     * Clone this record.
     */
    public Object clone() {
      FileSharingRecord clone = new FileSharingRecord();
      clone.setReadOnly(field_1_readonly);
      clone.setPassword(field_2_password);
      clone.setUsername(field_3_username_value);
      return clone;
    }
}
