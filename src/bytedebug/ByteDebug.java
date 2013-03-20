package bytedebug;

import java.util.Arrays;

public class ByteDebug {

	private static byte[] toByteArray(int seqNum) {
		byte[] data = new byte[2];
		data[0] = (byte) ((seqNum & 0xFF00) >> 8);
		data[1] = (byte) (seqNum & 0xFF);
		return data;
	}

	private static int fromByteArray(byte[] data) {
		return ((data[0] & 0x000000FF) << 8) | (data[1] & 0x000000FF);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		byte[] data = null;
		int decoded;
		int max = 0x0000FFFF;
		for (int i = 0; i < max; ++i) {
			data = toByteArray(i);
			decoded = fromByteArray(data);
			if (decoded != i) {
				System.out.println("Encode(" + i + "): " + Arrays.toString(data));
				System.out.println("Decoded: " + decoded);
				System.out.println();
			}
		}
		System.out.println("Done!");
	}
}
