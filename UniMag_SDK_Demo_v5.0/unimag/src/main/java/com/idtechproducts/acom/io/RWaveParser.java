package com.idtechproducts.acom.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
class RWaveParser {
	private static final byte H = 1;
	private static final byte M = 0;
	private static final byte L = -1;
	private final Client _cfg_client;
	private String _cfg_outFile_prefix = "./";
	private boolean _cfg_log = false;
	private boolean _cfg_logAll = false;
	private int _cfg_samplingRate = 44100;
	private int _cfg_baud = 9600;
	private int _cfg_inFilter_window = 1;
	private boolean _cfg_sdThresholdResolve_high = false;
	private boolean _cfg_sdThresholdResolve_low = false;
	private short[] _ftr_prev;
	private short _hml_sEnvP;
	private short _hml_sEnvN;
	private boolean _sd_inited;
	private AByteList _sd_sdList;
	private List<SDEntry> _sd_sdDbg;
	private byte _sd_sdLevel;
	private short _sd_lastLineLevel;
	private int _sd_noChangeCounter;
	private double _sd_accum;
	private double _sd_accumNext;
	private boolean _sd_snd;
	private List<short[]> _log_inp = new ArrayList<short[]>();
	private List<short[]> _log_ftr = new ArrayList<short[]>();
	private List<short[]> _log_hmlEnvP = new ArrayList<short[]>();
	private List<short[]> _log_hmlEnvN = new ArrayList<short[]>();
	private List<short[]> _log_hmlThrP = new ArrayList<short[]>();
	private List<short[]> _log_hmlThrN = new ArrayList<short[]>();
	private List<short[]> _log_hmlHML = new ArrayList<short[]>();

	public RWaveParser(Client client) {
		if (client == null) {
			throw new NullPointerException();
		}
		this._cfg_client = client;
	}

	static short[] medianFilter(int winSize, short[] prev, short[] src, int srcLen) {
		short[] ret;
		int ssi;
		int pa;
		assert (winSize % 2 == 1);
		assert (prev.length == winSize - 1);
		int mBefore = (winSize - 1) / 2;
		int mAfter = (winSize - 1) / 2;
		int midIndex = (winSize - 1) / 2;
		short[] win = new short[winSize];
		if (prev == null) {
			prev = new short[winSize - 1];
			Arrays.fill(prev, src[0]);
			ret = new short[srcLen - mAfter];
			pa = mAfter;
			ssi = mBefore;
		} else {
			ret = new short[srcLen];
			pa = prev.length;
			ssi = winSize - 1;
		}
		int i = 0;
		while (i < ssi) {
			System.arraycopy(prev, prev.length - pa, win, 0, pa);
			System.arraycopy(src, 0, win, pa, winSize - pa);
			Arrays.sort(win);
			ret[i] = win[midIndex];
			++i;
			--pa;
		}
		i = ssi;
		int srcI = 0;
		while (i < ret.length) {
			System.arraycopy(src, srcI, win, 0, winSize);
			Arrays.sort(win);
			ret[i] = win[midIndex];
			++i;
			++srcI;
		}
		return ret;
	}

	private static boolean matchAt(byte[] pattern, AByteList in, int inStart) {
		if (in.size() - inStart < pattern.length) {
			return false;
		}
		int i = 0;
		while (i < pattern.length) {
			if (pattern[i] != in.get(inStart + i)) {
				return false;
			}
			++i;
		}
		return true;
	}

	private static short[] copyOf(short[] original, int newLength) {
		short[] ret = new short[newLength];
		System.arraycopy(original, 0, ret, 0, original.length < newLength ? original.length : newLength);
		return ret;
	}

	private static short[] copyOfRange(short[] original, int from, int to) {
		short[] ret = new short[to - from];
		System.arraycopy(original, from, ret, 0, to - from);
		return ret;
	}

	private static byte[] getWaveHeader(int audioDataSize, int inputSamplingRate) {
		byte[] arrby = new byte[44];
		arrby[0] = 82;
		arrby[1] = 73;
		arrby[2] = 70;
		arrby[3] = 70;
		arrby[8] = 87;
		arrby[9] = 65;
		arrby[10] = 86;
		arrby[11] = 69;
		arrby[12] = 102;
		arrby[13] = 109;
		arrby[14] = 116;
		arrby[15] = 32;
		arrby[16] = 16;
		arrby[20] = 1;
		arrby[22] = 1;
		arrby[32] = 2;
		arrby[34] = 16;
		arrby[36] = 100;
		arrby[37] = 97;
		arrby[38] = 116;
		arrby[39] = 97;
		byte[] wavHeaderArray = arrby;
		ByteBuffer waveHeader = ByteBuffer.wrap(wavHeaderArray).order(ByteOrder.LITTLE_ENDIAN);
		waveHeader.position(4);
		waveHeader.putInt(36 + audioDataSize);
		waveHeader.position(24);
		waveHeader.putInt(inputSamplingRate);
		waveHeader.position(28);
		waveHeader.putInt(inputSamplingRate * 2);
		waveHeader.position(40);
		waveHeader.putInt(audioDataSize);
		return wavHeaderArray;
	}

	public void setLogFilePrefix(String prefix) {
		this._cfg_outFile_prefix = prefix;
	}

	public void setLog(boolean enabled, boolean logAll) {
		this._cfg_log = enabled;
		this._cfg_logAll = logAll;
	}

	public void setSamplingRate(int samplingRate) {
		this._cfg_samplingRate = samplingRate;
	}

	public void setBaud(int baud) {
		this._cfg_baud = baud;
	}

	public void setInputFilterWindow(int windowSize) {
		if (windowSize < 1 || windowSize % 2 == 0) {
			throw new IllegalArgumentException();
		}
		this._cfg_inFilter_window = windowSize;
	}

	public void setSdThresholdResolve(boolean high, boolean low) {
		this._cfg_sdThresholdResolve_high = high;
		this._cfg_sdThresholdResolve_low = low;
	}

	public void purge() {
		this._ftr_prev = null;
		this._hml_sEnvN = 0;
		this._hml_sEnvP = 0;
		this._sd_inited = false;
		this._sd_snd = false;
		this._log_inp.clear();
		this._log_ftr.clear();
		this._log_hmlEnvP.clear();
		this._log_hmlEnvN.clear();
		this._log_hmlThrP.clear();
		this._log_hmlThrN.clear();
		this._log_hmlHML.clear();
	}

	public void parseWaveData(short[] waveData, int length) {
		if (length == 0) {
			return;
		}
		if (waveData.length < length) {
			throw new ArrayIndexOutOfBoundsException();
		}
		if (this._cfg_log) {
			this._log_inp.add(waveData.length == length ? waveData : RWaveParser.copyOf(waveData, length));
		}
		short[] wFiltered = waveData;
		int wFilteredLen = length;
		if (this._cfg_inFilter_window != 1) {
			wFiltered = this.pipe_inputFilter(waveData, length);
			wFilteredLen = wFiltered.length;
		}
		short[] wHML = this.pipe_lineToHML(wFiltered, wFilteredLen);
		List<AByteList> sdPacketList = this.pipe_hmlToSd(wHML);
		this.pipe_sdToBytes(sdPacketList);
	}


	void pipe_sdToBytes(List<AByteList> sdPacketList) {
		label23:
		label591:
		label678:

		for (AByteList sdPacket : sdPacketList) {
			int i = 0;
			boolean continueParsing;
			do {
				byte gH;
				byte gL;
				byte[] frame66;
				do {
					do {
						byte[] uniPreamb = {1, -1, -1, 1, 1, -1, -1, 1};
						for (; i < sdPacket.size(); i++)
							if (matchAt(uniPreamb, sdPacket, i))
								break;
						i += uniPreamb.length;


						if (i + 30 >= sdPacket.size()) {
							break label678;
						}


						if (sdPacket.get(i) == -1) {
							i++;
							boolean isDirUp = true;
							break;
						}
					} while (!matchAt(new byte[]{-1, 1}, sdPacket, i + 1));

					i += 3;
					boolean isDirUp = false;

					gH = (byte) (isDirUp ? 1 : -1);
					gL = (byte) (isDirUp ? -1 : 1);

					byte[] frame5 = {gH, gH, gL, gL, gH, gH, gL, gL, gH, gL};
					frame66 = new byte[]{gH, gL, gH, gH, gL, gH, gL, gL, gH, gL,
							gH, gL, gH, gH, gL, gH, gL, gL, gH, gL};


					while (matchAt(frame5, sdPacket, i)) {
						i += frame5.length;
					}

				}
				while (!matchAt(frame66, sdPacket, i));

				i += frame66.length;

				AByteList packetBody = new AByteList();
				for (; i + 19 < sdPacket.size(); i += 20) {
					byte readByte = 0;
					int nibI = 0;
					int nshift = 4;

//					break label591;

					if (sdPacket.get(i + 10 * nibI + 0) != gH) {
						break label23;
					}

					byte nibble = 0;

					for (int bitI = 0; bitI < 4; bitI++) {
						byte bit = sdPacket.get(i + 10 * nibI + 1 + bitI * 2);

						if (sdPacket.get(i + 10 * nibI + 1 + bitI * 2 + 1) != bit * -1)
							break;
						nibble = (byte) (nibble | (bit == gH ? 1 << bitI : 0));
					}

					if (sdPacket.get(i + 10 * nibI + 9) != gL)
						break label23;
					readByte = (byte) (readByte | nibble << nshift);
					nibI++;
					nshift -= 4;
					if (nibI < 2) {
						break;
					}


					packetBody.add(readByte);
				}


				byte[] byteArr = new byte[packetBody.size()];
				for (int x = 0; x < byteArr.length; x++)
					byteArr[x] = packetBody.get(x);
				continueParsing = _cfg_client.processParsedData(byteArr);
			} while (continueParsing);
			break;
		}
	}

	public boolean isPacketDetected() {
		return this._sd_snd;
	}

	short[] pipe_inputFilter(short[] src, int srcLen) {
		short[] inputFiltered = RWaveParser.medianFilter(this._cfg_inFilter_window, this._ftr_prev, src, srcLen);
		this._ftr_prev = RWaveParser.copyOfRange(src, srcLen - (this._cfg_inFilter_window - 1), srcLen);
		if (this._cfg_log && this._cfg_logAll) {
			this._log_ftr.add(inputFiltered);
		}
		return inputFiltered;
	}

	short[] pipe_lineToHML(short[] wLine, int len) {
		short[] wHML = new short[len];

		short[] wEnvP = (_cfg_log) && (_cfg_logAll) ? new short[len] : null;
		short[] wEnvN = (_cfg_log) && (_cfg_logAll) ? new short[len] : null;
		short[] wThrP = (_cfg_log) && (_cfg_logAll) ? new short[len] : null;
		short[] wThrN = (_cfg_log) && (_cfg_logAll) ? new short[len] : null;

		for (int i = 0; i < len; i++) {
			short sLine = wLine[i];
			if (sLine >= 0) {
				if (sLine >= _hml_sEnvP) {
					_hml_sEnvP = ((short) (_hml_sEnvP / 2 + sLine / 2));
				} else {
					_hml_sEnvP = ((short) (19 * _hml_sEnvP / 20 + sLine / 20));
				}
			} else if (sLine <= _hml_sEnvN) {
				_hml_sEnvN = ((short) (_hml_sEnvN / 2 + sLine / 2));
			} else {
				_hml_sEnvN = ((short) (19 * _hml_sEnvN / 20 + sLine / 20));
			}


			int envDelta = _hml_sEnvP - _hml_sEnvN;
			if (envDelta < 4000) {
				i += 15;

			} else {

				int envDeltaDiv3 = envDelta / 3;
				short sThrP = (short) (_hml_sEnvN + 2 * envDeltaDiv3);
				short sThrN = (short) (_hml_sEnvN + envDeltaDiv3);


				if (sLine > sThrP) {
					wHML[i] = Short.MAX_VALUE;
				} else if (sLine >= sThrN) {
					wHML[i] = 0;
				} else {
					wHML[i] = Short.MIN_VALUE;
				}


				if ((_cfg_log) && (_cfg_logAll)) {
					wEnvP[i] = _hml_sEnvP;
					wEnvN[i] = _hml_sEnvN;
					wThrP[i] = sThrP;
					wThrN[i] = sThrN;
				}
			}
		}

		if ((_cfg_log) && (_cfg_logAll)) {
			_log_hmlEnvP.add(wEnvP);
			_log_hmlEnvN.add(wEnvN);
			_log_hmlThrP.add(wThrP);
			_log_hmlThrN.add(wThrN);
			_log_hmlHML.add(wHML);
		}

		return wHML;
	}

	List<AByteList> pipe_hmlToSd(short[] wLvl) {
		if (wLvl.length <= 0) {
			throw new RuntimeException("wLvl.length <= 0");
		}
		double sd_thresh = (double) this._cfg_samplingRate / (double) this._cfg_baud * 1.5;
		int packetEnd_thresh = (int) ((double) this._cfg_samplingRate / (double) this._cfg_baud * 2.0 * 3.0);
		ArrayList<AByteList> ret = new ArrayList<AByteList>(5);
		int startLoc = !this._sd_inited ? this.pipe_hmlToSd_init(wLvl, 0) : 0;
		int i = startLoc;
		while (i < wLvl.length) {
			if (wLvl[i] != 0) {
				if (wLvl[i] == this._sd_lastLineLevel) {
					this._sd_accum += 1.0;
				} else if (this._sd_sdLevel != (wLvl[i] > 0 ? (byte) 1 : -1)) {
					if (this._sd_accum == sd_thresh) {
						if (this._sd_sdLevel == 1 ? this._cfg_sdThresholdResolve_high : this._cfg_sdThresholdResolve_low) {
							this._sd_sdList.add(this._sd_sdLevel);
						}
					} else if (this._sd_accum > sd_thresh) {
						this._sd_sdList.add(this._sd_sdLevel);
					}
					this._sd_sdList.add(this._sd_sdLevel);
					if (this._cfg_log && this._cfg_logAll) {
						this._sd_sdDbg.get((int) (this._sd_sdDbg.size() - 1)).acc = this._sd_accum;
						this._sd_sdDbg.add(new SDEntry(i, 0.0));
					}
					if (!this._sd_snd && this._sd_sdList.size() > 200) {
						this._sd_snd = true;
						this._cfg_client.soundDetected();
					}
					this._sd_accum = this._sd_accumNext + 1.0;
					this._sd_accumNext = 0.0;
					this._sd_sdLevel = (byte) (wLvl[i] > 0 ? 1 : -1);
				} else {
					this._sd_accum += this._sd_accumNext + 1.0;
					this._sd_accumNext = 0.0;
				}
			} else {
				this._sd_accum += 0.5;
				this._sd_accumNext += 0.5;
			}
			this._sd_noChangeCounter = this._sd_lastLineLevel == wLvl[i] ? this._sd_noChangeCounter + 1 : 0;
			this._sd_lastLineLevel = wLvl[i];
			if (this._sd_noChangeCounter > packetEnd_thresh) {
				if (this._sd_accum > sd_thresh) {
					this._sd_sdList.add(this._sd_sdLevel);
				}
				this._sd_sdList.add(this._sd_sdLevel);
				if (this._cfg_log && this._cfg_logAll) {
					this._sd_sdDbg.get((int) (this._sd_sdDbg.size() - 1)).acc = this._sd_accum;
				}
				ret.add(this._sd_sdList);
				i = this.pipe_hmlToSd_init(wLvl, i) - 1;
			}
			++i;
		}
		return ret;
	}

	private int pipe_hmlToSd_init(short[] wLvl, int wLvlStart) {
		int startLoc;
		this._sd_sdList = new AByteList();

		if (this._cfg_log && this._cfg_logAll) {
			this._sd_sdDbg = new ArrayList<SDEntry>();
		}
		this._sd_noChangeCounter = 0;
		this._sd_accum = 0.0;
		this._sd_accumNext = 0.0;
		if (wLvl[wLvlStart] != 0) {
			int ti = wLvlStart;
			while (ti < wLvl.length && wLvl[wLvlStart] == wLvl[ti]) {
				++ti;
			}
			startLoc = ti - (int) ((double) this._cfg_samplingRate / (double) this._cfg_baud * 2.0);
			if (startLoc < wLvlStart) {
				startLoc = wLvlStart;
			}
		} else {
			startLoc = wLvlStart;
			while (startLoc < wLvl.length && wLvl[startLoc] == 0) {
				++startLoc;
			}
		}
		if (startLoc < wLvl.length) {
			this._sd_sdLevel = (byte) (wLvl[startLoc] > 0 ? 1 : -1);
			if (this._cfg_log && this._cfg_logAll) {
				this._sd_sdDbg.add(new SDEntry(startLoc, 0.0));
			}
			this._sd_lastLineLevel = wLvl[startLoc];
			this._sd_inited = true;
		} else {
			this._sd_inited = false;
		}
		return startLoc;
	}

	public void saveLoggedData() {
		this.writeToFile(".wav", this._log_inp);
		if (this._cfg_logAll) {
			this.writeToFile("_iftr.wav", this._log_ftr);
			this.writeToFile("_envP.wav", this._log_hmlEnvP);
			this.writeToFile("_envN.wav", this._log_hmlEnvN);
			this.writeToFile("_thrP.wav", this._log_hmlThrP);
			this.writeToFile("_thrN.wav", this._log_hmlThrN);
			this.writeToFile("_hml.wav", this._log_hmlHML);
		}
	}

	private void writeToFile(String path, List<short[]> datas) {
		block16:
		{
			File qualifiedPath = new File(String.valueOf(this._cfg_outFile_prefix) + path);
			OutputStream os = null;
			try {
				try {
					os = new BufferedOutputStream(new FileOutputStream(qualifiedPath));
					int dataLenSum = 0;
					for (short[] data : datas) {
						dataLenSum += data.length;
					}
					byte[] header = RWaveParser.getWaveHeader(dataLenSum * 2, this._cfg_samplingRate);
					os.write(header, 0, header.length);
					int buffSize = 4000;
					int buffItemCapacity = 2000;
					byte[] buff = new byte[4000];
					for (short[] data : datas) {
						int itemsLeft = data.length;
						int srcIndex = 0;
						while (itemsLeft > 0) {
							int itemsToCopy = 2000 < itemsLeft ? 2000 : itemsLeft;
							int i = 0;
							while (i < itemsToCopy) {
								buff[i * 2] = (byte) (data[srcIndex + i] & 255);
								buff[i * 2 + 1] = (byte) (data[srcIndex + i] >> 8 & 255);
								++i;
							}
							os.write(buff, 0, itemsToCopy * 2);
							itemsLeft -= itemsToCopy;
							srcIndex += itemsToCopy;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
					if (os == null) break block16;
					try {
						os.close();
					} catch (IOException iOException) {
					}
				}
			} finally {
				if (os != null) {
					try {
						os.close();
					} catch (IOException iOException) {
					}
				}
			}
		}
	}

	public static interface Client {
		public boolean processParsedData(byte[] var1);

		public void soundDetected();
	}

	private static class AByteList {
		private static final int INC = 150;
		private ArrayList<byte[]> _linkedArrays = null;
		private int _length;

		private AByteList() {
			this._linkedArrays = new ArrayList();
		}

		public void add(byte item) {
			if (this._linkedArrays.size() * 150 == this._length) {
				this._linkedArrays.add(new byte[150]);
			}
			this._linkedArrays.get((int) (this._length / 150))[this._length % 150] = item;
			++this._length;
		}

		public byte get(int index) {
			return this._linkedArrays.get(index / 150)[index % 150];
		}

		public int size() {
			return this._length;
		}
	}

	static class SDEntry
			implements Serializable {
		private static final long serialVersionUID = -9079481752499031780L;
		public int idx;
		public double acc;

		public SDEntry(int idx, double acc) {
			this.idx = idx;
			this.acc = acc;
		}
	}

}