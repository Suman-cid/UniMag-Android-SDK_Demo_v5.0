/*
 * Decompiled with CFR 0_132.
 *
 * Could not load the following classes:
 *  android.media.AudioTrack
 *  android.os.Build
 *  android.os.Build$VERSION
 */
package com.idtechproducts.acom.tasks;

import android.media.AudioTrack;
import android.os.Build;

import com.idtechproducts.acom.ACLog;
import com.idtechproducts.acom.AcomManager;
import com.idtechproducts.acom.Common;
import com.idtechproducts.acom.io.IOManager;
import com.idtechproducts.acom.io.ToneType;

import java.util.List;

import idtech.msr.xmlmanager.ConfigParameters;
import idtech.msr.xmlmanager.ReaderType;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class AutoConfigTask
		extends Task {
	private static final int[] recordSampList = Build.MANUFACTURER.equalsIgnoreCase("samsung") ? new int[]{48000, 44100, 22050} : new int[]{48000, 44100, 32000, 24000, 22050};
	private final boolean _cfg_readerSupportsCommand;
	private final List<ConfigParameters> _cfg_templates;
	private final byte[] cmd_getSettings = Common.base16Decode("02521F034C");
	private final byte[] cmd_getLong = Common.base16Decode("0252200373");
	private final byte[] resp_long = Common.base16Decode("06024142434445464748494a4b4c4d4e4f505152535455565758595a6162636465666768696a6b6c6d6e6f70717273747576030d");
	private final byte[] resp_long_unijack = Common.base16Decode("06024142434445464748494a4b4c4d4e4f505152535455565758595a6162636465666768696a6b6c6d6e6f707172737475767778797a4142434445464748494a4b4c4d4e4f505152535455565758595a6162636465666768696a6b6c6d6e6f707172737475767778797a4142434445464748494a4b4c4d4e4f505152535455565758595a6162636465666768696a6b6c6d6e6f707172737475767778797a0307");
	private ConfigParameters _originalConfig;
	private Boolean _isLongCommandSupported;
	private Boolean findgoodconfigure = false;

	public AutoConfigTask(AcomManager umMan, boolean readerSupportsCommand, List<ConfigParameters> templates) {
		super(umMan);
		this._cfg_readerSupportsCommand = readerSupportsCommand;
		this._cfg_templates = templates;
	}

	private static void generateModelName(ConfigParameters config) {
		String profileName = "<outDir=" + config.getDirectionOutputWave() + ",psamp=" + config.getFrequenceOutput() + ",baud=" + config.getBaudRate() + ",rsamp=" + config.getFrequenceInput() + ",vr=" + config.getUseVoiceRecognition() + (config.getShuttleChannel() != 48 ? ",shuttleChannel" : "") + ">";
		config.setModelNumber(profileName);
	}

	@Override
	protected void taskSetup() {
		this.tone_saveAndStop();
		this._originalConfig = this._ioManager.getConfigCopy();
	}

	@Override
	protected void taskCleanup() {
		this._ioManager.setConfig(this._originalConfig);
		this.tone_restore();
	}

	@Override
	public Task.TaskType getType() {
		return Task.TaskType.AutoConfig;
	}

	@Override
	protected Runnable taskMain() {
		this.reportProgressPercent(0);
		ConfigParameters config = new ConfigParameters();
		if (Build.VERSION.SDK_INT == 8) {
			config.setForceHeadsetPlug((short) 1);
		}
		boolean supportedInputSampFound = false;
		int[] arrn = recordSampList;
		int n = arrn.length;
		int n2 = 0;
		while (n2 < n) {
			int inputSamp = arrn[n2];
			if (Common.checkInputFrequencySupported(inputSamp)) {
				config.setFrequenceInput(inputSamp);
				supportedInputSampFound = true;
				break;
			}
			++n2;
		}
		if (!supportedInputSampFound) {
			ACLog.e(this.TAG, "aborted. Device does not support any input sampling rate");
			return null;
		}
		this.findgoodconfigure = false;
		FindParamRet output = this.findParam_output(config);
		if (output.isCanceled) {
			return null;
		}
		if (!this.findgoodconfigure.booleanValue()) {
			if (output.config != null) {
				config = output.config;
			}
			ACLog.i(this.TAG, "configuration not found");
			FindParamRet recordAndParseRet = this.findParam_recordAndParse(config);
			if (recordAndParseRet.isCanceled) {
				return null;
			}
			config = recordAndParseRet.config;
		} else {
			config = output.config;
		}
		if (config == null && this._cfg_templates != null) {
			ProgressContext pc = new ProgressContext();
			pc.indexWeights = new int[]{1};
			pc.totalSteps = this._cfg_templates.size();
			pc.percentStart = 50;
			pc.percentEnd = 100;
			ACLog.i(this.TAG, "jumps to rare case");
			int templateI = 0;
			while (templateI < this._cfg_templates.size()) {
				ConfigParameters template = this._cfg_templates.get(templateI);
				if (this._cfg_readerSupportsCommand || template.getBaudRate() == 9600) {
					ACLog.i(this.TAG, "template: " + template.getModelNumber());
					this.reportProgressByIndex(pc, templateI);
					FindParamRet templateRet = this.helper_testStability(template, template.getBaudRate());
					if (templateRet.isCanceled) {
						return null;
					}
					if (templateRet.config != null) {
						config = templateRet.config;
						break;
					}
				}
				++templateI;
			}
		}
		if (config != null) {
			ACLog.i(this.TAG, "profile found");
			if (this._originalConfig != null) {
				config.setSupportStatuses(this._originalConfig.getSupportStatuses());
			}
		}
		final ConfigParameters config_f = config;
		return new Runnable() {

			public void run() {
				AutoConfigTask.this._uniMagManager.getIntern_AcomManagerMsg().onAutoConfigStopped(config_f);
			}
		};
	}

	private void reportProgressPercent(final int percent) {
		this.post(new Runnable() {

			public void run() {
				AutoConfigTask.this._uniMagManager.getIntern_AcomManagerMsg().onAutoConfigProgress(percent);
			}
		});
	}

	private /* varargs */ void reportProgressByIndex(ProgressContext pc, int... indexes) {
		int loopStep = 0;
		int i = 0;
		while (i < indexes.length) {
			loopStep += indexes[i] * pc.indexWeights[i];
			++i;
		}
		double percentBeforeScaling = (double) loopStep / (double) pc.totalSteps;
		int percent = (int) (percentBeforeScaling * (double) (pc.percentEnd - pc.percentStart)) + pc.percentStart;
		if (percent == 0) {
			percent = 1;
		}
		if (percent >= 100) {
			percent = 99;
		}
		this.reportProgressPercent(percent);
	}

	private FindParamRet findParam_output(ConfigParameters config) {
		int[] arrn;
		config = config.clone();
		FindParamRet ret = new FindParamRet();
		ret.isCanceled = true;
		ret.config = null;
		short[] arrs = new short[2];
		arrs[0] = 1;
		short[] paramOutDir = arrs;
		if (Build.MANUFACTURER.equalsIgnoreCase("samsung")) {
			int[] arrn2 = new int[1];
			arrn = arrn2;
			arrn2[0] = 48000;
		} else {
			int[] arrn3 = new int[2];
			arrn3[0] = 24000;
			arrn = arrn3;
			arrn3[1] = 48000;
		}
		int[] paramOutSamp = arrn;
		ProgressContext pc = new ProgressContext();
		pc.totalSteps = paramOutDir.length * paramOutSamp.length;
		pc.indexWeights = new int[]{paramOutSamp.length, 1};
		pc.percentStart = 0;
		pc.percentEnd = 10;
		boolean found = false;
		boolean volumeAdjusted = false;
		int iOutDir = 0;
		block0:
		while (iOutDir < paramOutDir.length) {
			int iOutSamp = 0;
			while (iOutSamp < paramOutSamp.length) {
				config.setDirectionOutputWave(paramOutDir[iOutDir]);
				if (AudioTrack.getMinBufferSize((int) paramOutSamp[iOutSamp], (int) 12, (int) 3) < 0) {
					ACLog.i(this.TAG, "skipped unsupported output sampling rate: " + paramOutSamp[iOutSamp]);
				} else {
					IOManager.RPDResult r;
					config.setFrequenceOutput(paramOutSamp[iOutSamp]);
					AutoConfigTask.generateModelName(config);
					ACLog.i(this.TAG, config.getModelNumber());
					this._ioManager.setConfig(config);
					volumeAdjusted = this._ioManager.setDeviceMediaVolumeAutoConfig();
					if (!volumeAdjusted) {
						iOutDir = paramOutDir.length;
						break;
					}
					this.reportProgressByIndex(pc, iOutDir, iOutSamp);
					if (this._cfg_readerSupportsCommand) {
						r = this.recordPlayDecode(Common.makeSetBaudCommand(config), 2.0);
					} else {
						this._tonePlayer.setPlayingTone(ToneType.T_2400Hz);
						r = this.recordPlayDecode(null, 2.5);
						this._tonePlayer.setPlayingTone(null);
					}
					if (r.isCanceledOrFailed()) {
						return ret;
					}
					if (r.packetDetected) {
						found = true;
						if (!r.isParsed() || !r.matches("len=1,[0]=x06") && !r.containsStr("SPPMSR"))
							break block0;
						int baudrate = config.getBaudRate();
						FindParamRet fpr = this.helper_testStability(config, baudrate);
						if (fpr.isCanceled) {
							return ret;
						}
						if (fpr.config == null) break block0;
						config = fpr.config;
						AutoConfigTask.generateModelName(config);
						ACLog.i(this.TAG, "found stable profile: " + config.getModelNumber());
						this.findgoodconfigure = true;
						break block0;
					}
					if (this.safeWait(1.6)) {
						return ret;
					}
				}
				++iOutSamp;
			}
			++iOutDir;
		}
		if (!volumeAdjusted) {
			ret.isCanceled = true;
			this._uniMagManager.getIntern_AcomManagerMsg().onVolumeAdjustFailure(8, "Failed to increase media volume");
			return ret;
		}
		if (!found) {
			ret.isCanceled = false;
			return ret;
		}
		ret.isCanceled = false;
		ret.config = config;
		return ret;
	}

	private FindParamRet findParam_recordAndParse(ConfigParameters config) {
		short[] arrs;
		IOManager.RPDResult r;
		int[] arrn;
		config = config.clone();
		FindParamRet ret = new FindParamRet();
		ret.isCanceled = true;
		ret.config = null;
		ACLog.i(this.TAG, "set channel");
		int baudToTry = this._cfg_readerSupportsCommand ? 2400 : 9600;
		config.setBaudRate(baudToTry);
		this._ioManager.setConfig(config);
		if (this._cfg_readerSupportsCommand && (r = this.recordPlayDecode(Common.makeSetBaudCommand(config), 2.0)).isCanceledOrFailed()) {
			return ret;
		}
		byte[] arrby = new byte[2];
		arrby[0] = 48;
		byte[] shuttleChannelList = arrby;
		if (Build.VERSION.SDK_INT >= 7) {
			short[] arrs2 = new short[2];
			arrs = arrs2;
			arrs2[0] = 1;
		} else {
			arrs = new short[1];
		}
		short[] recordUseVrList = arrs;
		int shuttleChannelI = 0;
		int[] recordSampList = AutoConfigTask.recordSampList;
		ProgressContext pc = new ProgressContext();
		if (Build.MANUFACTURER.equalsIgnoreCase("samsung")) {
			int[] arrn2 = new int[1];
			arrn = arrn2;
			arrn2[0] = 48000;
		} else {
			int[] arrn3 = new int[2];
			arrn3[0] = 24000;
			arrn = arrn3;
			arrn3[1] = 48000;
		}
		int[] paramOutSamp = arrn;
		short[] arrs3 = new short[2];
		arrs3[0] = 1;
		short[] paramOutDir = arrs3;
		pc.indexWeights = new int[]{recordSampList.length * recordUseVrList.length, recordSampList.length, 1};
		pc.totalSteps = shuttleChannelList.length * recordUseVrList.length * recordSampList.length;
		pc.percentStart = 10;
		pc.percentEnd = 50;
		boolean found = false;
		int recordSrcI = 0;
		block0:
		while (recordSrcI < recordUseVrList.length) {
			int recordSampI = 0;
			while (recordSampI < recordSampList.length) {
				config.setShuttleChannel(shuttleChannelList[shuttleChannelI]);
				this._ioManager.setDeviceMediaVolumeAutoConfig();
				config.setUseVoiceRecognition(recordUseVrList[recordSrcI]);
				if (!Common.checkInputFrequencySupported(recordSampList[recordSampI])) {
					ACLog.i(this.TAG, "skipped unsupported input sampling rate: " + recordSampList[recordSampI]);
				} else {
					config.setFrequenceInput(recordSampList[recordSampI]);
					this._ioManager.setDeviceMediaVolumeAutoConfig();
					AutoConfigTask.generateModelName(config);
					ACLog.i(this.TAG, config.getModelNumber());
					this._ioManager.setConfig(config);
					this.reportProgressByIndex(pc, shuttleChannelI, recordSrcI, recordSampI);
					if (this._cfg_readerSupportsCommand) {
						r = this.recordPlayDecode(Common.makeSetBaudCommand(config), 2.0);
					} else {
						this._tonePlayer.setPlayingTone(ToneType.T_2400Hz);
						r = this.recordPlayDecode(null, 2.5);
						this._tonePlayer.setPlayingTone(null);
					}
					if (r.isCanceledOrFailed()) {
						return ret;
					}
					if (!r.packetDetected) {
						short CurrentOutDir = config.getDirectionOutputWave();
						int CurrentOutSamp = config.getFrequenceOutput();
						int iOutDir = 0;
						block2:
						while (iOutDir < paramOutDir.length) {
							int iOutSamp = 0;
							while (iOutSamp < paramOutSamp.length) {
								if (AudioTrack.getMinBufferSize((int) paramOutSamp[iOutSamp], (int) 12, (int) 3) < 0) {
									ACLog.i(this.TAG, "skipped unsupported output sampling rate: " + paramOutSamp[iOutSamp]);
								} else if (CurrentOutDir != iOutDir || CurrentOutSamp != iOutSamp) {
									this._ioManager.setDeviceMediaVolumeAutoConfig();
									config.setDirectionOutputWave(paramOutDir[iOutDir]);
									config.setFrequenceOutput(paramOutSamp[iOutSamp]);
									AutoConfigTask.generateModelName(config);
									ACLog.i(this.TAG, config.getModelNumber());
									this._ioManager.setConfig(config);
									if (this._cfg_readerSupportsCommand) {
										r = this.recordPlayDecode(Common.makeSetBaudCommand(config), 2.0);
									} else {
										this._tonePlayer.setPlayingTone(ToneType.T_2400Hz);
										r = this.recordPlayDecode(null, 2.5);
										this._tonePlayer.setPlayingTone(null);
									}
									if (r.isCanceledOrFailed()) {
										return ret;
									}
									if (r.packetDetected) break block2;
								}
								++iOutSamp;
							}
							++iOutDir;
						}
					}
					if (r.packetDetected && r.isParsed() && (r.matches("len=1,[0]=x06") || r.containsStr("SPPMSR"))) {
						FindParamRet fpr = this.findParam_highestStableBaud(config);
						if (fpr.isCanceled) {
							return ret;
						}
						if (fpr.config != null) {
							config = fpr.config;
							AutoConfigTask.generateModelName(config);
							ACLog.i(this.TAG, "found stable profile: " + config.getModelNumber());
							found = true;
							break block0;
						}
					}
					if (this.safeWait(1.6)) {
						return ret;
					}
				}
				++recordSampI;
			}
			++recordSrcI;
		}
		if (!found) {
			ret.isCanceled = false;
			return ret;
		}
		ret.isCanceled = false;
		ret.config = config;
		return ret;
	}

	private FindParamRet findParam_highestStableBaud(ConfigParameters config) {
		config = config.clone();
		FindParamRet ret = new FindParamRet();
		ret.isCanceled = true;
		ret.config = null;
		if (this._cfg_readerSupportsCommand) {
			FindParamRet testBaudRet = this.helper_testStability(config, 4800);
			if (testBaudRet.isCanceled) {
				return ret;
			}
			if (testBaudRet.config != null) {
				testBaudRet = this.helper_testStability(config, 9600);
				if (testBaudRet.isCanceled) {
					return ret;
				}
				config.setBaudRate(testBaudRet.config != null ? 9600 : 4800);
				ret.config = config;
				ret.isCanceled = false;
				return ret;
			}
			testBaudRet = this.helper_testStability(config, 2400);
			return testBaudRet;
		}
		FindParamRet testBaudRet = this.helper_testStability(config, 9600);
		return testBaudRet;
	}

	private Boolean helper_isLongCmdSupported() {
		if (!this._cfg_readerSupportsCommand)
			return false;

		if (this._isLongCommandSupported != null)
			return this._isLongCommandSupported;

		ACLog.i(this.TAG, "long response?");
		IOManager.RPDResult r = this.recordPlayDecode(this.cmd_getLong, 5.0);

		if (r.isCanceledOrFailed())
			return null;

		this._isLongCommandSupported =
				Common.ConnectedReader == ReaderType.UNIJACK ?
						Boolean.valueOf(r.isParsed() && r.matches(this.resp_long_unijack))
						: Boolean.valueOf(r.isParsed() && r.matches(this.resp_long));

		return this._isLongCommandSupported;
	}

	private FindParamRet helper_testStability(ConfigParameters config, int baud) {
		IOManager.RPDResult r;
		String localTag = "Stability test: ";
		config = config.clone();
		FindParamRet ret = new FindParamRet();
		ret.isCanceled = true;
		ret.config = null;
		Boolean isLongCmdSupported = this.helper_isLongCmdSupported();
		if (isLongCmdSupported == null) {
			return ret;
		}
		if (this._cfg_readerSupportsCommand) {
			ACLog.i(this.TAG, "Stability test: for baud: " + baud);
			config.setBaudRate(baud);
			this._ioManager.setConfig(config);
			r = this.recordPlayDecode(Common.makeSetBaudCommand(config), 2.0);
			if (r.isCanceledOrFailed()) {
				return ret;
			}
		} else {
			config.setBaudRate(9600);
			this._ioManager.setConfig(config);
			if (this.safeWait(2.0)) {
				return ret;
			}
		}
		int k = 1;
		while (k <= 4) {
			ACLog.i(this.TAG, "Stability test: trial " + k);
			if (this._cfg_readerSupportsCommand) {
				r = this.recordPlayDecode(isLongCmdSupported != false ? this.cmd_getLong : this.cmd_getSettings, 5.0);
			} else {
				this._tonePlayer.setPlayingTone(ToneType.T_2400Hz);
				r = this.recordPlayDecode(null, 2.5);
				this._tonePlayer.setPlayingTone(null);
			}
			if (r.isCanceledOrFailed()) {
				return ret;
			}
			boolean parsed = this._cfg_readerSupportsCommand && isLongCmdSupported.booleanValue() ? (Common.ConnectedReader == ReaderType.UNIJACK ? r.isParsed() && r.matches(this.resp_long_unijack) : r.isParsed() && r.matches(this.resp_long)) : (r != null && r.data != null && r.data.get(0) != null && r.data.get(0).length > 0 ? r.isParsed() && r.data.get(0)[0] != 21 : false);
			if (!parsed) {
				ACLog.i(this.TAG, "Stability test: failed");
				ret.config = null;
				ret.isCanceled = false;
				return ret;
			}
			if (this.safeWait(1.0)) {
				return ret;
			}
			++k;
		}
		ACLog.i(this.TAG, "Stability test: passed");
		ret.config = config;
		ret.isCanceled = false;
		return ret;
	}

	private static class FindParamRet {
		public boolean isCanceled;
		public ConfigParameters config = null;

		private FindParamRet() {
		}
	}

	private static class ProgressContext {
		public int[] indexWeights;
		public int totalSteps;
		public int percentStart;
		public int percentEnd;

		private ProgressContext() {
		}
	}
}