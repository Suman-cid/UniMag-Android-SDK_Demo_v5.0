package com.idtechproducts.unimagsdk.task;

import com.idtechproducts.acom.ACLog;
import com.idtechproducts.acom.AcomManager;
import com.idtechproducts.acom.io.IOManager;
import com.idtechproducts.acom.tasks.Task;
import com.idtechproducts.unimagsdk.task.SwipeTask;
import java.util.List;

import idtech.msr.unimag.uniMagReader;
import idtech.msr.unimag.uniMagReaderMsg;

public class SwipeAckTask
		extends Task {
	private final uniMagReaderMsg _umrMsg;
	private uniMagReader.TaskExport taskExport;

	public SwipeAckTask(uniMagReader.TaskExport umTaskExport) {
		super(umTaskExport.getAcomManager());
		this.taskExport = umTaskExport;
		this._umrMsg = umTaskExport.getuniMagReaderMsg();
	}

	public Task.TaskType getType() {
		return Task.TaskType.Command;
	}

	protected Runnable taskMain() {
		ACLog.i(this.TAG, "To Receive ACK for swipe");
		IOManager.RPDResult r = this.recordPlayDecode(new byte[1], 3.0);
		if (r.isCanceledOrFailed()) {
			return null;
		}
		if (r.isTimedOut()) {
			this._umrMsg.onReceiveMsgTimeout("Timeout error. Please try again after 10 seconds.");
		}
		final byte[] returnedData = r.isParsed() ? r.data.get(0) : new byte[1];
		return new Runnable(){

			public void run() {
				if (returnedData[0] == 6) {
					SwipeAckTask.this._umrMsg.onReceiveMsgToSwipeCard();
					SwipeAckTask.this.taskExport.getAcomManager().task_setAndStart(new SwipeTask(SwipeAckTask.this.taskExport, 10.0));
				} else {
					SwipeAckTask.this._umrMsg.onReceiveMsgTimeout("Timeout error. Please try again after 10 seconds.");
				}
			}
		};
	}

}