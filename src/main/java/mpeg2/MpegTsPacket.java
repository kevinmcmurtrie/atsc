package mpeg2;

import java.io.IOException;

public record MpegTsPacket (MpegTsPacketHeader header, byte[] payload) implements TsRecord {
	public static final int PAKCET_SIZE= 188;	

	@Override
	public int totalLength() {
		return PAKCET_SIZE;
	}
	
	public static MpegTsPacket from(final BigEndianBitStreamInput in) throws IOException {
		return readPayload(MpegTsPacketHeader.from(in), in);
	}
	
	public static MpegTsPacket seekFrom(final BigEndianBitStreamInput in) throws IOException {
		return readPayload(MpegTsPacketHeader.seekFrom(in), in);
	}
	
	private static MpegTsPacket readPayload(MpegTsPacketHeader header, final BigEndianBitStreamInput in) throws IOException {
		int payloadSize= PAKCET_SIZE-header.totalLength();
		if (payloadSize > 0) {
			byte[] payload= new byte[payloadSize];
			in.readNBytes(payload);
			return new MpegTsPacket(header, payload);
		} else if (payloadSize == 0) {
			return new MpegTsPacket(header, null);
		} else {
			throw new IOException("Header overrun by " + (-payloadSize) + " bytes");
		}

	}
}
