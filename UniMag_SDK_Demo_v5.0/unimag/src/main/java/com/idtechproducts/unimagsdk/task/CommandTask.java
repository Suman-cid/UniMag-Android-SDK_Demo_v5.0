/*
 * Decompiled with CFR 0_132.
 */
package com.idtechproducts.unimagsdk.task;

import com.idtechproducts.acom.io.IOManager;
import com.idtechproducts.acom.tasks.Task;

import idtech.msr.unimag.Common;
import idtech.msr.unimag.uniMagReader;
import idtech.msr.unimag.uniMagReaderMsg;

public class CommandTask
		extends Task {
	private static int calibrationTrial = 0;
	private final uniMagReaderMsg _umrMsg;
	private final byte[] _cmd;
	private final int _cmdID;
	private uniMagReader.TaskExport taskExport;

	public CommandTask(uniMagReader.TaskExport umTaskExport, byte[] command, int commandID) {
		super(umTaskExport.getAcomManager());
		this._umrMsg = umTaskExport.getuniMagReaderMsg();
		this._cmd = command;
		this._cmdID = commandID;
		this.taskExport = umTaskExport;
	}

	static /* synthetic */ void access$3(int n) {
		calibrationTrial = n;
	}

	public Task.TaskType getType() {
		return Task.TaskType.Command;
	}

	protected Runnable taskMain() {
		int maxTries = 103 != this._cmdID ? 3 : 1;
		double timeoutLen = 103 != this._cmdID ? 4 : 10;
		IOManager.RPDResult r = null;
		int trial = 1;
		while (trial <= maxTries) {
			r = this.recordPlayDecode(this._cmd, timeoutLen);
			if (r.isCanceledOrFailed()) {
				return null;
			}
			if (r.isParsed()) break;
			++trial;
		}
		final byte[] returnedData = r.isParsed() ? r.data.get(0) : new byte[1];
		return new Runnable() {

			public void run() {
				if (CommandTask.this._cmdID == 104) {
					if (returnedData.length <= 1) {
						CommandTask.this._umrMsg.onReceiveMsgCommandResult(CommandTask.this._cmdID, new byte[1]);
					} else {
						byte voltageValueHigh = 0;
						byte voltageValueLow = 0;
						int i = 0;
						while (i < returnedData.length - 3) {
							if (returnedData[i] == 111 && returnedData[i + 1] == 2) {
								voltageValueHigh = returnedData[i + 2];
								voltageValueLow = returnedData[i + 3];
								break;
							}
							++i;
						}
						if (voltageValueHigh == 0 && voltageValueLow == 0) {
							CommandTask.this._umrMsg.onReceiveMsgCommandResult(CommandTask.this._cmdID, new byte[1]);
							return;
						}
						CommandTask.this._umrMsg.onReceiveMsgCommandResult(CommandTask.this._cmdID, new byte[]{6, voltageValueHigh, voltageValueLow});
					}
				} else if (103 == CommandTask.this._cmdID) {
					if (calibrationTrial == 0 && returnedData.length >= 2 && Common.getHexStringFromBytes(returnedData).equalsIgnoreCase("06EE")) {
						CommandTask.access$3(calibrationTrial + 1);
						CommandTask.this.taskExport.startCalibrateReader();
						return;
					}
					CommandTask.this._umrMsg.onReceiveMsgCommandResult(CommandTask.this._cmdID, returnedData);
				} else {
					CommandTask.this._umrMsg.onReceiveMsgCommandResult(CommandTask.this._cmdID, returnedData);
				}
			}
		};
	}

}