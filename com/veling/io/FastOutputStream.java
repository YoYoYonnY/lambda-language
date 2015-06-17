package com.veling.io;

import java.io.*;

//copied, adapted slightly from com.lucene.store.FSOutputStream 
public class FastOutputStream extends OutputStream {
	final static int BUFFER_SIZE = 2048;

	private final byte[] buffer = new byte[BUFFER_SIZE];
	private long bufferStart = 0;			  // position in file of buffer
	private int bufferPosition = 0;		  // position in buffer

	private OutputStream out;

	public FastOutputStream(OutputStream out)  {
		this.out = out;
	}

	public final void writeByte(byte b) throws IOException {
		if (bufferPosition >= BUFFER_SIZE)  {
			flush();
		}
		buffer[bufferPosition++] = b;
	}

	public final void writeBytes(byte[] b, int length) throws IOException {
		if (bufferPosition + length >= BUFFER_SIZE)  {
			//do normal, byte-by-byte add to flush()
			for (int i = 0; i < length; i++)
				writeByte(b[i]);
		} else {
			//no flush() needed
			System.arraycopy(b,0,buffer,bufferPosition,length);
			bufferPosition += length;
		}	
	}
	
	public final void write(int i) throws IOException  {
		writeInt(i);
	}

	public final void writeInt(int i) throws IOException {
		writeByte((byte)(i >> 24));
		writeByte((byte)(i >> 16));
		writeByte((byte)(i >>  8));
		writeByte((byte) i);
	}

	public final void writeVInt(int i) throws IOException {
		while ((i & ~0x7F) != 0) {
			writeByte((byte)((i & 0x7f) | 0x80));
			i >>>= 7;
		}
		writeByte((byte)i);
	}
	
	public final void writeLong(long i) throws IOException {
		writeInt((int) (i >> 32));
		writeInt((int) i);
	}

	public final void writeVLong(long i) throws IOException {
		while ((i & ~0x7F) != 0) {
			writeByte((byte)((i & 0x7f) | 0x80));
			i >>>= 7;
		}
		writeByte((byte)i);
	}
	
	private byte[] charbytes = new byte[100];
	
	public final void writeString(String s) throws IOException  {
		final int length = s.length();
		
		if (length * 3 > charbytes.length)  {
			charbytes = new byte[length * 3];
		}
		
		int p = 0;
		for (int i=0; i<length; i++) {
			final int code = (int) s.charAt(i);
			if (code >= 0x01 && code <= 0x7F)  {
				charbytes[p++] = (byte) code;
			} else if (((code >= 0x80) && (code <= 0x7FF)) || code == 0) {
				charbytes[p++] = ((byte)(0xC0 | (code >> 6)));
				charbytes[p++] = ((byte)(0x80 | (code & 0x3F)));
			} else {
				charbytes[p++] = ((byte)(0xE0 | (code >>> 12)));
				charbytes[p++] = ((byte)(0x80 | ((code >> 6) & 0x3F)));
				charbytes[p++] = ((byte)(0x80 | (code & 0x3F)));
			}
		}
		writeVInt(p);
		writeBytes(charbytes,p);
	}
	
	public final void writeLowString(String s) throws IOException  {
		final int length = s.length();
	
		if (length > charbytes.length)  {
			charbytes = new byte[length];
		}
		
		for (int i=0; i<length; i++) {
			charbytes[i] = (byte) s.charAt(i);
		}
		writeBytes(charbytes,length);
	}
	
	public final void flush() throws IOException {
		flushBuffer(buffer, bufferPosition);
		bufferStart += bufferPosition;
		bufferPosition = 0;
	}

	public void close() throws IOException {
		flush();
		out.close();
	}

	public final long getFilePointer() throws IOException {
		return bufferStart + bufferPosition;
	}

	public final void flushBuffer(byte[] b, int size) throws IOException {
		out.write(b, 0, size);
	}
	
}
