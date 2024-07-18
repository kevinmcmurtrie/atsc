package mpeg2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import mpeg2.TableDemux.TableConsumer;

/**
 * PID 0, table 0
 */
public class ProgramAssociationTable implements TableConsumer {

	@FunctionalInterface
	public interface AssociationTableListener {
		void newAssociationTable(AssociationTable table);
	}

	public record AssociationTable(int tid, int version, Map<Integer, Integer> programToPid) {
		@Override
		public String toString() {
			return "AssociationTable [tid=" + Integer.toHexString(tid) + ", version=" + Integer.toHexString(version) + ", programToPid="
					+ programToPid.entrySet().stream().collect(Collectors.toMap(e -> Integer.toHexString(e.getKey()), e -> Integer.toHexString(e.getValue()))) + "]";
		}
	}

	private final AtomicReference<AssociationTableListener[]> listeners = new AtomicReference<>(new AssociationTableListener[0]);

	// Last complete build
	private AssociationTable current = null;

	// Data building from the stream
	private int tid;
	private int version;
	private final Map<Integer, Integer> programToPid = new HashMap<>();
	private int payloadRemaining;

	public void addAssociationTableListener(final AssociationTableListener l) {
		AssociationTableListener[] orig, modified;
		do {
			orig = listeners.get();
			final ArrayList<AssociationTableListener> origList = new ArrayList<>(Arrays.asList(orig));
			if (origList.contains(l)) {
				return;
			}
			origList.add(l);
			modified = origList.toArray(new AssociationTableListener[origList.size()]);
		} while (!listeners.compareAndSet(orig, modified));
		if (current != null) {
			l.newAssociationTable(current);
		}
	}

	public void removeAssociationTableListener(final AssociationTableListener l) {
		AssociationTableListener[] orig, modified;
		do {
			orig = listeners.get();
			final ArrayList<AssociationTableListener> origList = new ArrayList<>(Arrays.asList(orig));
			if (!origList.contains(l)) {
				return;
			}
			origList.remove(l);
			modified = origList.toArray(new AssociationTableListener[origList.size()]);
		} while (!listeners.compareAndSet(orig, modified));
	}

	@Override
	public int accept(final MpegTsPacketHeader packetHeader, final TableHeader startingHeader, final boolean start, final BigEndianBitStreamInput in, final int lengthAvailable) throws IOException {
		if (start) {
			payloadRemaining = startingHeader.sectionPayloadLength() - 4; // 4 for CRC
			tid = startingHeader.idExtension();
			version = startingHeader.versionNumber();
			programToPid.clear();
		}

		final int payloadCount = Math.min(lengthAvailable, payloadRemaining) / 4;
		for (int i = 0; i < payloadCount; ++i) {
			final int program = in.readBits(16);
			in.skipBits(3); // reserved
			final int pid = in.readBits(13);
			programToPid.put(program, pid);
		}

		payloadRemaining -= payloadCount * 4;

		if (payloadRemaining == 0) {
			// CRC
			if ((lengthAvailable - payloadCount * 4) >= 4) {
				final int actualCrc = in.getAndStopCrc();
				final int expectedCrc = in.readBits(32);
				if (actualCrc == expectedCrc) {
					if (startingHeader.currentNext()) {
						final AssociationTable newTable = new AssociationTable(tid, version, Map.copyOf(programToPid));
						final boolean updated = (current == null) || (current.version != version);
						current = newTable;
						programToPid.clear();

						if (updated) {
							for (final AssociationTableListener l : listeners.get()) {
								l.newAssociationTable(current);
							}
						}

						System.out.println(current);
					}
				} else {
					System.out.println("CRC Error");
				}

				return payloadCount * 4 + 4;
			} else {
				System.out.println("Data overrun");
			}
		}

		return payloadCount * 4;
	}

	@Override
	public int getTid() {
		return 0;
	}

}
