package com.idtechproducts.acom;

import android.content.Context;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.net.ConnectivityManager;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Locale;

import idtech.msr.xmlmanager.ConfigParameters;
import idtech.msr.xmlmanager.ReaderType;

public class Common {
	public static final int kAUDIO_CHANNEL = 12;
	public static final int kAUDIO_ENCODING = 3;
	private static final short[] crc_tabccitt = new short[0xFFFF];

	public static ReaderType ConnectedReader = ReaderType.UNKNOWN;

	static {
		int P_CCITT = 4129;
		for (int i = 0; i < 256; i++) {
			int crc = 0;
			int c = 0xFFFF & 0xFF00 & i << 8;
			for (int j = 0; j < 8; j++) {
				if (((crc ^ c) & 0x8000) != 0) {
					crc = 0xFFFFFFFE & crc << 1 ^ 0x1021;
				} else {
					crc = 0xFFFF & 0xFFFFFFFE & crc << 1;
				}
				c = (short) (0xFFFF & 0xFFFFFFFE & c << 1);
			}
			crc_tabccitt[i] = ((short) crc);
		}
	}

	public static boolean isStorageExist() {
		boolean sdCardExist = Environment.getExternalStorageState().equals("mounted");
		return sdCardExist;
	}

	public static String getSDRootFilePath() {
		String path = null;

		boolean sdCardExist = Environment.getExternalStorageState().equals("mounted");
		if (sdCardExist) {
			path = Environment.getExternalStorageDirectory().toString();
		}
		return path;
	}

	public static String getApplicationPath(Context ctx) {
		String path = null;
		File fileDir = ctx.getFilesDir();
		path = fileDir.getParent() + File.separator + fileDir.getName();
		return path;
	}

	public static boolean copyFile(String sourceFileName, String destFileName) {
		File f1 = new File(sourceFileName);
		File f2 = new File(destFileName);
		int length = 2097152;

		FileInputStream in = null;
		FileOutputStream out = null;
		try {
			in = new FileInputStream(f1);
			out = new FileOutputStream(f2);
			FileChannel inC = in.getChannel();
			FileChannel outC = out.getChannel();
			ByteBuffer b = null;
			for (; ; ) {
				if (inC.position() == inC.size()) {
					inC.close();
					outC.close();
					return true;
				}

				if (inC.size() - inC.position() < length) {
					length = (int) (inC.size() - inC.position());
				} else {
					length = 2097152;
				}

				b = ByteBuffer.allocateDirect(length);
				inC.read(b);
				b.flip();
				outC.write(b);
				outC.force(false);
			}
		} catch (FileNotFoundException localFileNotFoundException) {
		} catch (IOException localIOException4) {
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException localIOException7) {
			}
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException localIOException8) {
			}
		}

		return true;
	}

	public static File getDir_externalOrSandbox(Context context) {
		if (isStorageExist()) {
			return Environment.getExternalStorageDirectory();
		}
		return context.getFilesDir();
	}

	public static boolean isFileExist(String path) {
		if (path == null) {
			return false;
		}
		File file = new File(path);
		if (!file.exists()) {
			file = null;
			return false;
		}
		file = null;
		return true;
	}

	public static String base16Encode(byte[] data) {
		char[] ENC = {
				'0', '1', '2', '3', '4', '5', '6', '7',
				'8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
		char[] ret = new char[data.length * 2];
		for (int i = 0; i < data.length; i++) {
			ret[(i * 2)] = ENC[(data[i] >> 4 & 0xF)];
			ret[(i * 2 + 1)] = ENC[(data[i] & 0xF)];
		}
		return String.copyValueOf(ret);
	}

	public static byte[] base16Decode(String encodedString) {
		if (encodedString.length() % 2 != 0) {
			return null;
		}
		char[] str = encodedString.toCharArray();
		byte[] ret = new byte[encodedString.length() / 2];
		for (int i = 0; i < ret.length; i++) {
			char c = str[(i * 2)];
			byte n;
			if (('0' <= c) && (c <= '9')) {
				n = (byte) (c - '0');
			} else {
				if (('A' <= c) && (c <= 'F')) {
					n = (byte) (c - 'A' + 10);
				} else {
					if (('a' <= c) && (c <= 'f')) {
						n = (byte) (c - 'a' + 10);
					} else {
						return null;
					}
				}
			}

			byte b = (byte) (n << 4);

			c = str[(i * 2 + 1)];
			if (('0' <= c) && (c <= '9')) {
				n = (byte) (c - '0');
			} else if (('A' <= c) && (c <= 'F')) {
				n = (byte) (c - 'A' + 10);
			} else if (('a' <= c) && (c <= 'f')) {
				n = (byte) (c - 'a' + 10);
			} else {
				return null;
			}
			b = (byte) (b + n);

			ret[i] = b;
		}
		return ret;
	}

	public static boolean checkFrequencySupported(int inputFreq, int outputFreq) {
		if (-2 == AudioRecord.getMinBufferSize(
				inputFreq,
				2,
				2)) {
			return false;
		}
		if (-2 == AudioTrack.getMinBufferSize(
				outputFreq,
				12,
				3)) {
			return false;
		}
		return true;
	}

	public static boolean checkInputFrequencySupported(int inputFreq) {
		if (-2 == AudioRecord.getMinBufferSize(
				inputFreq,
				2,
				2)) {
			return false;
		}
		if (-2 == AudioTrack.getMinBufferSize(
				inputFreq,
				12,
				3)) {
			return false;
		}
		return true;
	}

	public static short crc_Update(int crc, byte c) {
		int short_c = 0xFF & c;
		int tmp = 0xFFFF & (0xFF & crc >> 8 ^ short_c);
		crc = 0xFFFF & (0xFF00 & crc << 8 ^ crc_tabccitt[tmp]);
		return (short) crc;
	}

	public static byte[] makeSetBaudCommand(ConfigParameters config) {
		int baud = config.getBaudRate();
		byte levelByte = config.getShuttleChannel();
		byte baudByte;

		if (ConnectedReader == ReaderType.UNIJACK) {
			switch (baud) {
				case 2400:
					baudByte = 3;
					break;
				case 4800:
					baudByte = 4;
					break;
				case 9600:
				default:
					baudByte = 5;
			}
			switch (levelByte) {
				case 0:
					levelByte = 64;
					break;
				case 32:
					levelByte = 48;
					break;
				case 48:
					levelByte = 16;
					break;
				case 8:
					levelByte = 0;
					break;
				default:
					levelByte = 16;

					break;
			}
		} else {
			switch (baud) {
				case 2400:
					baudByte = 3;
					break;
				case 4800:
					baudByte = 4;
					break;
				case 9600:
				default:
					baudByte = 5;
			}
			switch (levelByte) {
				case 0:
				case 8:
				case 32:
				case 48:
					break;
				default:
					levelByte = 48;
			}
		}
		byte channel = (byte) (baudByte + levelByte);
		byte[] ret = {2, 83, 65, 1, channel, 3};
		byte lrc = 0;
		byte[] arrayOfByte1;
		int j = (arrayOfByte1 = ret).length;
		for (int i = 0; i < j; i++) {
			byte b = arrayOfByte1[i];
			lrc = (byte) (lrc ^ b);
		}
		ret[(ret.length - 1)] = lrc;

		return ret;
	}

	public static String getByteArrDesc(byte[] d) {
		if (d == null) {
			return "(null)";
		}
		int dl = d.length;
		return String.format(Locale.US, "%d bytes: [%s%s%s%s]", new Object[]{Integer.valueOf(dl),
				dl >= 1 ? String.format(Locale.US, "%02X", new Object[]{Byte.valueOf(d[0])}) : "",
				dl >= 2 ? String.format(Locale.US, "%02X", new Object[]{Byte.valueOf(d[1])}) : "",
				dl >= 4 ? "..." : "",
				dl >= 3 ? String.format(Locale.US, "%02X", new Object[]{Byte.valueOf(d[(dl - 1)])}) : ""});
	}

	public static boolean containsStr(byte[] buff, String str) {
		char[] ca = str.toCharArray();
		byte[] bseq = new byte[ca.length];
		for (int i = 0; i < ca.length; i++) {
			bseq[i] = ((byte) (ca[i] & 0xFF));
		}
		for (int i = 0; i < buff.length - bseq.length + 1; i++) {
			int o = 0;
			while (buff[(i + o)] == bseq[o]) {
				o++;
				if (o >= bseq.length) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean bytesMatch(byte[] ba, String pattern)
			throws IllegalArgumentException {
		try {
			String[] exps = pattern.split(",");
			String[] arrayOfString1;
			int j = (arrayOfString1 = exps).length;
			for (int i = 0; i < j; i++) {
				String exp = arrayOfString1[i];
				IllegalArgumentException invalidExp = new IllegalArgumentException("invalid exp <" + exp + ">");
				String[] LR = exp.split("=");
				if (LR.length != 2) {
					throw invalidExp;
				}
				if (LR[0].equals("len")) {
					int val = Integer.parseInt(LR[1], 10);
					if (ba.length != val) {
						return false;
					}
				} else if ((LR[0].startsWith("[")) && (LR[0].endsWith("]")) && (LR[1].startsWith("x"))) {
					int idx = Integer.parseInt(LR[0].substring(1, LR[0].length() - 1), 10);
					if (idx < 0) {
						idx = ba.length + idx;
					}
					byte val = (byte) (0xFF & Integer.parseInt(LR[1].substring(1), 16));
					if (ba[idx] != val) {
						return false;
					}
				} else {
					throw invalidExp;
				}
			}
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("invalid pattern string <" + pattern + ">", e);
		}
		return true;
	}

	public static byte[] synthesizeStereoSquareWavePeriod(int squareWaveFrequency, int playbackFrequency, int outputWaveDirection) {
		if ((squareWaveFrequency <= 0) || (playbackFrequency <= 0)) {
			throw new IllegalArgumentException();
		}
		int frameCount_total = (int) Math.round(playbackFrequency / squareWaveFrequency);

		int frameCount_half = frameCount_total / 2;
		int frameCount_pos = frameCount_half - 1 + (frameCount_total & 0x1);
		int frameCount_neg = frameCount_half - 1;

		byte H = -1;
		byte M = Byte.MIN_VALUE;
		byte L = 1;
		int stereoFrameSize = 2;
		byte[] midFrame = {Byte.MIN_VALUE, Byte.MIN_VALUE};
		byte[] posFrame = {-1, 1};
		byte[] negFrame = {1, -1};
		if (outputWaveDirection != 0) {
			byte[] t = posFrame;
			posFrame = negFrame;
			negFrame = t;
		}
		byte[] ret = new byte[frameCount_total * 2];
		int retI = 0;

		System.arraycopy(midFrame, 0, ret, retI, 2);
		retI += 2;
		for (int i = 0; i < frameCount_pos; i++) {
			System.arraycopy(posFrame, 0, ret, retI, 2);
			retI += 2;
		}
		System.arraycopy(midFrame, 0, ret, retI, 2);
		retI += 2;
		for (int i = 0; i < frameCount_neg; i++) {
			System.arraycopy(negFrame, 0, ret, retI, 2);
			retI += 2;
		}
		return ret;
	}

	public static boolean isOnline(Context ctx) {
		try {
			ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
			return cm.getActiveNetworkInfo().isConnectedOrConnecting();
		} catch (Exception e) {
		}
		return false;
	}
}
