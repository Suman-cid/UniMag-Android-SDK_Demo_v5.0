package idtech.msr.unimag;


import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;

import com.idtechproducts.acom.ACLog;
import com.idtechproducts.acom.AcomManager;
import com.idtechproducts.acom.AcomManagerMsg;
import com.idtechproducts.acom.io.ToneType;
import com.idtechproducts.acom.tasks.Task;
import com.idtechproducts.unimagsdk.SdkCustomization;
import com.idtechproducts.unimagsdk.UmCommandEncoder;
import com.idtechproducts.unimagsdk.UniJackCommandEncoder;
import com.idtechproducts.unimagsdk.UniMagConfigHelper;
import com.idtechproducts.unimagsdk.task.ClearBufferTask;
import com.idtechproducts.unimagsdk.task.CommandTask;
import com.idtechproducts.unimagsdk.task.CommandTestTask;
import com.idtechproducts.unimagsdk.task.ConnectTask;
import com.idtechproducts.unimagsdk.task.FwGetChallengeTask;
import com.idtechproducts.unimagsdk.task.FwSendPowerTask;
import com.idtechproducts.unimagsdk.task.FwUpdateTask;
import com.idtechproducts.unimagsdk.task.FwUpdateTaskUniJack;
import com.idtechproducts.unimagsdk.task.GetReaderTypeTask;
import com.idtechproducts.unimagsdk.task.SwipeAckTask;
import com.idtechproducts.unimagsdk.task.SwipeCancelTask;
import com.idtechproducts.unimagsdk.task.SwipeTask;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;

import idtech.msr.unimag.unimagtools.uniMagReaderToolsMsg;
import idtech.msr.xmlmanager.ConfigParameters;
import idtech.msr.xmlmanager.ReaderType;
import idtech.msr.xmlmanager.StructConfigParameters;

public class uniMagReader {
	private static final String TAG = "SDK";
	private static final int SDK_VER_MAJOR = 5;
	private static final int SDK_VER_MINOR = 0;
	private static final String SDK_VER_STRING = "UniMag SDK Ver 5.0";
	private static int _swipe_error_counter = 0;
	private final uniMagReaderMsg _umrMsg;
	private final AcomManager _umMan;
	private final UniMagConfigHelper _configHelper;
	private final TaskExport _taskExport;
	private final FwExport _fwExport;
	private volatile boolean _state_isConnected;
	private volatile ReaderType _state_readerType;
	private boolean _cfg_connectReaderWithCommand;
	private double _cfg_swipeTimeoutSec;

	public uniMagReader(uniMagReaderMsg callback, Context contex) {
		this(callback, contex, SdkCustomization.CUST != 0);
	}

	public uniMagReader(uniMagReaderMsg callback, Context context, ReaderType type) {
		this._umrMsg = callback;
		this._cfg_swipeTimeoutSec = 20.0D;
		this._state_readerType = type;
		switch (this._state_readerType) {
			case UM:
			case UM_II:
			case UM_OR_PRO:
				this._umMan = new AcomManager(new UmAcomMsgImpl(null), new UmCommandEncoder(), context);
				this._cfg_connectReaderWithCommand = false;
				com.idtechproducts.acom.Common.ConnectedReader = ReaderType.UM_OR_PRO;
				this._state_readerType = null;
				break;
			case UM_PRO:
			case UNIJACK:
				this._umMan = new AcomManager(new UmAcomMsgImpl(null), new UmCommandEncoder(), context);
				this._cfg_connectReaderWithCommand = true;
				com.idtechproducts.acom.Common.ConnectedReader = ReaderType.SHUTTLE;
				this._state_readerType = null;
				break;
			case UNKNOWN:
				this._umMan = new AcomManager(new UmAcomMsgImpl(null), new UniJackCommandEncoder(), context);
				this._cfg_connectReaderWithCommand = true;
				com.idtechproducts.acom.Common.ConnectedReader = ReaderType.UNIJACK;
				this._cfg_swipeTimeoutSec = 10.0D;
				break;
			default:
				this._umMan = new AcomManager(new UmAcomMsgImpl(null), new UmCommandEncoder(), context);
				this._cfg_connectReaderWithCommand = false;
				com.idtechproducts.acom.Common.ConnectedReader = ReaderType.UNKNOWN;
		}
		this._configHelper = new UniMagConfigHelper(this._umrMsg, context);
		this._taskExport = new TaskExport(null);
		this._fwExport = new FwExport();

		this._state_isConnected = false;

		log_printAbout();
	}

	public uniMagReader(uniMagReaderMsg callback, Context contex, boolean enableConnectReaderWithCommand) {
		this._umrMsg = callback;

		this._umMan = new AcomManager(new UmAcomMsgImpl(null), new UmCommandEncoder(), contex);

		this._configHelper = new UniMagConfigHelper(this._umrMsg, contex);
		this._taskExport = new TaskExport(null);
		this._fwExport = new FwExport();

		this._state_isConnected = false;
		this._state_readerType = null;

		this._cfg_connectReaderWithCommand = enableConnectReaderWithCommand;
		this._cfg_swipeTimeoutSec = 20.0D;

		log_printAbout();
	}

	public void registerListen() {
		this._umMan.listener_register();
	}

	public void unregisterListen() {
		this._umMan.listener_unregister();
	}

	public void release() {
		this._umMan.release();
	}

	private void saveConfigToFile(ConfigParameters config)
			throws IOException {
		String CONFIG_FILE_NAME = "AutoConfig.data";
		String filePathName = "/sdcard/" + CONFIG_FILE_NAME;

		String storageState = Environment.getExternalStorageState();
		ACLog.e("SDK", storageState + ", " + "mounted");
		if ((!storageState.equalsIgnoreCase("mounted")) || (config == null)) {
			return;
		}
		try {
			File textFile = new File(filePathName);
			Writer output = new BufferedWriter(new FileWriter(textFile));
			String toWrite = config.toXMLString();
			if ((output != null) && (toWrite != null)) {
				output.write(toWrite);
			}
			output.close();
		} catch (FileNotFoundException localFileNotFoundException) {
		} catch (IOException localIOException) {
		}
	}

	private void log_printAbout() {
		ACLog.i("SDK", "UniMag SDK Ver 5.0");
		ACLog.i("SDK", "Device Manufacturer: " + this._umMan.getInfo_Manufacturer());
		ACLog.i("SDK", "Device Model: " + this._umMan.getInfo_Model());
		ACLog.i("SDK", "Android version: " + Build.VERSION.RELEASE);
	}

	private void cxn_setDisconnected() {
		if (!this._state_isConnected) {
			return;
		}
		this._state_isConnected = false;
		this._umMan.getIntern_IOManager().getTonePlayer().setPlayingTone(null);

		this._umrMsg.onReceiveMsgDisconnected();
	}

	private TaskStartRet task_checkStatusAndWarn(TaskStartRet... thingsToCheck) {
		TaskStartRet[] arrayOfTaskStartRet;
		int j = (arrayOfTaskStartRet = thingsToCheck).length;
		for (int i = 0; i < j; i++) {
			TaskStartRet thing = arrayOfTaskStartRet[i];
			switch (thing) {
				case NO_CONFIG:
					if (!this._umMan.getState_isAttached()) {
						ACLog.w("SDK", "Task not started: Reader not attached");
						return thing;
					}
					break;
				case NO_READER:
					if (this._umMan.getState_getRunningTask() != null) {
						ACLog.w("SDK", "Task not started: SDK busy");
						return thing;
					}
					break;
				case SDK_BUSY:
					if (this._umMan.getIntern_ConfigParameters() == null) {
						ACLog.w("SDK", "Task not started: SDK config not loaded");
						return thing;
					}
					break;
				case SUCCESS:
					if (!this._state_isConnected) {
						ACLog.w("SDK", "Task not started: SDK connection state is disconnected");
						return thing;
					}
					break;
				case NOT_CONNECTED:
					throw new IllegalArgumentException();
			}
		}
		return TaskStartRet.SUCCESS;
	}

	public String getSDKVersionInfo() {
		return "UniMag SDK Ver 5.0";
	}

	public String getXMLVersionInfo() {
		return this._umMan.getInfo_loadedXmlVersion();
	}

	public String getInfoManufacture() {
		return this._umMan.getInfo_Manufacturer();
	}

	public String getInfoModel() {
		return this._umMan.getInfo_Model();
	}

	public void setVerboseLoggingEnable(boolean enable) {
		this._umMan.log_setEnableVerbose(enable);
	}

	public void setSaveLogEnable(boolean enable) {
		this._umMan.log_setEnableSave(enable);
		log_printAbout();
	}

	public int deleteLogs() {
		return this._umMan.log_delete();
	}

	public void WriteLogIntoFile(String msg) {
		ACLog.i("sdkclient", msg);
	}

	public void setXMLFileNameWithPath(String path) {
		this._configHelper.setPathFileName(path);
	}

	public boolean loadingConfigurationXMLFile(boolean updateAutomatically) {
		if (this._configHelper.loadingXMLFile(updateAutomatically)) {
			ConfigParameters cp = this._configHelper.getConfigParams();
			boolean ret = this._umMan.setCfg_config(cp, this._configHelper.getLoadedXmlVersion());
			return ret;
		}
		return false;
	}

	public boolean connect() {
		TaskStartRet taskRet = task_start_connect();
		return TaskStartRet.SUCCESS == taskRet;
	}

	public void disconnect() {
		this._umMan.task_stop();
		cxn_setDisconnected();
	}

	public boolean connectWithProfile(StructConfigParameters profile) {
		ConfigParameters internalProfile = profile.convertConfigParameter();
		if (!this._umMan.setCfg_config(internalProfile, null)) {
			return false;
		}
		TaskStartRet taskRet = task_start_connect();
		return TaskStartRet.SUCCESS == taskRet;
	}

	public void setConnectReaderWithCommand(boolean enableConnectReaderWithCommand) {
		this._cfg_connectReaderWithCommand = enableConnectReaderWithCommand;
	}

	public boolean isReaderConnected() {
		return this._state_isConnected;
	}

	private TaskStartRet task_start_connect() {
		TaskStartRet chk = task_checkStatusAndWarn(new TaskStartRet[]{TaskStartRet.NO_READER, TaskStartRet.SDK_BUSY, TaskStartRet.NO_CONFIG});
		if (chk != TaskStartRet.SUCCESS) {
			return chk;
		}
		if (this._state_isConnected) {
			cxn_setDisconnected();
		}
		this._umMan.task_setAndStart(new ConnectTask(this._taskExport, this._cfg_connectReaderWithCommand));

		this._umrMsg.onReceiveMsgToConnect();

		return TaskStartRet.SUCCESS;
	}

	public boolean startSwipeCard() {
		TaskStartRet chk = task_checkStatusAndWarn(new TaskStartRet[]{TaskStartRet.NO_READER, TaskStartRet.SDK_BUSY, TaskStartRet.NO_CONFIG, TaskStartRet.NOT_CONNECTED});
		if (chk != TaskStartRet.SUCCESS) {
			return false;
		}
		if (this._state_readerType == ReaderType.UNIJACK) {
			this._umMan.task_setAndStart(new SwipeAckTask(this._taskExport));
		} else {
			this._umMan.task_setAndStart(new SwipeTask(this._taskExport, this._cfg_swipeTimeoutSec));

			this._umrMsg.onReceiveMsgToSwipeCard();
		}
		return true;
	}

	public void stopSwipeCard() {
		if (this._umMan.getState_getRunningTask() == Task.TaskType.Swipe) {
			this._umMan.task_stop();
			if ((this._state_readerType == ReaderType.UNIJACK) && (this._state_isConnected)) {
				this._umMan.task_setAndStart(new SwipeCancelTask(this._taskExport));
			}
		}
	}

	public boolean isSwipeCardRunning() {
		return this._umMan.getState_getRunningTask() == Task.TaskType.Swipe;
	}

	public boolean setTimeoutOfSwipeCard(int timeoutValue) {
		if (this._state_readerType == ReaderType.UNIJACK) {
			this._cfg_swipeTimeoutSec = 10.0D;
		} else {
			this._cfg_swipeTimeoutSec = timeoutValue;
		}
		return true;
	}

	public boolean testSwipeCard() {
		return startSwipeCard();
	}

	public void stopTestSwipeCard() {
		stopSwipeCard();
	}

	public boolean sendCommandEnableTDES() {
		return sendCommnad_helper("02534C0131032E", 1);
	}

	public boolean sendCommandEnableAES() {
		return sendCommnad_helper("02534C0132032D", 2);
	}

	public boolean sendCommandClearBuffer() {
		TaskStartRet chk = task_checkStatusAndWarn(new TaskStartRet[]{TaskStartRet.NO_READER, TaskStartRet.SDK_BUSY, TaskStartRet.NO_CONFIG, TaskStartRet.NOT_CONNECTED});
		if (chk != TaskStartRet.SUCCESS) {
			return false;
		}
		this._umMan.task_setAndStart(new ClearBufferTask(this._taskExport));
		return true;
	}

	private boolean sendCommandCalibrate() {
		byte year = (byte) (Calendar.getInstance().get(1) % 100);
		byte week = (byte) Calendar.getInstance().get(3);
		String yearStr = String.format("%02x", new Object[]{Integer.valueOf(0xFF & year)}).toUpperCase();
		String weekStr = String.format("%02x", new Object[]{Integer.valueOf(0xFF & week)}).toUpperCase();
		String command = "02537F02" + yearStr + weekStr + "03";

		String lrc = Common.getLRC(command);

		return sendCommnad_helper(command + lrc, 103);
	}

	public boolean sendCommandGetVersion() {
		return sendCommnad_helper("0252220371", 3);
	}

	public boolean sendCommandGetSettings() {
		return sendCommnad_helper("02521F034C", 4);
	}

	public boolean sendCommandDefaultGeneralSettings() {
		return sendCommnad_helper("025318034A", 6);
	}

	public boolean sendCommandGetSerialNumber() {
		return sendCommnad_helper("02524E031D", 7);
	}

	public boolean sendCommandGetNextKSN() {
		return sendCommnad_helper("0252510302", 8);
	}

	public boolean sendCommandEnableErrNotification() {
		return sendCommnad_helper("0253190134037E", 9);
	}

	public boolean sendCommandDisableErrNotification() {
		return sendCommnad_helper("0253190130037A", 10);
	}

	public boolean sendCommandDisnableErrNotification() {
		return sendCommandDisableErrNotification();
	}

	public boolean sendCommandEnableExpDate() {
		return sendCommnad_helper("02535001310332", 11);
	}

	public boolean sendCommandDisableExpDate() {
		return sendCommnad_helper("02535001300333", 12);
	}

	public boolean sendCommandEnableForceEncryption() {
		return sendCommnad_helper("025384013303E4", 13);
	}

	public boolean sendCommandDisableForceEncryption() {
		return sendCommnad_helper("025384013003E7", 14);
	}

	public boolean sendCommandSetPrePAN(int prePAN) {
		byte v = (byte) prePAN;
		if (v < 0) {
			v = 0;
		}
		if (v > 6) {
			v = 6;
		}
		byte[] cmd = com.idtechproducts.acom.Common.base16Decode("02534901000300");
		cmd[4] = v;
		cmd[6] = ((byte) (v ^ 0x1A));
		TaskStartRet r = task_start_command(cmd, 14);
		return r == TaskStartRet.SUCCESS;
	}

	public boolean sendCommandGetBatteryLevel() {
		if (this._state_readerType != ReaderType.UNIJACK) {
			return false;
		}
		return sendCommnad_helper("02521F034C", 104);
	}

	public boolean testCommand() {
		TaskStartRet chk = task_checkStatusAndWarn(new TaskStartRet[]{TaskStartRet.NO_READER, TaskStartRet.SDK_BUSY, TaskStartRet.NO_CONFIG});
		if (chk != TaskStartRet.SUCCESS) {
			return false;
		}
		this._umMan.task_setAndStart(new CommandTestTask(this._taskExport));
		return true;
	}

	private boolean sendCommnad_helper(String cmd, int cmdID) {
		TaskStartRet r = task_start_command(com.idtechproducts.acom.Common.base16Decode(cmd), cmdID);
		return r == TaskStartRet.SUCCESS;
	}

	private TaskStartRet task_start_command(byte[] command, int commandID) {
		TaskStartRet chk = task_checkStatusAndWarn(new TaskStartRet[]{TaskStartRet.NO_READER, TaskStartRet.SDK_BUSY, TaskStartRet.NO_CONFIG, TaskStartRet.NOT_CONNECTED});
		if (chk != TaskStartRet.SUCCESS) {
			return chk;
		}
		this._umMan.task_setAndStart(new CommandTask(this._taskExport, command, commandID));
		return TaskStartRet.SUCCESS;
	}

	public void setSDKToolProxy(uniMagReaderToolProxy toolProxy) {
	}

	public boolean startAutoConfig(boolean enableOverwriteResult) {
		return startAutoConfig(null, enableOverwriteResult);
	}

	public boolean startAutoConfig(String strXMLFilename, boolean enableOverwriteResult) {
		TaskStartRet chk = task_checkStatusAndWarn(new TaskStartRet[]{TaskStartRet.NO_READER, TaskStartRet.SDK_BUSY});
		if (chk != TaskStartRet.SUCCESS) {
			return false;
		}
		cxn_setDisconnected();

		this._umMan.task_start_autoConfig(strXMLFilename, this._cfg_connectReaderWithCommand);
		return true;
	}

	public void setTestModelForAutoConfig(boolean bTestMode) {
	}

	public void stopAutoConfig() {
		if (this._umMan.getState_getRunningTask() == Task.TaskType.AutoConfig) {
			this._umMan.task_stop();
		}
	}

	public ReaderType getAttachedReaderType() {
		if (this._state_readerType == ReaderType.UNIJACK) {
			return this._state_readerType;
		}
		if (!this._state_isConnected) {
			return null;
		}
		TaskStartRet chk = task_checkStatusAndWarn(new TaskStartRet[]{TaskStartRet.NO_READER, TaskStartRet.SDK_BUSY, TaskStartRet.NO_CONFIG});
		if (chk != TaskStartRet.SUCCESS) {
			return ReaderType.UNKNOWN;
		}
		this._umMan.task_setAndStart(new GetReaderTypeTask(this._taskExport));
		return ReaderType.UNKNOWN;
	}

	public SupportStatus getSupportStatus(ReaderType readerType) {
		if (readerType == null) {
			return null;
		}
		if (this._umMan.getIntern_ConfigParameters() == null) {
			ACLog.w("SDK", "get mobile device support status: no config loaded");
			return SupportStatus.UNSUPPORTED;
		}
		StructConfigParameters scp = StructConfigParameters.convert(this._umMan.getIntern_ConfigParameters());
		return scp.querySupportStatus(readerType);
	}

	public SupportStatus getSupportStatus() {
		return getSupportStatus(ReaderType.UM_II);
	}

	public static enum TaskStartRet {
		SUCCESS, NO_READER, SDK_BUSY, NO_CONFIG, NOT_CONNECTED;
	}

	public static enum SupportStatus {
		UNSUPPORTED, SUPPORTED, MAYBE_SUPPORTED;
	}

	private class UmAcomMsgImpl
			implements AcomManagerMsg {

		private UmAcomMsgImpl() {
		}

		private UmAcomMsgImpl(Object object) {
		}


		public void onAttachmentChange(boolean attach) {
			if (attach) {
				if (SdkCustomization.CUST == 0) {
					if (uniMagReader.this._umMan.getIntern_ConfigParameters() != null) {
						boolean grant = uniMagReader.this._umrMsg.getUserGrant(0,
								"Device detected in headphone. Press Yes if it is UniMag.");
						if (grant) {
							uniMagReader.this.task_start_connect();
						}
					} else {
						ACLog.w("SDK", "reader attached, but no config loaded");
					}
				}
			} else {
				boolean orig_connectionState = uniMagReader.this._state_isConnected;
				uniMagReader.this.cxn_setDisconnected();
				if (!orig_connectionState) {
					uniMagReader.this._umrMsg.onReceiveMsgDisconnected();
				}
			}
		}

		public void onAutoConfigProgress(int percent) {
			uniMagReader.this._umrMsg.onReceiveMsgAutoConfigProgress(percent);
		}

		public void onAutoConfigStopped(ConfigParameters config) {
			if (config != null) {
				try {
					uniMagReader.this.saveConfigToFile(config);
				} catch (IOException ie) {
					ACLog.i("SDK", "***=== saveConfigToFile:" + ie.toString());
				}
				uniMagReader.this._umrMsg.onReceiveMsgAutoConfigCompleted(StructConfigParameters.convert(config));
			} else {
				uniMagReader.this._umrMsg.onReceiveMsgTimeout("Auto Config failed.");
			}
		}

		public void onVolumeAdjustFailure(int index, String message) {
			uniMagReader.this._umrMsg.onReceiveMsgFailureInfo(index, message);
		}
	}

	public class TaskExport {
		private TaskExport() {
		}

		private TaskExport(Object object) {
		}

		public AcomManager getAcomManager() {
			return uniMagReader.this._umMan;
		}

		public uniMagReaderMsg getuniMagReaderMsg() {
			return uniMagReader.this._umrMsg;
		}

		public ReaderType getReaderType() {
			return uniMagReader.this._state_readerType;
		}

		public void cxn_setConnected(ToneType newTone) {
			if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
				throw new RuntimeException("this method must be called on main thread");
			}
			if (!uniMagReader.this._umMan.getState_isAttached()) {
				return;
			}
			uniMagReader.this._state_isConnected = true;
			uniMagReader.this._umMan.getIntern_IOManager().getTonePlayer().setPlayingTone(newTone);

			uniMagReader.this._umrMsg.onReceiveMsgConnected();
		}

		public void readerType_set(ReaderType newType) {
			uniMagReader.this._state_readerType = newType;
		}

		public int getSwipeErrorCounter() {
			return uniMagReader._swipe_error_counter;
		}

		public void initializeSwipeErrorCounter() {
			uniMagReader._swipe_error_counter = 0;
		}

		public void incrementSwipeErrorCounter() {
			uniMagReader._swipe_error_counter += 1;
		}

		public void startCalibrateReader() {
			uniMagReader.this.sendCommandCalibrate();
		}
	}

	public class FwExport {
		public FwExport() {
		}

		public uniMagReader.TaskStartRet task_start_fwGetChallenge(uniMagReaderToolsMsg umtMsg) {
			uniMagReader.TaskStartRet chk = uniMagReader.this.task_checkStatusAndWarn(new uniMagReader.TaskStartRet[]{uniMagReader.TaskStartRet.NO_READER, uniMagReader.TaskStartRet.SDK_BUSY, uniMagReader.TaskStartRet.NO_CONFIG});
			if (chk != uniMagReader.TaskStartRet.SUCCESS) {
				return chk;
			}
			uniMagReader.this._umMan.task_setAndStart(new FwGetChallengeTask(uniMagReader.this._umMan, umtMsg));
			return uniMagReader.TaskStartRet.SUCCESS;
		}

		public uniMagReader.TaskStartRet task_start_fwUpdate(uniMagReaderToolsMsg umtMsg, boolean isEncryptedUpdate, byte[] binFile, byte[] challengeResponse) {
			uniMagReader.TaskStartRet chk = uniMagReader.this.task_checkStatusAndWarn(new uniMagReader.TaskStartRet[]{uniMagReader.TaskStartRet.NO_READER, uniMagReader.TaskStartRet.SDK_BUSY, uniMagReader.TaskStartRet.NO_CONFIG});
			if (chk != uniMagReader.TaskStartRet.SUCCESS) {
				return chk;
			}
			if (uniMagReader.this._state_readerType == ReaderType.UNIJACK) {
				uniMagReader.this._umMan.task_setAndStart(new FwUpdateTaskUniJack(uniMagReader.this._umMan, umtMsg, binFile, challengeResponse));
			} else {
				uniMagReader.this._umMan.task_setAndStart(new FwUpdateTask(uniMagReader.this._umMan, umtMsg, isEncryptedUpdate, binFile, challengeResponse));
			}
			return uniMagReader.TaskStartRet.SUCCESS;
		}

		public uniMagReader.TaskStartRet task_start_fwSendPower(uniMagReaderToolsMsg umtMsg) {
			uniMagReader.TaskStartRet chk = uniMagReader.this.task_checkStatusAndWarn(new uniMagReader.TaskStartRet[]{uniMagReader.TaskStartRet.NO_READER, uniMagReader.TaskStartRet.SDK_BUSY, uniMagReader.TaskStartRet.NO_CONFIG});
			if (chk != uniMagReader.TaskStartRet.SUCCESS) {
				return chk;
			}
			uniMagReader.this._umMan.task_setAndStart(new FwSendPowerTask(uniMagReader.this._umMan, umtMsg));
			return uniMagReader.TaskStartRet.SUCCESS;
		}
	}
}
