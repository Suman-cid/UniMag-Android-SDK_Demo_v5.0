package com.idtechproducts.acom.io;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;

import com.idtechproducts.acom.ACLog;
import com.idtechproducts.acom.AcomManagerMsg;
import com.idtechproducts.acom.Common;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import idtech.msr.xmlmanager.ConfigParameters;

public class IOManager
		implements RWaveParser.Client {
	private static final String TAG = "IOMan";
	private final Context _context;
	private final TonePlayer _tonePlayer;
	private final Player _player;
	private final Recorder _recorder;
	private final RWaveParser _rWaveParser;
	private String _rWaveParserLogPrefix;
	private String logWavFilePrefix = "IDT_UniMag_Log_Wave_";
	private String logTextFilePrefix = "IDT_UniMag_Log_Txt_";

	private ConfigParameters _cfg_config;

	private boolean _cfg_logging;

	private List<byte[]> _rpd_parseResults;
	private RPDClient _rpd_client;

	public IOManager(Context context, AcomManagerMsg.CommandEncoder commandEncoder) {
		if ((context == null) || (commandEncoder == null)) {
			throw new NullPointerException();
		}
		_context = context;

		_tonePlayer = new TonePlayer();

		_player = new Player(commandEncoder);
		_recorder = new Recorder(context);


		_rWaveParser = new RWaveParser(this);
		_rWaveParser.setLog(false, false);
		_rWaveParserLogPrefix = (Common.getDir_externalOrSandbox(context).getAbsolutePath() + "/" + logWavFilePrefix);

		_cfg_config = null;
		_cfg_logging = false;
	}


	public void release() {
		_tonePlayer.release();

		_player.release();
		_recorder.release();
	}


	public void setConfig(ConfigParameters config) {
		if (config == null) {
			_cfg_config = null;
			return;
		}

		_cfg_config = config.clone();

		_tonePlayer.setConfig(_cfg_config);

		_player.setConfig(config);

		_recorder.setConfig(config);

		_rWaveParser.setSamplingRate(config.getFrequenceInput());
		_rWaveParser.setBaud(config.getBaudRate());


		if (Build.MODEL.equalsIgnoreCase("samsung-sgh-i337"))
			_rWaveParser.setSdThresholdResolve(true, false);
	}

	public ConfigParameters getConfigCopy() {
		return _cfg_config != null ? _cfg_config.clone() : null;
	}

	public void setSaveLog(boolean enabled) {
		_cfg_logging = enabled;

		_rWaveParser.setLog(true, false);
	}

	public int deleteLogs() {
		int r = 0;
		File logDir = new File(_rWaveParserLogPrefix).getParentFile();
		if ((logDir != null) && (logDir.isDirectory())) {
			File[] loglist = logDir.listFiles();
			for (int i = 0; (loglist != null) && (i < loglist.length); i++) {
				String name = loglist[i].getName();
				if ((name.endsWith(".wav")) && (name.startsWith(logWavFilePrefix))) {
					loglist[i].delete();
					r++;
				} else if ((name.endsWith(".txt")) && (name.startsWith(logTextFilePrefix))) {
					loglist[i].delete();
					r++;
				} else if ((name.endsWith(".data")) && (name.startsWith("AutoConfig"))) {
					loglist[i].delete();
					r++;
				}
			}
		}

		return r;
	}


	public void setDeviceMediaVolumeToMax() {
		int minus = _cfg_config != null ? _cfg_config.getVolumeLevelAdjust() : 0;
		setDeviceMediaVolumeToMaxMinusArg(minus);
	}

	public void setDeviceMediaVolumeToMaxMinusArg(int minus) {
		AudioManager audioManager = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);
		int max = audioManager.getStreamMaxVolume(3);


		int newVolume = max - (minus > 0 ? minus : 0);


		int currentVol = audioManager.getStreamVolume(3);
		if (currentVol != newVolume) {
			audioManager.setStreamVolume(3, newVolume, 0);
		}
	}

	public boolean setDeviceMediaVolumeAutoConfig() {
		int minus = _cfg_config != null ? _cfg_config.getVolumeLevelAdjust() : 0;
		AudioManager audioManager = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);
		int max = audioManager.getStreamMaxVolume(3);


		int newVolume = max - (minus > 0 ? minus : 0);


		int currentVol = audioManager.getStreamVolume(3);
		if (currentVol != newVolume) {
			audioManager.setStreamVolume(3, newVolume, 0);
		}
		int count = 0;
		currentVol = audioManager.getStreamVolume(3);
		if (newVolume == currentVol) {
			return true;
		}
		while ((currentVol != newVolume) && (count < 2)) {
			try {
				Thread.sleep(3000L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			audioManager.setStreamVolume(3, newVolume, 0);
			currentVol = audioManager.getStreamVolume(3);
			count++;
		}

		if (currentVol != newVolume) {
			ACLog.i("IOMan", "Volume level cannot be adjusted");
			return false;
		}

		return true;
	}

	public boolean setDeviceMediaVolumeToMaxMinusArgAutoConfig(int minus) {
		AudioManager audioManager = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);
		int max = audioManager.getStreamMaxVolume(3);


		int newVolume = max - (minus > 0 ? minus : 0);


		int currentVol = audioManager.getStreamVolume(3);
		if (currentVol != newVolume) {
			audioManager.setStreamVolume(3, newVolume, 0);
		}
		int count = 0;
		currentVol = audioManager.getStreamVolume(3);
		if (newVolume == currentVol) {
			return true;
		}
		while ((currentVol != newVolume) && (count < 2)) {
			try {
				Thread.sleep(3000L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			audioManager.setStreamVolume(3, newVolume, 0);
			currentVol = audioManager.getStreamVolume(3);
			count++;
		}

		if (currentVol != newVolume) {
			ACLog.i("IOMan", "Volume level cannot be adjusted");
			return false;
		}

		return true;
	}


	public TonePlayer getTonePlayer() {
		return _tonePlayer;
	}

	public RPDResult recordPlayDecode(RPDClient rproc, byte[] cmdString, double waitLen, boolean recordAfterPlay, boolean maximizeVolumeAfterPlay) throws Throwable {
		boolean flag_haslowLevelFailure;
		boolean flag_isCanceled;
		boolean flag_hasTimedOut;
		block39:
		{
			if (this._cfg_config.getReverseAudioEvents()) {
				recordAfterPlay = true;
			}
			if (this._cfg_config == null) {
				throw new IllegalStateException("not configured");
			}
			flag_isCanceled = rproc.isCanceled();
			flag_haslowLevelFailure = false;
			flag_hasTimedOut = false;
			boolean dbgFlag_hasRecordedSomething = false;
			boolean cfg_releaseRecorderAfterEachUse = true;

			if (!flag_isCanceled) {
				block38:
				{
					try {
						try {
							this._recorder.setConfig(this._cfg_config);
							if (!recordAfterPlay) {
								this._recorder.start();
							}
							if (cmdString != null) {
								if (this._cfg_config.getForceHeadsetPlug() == 1) {
									try {
										Thread.sleep(700L);
									} catch (InterruptedException ex) {
										ACLog.w(TAG, "wait before play interrupted");
									}
								}
								this._player.startPlayingCommand(cmdString);
								if (maximizeVolumeAfterPlay) {
									this.setDeviceMediaVolumeToMax();
								}
							}
							if (recordAfterPlay) {
								try {
									Thread.sleep(50L);
								} catch (InterruptedException ex) {
									ACLog.w(TAG, "wait before play interrupted");
								}
								this._recorder.start();
							}
							BlockingQueue<short[]> recQ = this._recorder.getDataQueue();
							this._rWaveParser.purge();
							if (this._cfg_logging) {
								String fileName = String.valueOf(this._rWaveParserLogPrefix) + new SimpleDateFormat("MM.dd_HH-mm-ss", Locale.US).format(new Date());
								this._rWaveParser.setLogFilePrefix(fileName);
								ACLog.i(TAG, "saving wave at: " + fileName);
							}
							boolean checkTimeout = waitLen > 0.0;
							long timeLimit = (long) (waitLen * 1000.0);
							long timeBegin = System.currentTimeMillis();
							this._rpd_parseResults = new ArrayList<byte[]>();
							this._rpd_client = rproc;
							do {
								short[] chunk = null;
								do {
									if (rproc.isCanceled()) {
										flag_isCanceled = true;
										break block38;
									}
									if (checkTimeout && System.currentTimeMillis() - timeBegin > timeLimit) {
										flag_hasTimedOut = true;
										break block38;
									}
									try {
										chunk = recQ.poll(250L, TimeUnit.MILLISECONDS);
									} catch (InterruptedException interruptedException) {
										// empty catch block
									}
								} while (chunk == null);
								dbgFlag_hasRecordedSomething = true;
								if (rproc.isCanceled()) {
									flag_isCanceled = true;
								} else {
									this._rWaveParser.parseWaveData(chunk, chunk.length);
									if (this._rpd_parseResults.isEmpty()) continue;
									boolean shouldStop = rproc.processResponse(this._rpd_parseResults);
									if (!shouldStop) {
										this._rpd_parseResults.clear();
										continue;
									}
								}
								break;
							} while (true);
						} catch (Exception ex) {
							ACLog.e(TAG, ex.toString());
							flag_haslowLevelFailure = true;
							if (!dbgFlag_hasRecordedSomething) {
								ACLog.e(TAG, "No audio was recorded");
							}
							this._recorder.stop();
							this._recorder.release();
							this._player.stopCommand();
							if (this._cfg_logging) {
								this._rWaveParser.saveLoggedData();
							}
							break block39;
						}
					} catch (Throwable throwable) {
						if (!dbgFlag_hasRecordedSomething) {
							ACLog.e(TAG, "No audio was recorded");
						}
						this._recorder.stop();
						this._recorder.release();
						this._player.stopCommand();
						if (this._cfg_logging) {
							this._rWaveParser.saveLoggedData();
						}

						throw throwable;
					}
				}
				if (!dbgFlag_hasRecordedSomething) {
					ACLog.e(TAG, "No audio was recorded");
				}
				this._recorder.stop();
				this._recorder.release();
				this._player.stopCommand();
				if (this._cfg_logging) {
					this._rWaveParser.saveLoggedData();
				}
			}
		}
		RPDResult ret = new RPDResult();
		ret.packetDetected = this._rWaveParser.isPacketDetected();
		ret.data = null;
		if (flag_isCanceled) {
			ret.status = RPDStatus.CANCELED;
		} else if (flag_hasTimedOut) {
			ret.status = RPDStatus.TIMEDOUT;
		} else if (flag_haslowLevelFailure || this._rpd_parseResults.isEmpty()) {
			ret.status = RPDStatus.FAILED;
		} else {
			ret.status = RPDStatus.PARSED;
			ret.packetDetected = true;
			ret.data = this._rpd_parseResults;
		}
		this._rpd_parseResults = null;
		this._rpd_client = null;
		return ret;
	}


	public boolean processParsedData(byte[] response) {
		_rpd_parseResults.add(response);
		return true;
	}

	public void soundDetected() {
		_rpd_client.processSound();
	}

	public static enum RPDStatus {
		PARSED,
		TIMEDOUT,
		CANCELED,
		FAILED;
	}


	public static abstract interface RPDClient {
		public abstract boolean isCanceled();

		public abstract boolean processResponse(List<byte[]> paramList);

		public abstract void processSound();
	}

	public static class RPDResult {
		public IOManager.RPDStatus status;
		public boolean packetDetected;
		public List<byte[]> data;
		public byte[] lastMatch;

		public RPDResult() {
		}

		public boolean isParsed() {
			return status == IOManager.RPDStatus.PARSED;
		}

		public boolean isTimedOut() {
			return status == IOManager.RPDStatus.TIMEDOUT;
		}

		public boolean isCanceledOrFailed() {
			return (status == IOManager.RPDStatus.CANCELED) || (status == IOManager.RPDStatus.FAILED);
		}


		public boolean matches(String pattern) {
			if (data == null) {
				throw new RuntimeException("no parsed data");
			}
			for (byte[] ba : data) {
				if (Common.bytesMatch(ba, pattern)) {
					lastMatch = ba;
					return true;
				}
			}

			return false;
		}

		public boolean matches(byte[] byteString) {
			if (data == null) {
				throw new RuntimeException("no parsed data");
			}
			for (byte[] ba : data) {
				if (Arrays.equals(ba, byteString)) {
					lastMatch = ba;
					return true;
				}
			}
			return false;
		}

		public boolean containsStr(String str) {
			if (data == null) {
				throw new RuntimeException("no parsed data");
			}
			for (byte[] ba : data) {
				if (Common.containsStr(ba, str)) {
					lastMatch = ba;
					return true;
				}
			}
			return false;
		}
	}
}