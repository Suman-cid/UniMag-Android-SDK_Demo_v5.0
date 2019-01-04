package com.idtechproducts.unimagsdk;

import android.media.AudioTrack;

import com.idtechproducts.acom.AcomManagerMsg;

import java.math.BigInteger;

import idtech.msr.unimag.Common;

public class UniJackCommandEncoder
		implements AcomManagerMsg.CommandEncoder {
	public AudioTrack getCommandAudioTrack(byte[] commandString, int outputFrequency, int outputWaveDirection, int powerupBeforeCmd) {
		int outputSampleRate = 48000;
		String cmdStr = Common.getHexStringFromBytes(commandString);
		String cmdBinStringToSend = binStringToCmdBinString(hexToBin(cmdStr));
		byte[] cmdStream;
		if (commandString != null) {
			if ((commandString.length == 1) && (commandString[0] == 0)) {
				cmdStream = createCommunicationWaveForSwipe(outputWaveDirection, outputSampleRate, powerupBeforeCmd);
			} else {
				if ((commandString.length == 1) && (commandString[0] == 5)) {
					cmdStream = createCommunicationWaveForCancelSwipe(outputWaveDirection, outputSampleRate, powerupBeforeCmd);
				} else {
					cmdStream = createCommunicationWave(cmdBinStringToSend, outputWaveDirection, outputSampleRate, powerupBeforeCmd);
				}
			}
		} else {
			return null;
		}
		AudioTrack ret = new AudioTrack(
				3,
				outputSampleRate,
				12,
				3,
				cmdStream.length,
				0);
		if (ret.write(cmdStream, 0, cmdStream.length) != cmdStream.length) {
			ret.release();
			throw new RuntimeException();
		}
		if (ret.getState() != 1) {
			throw new RuntimeException();
		}
		return ret;
	}

	private byte[] createCommunicationWaveForSwipe(int outputWaveDirection, int outputFrequency, int delay) {
		int outputLengthInMs = 1000;
		int cycleLength = outputFrequency / 1000 * 2;
		int totalLength = cycleLength * outputLengthInMs;

		totalLength = totalLength / cycleLength * cycleLength;

		byte[] ret = new byte[totalLength];
		byte H = -1;
		byte M = Byte.MIN_VALUE;
		byte L = 1;
		if (outputWaveDirection == 0) {
			H = 1;
			L = -1;
		}
		for (int i = 0; i < totalLength; i += cycleLength) {
			for (int j = 0; j < cycleLength; j += 2) {
				if (j < cycleLength / 2) {
					if (j == 0) {
						ret[(i + j)] = M;
						ret[(i + j + 1)] = M;
					} else {
						ret[(i + j)] = H;
						ret[(i + j + 1)] = H;
					}
				} else if (j == cycleLength / 2) {
					ret[(i + j)] = M;
					ret[(i + j + 1)] = M;
				} else {
					ret[(i + j)] = L;
					ret[(i + j + 1)] = L;
				}
			}
		}
		return ret;
	}

	private byte[] createCommunicationWaveForCancelSwipe(int outputWaveDirection, int outputFrequency, int delay) {
		int cycleLength = 12;
		int totalLength = 480000;

		byte[] ret = new byte[totalLength];
		byte H = -1;
		byte M = Byte.MIN_VALUE;
		byte L = 1;
		if (outputWaveDirection == 0) {
			H = 1;
			L = -1;
		}
		for (int i = 0; i < totalLength; i += cycleLength) {
			for (int j = 0; j < cycleLength; j += 2) {
				if (j < cycleLength / 2) {
					if (j == 0) {
						ret[(i + j)] = M;
						ret[(i + j + 1)] = M;
					} else {
						ret[(i + j)] = H;
						ret[(i + j + 1)] = H;
					}
				} else if (j == cycleLength / 2) {
					ret[(i + j)] = M;
					ret[(i + j + 1)] = M;
				} else {
					ret[(i + j)] = L;
					ret[(i + j + 1)] = L;
				}
			}
		}
		return ret;
	}

	private byte[] createCommunicationWave(String strData, int outputWaveDirection, int outputFrequency, int delay) {
		byte H = -1;
		byte M = Byte.MIN_VALUE;
		byte L = 1;
		if (outputWaveDirection == 0) {
			H = 1;
			L = -1;
		}
		int samplesPerMs = outputFrequency / 1000 * 2;

		int delayLength = 0;
		if (delay > 200) {
			delayLength = samplesPerMs * 300;
		}
		int squareWaveLength = samplesPerMs * 200;
		int pauseLength = samplesPerMs * 10;
		int delayEndLength = samplesPerMs * 20;
		int commandLength = strData.length() * 10 + 2 + delayEndLength;

		byte[] realBuffer = new byte[delayLength + squareWaveLength + 960 + commandLength];
		for (int i = 0; i < delayLength; i++) {
			realBuffer[i] = M;
		}
		for (int i = delayLength; i < squareWaveLength + delayLength; i += 48) {
			for (int j = 0; j < 48; j += 2) {
				if (j < 24) {
					if (j == 0) {
						realBuffer[(i + j)] = M;
						realBuffer[(i + j + 1)] = M;
					} else {
						realBuffer[(i + j)] = H;
						realBuffer[(i + j + 1)] = H;
					}
				} else if (j == 24) {
					realBuffer[(i + j)] = M;
					realBuffer[(i + j + 1)] = M;
				} else {
					realBuffer[(i + j)] = L;
					realBuffer[(i + j + 1)] = L;
				}
			}
		}
		for (int i = delayLength + squareWaveLength; i < delayLength + squareWaveLength + pauseLength; i++) {
			realBuffer[i] = M;
		}
		byte[] actualCmd = new byte[commandLength];
		for (int k = 0; k < 10; k++) {
			actualCmd[k] = H;
		}
		for (int i = 1; i < strData.length(); i++) {
			if (strData.charAt(i) == '0') {
				if (actualCmd[(i * 10 - 1)] == H) {
					actualCmd[(i * 10)] = H;
					actualCmd[(i * 10 + 1)] = H;
				} else if (actualCmd[(i * 10 - 1)] == L) {
					actualCmd[(i * 10)] = M;
					actualCmd[(i * 10 + 1)] = M;
				}
				for (int k = i * 10 + 2; k < i * 10 + 10; k++) {
					actualCmd[k] = H;
				}
			} else if (strData.charAt(i) == '1') {
				if (actualCmd[(i * 10 - 1)] == L) {
					actualCmd[(i * 10)] = L;
					actualCmd[(i * 10 + 1)] = L;
				} else if (actualCmd[(i * 10 - 1)] == H) {
					actualCmd[(i * 10)] = M;
					actualCmd[(i * 10 + 1)] = M;
				}
				for (int k = i * 10 + 2; k < i * 10 + 10; k++) {
					actualCmd[k] = L;
				}
			}
		}
		for (int i = actualCmd.length - delayEndLength - 2; i < actualCmd.length; i++) {
			actualCmd[i] = L;
		}
		System.arraycopy(actualCmd, 0, realBuffer, delayLength + squareWaveLength + pauseLength, commandLength);
		return realBuffer;
	}

	private String binStringToCmdBinString(String binString) {
		String result = "";
		for (int i = 0; i < binString.length(); i += 4) {
			String str = "0";
			for (int j = i + 3; j >= i; j--) {
				if (binString.charAt(j) == '0') {
					str = str + "10";
				} else if (binString.charAt(j) == '1') {
					str = str + "01";
				}
			}
			str = str + "1";
			result = result + str;
		}
		return result;
	}

	private String hexToBin(String s) {
		String result = "";
		for (int i = 0; i < s.length() / 2; i++) {
			String singleByte = new BigInteger(s.substring(i * 2, i * 2 + 2), 16).toString(2);
			while (singleByte.length() < 8) {
				singleByte = "0" + singleByte;
			}
			result = result + singleByte;
		}
		return result;
	}
}
