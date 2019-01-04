package com.idtechproducts.unimagsdk.task;

import com.idtechproducts.acom.ACLog;
import com.idtechproducts.acom.Common;
import com.idtechproducts.acom.io.IOManager;
import com.idtechproducts.acom.tasks.Task;
import com.idtechproducts.unimagsdk.PUSType;

import java.util.Calendar;
import java.util.List;

import idtech.msr.unimag.uniMagReader;
import idtech.msr.unimag.uniMagReaderMsg;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class SwipeTask
		extends Task {
	private final uniMagReaderMsg _umrMsg;
	private final uniMagReader.TaskExport _umTaskExport;
	private final double _timeoutIntervalSec;
	private byte[] _resp;

	public SwipeTask(uniMagReader.TaskExport umTaskExport, double timeoutIntervalSec) {
		super(umTaskExport.getAcomManager());
		this._umTaskExport = umTaskExport;
		this._umrMsg = umTaskExport.getuniMagReaderMsg();
		this._timeoutIntervalSec = timeoutIntervalSec;
	}

	@Override
	public Task.TaskType getType() {
		return Task.TaskType.Swipe;
	}

	@Override
	protected void taskSetup() {
	}

	@Override
	protected void taskCleanup() {
	}

	@Override
	protected Runnable taskMain() {
		IOManager.RPDResult r = this.recordPlayDecode(null, this._timeoutIntervalSec);
		if (r.isCanceledOrFailed()) {
			return null;
		}
		if (r.isTimedOut()) {
			return new Runnable() {

				public void run() {
					SwipeTask.this._umrMsg.onReceiveMsgTimeout("Timeout error. Please swipe card again.");
				}
			};
		}
		return new Runnable() {

			public void run() {
				byte[] dataWithoutFirstByte = new byte[SwipeTask.this._resp.length - 1];
				System.arraycopy(SwipeTask.this._resp, 1, dataWithoutFirstByte, 0, dataWithoutFirstByte.length);
				if (dataWithoutFirstByte.length > 5 && dataWithoutFirstByte[0] == 37 && dataWithoutFirstByte[1] == 69) {
					SwipeTask.this._umTaskExport.incrementSwipeErrorCounter();
					if (SwipeTask.this._umTaskExport.getSwipeErrorCounter() >= 3) {
						byte lastCalibrationYear = dataWithoutFirstByte[2];
						byte lastCalibrationWeek = dataWithoutFirstByte[3];
						byte currentYear = (byte) (Calendar.getInstance().get(1) % 100);
						byte currentWeek = (byte) Calendar.getInstance().get(3);
						if (lastCalibrationYear < currentYear || lastCalibrationYear == currentYear && currentWeek - lastCalibrationWeek > 2) {
							SwipeTask.this._umTaskExport.initializeSwipeErrorCounter();
							SwipeTask.this._umrMsg.onReceiveMsgTimeout("");
							SwipeTask.this._umrMsg.onReceiveMsgToCalibrateReader();
							SwipeTask.this._umTaskExport.startCalibrateReader();
							return;
						}
					}
				}
				SwipeTask.this._umrMsg.onReceiveMsgCardData(SwipeTask.this._resp[0], dataWithoutFirstByte);
			}
		};
	}

	@Override
	public boolean processResponse(List<byte[]> response) {
		boolean stopParsing = this.isResponseValid(response);
		return stopParsing;
	}

	private boolean isResponseValid(List<byte[]> response) {
		for (byte[] respI : response) {
			ACLog.i(this.TAG, "got: " + Common.getByteArrDesc(respI));
			if (respI.length < 2) continue;
			if (PUSType.parse(respI) != PUSType.INVALID) {
				ACLog.i(this.TAG, "ignored PUS");
				continue;
			}
			if ((respI[0] & 4) == 0) {
				if (13 == respI[respI.length - 1]) {
					this._resp = respI;
					return true;
				}
			} else if (2 == respI[1] && 3 == respI[respI.length - 1]) {
				this._resp = respI;
				return true;
			}
			ACLog.i(this.TAG, "ignored ill-formed swipe data");
		}
		return false;
	}

	@Override
	public void processSound() {
		ACLog.i(this.TAG, "swipe detected");
		this.post(new Runnable() {

			public void run() {
				SwipeTask.this._umrMsg.onReceiveMsgProcessingCardData();
			}
		});
	}

}