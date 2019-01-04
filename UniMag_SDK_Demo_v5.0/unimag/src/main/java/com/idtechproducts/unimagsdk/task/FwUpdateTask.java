package com.idtechproducts.unimagsdk.task;

import android.os.Handler;
import android.os.Looper;

import com.idtechproducts.acom.ACLog;
import com.idtechproducts.acom.AcomManager;
import com.idtechproducts.acom.Common;
import com.idtechproducts.acom.io.IOManager;
import com.idtechproducts.acom.io.ToneType;
import com.idtechproducts.acom.tasks.Task;
import com.idtechproducts.unimagsdk.SdkCustomization;

import java.util.Locale;

import idtech.msr.unimag.unimagtools.uniMagReaderToolsMsg;

public class FwUpdateTask
		extends Task {
	private final uniMagReaderToolsMsg _umtMsg;
	private final boolean _encryptedUpdate;
	private final byte[] _bin;
	private final byte[] _challengeResponse;

	public FwUpdateTask(AcomManager umMan, uniMagReaderToolsMsg umtMsg, boolean encryptedUpdate, byte[] bin, byte[] challengeResponse) {
		super(umMan);
		this._umtMsg = umtMsg;
		this._encryptedUpdate = encryptedUpdate;
		this._bin = bin;
		this._challengeResponse = challengeResponse;
	}

	private static int arrayAppend(byte[] dst, int dstPos, byte[] src) {
		System.arraycopy(src, 0, dst, dstPos, src.length);
		return dstPos + src.length;
	}

	private static byte[] makeChallengeResponseCmd(boolean encryptedUpdate, byte[] cr) {
		byte[] cmdStart = Common.base16Decode(encryptedUpdate ? "02537019FF" : "02537009FF");
		byte[] fullCmd = new byte[cmdStart.length + cr.length + 1 + 1];
		int offset = 0;
		offset = FwUpdateTask.arrayAppend(fullCmd, offset, cmdStart);
		offset = FwUpdateTask.arrayAppend(fullCmd, offset, cr);
		offset = FwUpdateTask.arrayAppend(fullCmd, offset, new byte[]{3});
		byte lrc = 0;
		byte[] arrby = fullCmd;
		int n = arrby.length;
		int n2 = 0;
		while (n2 < n) {
			byte b = arrby[n2];
			lrc = (byte) (lrc ^ b);
			++n2;
		}
		fullCmd[fullCmd.length - 1] = lrc;
		return fullCmd;
	}

	public Task.TaskType getType() {
		return Task.TaskType.FwUpdate;
	}

	protected Runnable taskMain() {
		IOManager.RPDResult r;
		Runnable retTimeout = new Runnable() {

			public void run() {
				ACLog.i(FwUpdateTask.this.TAG, "notify timeout");
				FwUpdateTask.this._umtMsg.onReceiveMsgUpdateFirmwareResult(305);
			}
		};
		Handler mainThreadHandler = new Handler(Looper.getMainLooper());
		Boolean isInBLM = null;
		int chkBlmTries = 3;
		int trial = 1;
		while (trial <= 3) {
			r = this.recordPlayDecode(Common.makeSetBaudCommand(this._config), 2.0);
			if (r.isCanceledOrFailed()) {
				return null;
			}
			if (r.isParsed()) {
				if (r.matches("len=15,[0]=x06,[1]=x56")) {
					isInBLM = true;
					break;
				}
				if (r.matches("len=1,[0]=x06")) {
					isInBLM = false;
					break;
				}
			}
			if (trial == 3) {
				return retTimeout;
			}
			++trial;
		}
		if (!isInBLM.booleanValue()) {
			boolean problemWithChallengeResponse;
			ACLog.i(this.TAG, "not in BLM");
			boolean bl = problemWithChallengeResponse = this._challengeResponse == null || !this._encryptedUpdate && this._challengeResponse.length != 8 || this._encryptedUpdate && this._challengeResponse.length != 24;
			if (problemWithChallengeResponse) {
				return new Runnable() {

					public void run() {
						ACLog.i(FwUpdateTask.this.TAG, "notify need CR");
						FwUpdateTask.this._umtMsg.onReceiveMsgUpdateFirmwareResult(!FwUpdateTask.this._encryptedUpdate ? 308 : 309);
					}
				};
			}
			byte[] challengeResponseCmd = FwUpdateTask.makeChallengeResponseCmd(this._encryptedUpdate, this._challengeResponse);
			r = this.recordPlayDecode(challengeResponseCmd, 3.0);
			if (r.isCanceledOrFailed()) {
				return null;
			}
			if (r.isTimedOut()) {
				return retTimeout;
			}
			if (!r.matches("len=1,[0]=x06") && !r.matches("len=15,[0]=x06,[1]=x56")) {
				return new Runnable() {

					public void run() {
						ACLog.i(FwUpdateTask.this.TAG, "notify failed to enter BLM");
						FwUpdateTask.this._umtMsg.onReceiveMsgUpdateFirmwareResult(302);
					}
				};
			}
		}
		ACLog.i(this.TAG, "now in BLM");
		int BLOCK_SIZE = this._encryptedUpdate ? 258 : 256;
		int BLOCK_COUNT = this._bin.length / BLOCK_SIZE;
		short crcTotal = -1;
		int curBlock = 0;
		while (curBlock < BLOCK_COUNT) {
			final int progress = curBlock;
			mainThreadHandler.post(new Runnable() {

				public void run() {
					FwUpdateTask.this._umtMsg.onReceiveMsgUpdateFirmwareProgress(progress);
				}
			});
			boolean appendCRC = !this._encryptedUpdate;
			byte[] blockCmd = new byte[3 + BLOCK_SIZE + (appendCRC ? 2 : 0)];
			FwUpdateTask.arrayAppend(blockCmd, 0, Common.base16Decode("5A9A"));
			blockCmd[2] = (byte) curBlock;
			int crcBlock = -1;
			int i = 0;
			while (i < BLOCK_SIZE) {
				byte b;
				blockCmd[3 + i] = b = this._bin[curBlock * BLOCK_SIZE + i];
				crcBlock = Common.crc_Update(crcBlock, b);
				crcTotal = Common.crc_Update(crcTotal, b);
				++i;
			}
			if (appendCRC) {
				blockCmd[blockCmd.length - 2] = (byte) (crcBlock >> 8 & 255);
				blockCmd[blockCmd.length - 1] = (byte) (crcBlock >> 0 & 255);
			}
			if (this.isCanceled()) {
				return null;
			}
			int sendBlockTries = 3;
			int sendTrialN = 1;
			while (sendTrialN <= 3) {
				r = this.recordPlayDecode(blockCmd, 10.0);
				if (r.isCanceledOrFailed()) {
					return null;
				}
				if (r.isParsed() && (r.matches("len=2,[0]=x06,[1]=x" + String.format("%02X", curBlock)) || r.matches("len=1,[0]=x06")))
					break;
				if (sendTrialN == 3) {
					return new Runnable() {

						public void run() {
							ACLog.i(FwUpdateTask.this.TAG, "notify send block failed");
							FwUpdateTask.this._umtMsg.onReceiveMsgUpdateFirmwareResult(303);
						}
					};
				}
				++sendTrialN;
			}
			++curBlock;
		}
		mainThreadHandler.post(new Runnable() {

			public void run() {
				ACLog.i(FwUpdateTask.this.TAG, "notify end block");
				FwUpdateTask.this._umtMsg.onReceiveMsgUpdateFirmwareProgress(90);
			}
		});
		byte[] fwEndCommand = Common.base16Decode("5A9AEE" + (this._encryptedUpdate ? "" : String.format(Locale.US, "%04X", crcTotal)));
		r = this.recordPlayDecode(fwEndCommand, 3.0);
		if (r.isCanceledOrFailed()) {
			return null;
		}
		if (!r.isParsed() || !r.matches("len=2,[0]=x06,[1]=xEE") && !r.matches("len=1,[0]=x06")) {
			return new Runnable() {

				public void run() {
					ACLog.i(FwUpdateTask.this.TAG, "notify end block failed");
					FwUpdateTask.this._umtMsg.onReceiveMsgUpdateFirmwareResult(304);
				}
			};
		}
		if (1 == SdkCustomization.CUST && 53 == this._bin.length / 256) {
			mainThreadHandler.post(new Runnable() {

				public void run() {
					ACLog.i(FwUpdateTask.this.TAG, "notify wait 2 min");
					FwUpdateTask.this._umtMsg.onReceiveMsgUpdateFirmwareResult(401);
				}
			});
			this._tonePlayer.setPlayingTone(ToneType.T_2000Hz);
			if (this.safeWait(90.0)) {
				return null;
			}
			return new Runnable() {

				public void run() {
					ACLog.i(FwUpdateTask.this.TAG, "notify 1st step done");
					FwUpdateTask.this._umtMsg.onReceiveMsgUpdateFirmwareResult(402);
				}
			};
		}
		return new Runnable() {

			public void run() {
				ACLog.i(FwUpdateTask.this.TAG, "notify finished successfully");
				FwUpdateTask.this._umtMsg.onReceiveMsgUpdateFirmwareProgress(100);
				FwUpdateTask.this._umtMsg.onReceiveMsgUpdateFirmwareResult(301);
			}
		};
	}

}