package mpeg2;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class BigEndianBitStreamInput implements Closeable {
	private final InputStream in;
	private int length = 0;
	private byte buffer = 0;
	private int crc;
	private boolean crcRecording= false;

	public BigEndianBitStreamInput(final InputStream in) {
		this.in = Objects.requireNonNull(in, "in");
	}
	
	public void startCrc() throws IOException {
		resumeCrc(0xffffffff);
	}
	
	public void resumeCrc(int startingPoint) throws IOException {
		crc= startingPoint;
		if (length != 0) {
			throw new IOException("Not on byte alignment.  Remainder: " + length);
		}
		crcRecording= true;
	}
	
	private void updateCrc (int b) {
		crc ^= b << 24;
		crc = ((crc & 0x80000000) != 0) ? (crc << 1) ^ 0x4c11db7 : crc << 1;
		crc = ((crc & 0x80000000) != 0) ? (crc << 1) ^ 0x4c11db7 : crc << 1;
		crc = ((crc & 0x80000000) != 0) ? (crc << 1) ^ 0x4c11db7 : crc << 1;
		crc = ((crc & 0x80000000) != 0) ? (crc << 1) ^ 0x4c11db7 : crc << 1;
		crc = ((crc & 0x80000000) != 0) ? (crc << 1) ^ 0x4c11db7 : crc << 1;
		crc = ((crc & 0x80000000) != 0) ? (crc << 1) ^ 0x4c11db7 : crc << 1;
		crc = ((crc & 0x80000000) != 0) ? (crc << 1) ^ 0x4c11db7 : crc << 1;
		crc = ((crc & 0x80000000) != 0) ? (crc << 1) ^ 0x4c11db7 : crc << 1;
	}
	
	public void stopCrc () throws IOException {
		crcRecording= false;
	}
	
	public int getAndStopCrc () throws IOException {
		crcRecording= false;
		if (length != 0) {
			throw new IOException("Not on byte alignment.  Remainder: " + length);
		}
		return crc;
	}

	/**
	 * Using this for post-fetch is prohibited because it complicates byte alignment.
	 * No public method may exit with length==8.
	 * */
	private void loadIfEmpty() throws IOException {
		if (length > 0) {
			return;
		}

		final int b = in.read();
		if (b < 0) {
			return;
		}
		if (crcRecording) {
			updateCrc(b);
		}
		buffer = (byte) b;
		length = 8;
	}

	public int readBits(int bitCount) throws IOException {
		loadIfEmpty();
		
		//The MPEG structure makes this a common case
		if ((bitCount == 8) && (length == 8)) {
			length= 0;
			return (buffer & 0xff);
		}
		
		if ((bitCount > 32) || (bitCount <= 0)) {
			throw new IllegalArgumentException(bitCount + " bits into int");
		}
		
		//Shift in full byte or some bits
		int c = Math.min(length, bitCount);
		if (c == 0) {
			throw new EOFException(bitCount + " bits short");
		}
		int acc= ((buffer & 0xff) >>> (8 - c));
		buffer <<= c;
		length -= c;
		bitCount -= c;
		
		//Done or buffer is empty.  Shift in full bytes
		while (bitCount > 8) {
			loadIfEmpty();
			if (length == 0) {
				throw new EOFException(bitCount + " bits short");
			}
			acc = (acc << 8) | (buffer & 0xff);
			length= 0;
			bitCount -= 8;
		}

		//Shift in remainder
		while (bitCount > 0) {
			loadIfEmpty();
			c = Math.min(length, bitCount);
			if (c == 0) {
				throw new EOFException(bitCount + " bits short");
			}
			acc = (acc << c) | ((buffer & 0xff) >>> (8 - c));
			buffer <<= c;
			length -= c;
			bitCount -= c;
		}
		return acc;
	}

	public long readLongBits(int bitCount) throws IOException {
		if ((bitCount > 64) || (bitCount <= 0)) {
			throw new IllegalArgumentException(bitCount + " bits into long");
		}
		loadIfEmpty();
		
		if ((bitCount == 8) && (length == 8)) {
			length= 0;
			return buffer & 0xff;
		}
		
		//Shift in full byte or some bits
		int c = Math.min(length, bitCount);
		if (c == 0) {
			throw new EOFException(bitCount + " bits short");
		}
		long acc= ((buffer & 0xff) >>> (8 - c));
		buffer <<= c;
		length -= c;
		bitCount -= c;
		
		//Done or buffer is empty.  Shift in full bytes
		while (bitCount > 8) {
			loadIfEmpty();
			if (length == 0) {
				throw new EOFException(bitCount + " bits short");
			}
			acc = (acc << 8) | (buffer & 0xff);
			length= 0;
			bitCount -= 8;
		}

		//Shift in remainder
		while (bitCount > 0) {
			loadIfEmpty();
			c = Math.min(length, bitCount);
			if (c == 0) {
				throw new EOFException(bitCount + " bits short");
			}
			acc = (acc << c) | ((buffer & 0xff) >>> (8 - c));
			buffer <<= c;
			length -= c;
			bitCount -= c;
		}
		return acc;
	}

	public void readNBytes(final byte[] b) throws IOException {
		readNBytes(b, 0, b.length);
	}

	public void readNBytes(final byte[] b, final int off, final int len) throws IOException {
		if (length != 0) {
			throw new IOException("Not on byte alignment.  Remainder: " + length);
		}
		
		if (crcRecording) {
			for (int i= 0; i < len; ++i) {
				b[off+i]= readByte();
			}
		} else {
			final int c = in.readNBytes(b, off, len);
			if (c < len) {
				throw new EOFException((len - c) + " bytes short");
			}
		}
	}

	public void skipNBytes(final int n) throws IOException {
		if (n == 0) {
			return;
		}

		if (length != 0) {
			throw new IOException("Not on byte alignment.  Remainder: " + length);
		}
		
		if (crcRecording) {
			for (int i= 0; i < n; ++i) {
				readByte();
			}
		} else {
			in.skipNBytes(n);
		}
	}

	public void skipBits(int bitCount) throws IOException {
		if (bitCount == 0) {
			return;
		}

		int c = Math.min(length, bitCount);
		buffer <<= c;
		length -= c;
		bitCount -= c;

		skipNBytes(bitCount / 8);
		bitCount = bitCount % 8;

		if (bitCount > 0) {
			loadIfEmpty();
			c = Math.min(length, bitCount);
			buffer <<= c;
			length -= c;
			bitCount -= c;
		}

		if (bitCount > 0) {
			throw new EOFException(bitCount + " bits short");
		}
	}

	public boolean readBooleanBit() throws IOException {
		return readBits(1) != 0;
	}

	public byte readByte() throws IOException {
		return (byte) readBits(8);
	}

	public int readUnsignedByte() throws IOException {
		return readBits(8);
	}

	public short readShort() throws IOException {
		return (short) readBits(16);
	}

	public int readUnsignedShort() throws IOException {
		return readBits(16);
	}

	public int readInt() throws IOException {
		return readBits(32);
	}

	public long readLong() throws IOException {
		return readLongBits(64);
	}

	@Override
	public void close() throws IOException {
		in.close();
		length = 0;
	}
}
