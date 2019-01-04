package com.idtechproducts.acom;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.idtechproducts.acom.io.IOManager;
import com.idtechproducts.acom.tasks.AutoConfigTask;
import com.idtechproducts.acom.tasks.Task;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

import idtech.msr.xmlmanager.ConfigParameters;

public class AcomManager {
	private static final String TAG = "SDK";
	private static final String DEVICE_MANUFACTURER = Build.MANUFACTURER.toLowerCase(Locale.US).replaceAll("\\s*", "");
	private static final String DEVICE_MODEL = Build.MODEL.toLowerCase(Locale.US).replaceAll("\\s*", "");
	private final AcomManagerMsg _acomMsg;
	private final Context _context;
	private final BroadcastReceiver _listener;
	private final IOManager _ioManager;
	private volatile boolean _state_isListenerRegistered;
	private volatile boolean _state_isAttached;
	private volatile Task _state_task;
	private volatile Thread _state_task_thread;
	private ConfigParameters _cfg_config;
	private String _cfg_loadedXmlVersion;

	public AcomManager(AcomManagerMsg acomMsg, AcomManagerMsg.CommandEncoder commandEncoder, Context context) {
		if ((acomMsg == null) || (commandEncoder == null) || (context == null)) {
			throw new NullPointerException();
		}
		this._state_isListenerRegistered = false;
		this._state_isAttached = false;
		this._state_task = null;
		this._state_task_thread = null;

		this._acomMsg = acomMsg;
		this._context = context;
		this._listener = new UMListener();
		this._ioManager = new IOManager(context, commandEncoder);

		this._cfg_config = null;
		this._cfg_loadedXmlVersion = null;

		ACLog.i("SDK", "initialized");
	}

	public void release() {
		ACLog.i("SDK", "un-initializing");

		task_stop();
		if (this._ioManager != null) {
			this._ioManager.release();
		}
		listener_unregister();

		ACLog.close();

		this._state_isListenerRegistered = false;
		this._state_isAttached = false;
		this._state_task = null;
		this._state_task_thread = null;

		this._cfg_config = null;
		this._cfg_loadedXmlVersion = null;
	}

	public void listener_register() {
		if (this._state_isListenerRegistered) {
			return;
		}
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.intent.action.HEADSET_PLUG");
		filter.addAction("android.intent.action.MEDIA_BUTTON");
		filter.addAction("android.media.AUDIO_BECOMING_NOISY");

		this._context.registerReceiver(this._listener, filter);

		listener_mediaButtonReceiver(true);

		this._state_isListenerRegistered = true;
		ACLog.i("SDK", "registered broadcast listener");

		AcomManager.this._state_isAttached = true;
		AcomManager.this._acomMsg.onAttachmentChange(true);
	}

	public void listener_unregister() {
		if (!this._state_isListenerRegistered) {
			return;
		}
		task_stop();

		this._context.unregisterReceiver(this._listener);

		listener_mediaButtonReceiver(false);

		this._state_isAttached = false;

		this._state_isListenerRegistered = false;
		ACLog.i("SDK", "un-registered broadcast listener");
	}

	private void listener_mediaButtonReceiver(boolean isRegistering) {
		if (Build.VERSION.SDK_INT >= 8) {
			AudioManager am = (AudioManager) this._context.getSystemService(Context.AUDIO_SERVICE);
			Exception ex = null;
			try {
				Method regReceiver = AudioManager.class.getMethod(
						isRegistering ? "registerMediaButtonEventReceiver" : "unregisterMediaButtonEventReceiver",
						new Class[]{ComponentName.class});
				regReceiver.invoke(am, new Object[]{new ComponentName(this._context, this._listener.getClass())});
			} catch (SecurityException e) {
				ex = e;
			} catch (NoSuchMethodException e) {
				ex = e;
			} catch (IllegalArgumentException e) {
				ex = e;
			} catch (IllegalAccessException e) {
				ex = e;
			} catch (InvocationTargetException e) {
				ex = e;
			}
			if (ex != null) {
				ACLog.w("SDK", (isRegistering ? "register" : "unregister") + " media button receiver failed: " + ex);
			}
		}
	}

	public boolean getState_isAttached() {
		return this._state_isAttached;
	}

	public Task.TaskType getState_getRunningTask() {
		return this._state_task != null ?
				this._state_task.getType() :
				null;
	}

	public String getInfo_Manufacturer() {
		return DEVICE_MANUFACTURER;
	}

	public String getInfo_Model() {
		return DEVICE_MODEL;
	}

	public String getInfo_loadedXmlVersion() {
		return this._cfg_loadedXmlVersion;
	}

	public IOManager getIntern_IOManager() {
		return this._ioManager;
	}

	public ConfigParameters getIntern_ConfigParameters() {
		return this._cfg_config;
	}

	public AcomManagerMsg getIntern_AcomManagerMsg() {
		return this._acomMsg;
	}

	public boolean setCfg_config(ConfigParameters config, String loadedXmlVersion) {
		if (this._state_task != null) {
			ACLog.w("SDK", "setting config: config not set, SDK busy running task");
			return false;
		}
		this._cfg_config = config.clone();
		this._ioManager.setConfig(this._cfg_config);
		this._cfg_loadedXmlVersion = loadedXmlVersion;
		return true;
	}

	public void log_setEnableVerbose(boolean enable) {
		ACLog.setEnableVerbose(enable);
	}

	public void log_setEnableSave(boolean enable) {
		if (enable) {
			File dir = Common.getDir_externalOrSandbox(this._context);
			ACLog.open(dir, log_getPrefix());
		} else {
			ACLog.close();
		}
		this._ioManager.setSaveLog(enable);
	}

	public int log_delete() {
		int ta = ACLog.deleteLogs(this._context.getFilesDir(), log_getPrefix());
		int ts = 0;
		if (Common.isStorageExist()) {
			ts = ACLog.deleteLogs(new File(Common.getSDRootFilePath()), log_getPrefix());
		}
		int w = this._ioManager.deleteLogs();
		return ta + ts + w;
	}

	private String log_getPrefix() {
		return "IDT_UniMag_Log_Txt_";
	}

	public void task_start_autoConfig(String strXMLFilename, boolean readerSupportsCommand) {
		if (!this._state_isAttached) {
			throw new IllegalStateException("cannot start AutoConfig: no reader attached");
		}
		List<ConfigParameters> templates = null;
		if (strXMLFilename != null) {
			AcomXmlParser.UMXmlParseResult parseResult =
					AcomXmlParser.parseFile(strXMLFilename, null, null, true, false);
			if (parseResult.fileExists) {
				if (parseResult.templates != null) {
					templates = parseResult.templates;
				} else {
					ACLog.w("SDK", "AutoConfig: templates not loaded: file parsing failed");
				}
			} else {
				ACLog.w("SDK", "AutoConfig: templates not loaded: cannot open file");
			}
		}
		task_setAndStart(new AutoConfigTask(this, readerSupportsCommand, templates));
	}

	public void task_setAndStart(Task task) {
		if (this._state_task != null) {
			throw new IllegalStateException("no Task should be running when starting a new Task");
		}
		this._state_task = task;
		this._state_task_thread = new Thread(task);

		this._state_task_thread.setName("UMTask_" + this._state_task.getType().toString());
		this._state_task_thread.start();
	}

	public void task_stop() {
		if (this._state_task == null) {
			return;
		}
		this._state_task.cancel();
		while ((this._state_task_thread != null) && (this._state_task_thread.isAlive())) {
			try {
				this._state_task_thread.join();
			} catch (InterruptedException localInterruptedException) {
			}
		}
		this._state_task = null;
		this._state_task_thread = null;
	}

	public void task_signalStoppedStatus() {
		Handler h = new Handler(Looper.getMainLooper());
		h.post(new Runnable() {
			public void run() {
				AcomManager.this._state_task = null;
				AcomManager.this._state_task_thread = null;
			}
		});
	}

	private class UMListener
			extends BroadcastReceiver {
		private UMListener() {
		}

		public void onReceive(Context context, Intent intent) {
			String act = intent.getAction();
			if (act.equals("android.intent.action.MEDIA_BUTTON")) {
				abortBroadcast();
			}
			if (act.equals("android.intent.action.HEADSET_PLUG")) {
				int invalidValue = 64537;
				int iPlugState = intent.getIntExtra("state", 64537);
				int iFalseEvent = intent.getIntExtra("false_event", 64537);
				if (iFalseEvent == 64537) {
					if (iPlugState == 64537) {
						ACLog.w("SDK", "headset plug broadcast: failed to determine plug state");
						processAttachmentStateChange(false);
					} else if (iPlugState == 0) {
						processAttachmentStateChange(false);
					} else {
						processAttachmentStateChange(true);
					}
				}
			}
			if (act.equals("android.media.AUDIO_BECOMING_NOISY")) {
				processAttachmentStateChange(false);
			}
		}

		private void processAttachmentStateChange(boolean newState) {
			if (AcomManager.this._state_isAttached == newState) {
				return;
			}
			ACLog.i("SDK", "headset " + (newState ? "attached" : "detached"));
			AcomManager.this._state_isAttached = newState;
			if (!newState) {
				AcomManager.this.task_stop();
			}
			AcomManager.this._acomMsg.onAttachmentChange(newState);
		}
	}
}
