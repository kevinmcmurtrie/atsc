package mpeg2;

import java.io.EOFException;
import java.io.IOException;

import mpeg2.AdaptationField.AdaptationFieldControl;

public record MpegTsPacketHeader(boolean transportError, boolean payloadStart, boolean priority, int pid, TransportScramblingControl tsc,
		AdaptationFieldControl afc, int continuityCounter, AdaptationField adaptationField) implements TsRecord {

	public static int SYNC = 0x47;

	
	@Override
	public String toString() {
		return "MpegTsPacketHeader [length= " + totalLength() + " transportError=" + transportError + ", payloadStart=" + payloadStart + ", priority=" + priority + ", pid=" + Integer.toHexString(pid) + ", tsc=" + tsc + ", afc=" + afc + ", continuityCounter=" + continuityCounter + ", adaptationField="
				+ adaptationField + "]";
	}


	public static MpegTsPacketHeader from(final BigEndianBitStreamInput in) throws IOException {
		final int b = in.readByte();
		if (b != SYNC) {
			throw new IOException("Not sync byte: " + Integer.toHexString(b));
		}

		return readAfterSync(in);
	}

	public static MpegTsPacketHeader seekFrom(final BigEndianBitStreamInput in) throws IOException {
		MpegTsPacketHeader header;
		do {
			while (in.readByte() != SYNC) {
				// seek
			}
			try {
				header = readAfterSync(in);
			} catch (EOFException exit) {
				throw exit;
			} catch (IOException err) {
				header = null;
			}
		} while (header == null);
		return header;
	}
	
	@Override
	public int totalLength() {
		return 4 + TsRecord.optLength(adaptationField);
	}

	private static MpegTsPacketHeader readAfterSync(final BigEndianBitStreamInput in) throws IOException {
		boolean transportError= in.readBooleanBit();
		boolean payloadStart =in.readBooleanBit();
		boolean priority= in.readBooleanBit();
		int pid= in.readBits(13);
		TransportScramblingControl tsc= TransportScramblingControl.fromCode(in.readBits(2));
		AdaptationFieldControl afc= AdaptationFieldControl.fromCode(in.readBits(2));
		int continuityCounter= in.readBits(4);
		AdaptationField adaptationField = afc.hasAdaptationField ? AdaptationField.from(in) : null;
		
		return new MpegTsPacketHeader(transportError, payloadStart, priority, pid, tsc,
				afc, continuityCounter, adaptationField);
	}
	
	
}
