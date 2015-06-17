package com.veling.io;

import java.io.*;

public class CompactOutputStream extends FastOutputStream  {
	protected static final byte MAGIC0 = (byte) 0xFF;
	protected static final byte MAGIC1 = (byte) 0xAA;
	protected int fNextSynchronization = 0;
	
	public CompactOutputStream(OutputStream out)  {
		super(out);
	}
	
	public void writeHeader(Object sender, int version) throws IOException  {
		writeString(sender.getClass().getName());
		writeInt(version);
	}
	
	public void writeSynchronization() throws IOException  {
		writeByte(MAGIC0);
		writeByte(MAGIC1);
		writeVInt(fNextSynchronization++);
	}
	
}