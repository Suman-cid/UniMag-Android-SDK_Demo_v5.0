package com.idtechproducts.unimagsdk.task;

import com.idtechproducts.acom.Common;
import com.idtechproducts.acom.io.IOManager;
import com.idtechproducts.acom.tasks.Task;

import idtech.msr.unimag.uniMagReader;
import idtech.msr.unimag.uniMagReaderMsg;

public class ClearBufferTask
		extends Task {

	private final uniMagReaderMsg _umrMsg;

	public ClearBufferTask(uniMagReader.TaskExport umTaskExport) {
		super(umTaskExport.getAcomManager());

		this._umrMsg = umTaskExport.getuniMagReaderMsg();
	}

	public Task.TaskType getType() {
		return Task.TaskType.Command;
	}

	protected Runnable taskMain() {
		IOManager.RPDResult r = this.recordPlayDecode(Common.base16Decode("0253D1313055555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555556603B1"), 2.5);
		if (r.isCanceledOrFailed()) {
			return null;
		}
		if (r.isParsed() && r.matches("len=1,[0]=x06")) {
			r = this.recordPlayDecode(Common.base16Decode("0253D1100F55555555555555555555555555556603FA"), 2.5);
		}
		final byte[] cmdReturn = r.isParsed() ? r.data.get(0) : new byte[1];

		return new Runnable() {

			public void run() {
				ClearBufferTask.this._umrMsg.onReceiveMsgCommandResult(16, cmdReturn);
			}
		};
	}
}
