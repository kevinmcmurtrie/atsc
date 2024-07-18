package mpeg2;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceArray;

import mpeg2.PacketDemux.PayloadConsumer;

public class TableDemux implements PayloadConsumer {
	@FunctionalInterface
	interface TablePacketConsumer {
		// CRC recording is on for extended TableHeader
		int accept(MpegTsPacketHeader packetHeader, TableHeader startingHeader, boolean start, final BigEndianBitStreamInput in, int lengthAvailable) throws IOException;
	}
	
	interface TableConsumer extends TablePacketConsumer {
		int getTid();
	}

	final int pid;
	final AtomicReferenceArray<TablePacketConsumer> tableMap = new AtomicReferenceArray<>(256);
	private TableHeader currentTableHeader;
	private int crcInProgress;
	private int lengthRemaining; // Remaining payload length

	public TableDemux(final int pid) {
		this.pid = pid;
	}

	public void setTableConsumer(TableConsumer consumer) {
		if (!tableMap.compareAndSet(Objects.requireNonNull(consumer, "consumer").getTid(), null, consumer)) {
			throw new IllegalArgumentException("Conflict on TID " + Integer.toHexString(consumer.getTid()));
		}
	}

	public void removeTableConsumer(TableConsumer consumer) {
		tableMap.compareAndSet(pid, Objects.requireNonNull(consumer, "consumer"), null);
	}

	private int nullConsumer(final MpegTsPacketHeader packetHeader, final TableHeader startingHeader, final boolean start, final BigEndianBitStreamInput in, final int lengthAvailable) {
		if (start) {
			System.out.println("No handler: pid=" + Integer.toHexString(pid) + " " + startingHeader);
		}
		return 0;
	}

	void dispatchSection(final MpegTsPacketHeader header, final BigEndianBitStreamInput in, int lengthAvailable, final boolean start) throws IOException {
		if (start) {
			in.startCrc();
			currentTableHeader = TableHeader.from(header, in);
			if (!currentTableHeader.extendedSyntax()) {
				in.stopCrc();
			}
			lengthAvailable -= currentTableHeader.totalLength();
			lengthRemaining = currentTableHeader.sectionPayloadLength();
		} else {
			if (currentTableHeader == null) {
				System.out.println("Broken table continuation. pid=" + Integer.toHexString(pid));
				in.skipNBytes(lengthAvailable);
				return;
			}
			if (currentTableHeader.extendedSyntax()) {
				in.resumeCrc(crcInProgress);
			}
		}

		if (lengthAvailable < 0) {
			throw new IOException("Table header overrun by " + -lengthAvailable + " bytes");
		}
		final int consumed = Objects.requireNonNullElse(tableMap.get(currentTableHeader.tableId()), this::nullConsumer).accept(header, currentTableHeader, start, in, lengthAvailable);
		if (consumed > lengthAvailable) {
			throw new IOException("Payload overrun.  Consumed " + consumed + " of " + lengthAvailable + " bytes");
		}

		in.skipNBytes(lengthAvailable - consumed);

		lengthRemaining -= lengthAvailable; // May go negative from padding
		if (lengthRemaining > 0) {
			if (currentTableHeader.extendedSyntax()) {
				crcInProgress = in.getAndStopCrc();
			}
		} else {
			if (currentTableHeader.extendedSyntax()) {
				in.stopCrc();
			}
			// Data loss recovery - Close current table
			currentTableHeader = null;
			crcInProgress = 0;
			lengthRemaining = 0;
		}
	}

	@Override
	public int accept(final MpegTsPacketHeader packetHeader, final BigEndianBitStreamInput in, final int lengthAvailable) throws IOException {
		if (packetHeader.pid() != pid) {
			throw new IllegalArgumentException("Mapped header " + packetHeader + " to TableDemux for pid=" + pid);
		}
		final boolean payloadStart = packetHeader.payloadStart(); // New payload may follow and old payload
		final int offset = payloadStart ? in.readBits(8) : 0;
		if (offset > 0) {
			dispatchSection(packetHeader, in, offset, false);
		}
		dispatchSection(packetHeader, in, lengthAvailable - (offset + (payloadStart ? 1 : 0)), payloadStart);
		return lengthAvailable;
	}

	@Override
	public int getPid() {
		return pid;
	}
}
