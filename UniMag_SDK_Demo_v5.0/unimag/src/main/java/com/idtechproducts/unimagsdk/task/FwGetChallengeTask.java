package com.idtechproducts.unimagsdk.task;

import com.idtechproducts.acom.AcomManager;
import com.idtechproducts.acom.Common;
import com.idtechproducts.acom.io.IOManager;
import com.idtechproducts.acom.tasks.Task;

import idtech.msr.unimag.unimagtools.uniMagReaderToolsMsg;

public class FwGetChallengeTask
		extends Task {
	private final uniMagReaderToolsMsg _umtMsg;

	public FwGetChallengeTask(AcomManager umMan, uniMagReaderToolsMsg umtMsg) {
		super(umMan);
		this._umtMsg = umtMsg;
	}

	public Task.TaskType getType() {
		return Task.TaskType.Command;
	}

	protected Runnable taskMain() {
		IOManager.RPDResult r = this.recordPlayDecode(Common.makeSetBaudCommand(this._config), 1.5);
		if (r.isCanceledOrFailed()) {
			return null;
		}
		if (r.isParsed() && r.matches("len=15,[0]=x06,[1]=x56")) {
			return this.getBlmResponseRunnable(r.lastMatch);
		}
		int i = 0;
		while (i < 3) {
			r = this.recordPlayDecode(Common.base16Decode("02528003D3"), 3.5);
			if (r.isCanceledOrFailed()) {
				return null;
			}
			if (!r.isTimedOut()) {
				if (r.matches("len=15,[0]=x06,[1]=x56")) {
					return this.getBlmResponseRunnable(r.lastMatch);
				}
				if (r.matches("[0]=x06,[1]=x02,[-2]=x03") || r.matches("len=1,[0]=x15")) {
					final byte[] dataReturned = r.lastMatch;
					return new Runnable() {

						public void run() {
							FwGetChallengeTask.this._umtMsg.onReceiveMsgChallengeResult(201, dataReturned);
						}
					};
				}
			}
			++i;
		}
		return new Runnable() {

			public void run() {
				FwGetChallengeTask.this._umtMsg.onReceiveMsgChallengeResult(204, null);
			}
		};
	}

	private Runnable getBlmResponseRunnable(final byte[] data) {
		return new Runnable() {

			public void run() {
				FwGetChallengeTask.this._umtMsg.onReceiveMsgChallengeResult(205, data);
			}
		};
	}

}