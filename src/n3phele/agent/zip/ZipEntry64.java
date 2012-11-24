/**
 * @author Nigel Cook
 *
 * (C) Copyright 2010-2011. All rights reserved.
 */
package n3phele.agent.zip;

import java.util.zip.ZipEntry;

public class ZipEntry64 extends java.util.zip.ZipEntry {
	long size = -1;
	public ZipEntry64(ZipEntry64 e) {
		super(e);
	}


	public ZipEntry64(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}


	/* (non-Javadoc)
	 * @see java.util.zip.ZipEntry#getSize()
	 */
	@Override
	public long getSize() {
		return size;
	}


	/* (non-Javadoc)
	 * @see java.util.zip.ZipEntry#setSize(long)
	 */
	@Override
	public void setSize(long size) {
        if (size < 0) {  
            throw new IllegalArgumentException("invalid entry size");
        }
        this.size = size;

	}


}
