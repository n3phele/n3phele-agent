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
 * Temporarily modified to use with Java 6.
 * 
 * 
 */

package n3phele.agent.zip;

import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.PushbackInputStream;
import java.util.zip.CRC32;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;


/**
 * This class implements an input stream filter for reading files in the
 * ZIP file format. Includes support for both compressed and uncompressed
 * entries.
 *
 * @author      David Connelly
 */
public
class ZipInputStream extends InflaterInputStream  {
    private ZipEntry entry;
    private int flag;
    private CRC32 crc = new CRC32();
    private long remaining;
    private byte[] tmpbuf = new byte[512];

    private static final int STORED = ZipEntry.STORED;
    private static final int DEFLATED = ZipEntry.DEFLATED;

    private boolean closed = false;
    // this flag is set to true after EOF has reached for
    // one entry
    private boolean entryEOF = false;

    /**
     * Check to make sure that this stream has not been closed
     */
    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
    }

    /**
     * Creates a new ZIP input stream.
     * @param in the actual input stream
     */
    public ZipInputStream(InputStream in) {
        super(new PushbackInputStream(in, 512), new Inflater(true), 512);
        //usesDefaultInflater = true;
        if(in == null) {
            throw new NullPointerException("in is null");
        }
    }

    /**
     * Reads the next ZIP file entry and positions the stream at the
     * beginning of the entry data.
     * @return the next ZIP file entry, or null if there are no more entries
     * @exception ZipException if a ZIP file error has occurred
     * @exception IOException if an I/O error has occurred
     */
    public ZipEntry getNextEntry() throws IOException {
        ensureOpen();
        if (entry != null) {
            closeEntry();
        }
        crc.reset();
        inf.reset();
        if ((entry = readLOC()) == null) {
            return null;
        }
        if (entry.getMethod() == STORED) {
            remaining = entry.getSize();
        }
        entryEOF = false;
        return entry;
    }

    /**
     * Closes the current ZIP entry and positions the stream for reading the
     * next entry.
     * @exception ZipException if a ZIP file error has occurred
     * @exception IOException if an I/O error has occurred
     */
    public void closeEntry() throws IOException {
        ensureOpen();
        while (read(tmpbuf, 0, tmpbuf.length) != -1) ;
        entryEOF = true;
    }

    /**
     * Returns 0 after EOF has reached for the current entry data,
     * otherwise always return 1.
     * <p>
     * Programs should not count on this method to return the actual number
     * of bytes that could be read without blocking.
     *
     * @return     1 before EOF and 0 after EOF has reached for current entry.
     * @exception  IOException  if an I/O error occurs.
     *
     */
    public int available() throws IOException {
        ensureOpen();
        if (entryEOF) {
            return 0;
        } else {
            return 1;
        }
    }

    /**
     * Reads from the current ZIP entry into an array of bytes.
     * If <code>len</code> is not zero, the method
     * blocks until some input is available; otherwise, no
     * bytes are read and <code>0</code> is returned.
     * @param b the buffer into which the data is read
     * @param off the start offset in the destination array <code>b</code>
     * @param len the maximum number of bytes read
     * @return the actual number of bytes read, or -1 if the end of the
     *         entry is reached
     * @exception  NullPointerException If <code>b</code> is <code>null</code>.
     * @exception  IndexOutOfBoundsException If <code>off</code> is negative,
     * <code>len</code> is negative, or <code>len</code> is greater than
     * <code>b.length - off</code>
     * @exception ZipException if a ZIP file error has occurred
     * @exception IOException if an I/O error has occurred
     */
    public int read(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        if (entry == null) {
            return -1;
        }
        switch (entry.getMethod()) {
        case DEFLATED:
            len = super.read(b, off, len);
            if (len == -1) {
                readEnd(entry);
                entryEOF = true;
                entry = null;
            } else {
                crc.update(b, off, len);
            }
            return len;
        case STORED:
            if (remaining <= 0) {
                entryEOF = true;
                entry = null;
                return -1;
            }
            if (len > remaining) {
                len = (int)remaining;
            }
            len = in.read(b, off, len);
            if (len == -1) {
                throw new ZipException("unexpected EOF");
            }
            crc.update(b, off, len);
            remaining -= len;
            if (remaining == 0 && entry.getCrc() != crc.getValue()) {
                throw new ZipException(
                    "invalid entry CRC (expected 0x" + Long.toHexString(entry.getCrc()) +
                    " but got 0x" + Long.toHexString(crc.getValue()) + ")");
            }
            return len;
        default:
            throw new ZipException("invalid compression method");
        }
    }

    /**
     * Skips specified number of bytes in the current ZIP entry.
     * @param n the number of bytes to skip
     * @return the actual number of bytes skipped
     * @exception ZipException if a ZIP file error has occurred
     * @exception IOException if an I/O error has occurred
     * @exception IllegalArgumentException if n < 0
     */
    public long skip(long n) throws IOException {
        if (n < 0) {
            throw new IllegalArgumentException("negative skip length");
        }
        ensureOpen();
        int max = (int)Math.min(n, Integer.MAX_VALUE);
        int total = 0;
        while (total < max) {
            int len = max - total;
            if (len > tmpbuf.length) {
                len = tmpbuf.length;
            }
            len = read(tmpbuf, 0, len);
            if (len == -1) {
                entryEOF = true;
                break;
            }
            total += len;
        }
        return total;
    }

    /**
     * Closes this input stream and releases any system resources associated
     * with the stream.
     * @exception IOException if an I/O error has occurred
     */
    public void close() throws IOException {
        if (!closed) {
            super.close();
            closed = true;
        }
    }

    private byte[] b = new byte[256];

    /*
     * Reads local file (LOC) header for next entry.
     */
    private ZipEntry readLOC() throws IOException {
        try {
            readFully(tmpbuf, 0, LOCHDR);
        } catch (EOFException e) {
            return null;
        }
        if (get32(tmpbuf, 0) != LOCSIG) {
            return null;
        }
        // get the entry name and create the ZipEntry64 first
        int len = get16(tmpbuf, LOCNAM);
        int blen = b.length;
        if (len > blen) {
            do
                blen = blen * 2;
            while (len > blen);
            b = new byte[blen];
        }
        readFully(b, 0, len);
        ZipEntry e = createZipEntry(getUTF8String(b, 0, len));
        // now get the remaining fields for the entry
        flag = get16(tmpbuf, LOCFLG);
        if ((flag & 1) == 1) {
            throw new ZipException("encrypted ZIP entry not supported");
        }
        e.setMethod(get16(tmpbuf, LOCHOW));
        e.setTime(get32(tmpbuf, LOCTIM));
        if ((flag & 8) == 8) {
            /* "Data Descriptor" present */
            if (e.getMethod() != DEFLATED) {
                throw new ZipException(
                        "only DEFLATED entries can have EXT descriptor");
            }
        } else {
            e.setCrc(get32(tmpbuf, LOCCRC));
            e.setCompressedSize(get32(tmpbuf, LOCSIZ));
            e.setSize(get32(tmpbuf, LOCLEN));
        }
        len = get16(tmpbuf, LOCEXT);
        if (len > 0) {
            byte[] bb = new byte[len];
            readFully(bb, 0, len);
            e.setExtra(bb);
            // extra fields are in "HeaderID(2)DataSize(2)Data... format
            if (e.getCompressedSize() == ZIP64_MAGICVAL || e.getSize() == ZIP64_MAGICVAL) {
                int off = 0;
	        while (off + 4 < len) {
                    int sz = get16(bb, off + 2);
                    if (get16(bb, off) == ZIP64_EXTID) {
                        off += 4;
                        // LOC extra zip64 entry MUST include BOTH original and
                        // compressed file size fields
                        if (sz < 16 || (off + sz) > len ) {
			    // invalid zip64 extra fields, simply skip.
			    // throw new ZipException("invalid Zip64 extra fields in LOC");
                            return e;
                        }
                        e.setSize(get64(bb, off));
                        e.setCompressedSize(get64(bb, off + 8));
                        break;
                    }
                    off += (sz + 4);
	        }
            }
        }
        return e;
    }

    /*
     * Fetches a UTF8-encoded String from the specified byte array.
     */
    private static String getUTF8String(byte[] b, int off, int len) {
        // First, count the number of characters in the sequence
        int count = 0;
        int max = off + len;
        int i = off;
        while (i < max) {
            int c = b[i++] & 0xff;
            switch (c >> 4) {
            case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
                // 0xxxxxxx
                count++;
                break;
            case 12: case 13:
                // 110xxxxx 10xxxxxx
                if ((b[i++] & 0xc0) != 0x80) {
                    throw new IllegalArgumentException();
                }
                count++;
                break;
            case 14:
                // 1110xxxx 10xxxxxx 10xxxxxx
                if (((b[i++] & 0xc0) != 0x80) ||
                    ((b[i++] & 0xc0) != 0x80)) {
                    throw new IllegalArgumentException();
                }
                count++;
                break;
            default:
                // 10xxxxxx, 1111xxxx
                throw new IllegalArgumentException();
            }
        }
        if (i != max) {
            throw new IllegalArgumentException();
        }
        // Now decode the characters...
        char[] cs = new char[count];
        i = 0;
        while (off < max) {
            int c = b[off++] & 0xff;
            switch (c >> 4) {
            case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
                // 0xxxxxxx
                cs[i++] = (char)c;
                break;
            case 12: case 13:
                // 110xxxxx 10xxxxxx
                cs[i++] = (char)(((c & 0x1f) << 6) | (b[off++] & 0x3f));
                break;
            case 14:
                // 1110xxxx 10xxxxxx 10xxxxxx
                int t = (b[off++] & 0x3f) << 6;
                cs[i++] = (char)(((c & 0x0f) << 12) | t | (b[off++] & 0x3f));
                break;
            default:
                // 10xxxxxx, 1111xxxx
                throw new IllegalArgumentException();
            }
        }
        return new String(cs, 0, count);
    }

    /**
     * Creates a new <code>ZipEntry64</code> object for the specified
     * entry name.
     *
     * @param name the ZIP file entry name
     * @return the ZipEntry64 just created
     */
    protected ZipEntry createZipEntry(String name) {
        return new ZipEntry64(name);
    }

    /*
     * Reads end of deflated entry as well as EXT descriptor if present.
     */
    private void readEnd(ZipEntry e) throws IOException {
        int n = inf.getRemaining();
        if (n > 0) {
            ((PushbackInputStream)in).unread(buf, len - n, n);
        }
        if ((flag & 8) == 8) {
            /* "Data Descriptor" present */
            if (inf.getBytesWritten() > ZIP64_MAGICVAL ||
                inf.getBytesRead() > ZIP64_MAGICVAL) {
                // ZIP64 format
                readFully(tmpbuf, 0, ZIP64_EXTHDR);
                long sig = get32(tmpbuf, 0);
                if (sig != EXTSIG) { // no EXTSIG present
                    e.setCrc(sig);
                    e.setCompressedSize(get64(tmpbuf, ZIP64_EXTSIZ - ZIP64_EXTCRC));
                    e.setSize(get64(tmpbuf, ZIP64_EXTLEN - ZIP64_EXTCRC));
                    ((PushbackInputStream)in).unread(
                        tmpbuf, ZIP64_EXTHDR - ZIP64_EXTCRC - 1, ZIP64_EXTCRC);
                } else {
                    e.setCrc(get32(tmpbuf, ZIP64_EXTCRC));
                    e.setCompressedSize(get64(tmpbuf, ZIP64_EXTSIZ));
                    e.setSize(get64(tmpbuf, ZIP64_EXTLEN));
                }
            } else {
                readFully(tmpbuf, 0, EXTHDR);
                long sig = get32(tmpbuf, 0);
                if (sig != EXTSIG) { // no EXTSIG present
                    e.setCrc(sig);
                    e.setCompressedSize(get32(tmpbuf, EXTSIZ - EXTCRC));
                    e.setSize(get32(tmpbuf, EXTLEN - EXTCRC));
                    ((PushbackInputStream)in).unread(
                                               tmpbuf, EXTHDR - EXTCRC - 1, EXTCRC);
                } else {
                    e.setCrc(get32(tmpbuf, EXTCRC));
                    e.setCompressedSize(get32(tmpbuf, EXTSIZ));
                    e.setSize(get32(tmpbuf, EXTLEN));
                }
            }
        }
        if (e.getSize() != inf.getBytesWritten()) {
        	if((e.getSize()& 0x7FFFFFFFL) != (inf.getBytesWritten()& 0x7FFFFFFF))
            throw new ZipException(
                "invalid entry size (expected " + e.getSize() +
                " but got " + inf.getBytesWritten() + " bytes)");
        }
        if (e.getCompressedSize() != inf.getBytesRead()) {
            throw new ZipException(
                "invalid entry compressed size (expected " + e.getCompressedSize() +
                " but got " + inf.getBytesRead() + " bytes)");
        }
        if (e.getCrc() != crc.getValue()) {
            throw new ZipException(
                "invalid entry CRC (expected 0x" + Long.toHexString(e.getCrc()) +
                " but got 0x" + Long.toHexString(crc.getValue()) + ")");
        }
    }

    /*
     * Reads bytes, blocking until all bytes are read.
     */
    private void readFully(byte[] b, int off, int len) throws IOException {
        while (len > 0) {
            int n = in.read(b, off, len);
            if (n == -1) {
                throw new EOFException();
            }
            off += n;
            len -= n;
        }
    }

    /*
     * Fetches unsigned 16-bit value from byte array at specified offset.
     * The bytes are assumed to be in Intel (little-endian) byte order.
     */
    private static final int get16(byte b[], int off) {
        return (b[off] & 0xff) | ((b[off+1] & 0xff) << 8);
    }

    /*
     * Fetches unsigned 32-bit value from byte array at specified offset.
     * The bytes are assumed to be in Intel (little-endian) byte order.
     */
    private static final long get32(byte b[], int off) {
        return (get16(b, off) | ((long)get16(b, off+2) << 16)) & 0xffffffffL;
    }

    /*
     * Fetches signed 64-bit value from byte array at specified offset.
     * The bytes are assumed to be in Intel (little-endian) byte order.
     */
    private static final long get64(byte b[], int off) {
        return get32(b, off) | (get32(b, off+4) << 32);
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
