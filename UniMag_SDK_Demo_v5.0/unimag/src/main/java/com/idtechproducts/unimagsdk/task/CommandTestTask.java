/*
 * Decompiled with CFR 0_132.
 */
package com.idtechproducts.unimagsdk.task;

import com.idtechproducts.acom.AcomManager;
import com.idtechproducts.acom.Common;
import com.idtechproducts.acom.io.IOManager;
import com.idtechproducts.acom.tasks.Task;
import java.util.List;

import idtech.msr.unimag.uniMagReader;
import idtech.msr.unimag.uniMagReaderMsg;

public class CommandTestTask
		extends Task {
	private final uniMagReaderMsg _umrMsg;

	public CommandTestTask(uniMagReader.TaskExport umTaskExport) {
		super(umTaskExport.getAcomManager());
		this._umrMsg = umTaskExport.getuniMagReaderMsg();
	}

	public Task.TaskType getType() {
		return Task.TaskType.Command;
	}

	protected Runnable taskMain() {
		this.recordPlayDecode(Common.makeSetBaudCommand(this._config), 2.0);
		byte[] dataToReturn = new byte[1];
		IOManager.RPDResult r = this.recordPlayDecode(Common.base16Decode("0252220371"), 2.0);
		if (r.isCanceledOrFailed()) {
			return null;
		}
		byte[] getVerData = r.data.get(0);
		if (CommandTestTask.isExpectedResp(r)) {
			r = this.recordPlayDecode(Common.base16Decode("02521F034C"), 2.0);
			if (r.isCanceledOrFailed()) {
				return null;
			}
			if (CommandTestTask.isExpectedResp(r)) {
				dataToReturn = getVerData;
			}
		}
		final byte[] dataReturned_f = dataToReturn;
		return new Runnable(){

			public void run() {
				CommandTestTask.this._umrMsg.onReceiveMsgCommandResult(102, dataReturned_f);
			}
		};
	}

	private static boolean isExpectedResp(IOManager.RPDResult r) {
		if (!r.isParsed()) {
			return false;
		}
		for (byte[] respI : r.data) {
			if (respI.length < 4 || !Common.bytesMatch(respI, "[0]=x06,[1]=x02,[-2]=x03")) continue;
			return true;
		}
		return false;
	}

}