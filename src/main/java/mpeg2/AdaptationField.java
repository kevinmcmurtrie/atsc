package mpeg2;

import static mpeg2.TsRecord.optLength;

import java.io.IOException;

public record AdaptationField(int length, boolean discontinuity, boolean randomAccess, boolean priority, ProgramClockReference pcr, ProgramClockReference opcr, Byte spliceCountdown, PrivateData privateData, AdaptationExtension adaptationExt,
		int stuffingCount) implements TsRecord {

	// https://ocw.unican.es/pluginfile.php/2825/course/section/2777/iso13818-1.pdf
	// length == 0 means none

	public static AdaptationField from(final BigEndianBitStreamInput in) throws IOException {
		final int length = in.readBits(8); // Self not part of length
		if (length == 0) {
			//This happens.  Need to account for consumed length byte.
			return new AdaptationField(length, false, false, false, null, null, null, null, null, 0);
		}
		
		final boolean discontinuity = in.readBooleanBit();
		final boolean randomAccess = in.readBooleanBit();
		final boolean priority = in.readBooleanBit();
		final boolean havePcr = in.readBooleanBit();
		final boolean haveOpcr = in.readBooleanBit();
		final boolean haveSplicingPoint = in.readBooleanBit();
		final boolean havePrivateData = in.readBooleanBit();
		final boolean haveExtension = in.readBooleanBit();

		final ProgramClockReference pcr = havePcr ? ProgramClockReference.from(in) : null;
		final ProgramClockReference opcr = haveOpcr ? ProgramClockReference.from(in) : null;
		final Byte spliceCountdown = haveSplicingPoint ? Byte.valueOf(in.readByte()) : null;
		final PrivateData privateData = havePrivateData ? PrivateData.from(in) : null;
		final AdaptationExtension adaptationExt = haveExtension ? AdaptationExtension.from(in) : null;

		int stuffingLen = length - (1 + optLength(pcr) + optLength(opcr) + (haveSplicingPoint ? 1 : 0) + optLength(privateData) + optLength(adaptationExt));
		if (stuffingLen < 0) {
			throw new IOException("Expected " + length + " bytes but over-read by " + -stuffingLen);
		}
		while (stuffingLen > 0) {
			int b= in.readBits(8);
			stuffingLen-= 1;
			if (b != 0xff) {
				throw new IOException ("Invalid stuffing byte: " + Integer.toHexString(b));
			}
		}
		return new AdaptationField(length, discontinuity, randomAccess, priority, pcr, opcr, spliceCountdown, privateData, adaptationExt, stuffingLen);
	}

	@Override
	public int totalLength() {
		return 1 + length;
	}

	public enum AdaptationFieldControl {
		Reserved0(0, false, false),
		Payload(1, true, false),
		AdaptationField(2, false, true),
		Both(3, true, true);

		public final int code;
		public final boolean hasPayload;
		public final boolean hasAdaptationField;

		AdaptationFieldControl(final int code, boolean hasPayload, boolean hasAdaptationField) {
			this.code = code;
			this.hasPayload = hasPayload;
			this.hasAdaptationField = hasAdaptationField;
		}

		public static AdaptationFieldControl fromCode(final int code) {
			return switch (code) {
				case 0 -> Reserved0;
				case 1 -> Payload;
				case 2 -> AdaptationField;
				case 3 -> Both;
				default -> throw new IllegalArgumentException("Code " + code);
			};
		}
	}
}
