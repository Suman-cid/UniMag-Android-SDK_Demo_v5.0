
package com.idtechproducts.unimagsdk.task;

import com.idtechproducts.acom.io.IOManager;
import com.idtechproducts.acom.io.TonePlayer;
import com.idtechproducts.acom.io.ToneType;
import com.idtechproducts.acom.tasks.Task;
import com.idtechproducts.unimagsdk.PUSType;
import java.util.List;

import idtech.msr.unimag.uniMagReader;
import idtech.msr.unimag.uniMagReaderMsg;
import idtech.msr.xmlmanager.ReaderType;

public class GetReaderTypeTask
		extends Task {
	private final uniMagReaderMsg _umrMsg;
	private final uniMagReader.TaskExport _umTaskExport;

	public GetReaderTypeTask(uniMagReader.TaskExport umTaskExport) {
		super(umTaskExport.getAcomManager());
		this._umrMsg = umTaskExport.getuniMagReaderMsg();
		this._umTaskExport = umTaskExport;
	}

	public Task.TaskType getType() {
		return Task.TaskType.Command;
	}

	protected Runnable taskMain() {
		if (this.safeWait(2.0)) {
			return null;
		}
		PUSType pus = null;
		int maxTries = 3;
		int trial = 1;
		while (trial <= 3) {
			this._tonePlayer.setPlayingTone(ToneType.T_2400Hz);
			IOManager.RPDResult r = this.recordPlayDecode(null, 2.6);
			this._tonePlayer.setPlayingTone(null);
			if (r.isCanceledOrFailed()) {
				return null;
			}
			if (r.isParsed()) {
				pus = PUSType.parse(r.data);
				if (pus.readerType != null) break;
			}
			if (trial == 3) {
				return new Runnable(){

					public void run() {
						GetReaderTypeTask.this._umrMsg.onReceiveMsgCommandResult(17, new byte[1]);
					}
				};
			}
			if (this.safeWait(0.5)) {
				return null;
			}
			++trial;
		}
		final PUSType fpus = pus;
		return new Runnable(){

			public void run() {
				if (GetReaderTypeTask.this._umTaskExport.getReaderType() == ReaderType.UNIJACK) {
					byte[] uniJackType = new byte[]{5};
					GetReaderTypeTask.this._umrMsg.onReceiveMsgCommandResult(17, uniJackType);
				} else {
					GetReaderTypeTask.this._umTaskExport.readerType_set(fpus.readerType);
					GetReaderTypeTask.this._umrMsg.onReceiveMsgCommandResult(17, new byte[]{(byte)fpus.stateListVal});
				}
			}
		};
	}

}