package com.idtechproducts.acom.io;

import android.media.AudioTrack;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.idtechproducts.acom.ACLog;
import com.idtechproducts.acom.Common;

import idtech.msr.xmlmanager.ConfigParameters;

public class TonePlayer {
	private static final String TAG = "TonePlayer";
	private static final Handler mainHandler = new Handler(Looper.getMainLooper());

	private ConfigParameters config;

	private ToneType tone_state;
	private AudioTrack tone_at = null;
	private ToneType tone_at_track = null;

	public TonePlayer() {
	}

	private static AudioTrack getToneAudioTrack(ConfigParameters config, int squareWaveFreq) {
		int outputFreq = config.getFrequenceOutput();
		int outputWaveDirection = config.getDirectionOutputWave();

		byte[] toneDataPeriod = Common.synthesizeStereoSquareWavePeriod(
				squareWaveFreq, outputFreq, outputWaveDirection);


		double staticAudioLenSec = 0.1D;
		int periodCount = (int) (outputFreq * 0.1D / (
				toneDataPeriod.length / 2));


		byte[] toneDataRepeated = new byte[toneDataPeriod.length * periodCount];
		for (int i = 0; i < periodCount; i++) {
			System.arraycopy(toneDataPeriod, 0,
					toneDataRepeated, i * toneDataPeriod.length, toneDataPeriod.length);
		}

		AudioTrack ret = new AudioTrack(
				3,
				outputFreq,
				12,
				3,
				toneDataRepeated.length,
				0);

		if (ret.write(toneDataRepeated, 0, toneDataRepeated.length) != toneDataRepeated.length) {
			ret.release();
			throw new RuntimeException();
		}


		if (ret.setLoopPoints(0, toneDataRepeated.length / 2, -1) != 0) {
			ret.release();
			throw new RuntimeException();
		}


		if (ret.getState() != 1) {
			throw new RuntimeException();
		}
		return ret;
	}

	public synchronized void release() {
		if (tone_state != null) {
			mainHandler.post(new Runnable() {
				public void run() {
					TonePlayer.this.releaseAudioTrack();
				}
			});
			tone_state = null;
		}
	}

	public synchronized void setConfig(ConfigParameters newConfig) {
		if (newConfig == null) {
			throw new NullPointerException();
		}

		boolean isPlayerParamEqual =
				(config != null) &&
						(config.getFrequenceOutput() == newConfig.getFrequenceOutput()) &&
						(config.getDirectionOutputWave() == newConfig.getDirectionOutputWave());


		config = newConfig.clone();


		if (!isPlayerParamEqual) {
			ToneType originallyPlaying = tone_state;
			release();
			setPlayingTone(originallyPlaying);
		}
	}

	public synchronized ToneType getPlayingTone() {
		return tone_state;
	}

	public synchronized void setPlayingTone(final ToneType newTone) {
		if (tone_state == newTone) {
			return;
		}

		if ((newTone != null) && (config == null)) {
			throw new IllegalStateException("no config set");
		}

		final ConfigParameters config_copy = config;
		mainHandler.post(new Runnable() {
			public void run() {
				if (newTone == null) {
					tone_at.pause();

				} else {
					if (tone_at_track != newTone) {
						TonePlayer.this.releaseAudioTrack();
					}

					if (tone_at == null) {
						tone_at = TonePlayer.getToneAudioTrack(config_copy, newTone == ToneType.T_2000Hz ? 2000 : 2400);
						tone_at_track = newTone;
					}


					if (Build.MODEL.equalsIgnoreCase("SM-T210R"))
						try {
							Thread.sleep(1000L);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					tone_at.play();
				}
			}
		});
		tone_state = newTone;
	}

	private void releaseAudioTrack() {
		if (tone_at != null) {
			tone_at.stop();
			tone_at.release();
			tone_at = null;
			tone_at_track = null;
			ACLog.i("TonePlayer", "AudioTrack released");
		}
	}
}