package mpeg2;

import java.io.IOException;

public record TableHeader(int tableId, boolean extendedSyntax, boolean privateIndicator, int reserved1, int sectionLength, int idExtension, int reserved2, int versionNumber, boolean currentNext, int sectionNumber, int lastSectionNumber)
		implements TsRecord {

	public static TableHeader from(final MpegTsPacketHeader header, final BigEndianBitStreamInput in) throws IOException {
		final int tableId = in.readBits(8);
		final boolean extendedSyntax = in.readBooleanBit();
		final boolean privateIndicator = in.readBooleanBit();
		final int reserved1 = in.readBits(2); // Poorly documented reserved bit
		final int sectionLength = in.readBits(12); // Byte count after this field, including CRC for extended
		final int idExtension;
		final int reserved2;
		final int versionNumber;
		final boolean currentNext;
		final int sectionNumber;
		final int lastSectionNumber;

		if (extendedSyntax) {
			idExtension = in.readBits(16);
			reserved2 = in.readBits(2); // Reserved
			versionNumber = in.readBits(5);
			currentNext = in.readBooleanBit();
			sectionNumber = in.readBits(8);
			lastSectionNumber = in.readBits(8);
		} else {
			idExtension = -1;
			reserved2 = -1;
			versionNumber = -1;
			currentNext = false;
			sectionNumber = -1;
			lastSectionNumber = -1;
		}

		return new TableHeader(tableId, extendedSyntax, privateIndicator, reserved1, sectionLength, idExtension, reserved2, versionNumber, currentNext, sectionNumber, lastSectionNumber);
	}

	@Override
	public int totalLength() {
		return extendedSyntax ? 8 : 3;
	}
	
	public int sectionPayloadLength() {
		return sectionLength - (extendedSyntax ? 5 : 0);
	}

	@Override
	public String toString() {
		if (extendedSyntax) {
			return "TableHeader [tableId=" + tableId + ", extendedSyntax=" + extendedSyntax + ", privateIndicator=" + privateIndicator + ", reserved1=" + reserved1 + ", sectionLength=" + sectionLength + ", idExtension=" + idExtension + ", reserved2="
					+ reserved2 + ", versionNumber=" + versionNumber + ", currentNext=" + currentNext + ", sectionNumber=" + sectionNumber + ", lastSectionNumber=" + lastSectionNumber + "]";
		} else {
			return "TableHeader [tableId=" + tableId + ", extendedSyntax=" + extendedSyntax + ", privateIndicator=" + privateIndicator + ", reserved1=" + reserved1 + ", sectionLength=" + sectionLength + "]";

		}
	}
	
	

}
