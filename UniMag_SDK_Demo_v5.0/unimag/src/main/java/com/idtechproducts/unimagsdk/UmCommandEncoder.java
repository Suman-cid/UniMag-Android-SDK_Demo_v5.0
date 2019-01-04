package com.idtechproducts.unimagsdk;

import android.media.AudioTrack;

import com.idtechproducts.acom.AcomManagerMsg;
import com.idtechproducts.acom.Common;

public class UmCommandEncoder
		implements AcomManagerMsg.CommandEncoder {
	private static final int kFRAME_SIZE = 2;
	private static final int[] kQIT_PERIOD_FREQ = new int[]{2400, 3000, 3400, 4000};
	private static byte[][] cachedFskChunk;
	private static int cachedFskChunk_freq;
	private static int cachedFskChunk_dir;

	private static byte[] synthesizeCommandString(byte[] command_string, int playbackFrequency, int outputWaveDirection, int[] ret_loopFrameIndex) {
		byte[][] qperiod;
		byte qit;
		int K_before = 720;
		int K_after = 12000;
		byte[] arrby = new byte[2];
		arrby[0] = -1;
		byte[] ecm_single = arrby;
		int ecm_count = 3;
		byte[] ecm = new byte[ecm_single.length * 3];
		int i = 0;
		while (i < 3) {
			System.arraycopy(ecm_single, 0, ecm, i * ecm_single.length, ecm_single.length);
			++i;
		}
		byte[] qitStr_ecm = UmCommandEncoder.synth_cmdToQit(ecm);
		byte[] qitStr_cmd = UmCommandEncoder.synth_cmdToQit(command_string);
		int qperiod_repCount = 10;
		byte[] kperiod = Common.synthesizeStereoSquareWavePeriod(2000, playbackFrequency, outputWaveDirection);
		if (cachedFskChunk != null && cachedFskChunk_freq == playbackFrequency && cachedFskChunk_dir == outputWaveDirection) {
			qperiod = cachedFskChunk;
		} else {
			qperiod = new byte[4][];
			int i2 = 0;
			while (i2 < 4) {
				byte[] period = Common.synthesizeStereoSquareWavePeriod(kQIT_PERIOD_FREQ[i2], playbackFrequency, outputWaveDirection);
				qperiod[i2] = new byte[period.length * 10];
				int j = 0;
				while (j < 10) {
					System.arraycopy(period, 0, qperiod[i2], j * period.length, period.length);
					++j;
				}
				++i2;
			}
			cachedFskChunk = qperiod;
			cachedFskChunk_freq = playbackFrequency;
			cachedFskChunk_dir = outputWaveDirection;
		}
		int totalFrames = kperiod.length / 2 * (K_before + K_after) + (UmCommandEncoder.synth_qperiodLenSum(qitStr_ecm, playbackFrequency) + UmCommandEncoder.synth_qperiodLenSum(qitStr_cmd, playbackFrequency)) * 10;
		byte[] ret = new byte[totalFrames * 2];
		int retI = 0;
		int i3 = 0;
		while (i3 < K_before) {
			System.arraycopy(kperiod, 0, ret, retI, kperiod.length);
			retI += kperiod.length;
			++i3;
		}
		i3 = 0;
		while (i3 < qitStr_ecm.length) {
			qit = qitStr_ecm[i3];
			System.arraycopy(qperiod[qit], 0, ret, retI, qperiod[qit].length);
			retI += qperiod[qit].length;
			++i3;
		}
		i3 = 0;
		while (i3 < qitStr_cmd.length) {
			qit = qitStr_cmd[i3];
			System.arraycopy(qperiod[qit], 0, ret, retI, qperiod[qit].length);
			retI += qperiod[qit].length;
			++i3;
		}
		ret_loopFrameIndex[0] = retI / 2;
		i3 = 0;
		while (i3 < K_after) {
			System.arraycopy(kperiod, 0, ret, retI, kperiod.length);
			retI += kperiod.length;
			++i3;
		}
		ret_loopFrameIndex[1] = retI / 2;
		return ret;
	}

	private static int synth_qperiodLenSum(byte[] qitString, int playbackFrequency) {
		int[] qitPeriodLen = new int[4];
		int i = 0;
		while (i < 4) {
			qitPeriodLen[i] = (int) Math.round((double) playbackFrequency / (double) kQIT_PERIOD_FREQ[i]);
			++i;
		}
		int ret = 0;
		int i2 = 0;
		while (i2 < qitString.length) {
			ret += qitPeriodLen[qitString[i2]];
			++i2;
		}
		return ret;
	}

	private static byte[] synth_cmdToQit(byte[] command_string) {
		byte[] stringQ = new byte[command_string.length * 4];
		int iStringQ = 0;
		int iChar = 0;
		while (iChar < command_string.length) {
			byte c = command_string[iChar];
			int iQit = 0;
			while (iQit < 4) {
				byte qit;
				stringQ[iStringQ] = qit = (byte) (c >> iQit * 2 & 3);
				++iStringQ;
				++iQit;
			}
			++iChar;
		}
		return stringQ;
	}

	public AudioTrack getCommandAudioTrack(byte[] commandString, int outputFrequency, int outputWaveDirection, int powerupBeforeCmd) {
		int[] ret_loopFrameIndex = new int[2];
		byte[] toneData = UmCommandEncoder.synthesizeCommandString(commandString, outputFrequency, outputWaveDirection, ret_loopFrameIndex);
		byte[] toneData_ = new byte[2000];
		System.arraycopy(toneData, 35000, toneData_, 0, 2000);
		AudioTrack ret = new AudioTrack(3, outputFrequency, 12, 3, toneData.length, 0);
		if (ret.write(toneData, 0, toneData.length) != toneData.length) {
			ret.release();
			throw new RuntimeException();
		}
		if (ret.getState() != 1) {
			throw new RuntimeException();
		}
		return ret;
	}
}