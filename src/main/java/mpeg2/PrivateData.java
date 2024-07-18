package mpeg2;

import java.io.IOException;

/**
 * Not really useful
 */
public record PrivateData(int length, byte[] rawData) implements TsRecord {
	public static PrivateData from(final BigEndianBitStreamInput in) throws IOException {
		final int length = in.readBits(8);
		if (length < 5) {
			throw new IOException("Invalid length: " + length);
		}
		final byte[] rawData = new byte[length];
		in.readNBytes(rawData);

		return new PrivateData(length, rawData);
	}

	@Override
	public int totalLength() {
		return length;
	}
}
