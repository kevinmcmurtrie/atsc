package mpeg2;

public interface TsRecord {
	int totalLength(); // Bytes

	public static int optLength(final TsRecord ts) {
		return (ts != null) ? ts.totalLength() : 0;
	}
}
