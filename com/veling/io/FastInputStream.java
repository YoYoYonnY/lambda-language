package com.veling.io;

import java.io.*;

//copied, adapted from com.lucene.store.FSInputStream
public class FastInputStream extends InputStream  {
	final static int BUFFER_SIZE = FastOutputStream.BUFFER_SIZE;

	private final byte[] buffer = new byte[BUFFER_SIZE];

	private long bufferStart = 0;			  // position in file of buffer
	private int bufferLength = 0;			  // end of valid bytes
	private int bufferPosition = 0;		  // next byte to read

	private InputStream in;
	
	public boolean eof = false;
	
	public FastInputStream(InputStream in)  {
		this.in = in;
	}
	
	public final byte readByte() throws IOException {
		if (bufferPosition >= bufferLength)  {
			refill();
			if (eof) throw new EOFException();
		}
		return buffer[bufferPosition++];
	}

	public final void readBytes(byte[] b, int offset, int len) throws IOException {
		if (bufferPosition + len >= bufferLength)  {
			//not enough room in this buffer, do normal byte by byte
			while (len > 0) {
				//copy what is still available in the buffer
				int avail = (bufferLength-bufferPosition);
				if (len < avail)  {
					avail = len;
				}
				System.arraycopy(buffer,bufferPosition,b,offset,avail);
				//update offsets
				offset += avail;
				bufferPosition += avail;
				len -= avail;
				if (len > 0)  {
					refill(); //now bufferPosition is set to 0 again
					if (eof) throw new EOFException();
				}
			}
		} else {
			//requested length still fits in buffer, no refill() needed
			System.arraycopy(buffer,bufferPosition,b,offset,len);
			bufferPosition += len;
		}
	}
	
	public final int read() throws IOException  {
		return readByte();
	}
	
	public final int readInt() throws IOException {
		if (bufferPosition + 4 >= bufferLength)  {
			return ((readByte() & 0xFF) << 24) | ((readByte() & 0xFF) << 16)
				| ((readByte() & 0xFF) <<  8) |  (readByte() & 0xFF);
		} else {
			return ((buffer[bufferPosition++] & 0xFF) << 24) | ((buffer[bufferPosition++] & 0xFF) << 16)
				| ((buffer[bufferPosition++] & 0xFF) <<  8) |  (buffer[bufferPosition++] & 0xFF);
		}
	}

	public final int readVInt() throws IOException {
		byte b = readByte();
		int i = b & 0x7F;
		for (int shift = 7; (b & 0x80) != 0; shift += 7) {
			b = readByte();
			i |= (b & 0x7F) << shift;
		}
		return i;
	}
	
	public final long readLong() throws IOException {
		return (((long)readInt()) << 32) | (readInt() & 0xFFFFFFFFL);
	}

	public final long readVLong() throws IOException {
		byte b = readByte();
		long i = b & 0x7F;
		for (int shift = 7; (b & 0x80) != 0; shift += 7) {
			b = readByte();
			i |= (b & 0x7FL) << shift;
		}
		return i;
	}

	private byte[] bytes = new byte[100];
	private char[] chars = new char[100];
	
	public final String readString() throws IOException  {
		int length = readVInt();
		if (length > bytes.length)  {
			//make byte buffer bigger
			bytes = new byte[length];
			chars = new char[length]; //could use only 1/3rd of bytes length
		}
		
		if (bufferPosition + length >= bufferLength)  {
			//string not yet in buffer, read
			readBytes(bytes,0,length);
			return readChars(bytes,0,length);
		} else {
			//use offset directly
			final int start = bufferPosition;
			bufferPosition += length; //skip over what am about to read
			return readChars(buffer,start,length);
		}
	}

	protected final String readChars(byte[] bs, int offset, int length)  {
		//convert character encoding to chars
		int p = 0;
		final int end = offset+length;
		for (int i=offset; i<end; i++) {
			byte b = bs[i];
			if ((b & 0x80) == 0)  {
				chars[p++] = (char)(b & 0x7F);
			} else if ((b & 0xE0) != 0xE0) {
				chars[p++] = (char)(((b & 0x1F) << 6)
					| (bs[++i] & 0x3F));
			} else  {
				chars[p++] = (char)(((b & 0x0F) << 12)
				| ((bs[++i] & 0x3F) << 6)
				|  (bs[++i] & 0x3F));
			}	
		}
		return new String(chars,0,p);
	}
	
	private char lineBuffer[];
	private boolean lastr = false;
	
	public final String readLine() throws IOException {
		char buf[] = lineBuffer;
		if (buf == null) {
			buf = lineBuffer = new char[128];
		}
		int room = buf.length;
		int offset = 0;
		int c = -1;

		loop:	while (true)  {
			while (bufferPosition < bufferLength) {
				switch (c = buffer[bufferPosition++]) {
					case -1:
					case '\n':
						if (lastr)  {
							//ignore
							lastr = false;
							break;
						} else {
							break loop;
						}
					case '\r':
						lastr = true;
						break loop;
					default:
						if (--room < 0) {
							buf = new char[offset + 128];
							room = buf.length - offset - 1;
							System.arraycopy(lineBuffer, 0, buf, 0, offset);
							lineBuffer = buf;
						}
						buf[offset++] = (char) c;
						break;
				}
			}
			//if i get here, then i need a refill
			refill();
			if (eof) throw new EOFException();
		}
		if ((c == -1) && (offset == 0)) {
			return null;
		}
		return String.copyValueOf(buf, 0, offset);
	}

	public byte[] lastline;
	public int lastlineoffset;
	
	private byte[] lastlinebuffer;
	private boolean lastrbyte = false;
	
	
	public final int readNextLine() throws IOException  {
		byte[] buf = lastlinebuffer;
		if (buf == null)  {
			buf = lastline = lastlinebuffer = new byte[100];
		}
		int offset = 0;
		
		//init
		lastline = lastlinebuffer;
		lastlineoffset = 0;
		
		while (true) {
			int eol = bufferPosition-1;
			//first see if there is a newline in the rest of this buffer
			loop: for (int i=bufferPosition; i<bufferLength; i++) {
				switch (buffer[i]) {
					case '\r':
						eol = i;
						lastrbyte = true;
						break loop;
					case '\n':
						if (lastrbyte && (i==bufferPosition))  {
							//ignore
							bufferPosition++;
							lastrbyte = false;
							break;
						} else {
							eol = i;
							lastrbyte = false;
							break loop;
						}
				}
			}
			if (eol >= bufferPosition)  {
				//eol found
				int len = eol - bufferPosition;
				if (offset==0)  {
					//still fits in buffer, give that one
					lastline = buffer;
					lastlineoffset = bufferPosition;
					bufferPosition = eol+1;
					return len;
				} else {
					//more than this buffer
					if (offset+len > buf.length)  {
						buf = new byte[offset+len];
						System.arraycopy(lastlinebuffer,0,buf,0,lastlinebuffer.length);
						lastlinebuffer = buf;
					}
					System.arraycopy(buffer,bufferPosition,buf,offset,len);
					lastline = buf;
					bufferPosition = eol+1;
					return offset+len;
				}
			} else {
				//no eol found in rest of buffer
				//copy this part in buffer
				int len = bufferLength - bufferPosition;
				if (offset+len > buf.length)  {
					buf = new byte[offset+len];
					System.arraycopy(lastline,0,buf,0,lastline.length);
					lastline = buf;
				}
				System.arraycopy(buffer,bufferPosition,lastline,offset,len);
				offset += len;
				bufferPosition = bufferLength;
				//need a refill
				refill();
				if (eof)  {
					//EOF
					//return what we have until now
					return offset+len;
				}
				//and continue check
			}
		}
	}
	

	protected final void refill() throws IOException {
		final int oldbufferlength = bufferLength;
		bufferLength = BUFFER_SIZE;

		int bytesread = in.read(buffer, 0, bufferLength);
		if (bytesread != bufferLength)  {
			//less bytes were read, or eof (empty)
			if (bytesread == -1)  {
				//EOF, empty buffer
				bufferLength = 0;
				eof = true;
			} else {
				//some bytes could be read
				bufferLength = bytesread;
			}
		}
		bufferStart += oldbufferlength;
		bufferPosition = 0;
	}

	public final long getFilePointer() throws IOException {
		return bufferStart + bufferPosition;
	}
	
	public final void close() throws IOException {
		in.close();
	}

}
