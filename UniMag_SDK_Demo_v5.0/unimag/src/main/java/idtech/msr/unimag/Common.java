package idtech.msr.unimag;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.Locale;

public class Common {
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
		path = String.valueOf(fileDir.getParent()) + File.separator + fileDir.getName();
		return path;
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

	public static String getTimeInfoMs(long timeBase) {
		float time = (float) (Common.getCurrentTime() - timeBase) / 1000.0f;
		String strtime = String.format(Locale.US, "%03f", Float.valueOf(time));
		return strtime;
	}

	public static long getCurrentTime() {
		return System.currentTimeMillis();
	}

	public static String getDoubleValue(double dbValue) {
		String strtime = String.format(Locale.US, "%08f", dbValue);
		return strtime;
	}

	public static String getHexStringFromBytes(byte[] data) {
		return com.idtechproducts.acom.Common.base16Encode(data);
	}

	public static String getLRC(String strData) {
		String strLRC = null;
		byte[] toCharArray = Common.getByteArray(strData);
		byte[] lrc = new byte[]{toCharArray[0]};
		int i = 1;
		while (i < toCharArray.length) {
			byte[] arrby = lrc;
			arrby[0] = (byte) (arrby[0] ^ toCharArray[i]);
			++i;
		}
		strLRC = Common.getHexStringFromBytes(lrc);
		return strLRC;
	}

	private static byte[] getByteArray(String hexString) {
		if (hexString == null) {
			throw new IllegalArgumentException("this hexString must not be empty");
		}
		hexString = hexString.toLowerCase();
		byte[] byteArray = new byte[hexString.length()];
		int i = 0;
		while (i < byteArray.length / 2) {
			byte dataH = (byte) (Character.digit(hexString.charAt(2 * i), 16) & 15);
			byte dataL = (byte) (Character.digit(hexString.charAt(2 * i + 1), 16) & 15);
			byteArray[i] = (byte) (dataH << 4 & 240 | dataL & 15);
			++i;
		}
		return byteArray;
	}
}