/*
 * Decompiled with CFR 0_132.
 */
package com.idtechproducts.unimagsdk.task;

import com.idtechproducts.acom.AcomManager;
import com.idtechproducts.acom.io.ToneType;
import com.idtechproducts.acom.tasks.Task;

import idtech.msr.unimag.unimagtools.uniMagReaderToolsMsg;

public class FwSendPowerTask
		extends Task {
	private final uniMagReaderToolsMsg _umtMsg;

	public FwSendPowerTask(AcomManager umMan, uniMagReaderToolsMsg umtMsg) {
		super(umMan);
		this._umtMsg = umtMsg;
	}

	public Task.TaskType getType() {
		return Task.TaskType.FwUpdate;
	}

	protected Runnable taskMain() {
		this._tonePlayer.setPlayingTone(ToneType.T_2000Hz);
		if (this.safeWait(90.0)) {
			return null;
		}
		return new Runnable() {

			public void run() {
				FwSendPowerTask.this._umtMsg.onReceiveMsgUpdateFirmwareResult(402);
			}
		};
	}
}