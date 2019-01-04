package com.idtechproducts.unimagsdk.task;

import android.os.Handler;
import android.os.Looper;
import com.idtechproducts.acom.ACLog;
import com.idtechproducts.acom.AcomManager;
import com.idtechproducts.acom.Common;
import com.idtechproducts.acom.io.IOManager;
import com.idtechproducts.acom.tasks.Task;

import idtech.msr.unimag.unimagtools.uniMagReaderToolsMsg;

public class FwUpdateTaskUniJack
        extends Task {
  private final uniMagReaderToolsMsg _umtMsg;
  private final byte[] _bin;
  private final byte[] _challengeResponse;

  public FwUpdateTaskUniJack(AcomManager umMan, uniMagReaderToolsMsg umtMsg, byte[] bin, byte[] challengeResponse) {
    super(umMan);
    this._umtMsg = umtMsg;
    this._bin = bin;
    this._challengeResponse = challengeResponse;
  }

  public Task.TaskType getType() {
    return Task.TaskType.FwUpdate;
  }

  protected Runnable taskMain() {
    IOManager.RPDResult r;
    Runnable retTimeout = new Runnable(){

      public void run() {
        ACLog.i(FwUpdateTaskUniJack.this.TAG, "notify timeout");
        FwUpdateTaskUniJack.this._umtMsg.onReceiveMsgUpdateFirmwareResult(305);
      }
    };
    Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    Boolean isInBLM = null;
    int chkBlmTries = 3;
    int trial = 1;
    while (trial <= 3) {
      r = this.recordPlayDecode(Common.makeSetBaudCommand(this._config), 3.5);
      if (r.isCanceledOrFailed()) {
        return null;
      }
      if (r.isParsed()) {
        if (r.matches("len=1,[0]=x06")) {
          isInBLM = false;
          break;
        }
        if (r.matches("len=2,[0]=x06,[1]=x56")) {
          isInBLM = true;
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
      boolean bl = problemWithChallengeResponse = this._challengeResponse == null || this._challengeResponse.length != 24;
      if (problemWithChallengeResponse) {
        return new Runnable(){

          public void run() {
            ACLog.i(FwUpdateTaskUniJack.this.TAG, "notify need CR");
            FwUpdateTaskUniJack.this._umtMsg.onReceiveMsgUpdateFirmwareResult(309);
          }
        };
      }
      byte[] challengeResponseCmd = FwUpdateTaskUniJack.makeChallengeResponseCmd(this._challengeResponse);
      int trial2 = 1;
      while (trial2 <= 3) {
        r = this.recordPlayDecode(challengeResponseCmd, 3.0);
        if (r.isCanceledOrFailed()) {
          return null;
        }
        if (r.isParsed() && (r.matches("len=1,[0]=x06") || r.matches("len=15,[0]=x06,[1]=x56"))) break;
        if (trial2 == 3) {
          return new Runnable(){

            public void run() {
              ACLog.i(FwUpdateTaskUniJack.this.TAG, "notify failed to enter BLM");
              FwUpdateTaskUniJack.this._umtMsg.onReceiveMsgUpdateFirmwareResult(302);
            }
          };
        }
        ++trial2;
      }
    }
    ACLog.i(this.TAG, "now in BLM");
    int BLOCK_SIZE = 1024;
    int BLOCK_COUNT = this._bin.length / 1024;
    int curBlock = 0;
    while (curBlock < BLOCK_COUNT) {
      final int progress = (int)((double)curBlock * 1.0 / (double)BLOCK_COUNT * 100.0);
      mainThreadHandler.post(new Runnable(){

        public void run() {
          FwUpdateTaskUniJack.this._umtMsg.onReceiveMsgUpdateFirmwareProgress(progress);
        }
      });
      byte[] blockCmd = new byte[1032];
      FwUpdateTaskUniJack.arrayAppend(blockCmd, 0, Common.base16Decode("025A9A"));
      blockCmd[3] = (byte)curBlock;
      System.arraycopy(this._bin, curBlock * 1024, blockCmd, 4, 1024);
      int binLrc = 0;
      byte binChecksum = 0;
      int i = 4;
      while (i < 1028) {
        binLrc = (byte)(binLrc ^ blockCmd[i]);
        binChecksum = (byte)(binChecksum + blockCmd[i]);
        ++i;
      }
      byte lrc = (byte)(binLrc ^ 2);
      lrc = (byte)(lrc ^ 90);
      lrc = (byte)(lrc ^ 154);
      lrc = (byte)(lrc ^ (byte)curBlock);
      lrc = (byte)(lrc ^ binLrc);
      lrc = (byte)(lrc ^ binChecksum);
      lrc = (byte)(lrc ^ 3);

      blockCmd[blockCmd.length - 4] = (byte) binLrc;
      blockCmd[blockCmd.length - 3] = binChecksum;
      blockCmd[blockCmd.length - 2] = 3;
      blockCmd[blockCmd.length - 1] = lrc;
      if (this.isCanceled()) {
        return null;
      }
      int sendBlockTries = 3;
      int sendTrialN = 1;
      while (sendTrialN <= 3) {
        r = this.recordPlayDecode(blockCmd, 20.0);
        if (r.isCanceledOrFailed()) {
          return null;
        }
        if (r.isParsed() && (r.matches("len=2,[0]=x06,[1]=x" + String.format("%02X", curBlock)) || r.matches("len=1,[0]=x06"))) break;
        if (sendTrialN == 3) {
          return new Runnable(){

            public void run() {
              ACLog.i(FwUpdateTaskUniJack.this.TAG, "notify send block failed");
              FwUpdateTaskUniJack.this._umtMsg.onReceiveMsgUpdateFirmwareResult(303);
            }
          };
        }
        ++sendTrialN;
      }
      ++curBlock;
    }
    mainThreadHandler.post(new Runnable(){

      public void run() {
        ACLog.i(FwUpdateTaskUniJack.this.TAG, "notify end block");
        FwUpdateTaskUniJack.this._umtMsg.onReceiveMsgUpdateFirmwareProgress(99);
      }
    });
    byte[] fwEndCommand = Common.base16Decode("025A9AEE0300");
    byte lrc = 0;
    int i = 0;
    while (i < 5) {
      lrc = (byte)(lrc ^ fwEndCommand[i]);
      ++i;
    }
    fwEndCommand[5] = lrc;
    r = this.recordPlayDecode(fwEndCommand, 3.0);
    if (r.isCanceledOrFailed()) {
      return null;
    }
    if (!r.isParsed() || !r.matches("len=2,[0]=x06,[1]=xEE") && !r.matches("len=1,[0]=x06")) {
      return new Runnable(){

        public void run() {
          ACLog.i(FwUpdateTaskUniJack.this.TAG, "notify end block failed");
          FwUpdateTaskUniJack.this._umtMsg.onReceiveMsgUpdateFirmwareResult(304);
        }
      };
    }
    return new Runnable(){

      public void run() {
        ACLog.i(FwUpdateTaskUniJack.this.TAG, "notify finished successfully");
        FwUpdateTaskUniJack.this._umtMsg.onReceiveMsgUpdateFirmwareProgress(100);
        FwUpdateTaskUniJack.this._umtMsg.onReceiveMsgUpdateFirmwareResult(301);
      }
    };
  }

  private static int arrayAppend(byte[] dst, int dstPos, byte[] src) {
    System.arraycopy(src, 0, dst, dstPos, src.length);
    return dstPos + src.length;
  }

  private static byte[] makeChallengeResponseCmd(byte[] cr) {
    byte[] cmdStart = Common.base16Decode("02537009FF");
    byte[] fullCmd = new byte[cmdStart.length + cr.length + 1 + 1];
    int offset = 0;
    offset = FwUpdateTaskUniJack.arrayAppend(fullCmd, offset, cmdStart);
    offset = FwUpdateTaskUniJack.arrayAppend(fullCmd, offset, cr);
    offset = FwUpdateTaskUniJack.arrayAppend(fullCmd, offset, new byte[]{3});
    byte lrc = 0;
    byte[] arrby = fullCmd;
    int n = arrby.length;
    int n2 = 0;
    while (n2 < n) {
      byte b = arrby[n2];
      lrc = (byte)(lrc ^ b);
      ++n2;
    }
    fullCmd[fullCmd.length - 1] = lrc;
    return fullCmd;
  }

}