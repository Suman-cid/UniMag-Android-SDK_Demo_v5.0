package com.idtechproducts.unimagsdk.task;

import com.idtechproducts.acom.ACLog;
import com.idtechproducts.acom.AcomManager;
import com.idtechproducts.acom.Common;
import com.idtechproducts.acom.io.IOManager;
import com.idtechproducts.acom.tasks.Task;
import java.util.List;

import idtech.msr.unimag.uniMagReader;
import idtech.msr.unimag.uniMagReaderMsg;

public class SwipeCancelTask
		extends Task {
	private final uniMagReaderMsg _umrMsg;
	private uniMagReader.TaskExport taskExport;

	public SwipeCancelTask(uniMagReader.TaskExport umTaskExport) {
		super(umTaskExport.getAcomManager());
		this.taskExport = umTaskExport;
		this._umrMsg = umTaskExport.getuniMagReaderMsg();
	}

	public Task.TaskType getType() {
		return Task.TaskType.Command;
	}

	protected Runnable taskMain() {
		ACLog.i(this.TAG, "To cancel swipe");
		IOManager.RPDResult r = this.recordPlayDecode(new byte[]{5}, 3.0);
		if (r.isCanceledOrFailed()) {
			return null;
		}
		final byte[] returnedData = r.isParsed() ? r.data.get(0) : new byte[1];
		return new Runnable(){

			public void run() {
				if (returnedData[0] == 2) {
					SwipeCancelTask.this._umrMsg.onReceiveMsgTimeout("Card swipe cancelled successfully. (0x" + Common.base16Encode(returnedData) + ")");
				} else {
					SwipeCancelTask.this._umrMsg.onReceiveMsgTimeout("Failed to cancel card swipe (0x" + Common.base16Encode(returnedData) + ")");
				}
			}
		};
	}

}