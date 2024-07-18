package mpeg2;

import java.io.IOException;

public record AdaptationExtension(int length, byte reserved, LegalTimeWindow legalTimeWindow, PiecewiseRate piecewiseRate, SeamlessSplice seamlessSplice) implements TsRecord {

	public record LegalTimeWindow(boolean valid, int offset) {
		public static LegalTimeWindow from(final BigEndianBitStreamInput in) throws IOException {
			return new LegalTimeWindow(in.readBooleanBit(), in.readBits(15));
		}
	}

	public record PiecewiseRate(byte reserved, int rate) {
		public static PiecewiseRate from(final BigEndianBitStreamInput in) throws IOException {
			return new PiecewiseRate((byte) in.readBits(2), in.readBits(22));
		}
	}

	public record SeamlessSplice(byte type, long dtsNextAu, boolean valid) {
		public static SeamlessSplice from(final BigEndianBitStreamInput in) throws IOException {
			// https://ocw.unican.es/pluginfile.php/2825/course/section/2777/iso13818-1.pdf
			final byte type = (byte) in.readBits(4);
			long dtsNextAu = in.readBits(3);
			final boolean marker1 = in.readBooleanBit();
			dtsNextAu = (dtsNextAu << 15) | in.readBits(15);
			final boolean marker2 = in.readBooleanBit();
			dtsNextAu = (dtsNextAu << 15) | in.readBits(15);
			final boolean marker3 = in.readBooleanBit();
			return new SeamlessSplice(type, dtsNextAu, marker1 & marker2 & marker3);
		}
	}

	public static AdaptationExtension from(final BigEndianBitStreamInput in) throws IOException {
		final int length = in.readBits(8); // Self not part of length
		if (length == 0) {
			return null;
		}

		final boolean haveLtw = in.readBooleanBit();
		final boolean havePwr = in.readBooleanBit();
		final boolean haveSs = in.readBooleanBit();
		final byte reserved = (byte) in.readBits(5);

		final int dataLength = 1 + (haveLtw ? 2 : 0) + (havePwr ? 3 : 0) + (haveSs ? 5 : 0);
		if (dataLength > length) {
			throw new IOException("length=" + length + " ltw=" + haveLtw + " pwr=" + havePwr + " ss=" + haveSs + " but need length " + dataLength);
		}

		final AdaptationExtension ae = new AdaptationExtension(length, reserved, haveLtw ? LegalTimeWindow.from(in) : null, havePwr ? PiecewiseRate.from(in) : null, haveSs ? SeamlessSplice.from(in) : null);

		// Unknown data or stuffing may follow. Can't discern which so skip.
		in.skipNBytes(length - dataLength);

		return ae;
	}

	@Override
	public int totalLength() {
		return length + 1;
	}
}
