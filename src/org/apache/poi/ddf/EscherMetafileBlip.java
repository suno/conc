/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.poi.ddf;

import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.InflaterInputStream;

/**
 * @author Daniel Noll
 * @version $Id$
 */
public class EscherMetafileBlip
        extends EscherBlipRecord
{
    private static final POILogger log = POILogFactory.getLogger(EscherMetafileBlip.class);

    public static final short RECORD_ID_EMF = (short) 0xF018 + 2;
    public static final short RECORD_ID_WMF = (short) 0xF018 + 3;
    public static final short RECORD_ID_PICT = (short) 0xF018 + 4;

    /**
     * BLIP signatures as defined in the escher spec
     */
    public static final short SIGNATURE_EMF  = 0x3D40;
    public static final short SIGNATURE_WMF  = 0x2160;
    public static final short SIGNATURE_PICT = 0x5420;

    private static final int HEADER_SIZE = 8;

    private byte[] field_1_UID;
    /**
     * The primary UID is only saved to disk if (blip_instance ^ blip_signature == 1)
     */
    private byte[] field_2_UID;
    private int field_2_cb;
    private int field_3_rcBounds_x1;
    private int field_3_rcBounds_y1;
    private int field_3_rcBounds_x2;
    private int field_3_rcBounds_y2;
    private int field_4_ptSize_w;
    private int field_4_ptSize_h;
    private int field_5_cbSave;
    private byte field_6_fCompression;
    private byte field_7_fFilter;

    private byte[] raw_pictureData;

    /**
     * This method deserializes the record from a byte array.
     *
     * @param data          The byte array containing the escher record information
     * @param offset        The starting offset into <code>data</code>.
     * @param recordFactory May be null since this is not a container record.
     * @return The number of bytes read from the byte array.
     */
    public int fillFields( byte[] data, int offset, EscherRecordFactory recordFactory )
    {
        int bytesAfterHeader = readHeader( data, offset );
        int pos = offset + HEADER_SIZE;

        field_1_UID = new byte[16];
        System.arraycopy( data, pos, field_1_UID, 0, 16 ); pos += 16;

        if((getOptions() ^ getSignature()) == 0x10){
            field_2_UID = new byte[16];
            System.arraycopy( data, pos, field_2_UID, 0, 16 ); pos += 16;
        }

        field_2_cb = LittleEndian.getInt( data, pos ); pos += 4;
        field_3_rcBounds_x1 = LittleEndian.getInt( data, pos ); pos += 4;
        field_3_rcBounds_y1 = LittleEndian.getInt( data, pos ); pos += 4;
        field_3_rcBounds_x2 = LittleEndian.getInt( data, pos ); pos += 4;
        field_3_rcBounds_y2 = LittleEndian.getInt( data, pos ); pos += 4;
        field_4_ptSize_w = LittleEndian.getInt( data, pos ); pos += 4;
        field_4_ptSize_h = LittleEndian.getInt( data, pos ); pos += 4;
        field_5_cbSave = LittleEndian.getInt( data, pos ); pos += 4;
        field_6_fCompression = data[pos]; pos++;
        field_7_fFilter = data[pos]; pos++;

        raw_pictureData = new byte[field_5_cbSave];
        System.arraycopy( data, pos, raw_pictureData, 0, field_5_cbSave );

        // 0 means DEFLATE compression
        // 0xFE means no compression
        if (field_6_fCompression == 0)
        {
            field_pictureData = inflatePictureData(raw_pictureData);
        }
        else
        {
            field_pictureData = raw_pictureData;
        }

        return bytesAfterHeader + HEADER_SIZE;
    }

    /**
     * Serializes the record to an existing byte array.
     *
     * @param offset    the offset within the byte array
     * @param data      the data array to serialize to
     * @param listener  a listener for begin and end serialization events.  This
     *                  is useful because the serialization is
     *                  hierarchical/recursive and sometimes you need to be able
     *                  break into that.
     * @return the number of bytes written.
     */
    public int serialize( int offset, byte[] data, EscherSerializationListener listener )
    {
        listener.beforeRecordSerialize(offset, getRecordId(), this);

        int pos = offset;
        LittleEndian.putShort( data, pos, getOptions() ); pos += 2;
        LittleEndian.putShort( data, pos, getRecordId() ); pos += 2;
        LittleEndian.putInt( data, pos, getRecordSize() - HEADER_SIZE ); pos += 4;

        System.arraycopy( field_1_UID, 0, data, pos, field_1_UID.length ); pos += field_1_UID.length;
        if((getOptions() ^ getSignature()) == 0x10){
            System.arraycopy( field_2_UID, 0, data, pos, field_2_UID.length ); pos += field_2_UID.length;
        }
        LittleEndian.putInt( data, pos, field_2_cb ); pos += 4;
        LittleEndian.putInt( data, pos, field_3_rcBounds_x1 ); pos += 4;
        LittleEndian.putInt( data, pos, field_3_rcBounds_y1 ); pos += 4;
        LittleEndian.putInt( data, pos, field_3_rcBounds_x2 ); pos += 4;
        LittleEndian.putInt( data, pos, field_3_rcBounds_y2 ); pos += 4;
        LittleEndian.putInt( data, pos, field_4_ptSize_w ); pos += 4;
        LittleEndian.putInt( data, pos, field_4_ptSize_h ); pos += 4;
        LittleEndian.putInt( data, pos, field_5_cbSave ); pos += 4;
        data[pos] = field_6_fCompression; pos++;
        data[pos] = field_7_fFilter; pos++;

        System.arraycopy( raw_pictureData, 0, data, pos, raw_pictureData.length );

        listener.afterRecordSerialize(offset + getRecordSize(), getRecordId(), getRecordSize(), this);
        return getRecordSize();
    }

    /**
     * Decompresses the provided data, returning the inflated result.
     *
     * @param data the deflated picture data.
     * @return the inflated picture data.
     */
    private static byte[] inflatePictureData(byte[] data)
    {
        try
        {
            InflaterInputStream in = new InflaterInputStream(
                new ByteArrayInputStream( data ) );
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int readBytes;
            while ((readBytes = in.read(buf)) > 0)
            {
                out.write(buf, 0, readBytes);
            }
            return out.toByteArray();
        }
        catch ( IOException e )
        {
            log.log(POILogger.WARN, "Possibly corrupt compression or non-compressed data", e);
            return data;
        }
    }

    /**
     * Returns the number of bytes that are required to serialize this record.
     *
     * @return Number of bytes
     */
    public int getRecordSize()
    {
        int size = 8 + 50 + raw_pictureData.length;
        if((getOptions() ^ getSignature()) == 0x10){
            size += field_2_UID.length;
        }
        return size;
    }

    public byte[] getUID()
    {
        return field_1_UID;
    }

    public void setUID( byte[] field_1_UID )
    {
        this.field_1_UID = field_1_UID;
    }

    public byte[] getPrimaryUID()
    {
        return field_2_UID;
    }

    public void setPrimaryUID( byte[] field_2_UID )
    {
        this.field_2_UID = field_2_UID;
    }

    public int getUncompressedSize()
    {
        return field_2_cb;
    }

    public void setUncompressedSize(int uncompressedSize)
    {
        field_2_cb = uncompressedSize;
    }

    public Rectangle getBounds()
    {
        return new Rectangle(field_3_rcBounds_x1,
                             field_3_rcBounds_y1,
                             field_3_rcBounds_x2 - field_3_rcBounds_x1,
                             field_3_rcBounds_y2 - field_3_rcBounds_y1);
    }

    public void setBounds(Rectangle bounds)
    {
        field_3_rcBounds_x1 = bounds.x;
        field_3_rcBounds_y1 = bounds.y;
        field_3_rcBounds_x2 = bounds.x + bounds.width;
        field_3_rcBounds_y2 = bounds.y + bounds.height;
    }

    public Dimension getSizeEMU()
    {
        return new Dimension(field_4_ptSize_w, field_4_ptSize_h);
    }

    public void setSizeEMU(Dimension sizeEMU)
    {
        field_4_ptSize_w = sizeEMU.width;
        field_4_ptSize_h = sizeEMU.height;
    }

    public int getCompressedSize()
    {
        return field_5_cbSave;
    }

    public void setCompressedSize(int compressedSize)
    {
        field_5_cbSave = compressedSize;
    }

    public boolean isCompressed()
    {
        return (field_6_fCompression == 0);
    }

    public void setCompressed(boolean compressed)
    {
        field_6_fCompression = compressed ? 0 : (byte)0xFE;
    }

    // filtering is always 254 according to available docs, so no point giving it a setter method.

    public String toString()
    {
        String nl = System.getProperty( "line.separator" );

        String extraData;
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try
        {
            HexDump.dump( this.field_pictureData, 0, b, 0 );
            extraData = b.toString();
        }
        catch ( Exception e )
        {
            extraData = e.toString();
        }
        return getClass().getName() + ":" + nl +
                "  RecordId: 0x" + HexDump.toHex( getRecordId() ) + nl +
                "  Options: 0x" + HexDump.toHex( getOptions() ) + nl +
                "  UID: 0x" + HexDump.toHex( field_1_UID ) + nl +
                (field_2_UID == null ? "" : ("  UID2: 0x" + HexDump.toHex( field_2_UID ) + nl)) +
                "  Uncompressed Size: " + HexDump.toHex( field_2_cb ) + nl +
                "  Bounds: " + getBounds() + nl +
                "  Size in EMU: " + getSizeEMU() + nl +
                "  Compressed Size: " + HexDump.toHex( field_5_cbSave ) + nl +
                "  Compression: " + HexDump.toHex( field_6_fCompression ) + nl +
                "  Filter: " + HexDump.toHex( field_7_fFilter ) + nl +
                "  Extra Data:" + nl + extraData;
    }

    /**
     * Return the blip signature
     *
     * @return the blip signature
     */
    public short getSignature(){
        short sig = 0;
        switch(getRecordId()){
            case RECORD_ID_EMF: sig = SIGNATURE_EMF; break;
            case RECORD_ID_WMF: sig = SIGNATURE_WMF; break;
            case RECORD_ID_PICT: sig = SIGNATURE_PICT; break;
            default: log.log(POILogger.WARN, "Unknown metafile: " + getRecordId()); break;
        }
        return sig;
    }
}
