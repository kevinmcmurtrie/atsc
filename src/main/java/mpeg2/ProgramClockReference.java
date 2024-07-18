package mpeg2;

import java.io.IOException;

public record ProgramClockReference(long clock90kHz, int reserved, int clock27Mhz) implements TsRecord {
	public static ProgramClockReference from(final BigEndianBitStreamInput in) throws IOException {
		return new ProgramClockReference(in.readLongBits(33), in.readBits(6), in.readBits(9));
	}

	@Override
	public int totalLength() {
		return 6;
	}
}
