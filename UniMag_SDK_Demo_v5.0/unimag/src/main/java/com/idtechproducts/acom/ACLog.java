package com.idtechproducts.acom;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ACLog {
	private static final String UMSDK_LOGCAT_TAG = "UMSDK";
	private static final SimpleDateFormat _dateFormat;
	private static final String[] _levTable;
	private static boolean _isVerbose = false;
	private static PrintWriter _writer;
	private static File _log_dir;
	private static String _log_prefix;
	private static File _log_fullPath;

	static {
		_dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US);
		_levTable = new String[10];
		_levTable[4] = " I";
		_levTable[5] = " W";
		_levTable[6] = " E";
	}

	public static synchronized void setEnableVerbose(boolean enable) {
		_isVerbose = enable;
	}

	public static synchronized void open(File dir, String prefix) {

		try {
			String time = new SimpleDateFormat("MM.dd_HH-mm-ss", Locale.US).format(new Date());
			File f = new File(dir, prefix + time + ".txt");

			_writer = new PrintWriter(f);
			_log_dir = dir;
			_log_prefix = prefix;
			_log_fullPath = f;
		} catch (FileNotFoundException e) {
			Log.w("UMSDK", "UMLog: log file not opened: " + e);
		}
	}

	public static synchronized void close() {
		if (_writer != null) {
			_writer.close();
			_writer = null;
			_log_dir = null;
			_log_prefix = null;
			_log_fullPath = null;
		}
	}

	public static synchronized File getPath() {
		return _log_fullPath;
	}

	public static synchronized int deleteLogs(File dir, String prefix) {
		File[] list = dir.listFiles();
		if (list == null) {
			Log.w("UMSDK", "UMLog: delete: invalid dir: " + dir);
			return 0;
		}
		boolean shouldRecreateLog = false;
		int deleteCount = 0;
		File[] arrayOfFile1;
		int j = (arrayOfFile1 = list).length;
		for (int i = 0; i < j; i++) {
			File f = arrayOfFile1[i];
			if (f.getName().startsWith(prefix)) {
				f.delete();
				deleteCount++;
				if (f.equals(_log_fullPath)) {
					Log.w("UMSDK", "UMLog: deleted currently opened log file. Recreating");
					shouldRecreateLog = true;
				}
			}
		}
		if (shouldRecreateLog) {
			open(_log_dir, _log_prefix);
		}
		return deleteCount;
	}

	public static synchronized void i(String tag, String msg) {
		if (_isVerbose) {
			log(tag, msg, null, 4);
		}
	}

	public static synchronized void w(String tag, String msg) {
		log(tag, msg, null, 5);
	}

	public static synchronized void e(String tag, String msg) {
		log(tag, msg, null, 6);
	}

	public static synchronized void w(String tag, String msg, Throwable tr) {
		log(tag, msg, tr, 5);
	}

	public static synchronized void e(String tag, String msg, Throwable tr) {
		log(tag, msg, tr, 6);
	}

	private static void log(String tag, String msg, Throwable tr, int level) {
		if (tr == null) {
			Log.println(level, "UMSDK", tag + ": " + msg);
		} else {
			Log.println(level, "UMSDK", tag + ": " + msg + "\n" +
					Log.getStackTraceString(tr));
		}
		if (_writer != null) {
			_writer.append(_dateFormat.format(new Date()));
			_writer.append(_levTable[level]);
			_writer.append(" |");
			_writer.append(tag);
			_writer.append("| ");
			_writer.append(msg);
			_writer.append("\n");
			if (tr != null) {
				tr.printStackTrace(_writer);
			}
			_writer.flush();
		}
	}
}
