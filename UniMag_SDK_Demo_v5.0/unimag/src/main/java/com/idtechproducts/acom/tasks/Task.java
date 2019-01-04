package com.idtechproducts.acom.tasks;

import android.os.Handler;
import android.os.Looper;

import com.idtechproducts.acom.ACLog;
import com.idtechproducts.acom.AcomManager;
import com.idtechproducts.acom.Common;
import com.idtechproducts.acom.io.IOManager;
import com.idtechproducts.acom.io.TonePlayer;
import com.idtechproducts.acom.io.ToneType;

import java.util.Arrays;
import java.util.List;

import idtech.msr.xmlmanager.ConfigParameters;

public abstract class Task
		implements Runnable,
		IOManager.RPDClient {

	protected final AcomManager _uniMagManager;
	protected final IOManager _ioManager;
	protected final TonePlayer _tonePlayer;
	protected final ConfigParameters _config;
	protected final String TAG;
	private final Object _sleepMonitor = new Object();
	private volatile boolean _isCanceled = false;
	private ToneType _originalTone;

	protected Task(AcomManager umMan) {
		this._uniMagManager = umMan;
		this._ioManager = umMan.getIntern_IOManager();
		this._tonePlayer = this._ioManager.getTonePlayer();
		this._config = umMan.getIntern_ConfigParameters();
		this.TAG = String.valueOf(this.getType().name()) + "Task";
	}

	/*
	 * WARNING - Removed try catching itself - possible behaviour change.
	 */
	public void cancel() {
		this._isCanceled = true;
		Object object = this._sleepMonitor;
		synchronized (object) {
			this._sleepMonitor.notify();
		}
	}

	@Override
	public boolean isCanceled() {
		return this._isCanceled;
	}

	/*
	 * WARNING - Removed try catching itself - possible behaviour change.
	 */
	protected boolean safeWait(double waitLen) {
		long waitLenMillis;
		long time_start;
		waitLenMillis = (long) (waitLen * 1000.0);
		time_start = System.currentTimeMillis();
		try {
			Object object = this._sleepMonitor;
			synchronized (object) {
				this._sleepMonitor.wait(waitLenMillis);
			}
		} catch (InterruptedException interruptedException) {
			// empty catch block
		}
		long time_end = System.currentTimeMillis();
		boolean wasInterrupted = time_end - time_start < waitLenMillis;
		return wasInterrupted;
	}

	protected IOManager.RPDResult recordPlayDecode(byte[] cmdString, double waitLen) {
		try {
			return this.recordPlayDecode(cmdString, waitLen, false, false);
		} catch (Throwable throwable) {
			throwable.printStackTrace();
		}
		return null;
	}

	protected IOManager.RPDResult recordPlayDecode(byte[] cmdString, double waitLen, boolean recordAfterPlay, boolean maximizeVolumeAfterPlay) {
		long tStart = System.currentTimeMillis();
		IOManager.RPDResult r = null;
		try {
			r = this._ioManager.recordPlayDecode(this, cmdString, waitLen, recordAfterPlay, maximizeVolumeAfterPlay);
		} catch (Throwable throwable) {
			throwable.printStackTrace();
		}
		long tEnd = System.currentTimeMillis();
		String respDesc = "";
		if (r.data != null) {
			respDesc = "Read " + Common.getByteArrDesc(r.data.get(0));
			if (r.data.size() > 1 && !Arrays.equals(r.data.get(0), r.data.get(1))) {
				respDesc = String.valueOf(respDesc) + " " + Common.getByteArrDesc(r.data.get(0));
			}
		}
		ACLog.i("RPD", String.format("%s after %.3fs. %s", r.status.name(), (double) (tEnd - tStart) / 1000.0, respDesc));
		return r;
	}

	@Override
	public boolean processResponse(List<byte[]> response) {
		return true;
	}

	@Override
	public void processSound() {
	}

	protected void tone_saveAndStop() {
		this._originalTone = this._tonePlayer.getPlayingTone();
		this._tonePlayer.setPlayingTone(null);
	}

	protected void tone_restore() {
		this._tonePlayer.setPlayingTone(this._originalTone);
	}

	protected void post(Runnable r) {
		Handler h = new Handler(Looper.getMainLooper());
		h.post(r);
	}

	@Override
	public void run() {
		ACLog.i(this.TAG, "started");

		this.taskSetup();

		Runnable actionAfterTaskEnd = this.taskMain();

		this.taskCleanup();

		this._uniMagManager.task_signalStoppedStatus();

		ACLog.i(this.TAG, "stopped");
		if (actionAfterTaskEnd != null) {
			Handler h = new Handler(Looper.getMainLooper());
			h.post(actionAfterTaskEnd);
		}
	}

	public abstract TaskType getType();

	protected void taskSetup() {
		this.tone_saveAndStop();
	}

	protected void taskCleanup() {
		this.tone_restore();
	}

	protected abstract Runnable taskMain();

	/*
	 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
	 */
	public static enum TaskType {
		Connect,
		Swipe,
		Command,
		FwUpdate,
		AutoConfig;


		private TaskType() {
		}
	}
}