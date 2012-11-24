/*
 * Copyright 1996-2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 * 
 * 
 * Temporarily modified to use with Java 6.
 * 
 * 
 */


package n3phele.agent.zip;

import java.io.OutputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.HashSet;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

/**
 * This class implements an output stream filter for writing files in the
 * ZIP file format. Includes support for both compressed and uncompressed
 * entries.
 *
 * @author      David Connelly
 */
public
class ZipOutputStream extends DeflaterOutputStream  {

    private static class XEntry {
        public final ZipEntry entry;
        public final long offset;
        public final int flag;
        public XEntry(ZipEntry entry, long offset) {
            this.entry = entry;
            this.offset = offset;
            this.flag = (entry.getMethod() == DEFLATED &&
                         (entry.getSize()  == -1 ||
                          entry.getCompressedSize() == -1 ||
                          entry.getCrc()   == -1))
                // store size, compressed size, and crc-32 in data descriptor
                // immediately following the compressed entry data
                ? 8
                // store size, compressed size, and crc-32 in LOC header
                : 0;
        }
    }

    private XEntry current;
    private Vector<XEntry> xentries = new Vector<XEntry>();
    private HashSet<String> names = new HashSet<String>();
    private CRC32 crc = new CRC32();
    private long written = 0;
    private long locoff = 0;
    private String comment;
    private int method = DEFLATED;
    private boolean finished;

    private boolean closed = false;

    private static int version(ZipEntry e) throws ZipException {
        switch (e.getMethod()) {
        case DEFLATED: return 20;
        case STORED:   return 10;
        default: throw new ZipException("unsupported compression method");
        }
    }

    /**
     * Checks to make sure that this stream has not been closed.
     */
    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
    }
    /**
     * Compression method for uncompressed (STORED) entries.
     */
    public static final int STORED = ZipEntry.STORED;

    /**
     * Compression method for compressed (DEFLATED) entries.
     */
    public static final int DEFLATED = ZipEntry.DEFLATED;

    /**
     * Creates a new ZIP output stream.
     * @param out the actual output stream
     */
    public ZipOutputStream(OutputStream out) {
    	 super(out);
        //super(out, new Deflater(Deflater.DEFAULT_COMPRESSION, true));
       // usesDefaultDeflater = true;
    }

    /**
     * Sets the ZIP file comment.
     * @param comment the comment string
     * @exception IllegalArgumentException if the length of the specified
     *            ZIP file comment is greater than 0xFFFF bytes
     */
    public void setComment(String comment) {
        if (comment != null && comment.length() > 0xffff/3
                                           && getUTF8Length(comment) > 0xffff) {
            throw new IllegalArgumentException("ZIP file comment too long.");
        }
        this.comment = comment;
    }

    /**
     * Sets the default compression method for subsequent entries. This
     * default will be used whenever the compression method is not specified
     * for an individual ZIP file entry, and is initially set to DEFLATED.
     * @param method the default compression method
     * @exception IllegalArgumentException if the specified compression method
     *            is invalid
     */
    public void setMethod(int method) {
        if (method != DEFLATED && method != STORED) {
            throw new IllegalArgumentException("invalid compression method");
        }
        this.method = method;
    }

    /**
     * Sets the compression level for subsequent entries which are DEFLATED.
     * The default setting is DEFAULT_COMPRESSION.
     * @param level the compression level (0-9)
     * @exception IllegalArgumentException if the compression level is invalid
     */
    public void setLevel(int level) {
        def.setLevel(level);
    }

    /**
     * Begins writing a new ZIP file entry and positions the stream to the
     * start of the entry data. Closes the current entry if still active.
     * The default compression method will be used if no compression method
     * was specified for the entry, and the current time will be used if
     * the entry has no set modification time.
     * @param e the ZIP entry to be written
     * @exception ZipException if a ZIP format error has occurred
     * @exception IOException if an I/O error has occurred
     */
    public void putNextEntry(ZipEntry e) throws IOException {
        ensureOpen();
        if (current != null) {
            closeEntry();       // close previous entry
        }
        if (e.getTime() == -1) {
            e.setTime(System.currentTimeMillis());
        }
        if (e.getMethod() == -1) {
            e.setMethod(method);  // use default method
        }
        switch (e.getMethod()) {
        case DEFLATED:
            break;
        case STORED:
            // compressed size, uncompressed size, and crc-32 must all be
            // set for entries using STORED compression method
            if (e.getSize() == -1) {
                e.setSize(e.getCompressedSize());
            } else if (e.getCompressedSize() == -1) {
                e.setCompressedSize(e.getSize());
            } else if (e.getSize() != e.getCompressedSize()) {
                throw new ZipException(
                    "STORED entry where compressed != uncompressed size");
            }
            if (e.getSize() == -1 || e.getCrc() == -1) {
                throw new ZipException(
                    "STORED entry missing size, compressed size, or crc-32");
            }
            break;
        default:
            throw new ZipException("unsupported compression method");
        }
        if (! names.add(e.getName())) {
            throw new ZipException("duplicate entry: " + e.getName());
        }
        current = new XEntry(e, written);
        xentries.add(current);
        writeLOC(current);
    }

    /**
     * Closes the current ZIP entry and positions the stream for writing
     * the next entry.
     * @exception ZipException if a ZIP format error has occurred
     * @exception IOException if an I/O error has occurred
     */
    public void closeEntry() throws IOException {
        ensureOpen();
        if (current != null) {
            ZipEntry e = current.entry;
            switch (e.getMethod()) {
            case DEFLATED:
                def.finish();
                while (!def.finished()) {
                    deflate();
                }
                if ((current.flag & 8) == 0) {
                    // verify size, compressed size, and crc-32 settings
                    if (e.getSize() != def.getBytesRead()) {
                        throw new ZipException(
                            "invalid entry size (expected " + e.getSize() +
                            " but got " + def.getBytesRead() + " bytes)");
                    }
                    if (e.getCompressedSize() != def.getBytesWritten()) {
                        throw new ZipException(
                            "invalid entry compressed size (expected " +
                            e.getCompressedSize() + " but got " + def.getBytesWritten() + " bytes)");
                    }
                    if (e.getCrc() != crc.getValue()) {
                        throw new ZipException(
                            "invalid entry CRC-32 (expected 0x" +
                            Long.toHexString(e.getCrc()) + " but got 0x" +
                            Long.toHexString(crc.getValue()) + ")");
                    }
                } else {
                    e.setSize(def.getBytesRead());
                    e.setCompressedSize(def.getBytesWritten());
                    e.setCrc(crc.getValue());
                    writeEXT(e);
                }
                def.reset();
                written += e.getCompressedSize();
                break;
            case STORED:
                // we already know that both e.size and e.csize are the same
                if (e.getSize() != written - locoff) {
                    throw new ZipException(
                        "invalid entry size (expected " + e.getSize() +
                        " but got " + (written - locoff) + " bytes)");
                }
                if (e.getCrc() != crc.getValue()) {
                    throw new ZipException(
                         "invalid entry crc-32 (expected 0x" +
                         Long.toHexString(e.getCrc()) + " but got 0x" +
                         Long.toHexString(crc.getValue()) + ")");
                }
                break;
            default:
                throw new ZipException("invalid compression method");
            }
            crc.reset();
            current = null;
        }
    }

    /**
     * Writes an array of bytes to the current ZIP entry data. This method
     * will block until all the bytes are written.
     * @param b the data to be written
     * @param off the start offset in the data
     * @param len the number of bytes that are written
     * @exception ZipException if a ZIP file error has occurred
     * @exception IOException if an I/O error has occurred
     */
    public synchronized void write(byte[] b, int off, int len)
        throws IOException
    {
        ensureOpen();
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }

        if (current == null) {
            throw new ZipException("no current ZIP entry");
        }
        ZipEntry entry = current.entry;
        switch (entry.getMethod()) {
        case DEFLATED:
            super.write(b, off, len);
            break;
        case STORED:
            written += len;
            if (written - locoff > entry.getSize()) {
                throw new ZipException(
                    "attempt to write past end of STORED entry");
            }
            out.write(b, off, len);
            break;
        default:
            throw new ZipException("invalid compression method");
        }
        crc.update(b, off, len);
    }

    /**
     * Finishes writing the contents of the ZIP output stream without closing
     * the underlying stream. Use this method when applying multiple filters
     * in succession to the same output stream.
     * @exception ZipException if a ZIP file error has occurred
     * @exception IOException if an I/O exception has occurred
     */
    public void finish() throws IOException {
        ensureOpen();
        if (finished) {
            return;
        }
        if (current != null) {
            closeEntry();
        }
        // write central directory
        long off = written;
        for (XEntry xentry : xentries)
            writeCEN(xentry);
        writeEND(off, written - off);
        finished = true;
    }

    /**
     * Closes the ZIP output stream as well as the stream being filtered.
     * @exception ZipException if a ZIP file error has occurred
     * @exception IOException if an I/O error has occurred
     */
    public void close() throws IOException {
        if (!closed) {
            super.close();
            closed = true;
        }
    }

    /*
     * Writes local file (LOC) header for specified entry.
     */
    private void writeLOC(XEntry xentry) throws IOException {
        ZipEntry e = xentry.entry;
        int flag = xentry.flag;
        int elen = (e.getExtra() != null) ? e.getExtra().length : 0;
        boolean hasZip64 = false;

        writeInt(LOCSIG);           // LOC header signature

        if ((flag & 8) == 8) {
            writeShort(version(e));     // version needed to extract
            writeShort(flag);           // general purpose bit flag
            writeShort(e.getMethod());       // compression method
            writeInt(e.getTime());           // last modification time

            // store size, uncompressed size, and crc-32 in data descriptor
            // immediately following compressed entry data
            writeInt(0);
            writeInt(0);
            writeInt(0);
        } else {
            if (e.getCompressedSize() >= ZIP64_MAGICVAL || e.getSize() >= ZIP64_MAGICVAL) {
                hasZip64 = true;
                writeShort(45);         // ver 4.5 for zip64
            } else {
                writeShort(version(e)); // version needed to extract
            }
            writeShort(flag);           // general purpose bit flag
            writeShort(e.getMethod());       // compression method
            writeInt(e.getTime());           // last modification time
            writeInt(e.getCrc());            // crc-32
            if (hasZip64) {
                writeInt(ZIP64_MAGICVAL);
                writeInt(ZIP64_MAGICVAL);
                elen += 20;        //headid(2) + size(2) + size(8) + csize(8)
            } else {
                writeInt(e.getCompressedSize());  // compressed size
                writeInt(e.getSize());   // uncompressed size
            }
        }
        byte[] nameBytes = getUTF8Bytes(e.getName());
        writeShort(nameBytes.length);
        writeShort(elen);
        writeBytes(nameBytes, 0, nameBytes.length);
        if (hasZip64) {
            writeShort(ZIP64_EXTID);
            writeShort(16);
            writeLong(e.getSize());
            writeLong(e.getCompressedSize());
        }
        if (e.getExtra() != null) {
            writeBytes(e.getExtra(), 0, e.getExtra().length);
        }
        locoff = written;
    }

    /*
     * Writes extra data descriptor (EXT) for specified entry.
     */
    private void writeEXT(ZipEntry e) throws IOException {
        writeInt(EXTSIG);           // EXT header signature
        writeInt(e.getCrc());            // crc-32
        if (e.getCompressedSize() >= ZIP64_MAGICVAL || e.getSize() >= ZIP64_MAGICVAL) {
            writeLong(e.getCompressedSize());
            writeLong(e.getSize());
        } else {
            writeInt(e.getCompressedSize());          // compressed size
            writeInt(e.getSize());           // uncompressed size
        }
    }

    /*
     * Write central directory (CEN) header for specified entry.
     * REMIND: add support for file attributes
     */
    private void writeCEN(XEntry xentry) throws IOException {
        ZipEntry e  = xentry.entry;
        int flag = xentry.flag;
        int version = version(e);

        long csize = e.getCompressedSize();
        long size = e.getSize();
        long offset = xentry.offset;
        int e64len = 0;
        boolean hasZip64 = false; 
        if (e.getCompressedSize() >= ZIP64_MAGICVAL) {
            csize = ZIP64_MAGICVAL;
            e64len += 8;              // csize(8)
            hasZip64 = true;
        }
        if (e.getSize() >= ZIP64_MAGICVAL) {
            size = ZIP64_MAGICVAL;    // size(8) 
            e64len += 8;
            hasZip64 = true;
        } 
        if (xentry.offset >= ZIP64_MAGICVAL) {
            offset = ZIP64_MAGICVAL;
            e64len += 8;              // offset(8)
            hasZip64 = true;
        } 
        writeInt(CENSIG);           // CEN header signature
        if (hasZip64) {
            writeShort(45);         // ver 4.5 for zip64
            writeShort(45);
        } else {
            writeShort(version);    // version made by
            writeShort(version);    // version needed to extract
        }
        writeShort(flag);           // general purpose bit flag
        writeShort(e.getMethod());       // compression method
        writeInt(e.getTime());           // last modification time
        writeInt(e.getCrc());            // crc-32
        writeInt(csize);            // compressed size
        writeInt(size);             // uncompressed size
        byte[] nameBytes = getUTF8Bytes(e.getName());
        writeShort(nameBytes.length);
        if (hasZip64) {
            // + headid(2) + datasize(2)
            writeShort(e64len + 4 + (e.getExtra() != null ? e.getExtra().length : 0));
        } else {
            writeShort(e.getExtra() != null ? e.getExtra().length : 0);
        }
        byte[] commentBytes;
        if (e.getComment() != null) {
            commentBytes = getUTF8Bytes(e.getComment());
            writeShort(commentBytes.length);
        } else {
            commentBytes = null;
            writeShort(0);
        }
        writeShort(0);              // starting disk number
        writeShort(0);              // internal file attributes (unused)
        writeInt(0);                // external file attributes (unused)
        writeInt(offset);           // relative offset of local header
        writeBytes(nameBytes, 0, nameBytes.length);
        if (hasZip64) {
            writeShort(ZIP64_EXTID);// Zip64 extra
            writeShort(e64len);
            if (size == ZIP64_MAGICVAL)
                writeLong(e.getSize());
            if (csize == ZIP64_MAGICVAL)
                writeLong(e.getCompressedSize());
            if (offset == ZIP64_MAGICVAL)
                writeLong(xentry.offset);
        }
        if (e.getExtra() != null) {
            writeBytes(e.getExtra(), 0, e.getExtra().length);
        }
        if (commentBytes != null) {
            writeBytes(commentBytes, 0, commentBytes.length);
        }
    }

    /*
     * Writes end of central directory (END) header.
     */
    private void writeEND(long off, long len) throws IOException {
        boolean hasZip64 = false;
        long xlen = len;
        long xoff = off;
        if (xlen >= ZIP64_MAGICVAL) {
            xlen = ZIP64_MAGICVAL;
            hasZip64 = true;
        }
        if (xoff >= ZIP64_MAGICVAL) {
            xoff = ZIP64_MAGICVAL;
            hasZip64 = true;
        }
        int count = xentries.size();
        if (count >= ZIP64_MAGICCOUNT) {
            count = ZIP64_MAGICCOUNT;
            hasZip64 = true;
        }
        if (hasZip64) {
            long off64 = written;
	    //zip64 end of central directory record
            writeInt(ZIP64_ENDSIG);        // zip64 END record signature
            writeLong(ZIP64_ENDHDR - 12);  // size of zip64 end
            writeShort(45);                // version made by
            writeShort(45);                // version needed to extract
            writeInt(0);                   // number of this disk
            writeInt(0);                   // central directory start disk
            writeLong(xentries.size());    // number of directory entires on disk
            writeLong(xentries.size());    // number of directory entires
            writeLong(len);                // length of central directory
            writeLong(off);                // offset of central directory

	    //zip64 end of central directory locator
            writeInt(ZIP64_LOCSIG);        // zip64 END locator signature
            writeInt(0);                   // zip64 END start disk
            writeLong(off64);              // offset of zip64 END
            writeInt(1);                   // total number of disks (?)
        }
        writeInt(ENDSIG);                 // END record signature
        writeShort(0);                    // number of this disk
        writeShort(0);                    // central directory start disk
        writeShort(count);                // number of directory entries on disk
        writeShort(count);                // total number of directory entries
        writeInt(xlen);                   // length of central directory
        writeInt(xoff);                   // offset of central directory
        if (comment != null) {            // zip file comment
            byte[] b = getUTF8Bytes(comment);
            writeShort(b.length);
            writeBytes(b, 0, b.length);
        } else {
            writeShort(0);
        }
    }

    /*
     * Writes a 16-bit short to the output stream in little-endian byte order.
     */
    private void writeShort(int v) throws IOException {
        OutputStream out = this.out;
        out.write((v >>> 0) & 0xff);
        out.write((v >>> 8) & 0xff);
        written += 2;
    }

    /*
     * Writes a 32-bit int to the output stream in little-endian byte order.
     */
    private void writeInt(long v) throws IOException {
        OutputStream out = this.out;
        out.write((int)((v >>>  0) & 0xff));
        out.write((int)((v >>>  8) & 0xff));
        out.write((int)((v >>> 16) & 0xff));
        out.write((int)((v >>> 24) & 0xff));
        written += 4;
    }

    /*
     * Writes a 64-bit int to the output stream in little-endian byte order.
     */
    private void writeLong(long v) throws IOException {
        OutputStream out = this.out;
        out.write((int)((v >>>  0) & 0xff));
        out.write((int)((v >>>  8) & 0xff));
        out.write((int)((v >>> 16) & 0xff));
        out.write((int)((v >>> 24) & 0xff));
        out.write((int)((v >>> 32) & 0xff));
        out.write((int)((v >>> 40) & 0xff));
        out.write((int)((v >>> 48) & 0xff));
        out.write((int)((v >>> 56) & 0xff));
        written += 8;
    }

    /*
     * Writes an array of bytes to the output stream.
     */
    private void writeBytes(byte[] b, int off, int len) throws IOException {
        super.out.write(b, off, len);
        written += len;
    }

    /*
     * Returns the length of String's UTF8 encoding.
     */
    static int getUTF8Length(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch <= 0x7f) {
                count++;
            } else if (ch <= 0x7ff) {
                count += 2;
            } else {
                count += 3;
            }
        }
        return count;
    }

    /*
     * Returns an array of bytes representing the UTF8 encoding
     * of the specified String.
     */
    private static byte[] getUTF8Bytes(String s) {
        char[] c = s.toCharArray();
        int len = c.length;
        // Count the number of encoded bytes...
        int count = 0;
        for (int i = 0; i < len; i++) {
            int ch = c[i];
            if (ch <= 0x7f) {
                count++;
            } else if (ch <= 0x7ff) {
                count += 2;
            } else {
                count += 3;
            }
        }
        // Now return the encoded bytes...
        byte[] b = new byte[count];
        int off = 0;
        for (int i = 0; i < len; i++) {
            int ch = c[i];
            if (ch <= 0x7f) {
                b[off++] = (byte)ch;
            } else if (ch <= 0x7ff) {
                b[off++] = (byte)((ch >> 6) | 0xc0);
                b[off++] = (byte)((ch & 0x3f) | 0x80);
            } else {
                b[off++] = (byte)((ch >> 12) | 0xe0);
                b[off++] = (byte)(((ch >> 6) & 0x3f) | 0x80);
                b[off++] = (byte)((ch & 0x3f) | 0x80);
            }
        }
        return b;
 
    }
    
    
    /*
     * ZIP64 constants
     */
    static final long ZIP64_ENDSIG = 0x06064b50L;  // "PK\006\006"
    static final long ZIP64_LOCSIG = 0x07064b50L;  // "PK\006\007"
    static final int  ZIP64_ENDHDR = 56;           // ZIP64 end header size
    static final int  ZIP64_LOCHDR = 20;           // ZIP64 end loc header size
    static final int  ZIP64_EXTHDR = 24;           // EXT header size
    static final int  ZIP64_EXTID  = 0x0001;       // Extra field Zip64 header ID

    static final int  ZIP64_MAGICCOUNT = 0xFFFF;
    static final long ZIP64_MAGICVAL = 0xFFFFFFFFL;

    /*
     * Zip64 End of central directory (END) header field offsets
     */
    static final int  ZIP64_ENDLEN = 4;       // size of zip64 end of central dir
    static final int  ZIP64_ENDVEM = 12;      // version made by
    static final int  ZIP64_ENDVER = 14;      // version needed to extract
    static final int  ZIP64_ENDNMD = 16;      // number of this disk 
    static final int  ZIP64_ENDDSK = 20;      // disk number of start
    static final int  ZIP64_ENDTOD = 24;      // total number of entries on this disk
    static final int  ZIP64_ENDTOT = 32;      // total number of entries
    static final int  ZIP64_ENDSIZ = 40;      // central directory size in bytes
    static final int  ZIP64_ENDOFF = 48;      // offset of first CEN header
    static final int  ZIP64_ENDEXT = 56;      // zip64 extensible data sector

    /*
     * Zip64 End of central directory locator field offsets
     */
    static final int  ZIP64_LOCDSK = 4;       // disk number start
    static final int  ZIP64_LOCOFF = 8;       // offset of zip64 end
    static final int  ZIP64_LOCTOT = 16;      // total number of disks

    /*
     * Zip64 Extra local (EXT) header field offsets
     */
    static final int  ZIP64_EXTCRC = 4;       // uncompressed file crc-32 value
    static final int  ZIP64_EXTSIZ = 8;       // compressed size, 8-byte
    static final int  ZIP64_EXTLEN = 16;      // uncompressed size, 8-byte
    
       /*
        * Header signatures
        */
       static long LOCSIG = 0x04034b50L;   // "PK\003\004"
       static long EXTSIG = 0x08074b50L;   // "PK\007\008"
      static long CENSIG = 0x02014b50L;   // "PK\001\002"
       static long ENDSIG = 0x06054b50L;   // "PK\005\006"
   
       /*
        * Header sizes in bytes (including signatures)
        */
       static final int LOCHDR = 30;       // LOC header size
       static final int EXTHDR = 16;       // EXT header size
       static final int CENHDR = 46;       // CEN header size
       static final int ENDHDR = 22;       // END header size

       /*
        * Local file (LOC) header field offsets
        */
       static final int LOCVER = 4;        // version needed to extract
       static final int LOCFLG = 6;        // general purpose bit flag
       static final int LOCHOW = 8;        // compression method
       static final int LOCTIM = 10;       // modification time
       static final int LOCCRC = 14;       // uncompressed file crc-32 value
       static final int LOCSIZ = 18;       // compressed size
       static final int LOCLEN = 22;       // uncompressed size
       static final int LOCNAM = 26;       // filename length
       static final int LOCEXT = 28;       // extra field length

       /*
        * Extra local (EXT) header field offsets
        */
       static final int EXTCRC = 4;        // uncompressed file crc-32 value
       static final int EXTSIZ = 8;        // compressed size
       static final int EXTLEN = 12;       // uncompressed size

       /*
        * Central directory (CEN) header field offsets
        */
       static final int CENVEM = 4;        // version made by
       static final int CENVER = 6;        // version needed to extract
       static final int CENFLG = 8;        // encrypt, decrypt flags
       static final int CENHOW = 10;       // compression method
       static final int CENTIM = 12;       // modification time
       static final int CENCRC = 16;       // uncompressed file crc-32 value
       static final int CENSIZ = 20;       // compressed size
       static final int CENLEN = 24;       // uncompressed size
       static final int CENNAM = 28;       // filename length
       static final int CENEXT = 30;       // extra field length
       static final int CENCOM = 32;       // comment length
       static final int CENDSK = 34;       // disk number start
       static final int CENATT = 36;       // internal file attributes
       static final int CENATX = 38;       // external file attributes
       static final int CENOFF = 42;       // LOC header offset

       /*
        * End of central directory (END) header field offsets
        */
       static final int ENDSUB = 8;        // number of entries on this disk
       static final int ENDTOT = 10;       // total number of entries
       static final int ENDSIZ = 12;       // central directory size in bytes
       static final int ENDOFF = 16;       // offset of first CEN header
       static final int ENDCOM = 20;       // zip file comment length

    
    
}
