/*
 * Decompiled with CFR 0_132.
 *
 * Could not load the following classes:
 *  android.os.Build
 *  android.os.Build$VERSION
 */
package com.idtechproducts.unimagsdk.task;

import android.os.Build;
import android.util.Log;

import com.idtechproducts.acom.ACLog;
import com.idtechproducts.acom.Common;
import com.idtechproducts.acom.io.IOManager;
import com.idtechproducts.acom.io.ToneType;
import com.idtechproducts.acom.tasks.Task;
import com.idtechproducts.unimagsdk.PUSType;

import idtech.msr.unimag.uniMagReader;
import idtech.msr.unimag.uniMagReaderMsg;
import idtech.msr.xmlmanager.ReaderType;


public class ConnectTask
		extends Task {
	private final uniMagReaderMsg _umrMsg;
	private final uniMagReader.TaskExport _umTaskExport;
	private final boolean _connectWithCmd;

	public ConnectTask(uniMagReader.TaskExport umTaskExport, boolean connectWithCmd) {
		super(umTaskExport.getAcomManager());
		this._umrMsg = umTaskExport.getuniMagReaderMsg();
		this._umTaskExport = umTaskExport;
		this._connectWithCmd = connectWithCmd;
	}

	public Task.TaskType getType() {
		return Task.TaskType.Connect;
	}

	protected void taskSetup() {
	}

	protected void taskCleanup() {
	}

	protected Runnable taskMain() {
		PUSType pus = null;

		int repN = this._umTaskExport.getReaderType() != ReaderType.UNIJACK ? 5 : 7;

		int reps = 1;
		while (reps <= repN) {
			boolean run_setChan_nPlayTone;

			IOManager.RPDResult r;

			String version;
			if ((reps == 3 || reps == 5) && this._config.getVersionToTestOtherDirection() != null && (version = this._config.getVersionToTestOtherDirection()).compareToIgnoreCase(Build.VERSION.RELEASE) <= 0) {
				ACLog.i(this.TAG, "Connect Tast: changing output wave direction");
				this._config.setDirectionOutputWave((short) (this._config.getDirectionOutputWave() != 1 ? 1 : 0));
				this._ioManager.setConfig(this._config);
			}
			if (run_setChan_nPlayTone = !this._connectWithCmd ? reps == 3 || reps == 4 : true) {
				ACLog.i(this.TAG, "set channel");
				boolean maximizeVolumeAfterPlay = true;
				if (this._umTaskExport.getReaderType() == ReaderType.UNIJACK) {
					maximizeVolumeAfterPlay = false;
					if (this._config.getVolumeLevelAdjust() == 0) {
						this._ioManager.setDeviceMediaVolumeToMaxMinusArg(3);
					} else {
						this._ioManager.setDeviceMediaVolumeToMaxMinusArg(this._config.getVolumeLevelAdjust());
					}
				}
				if ((r = this.recordPlayDecode(Common.makeSetBaudCommand(this._config), 1.5, false, maximizeVolumeAfterPlay)).isCanceledOrFailed()) {
					return null;
				}
				if (r.isParsed() && (pus = PUSType.parse(r.data)) != PUSType.INVALID) {
					break;
				}
			} else {
				ACLog.i(this.TAG, "tone");

				Log.i(TAG, "setPlayingTone()");
				this._tonePlayer.setPlayingTone(ToneType.T_2400Hz);

				Log.i(TAG, "setDeviceMediaVolumeToMax()");
				this._ioManager.setDeviceMediaVolumeToMax();

				Log.i(TAG, "recordPlayDecode()");
				r = this.recordPlayDecode(null, 2.6);

				this._tonePlayer.setPlayingTone(null);

				if (r.isCanceledOrFailed()) {
					return null;
				}

				if (r.isParsed() && (pus = PUSType.parse(r.data)) != PUSType.INVALID) break;
			}
			if (this.safeWait(0.4)) {
				return null;
			}
			++reps;
		}

		if (this.isCanceled()) {
			return null;
		}

		if (pus == null || PUSType.INVALID == pus) {
			ACLog.i(this.TAG, "timed out");
			return new Runnable() {

				public void run() {
					ConnectTask.this._umrMsg.onReceiveMsgTimeout("Timeout error. Can't detect UniMag reader, please check the device connection.");
				}
			};
		}
		ACLog.i(this.TAG, "connected " + pus.name);
		if (this._config.getVolumeLevelAdjust() > 1) {
			this._ioManager.setDeviceMediaVolumeToMaxMinusArg(1);
		}
		PUSType fpus = pus;
		return new Runnable() {

			public void run() {
				ConnectTask.this._umTaskExport.initializeSwipeErrorCounter();
				if (ConnectTask.this._umTaskExport.getReaderType() == ReaderType.UNIJACK) {
					ConnectTask.this._umTaskExport.cxn_setConnected(null);
				} else {
					ConnectTask.this._umTaskExport.cxn_setConnected(ToneType.T_2000Hz);
				}
			}
		};
	}

}