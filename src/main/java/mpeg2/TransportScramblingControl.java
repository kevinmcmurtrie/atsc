package mpeg2;

public enum TransportScramblingControl {
	No(0), Reserved1(1), Even(2), Odd(3);

	public int code;

	TransportScramblingControl(final int code) {
		this.code = code;
	}

	public static TransportScramblingControl fromCode(final int code) {
		return switch (code) {
			case 0 -> No;
			case 1 -> Reserved1;
			case 2 -> Even;
			case 3 -> Odd;
			default -> throw new IllegalArgumentException("Code " + code);
		};
	}
}
