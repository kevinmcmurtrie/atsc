package mpeg2;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class PacketDemux {
	public static final int PAKCET_SIZE = 188;
	public static final int ProgramAssociationTable = 0;
	public static final int ConditionalAccessTable = 1;
	public static final int TransportStreamDescriptionTable = 2;
	public static final int NullPacket = 0x1fff;

	@FunctionalInterface
	interface PayloadPacketConsumer {
		int accept(MpegTsPacketHeader packetHeader, final BigEndianBitStreamInput in, int lengthAvailable) throws IOException;
	}
	
	interface PayloadConsumer extends PayloadPacketConsumer {
		int getPid();
	}

	final AtomicReferenceArray<PayloadPacketConsumer> programMap = new AtomicReferenceArray<>(0x2000);

	public void setProgramConsumer(final PayloadConsumer consumer) {
		if (!programMap.compareAndSet(Objects.requireNonNull(consumer, "consumer").getPid(), null, consumer)) {
			throw new IllegalArgumentException("Conflict: " + programMap.get(consumer.getPid()) + " / " + consumer);
		}
	}

	public void removeProgramConsumer(final PayloadConsumer consumer) {
		programMap.compareAndSet(consumer.getPid(), Objects.requireNonNull(consumer, "consumer"), null);
	}

	private int nullConsumer(final MpegTsPacketHeader packetHeader, final BigEndianBitStreamInput in, final int lengthAvailable) {
		int pid= packetHeader.pid();
		if ((pid == 0x30) || (pid == 0x40) || (pid == 0x50) || (pid == 0x60) || (pid == 0x70)) {
			System.out.println("Found it");//DEBUG
		}
		return 0;
	}

	public void processPayload(final MpegTsPacketHeader header, final BigEndianBitStreamInput in) throws IOException {
		final int payloadSize = PAKCET_SIZE - header.totalLength();
		if (payloadSize > 0) {
			final int consumed = Objects.requireNonNullElse(programMap.get(header.pid()), this::nullConsumer).accept(header, in, payloadSize);
			if (consumed > payloadSize) {
				throw new IOException("Payload overrun.  Consumed " + consumed + " of " + payloadSize + " bytes");
			}
			in.skipNBytes(payloadSize - consumed);
		} else if (payloadSize == 0) {
			System.out.println("No payload");
		} else {
			throw new IOException("Header overrun by " + (-payloadSize) + " bytes");
		}
	}
}
