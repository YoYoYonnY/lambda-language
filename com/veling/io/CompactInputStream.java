package com.veling.io;

import com.veling.util.*;
import java.io.*;

public class CompactInputStream extends FastInputStream  {
	protected int fLastSynchronization = -1;

	public CompactInputStream(InputStream in)  {
		super(in);
	}
	
	public int readHeader(Object sender) throws IOException  {
		String classname = readString();
		if (!classname.equals(sender.getClass().getName()))  {
			throw new IOException("Illegal file format: ["+classname+"], expected ["+sender.getClass().getName()+"]");
		}
		//version
		return readInt();
	}
	
	public void readSynchronization() throws IOException  {
		int magic = readByte();
		if (magic == CompactOutputStream.MAGIC0)  {
			magic = readByte();
			if (magic == CompactOutputStream.MAGIC1)  {
				int syncnum = readVInt();
				if (syncnum == fLastSynchronization+1)  {
					//ok
					fLastSynchronization++;
				} else {
					throw new IOException("synchronization flag number invalid; should be "+(fLastSynchronization+1)+" but found "+syncnum);
				}
			} else {
				throw new IOException("synchronization flag 1 expected; found "+magic+" lastsynch="+fLastSynchronization);
			}
		} else {
			throw new IOException("synchronization flag 0 expected; found "+magic+" lastsynch="+fLastSynchronization);
		}
	}
	
}