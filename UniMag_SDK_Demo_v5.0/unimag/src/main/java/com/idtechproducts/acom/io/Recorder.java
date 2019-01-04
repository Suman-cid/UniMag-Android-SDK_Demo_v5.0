package com.idtechproducts.acom.io;

import android.content.Context;
import android.content.Intent;
import android.media.AudioRecord;
import android.os.Build;

import com.idtechproducts.acom.ACLog;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import idtech.msr.xmlmanager.ConfigParameters;

class Recorder {
	private static final String TAG = "Recorder";
	private final Context _context;
	private final BlockingQueue<short[]> _dataQueue = new LinkedBlockingQueue();
	private boolean _state_isRecording = false;
	private volatile boolean _state_keepRunning = false;
	private ConfigParameters _config;
	private AudioRecord _recorder;
	private Thread _thread;


	public Recorder(Context context) {
		if (context == null)
			throw new NullPointerException();
		_context = context;
	}


	public void release() {
		stop();

		if (_recorder != null) {
			_recorder.release();
			_recorder = null;
		}
	}


	public void setConfig(ConfigParameters newConfig) {
		if (_state_isRecording)
			throw new IllegalStateException("cannot set config while recording");
		if (newConfig == null) {
			throw new NullPointerException();
		}
		boolean isRecordParamEqual =
				(_config != null) &&
						(_config.getFrequenceInput() == newConfig.getFrequenceInput()) &&
						(_config.getRecordReadBufferSize() == newConfig.getRecordReadBufferSize()) &&
						(_config.getRecordBufferSize() == newConfig.getRecordBufferSize()) &&
						(_config.getUseVoiceRecognition() == newConfig.getUseVoiceRecognition());


		if (!isRecordParamEqual) {
			release();
		}

		_config = newConfig.clone();
	}

	public void start()
			throws IllegalStateException {
		if (_config == null)
			throw new IllegalStateException("Recorder has no config");
		if (_state_isRecording) {
			throw new IllegalStateException("already recording");
		}

		boolean vrAvailable = Build.VERSION.SDK_INT >= 7;
		int source_mic = 1;
		int source_vr = vrAvailable ? 6 : 1;

		int ar_audioSource = _config.getUseVoiceRecognition() == 0 ?
				1 : source_vr;
		int ar_sampleRate = _config.getFrequenceInput();
		int ar_channelConfig = 16;
		int ar_audioFormat = 2;
		int ar_bufferSize = _config.getRecordReadBufferSize();
		if (ar_bufferSize <= 0) {
			ar_bufferSize = AudioRecord.getMinBufferSize(ar_sampleRate, ar_channelConfig, ar_audioFormat) * 4;
		}


		if (_recorder == null) {
			try {
				_recorder = new AudioRecord(
						ar_audioSource, ar_sampleRate, ar_channelConfig, ar_audioFormat, ar_bufferSize);
			} catch (IllegalArgumentException e) {
				throw new RuntimeException("error instantiating AudioRecord");
			}

			if (_recorder.getState() != 1) {
				release();
				throw new RuntimeException("error instantiating AudioRecord");
			}
		}

		_recorder.startRecording();

		if (_recorder.getRecordingState() != 3) {
			release();
			ACLog.i("Recorder", "error starting audiorecord");
			throw new RuntimeException("error starting AudioRecord");
		}

		broadcastHeadsetPlugEvent(_config);

		_state_isRecording = true;

		final BlockingQueue<short[]> queue = _dataQueue;
		final AudioRecord ar = _recorder;
		int shovelSize_byte =
				_config.getRecordBufferSize() > 0 ? _config.getRecordBufferSize() : ar_bufferSize / 2;
		final int shovelSize_elements = shovelSize_byte / 2;

		_dataQueue.clear();
		_state_keepRunning = true;

		_thread = new Thread() {
			public void run() {
				while (_state_keepRunning) {
					short[] shovel = new short[shovelSize_elements];
					int elementsInShovel = 0;

					while (elementsInShovel < shovel.length) {
						int elementsRead = ar.read(shovel, elementsInShovel, shovel.length - elementsInShovel);

						if (!_state_keepRunning)
							break;

						if (elementsRead < 0) {
							ACLog.e("Recorder", "reading thread: read() returned error code " + elementsRead);
						} else elementsInShovel += elementsRead;
					}

					if (queue.size() > 100) {
						queue.remove();
						ACLog.w("Recorder", "queue overflowing");
					}

					queue.add(shovel);
				}
				ar.stop();
			}
		};
		_thread.setName("UMRecorder");
		_thread.start();
	}


	public BlockingQueue<short[]> getDataQueue() {
		return _dataQueue;
	}

	public void stop() {
		_state_keepRunning = false;

		if (_thread != null) {
			while (_thread.isAlive()) {
				try {
					_thread.join();
				} catch (InterruptedException localInterruptedException) {
				}
			}
			_thread = null;
		}
		_state_isRecording = false;
	}


	private void broadcastHeadsetPlugEvent(ConfigParameters config) {
		if ((config.getForceHeadsetPlug() == 1) && (Build.VERSION.SDK_INT == 8)) {
			Intent intent = new Intent("android.intent.action.HEADSET_PLUG");
			intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
			intent.putExtra("state", 1);
			intent.putExtra("microphone", 1);


			intent.putExtra("false_event", 1);
			_context.sendBroadcast(intent);
		}
	}
}