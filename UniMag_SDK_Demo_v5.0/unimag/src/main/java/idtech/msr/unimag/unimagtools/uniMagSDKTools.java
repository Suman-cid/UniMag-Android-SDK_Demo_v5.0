package idtech.msr.unimag.unimagtools;

import android.content.Context;

import com.idtechproducts.acom.ACLog;
import com.idtechproducts.acom.Common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;

import idtech.msr.unimag.uniMagReader;
import idtech.msr.unimag.uniMagReaderToolProxy;
import idtech.msr.xmlmanager.ReaderType;

public class uniMagSDKTools {
	private final uniMagReaderToolsMsg _umtMsg;
	private uniMagReader.FwExport _umFwExport = null;
	private boolean _cfg_isEncryptedUpdate = false;
	private byte[] _cfg_bin = null;
	private Short _cfg_bin_crc = null;
	private ReaderType readerType;

	public uniMagSDKTools(uniMagReaderToolsMsg callback, Context context) {
		if (callback == null) {
			throw new NullPointerException("argument must not be null");
		}
		this._umtMsg = callback;
	}

	private static byte[] binfile_checkAndLoad(String path, boolean isEncrypted) {
		if (path == null) {
			return null;
		}
		File file = new File(path);
		if (!file.exists()) {
			return null;
		}
		int expectedMultiple = isEncrypted ? 258 : 256;
		int fileLen = (int) file.length();
		if ((fileLen == 0) ||
				(fileLen % expectedMultiple != 0)) {
			return null;
		}
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(path);
			byte[] ret = new byte[fileLen];
			fis.read(ret);
			return ret;
		} catch (IOException e) {
			ACLog.e("FWUpdate", "file failed to load: " + e);
		} finally {
			try {
				if (fis != null) {
					fis.close();
				}
			} catch (IOException localIOException3) {
			}
		}
		return null;
	}

	private static byte[] binfileForUniJack_checkAndLoad(String path) {
		if (path == null) {
			return null;
		}
		File file = new File(path);
		if (!file.exists()) {
			return null;
		}
		int expectedMultiple = 1024;
		int fileLen = (int) file.length();
		if ((fileLen == 0) ||
				(fileLen % 1024 != 0)) {
			return null;
		}
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(path);
			byte[] ret = new byte[fileLen];
			fis.read(ret);
			return ret;
		} catch (IOException e) {
			ACLog.e("FWUpdate", "file failed to load: " + e);
		} finally {
			try {
				if (fis != null) {
					fis.close();
				}
			} catch (IOException localIOException3) {
			}
		}
		return null;
	}

	public void setUniMagReader(uniMagReader instance) {
		this._umFwExport = null;
		this.readerType = instance.getAttachedReaderType();
		Field[] arrayOfField;
		int j = (arrayOfField = instance.getClass().getDeclaredFields()).length;
		for (int i = 0; i < j; i++) {
			Field field = arrayOfField[i];
			if (field.getType().equals(uniMagReader.FwExport.class)) {
				try {
					boolean origVal = field.isAccessible();
					field.setAccessible(true);
					this._umFwExport = ((uniMagReader.FwExport) field.get(instance));
					field.setAccessible(origVal);
				} catch (IllegalArgumentException localIllegalArgumentException) {
				} catch (IllegalAccessException localIllegalAccessException) {
				}
			}
		}
	}

	public uniMagReaderToolProxy getSDKToolProxy() {
		return null;
	}

	public boolean setFirmwareBINFile(String strFilePathName) {
		return setFirmwareBINFile(strFilePathName, false);
	}

	public boolean setFirmwareEncryptedBINFile(String strFilePathName) {
		return setFirmwareBINFile(strFilePathName, true);
	}

	private boolean setFirmwareBINFile(String binPath, boolean isEncryptedUpdate) {
		this._cfg_isEncryptedUpdate = isEncryptedUpdate;
		if (this.readerType == ReaderType.UNIJACK) {
			this._cfg_bin = binfileForUniJack_checkAndLoad(binPath);
		} else {
			this._cfg_bin = binfile_checkAndLoad(binPath, isEncryptedUpdate);
		}
		if (this._cfg_bin == null) {
			this._cfg_bin_crc = null;
			return false;
		}
		short crc = -1;
		byte[] arrayOfByte;
		int j = (arrayOfByte = this._cfg_bin).length;
		for (int i = 0; i < j; i++) {
			byte b = arrayOfByte[i];
			crc = Common.crc_Update(crc, b);
		}
		this._cfg_bin_crc = Short.valueOf(crc);
		return true;
	}

	public int getBINFileBlockNumber() {
		return this._cfg_isEncryptedUpdate ? 258 : 256;
	}

	public int getRequiredChallengeResponseLength() {
		return this._cfg_isEncryptedUpdate ? 24 : 8;
	}

	public Integer getCRCOfBinFile() {
		return this._cfg_bin_crc == null ? null : Integer.valueOf(this._cfg_bin_crc.shortValue());
	}

	public boolean getChallenge() {
		uniMagReader.TaskStartRet r = this._umFwExport.task_start_fwGetChallenge(this._umtMsg);
		return r == uniMagReader.TaskStartRet.SUCCESS;
	}

	public boolean updateFirmware(byte[] challengeResponse) {
		if (this._cfg_bin == null) {
			this._umtMsg.onReceiveMsgUpdateFirmwareResult(306);
			return false;
		}
		if (challengeResponse != null) {
			if ((!this._cfg_isEncryptedUpdate) && (challengeResponse.length != 8)) {
				this._umtMsg.onReceiveMsgUpdateFirmwareResult(308);
				return false;
			}
			if ((this._cfg_isEncryptedUpdate) && (challengeResponse.length != 24)) {
				this._umtMsg.onReceiveMsgUpdateFirmwareResult(309);
				return false;
			}
			challengeResponse = (byte[]) challengeResponse.clone();
		}
		uniMagReader.TaskStartRet r = this._umFwExport.task_start_fwUpdate(this._umtMsg, this._cfg_isEncryptedUpdate, this._cfg_bin, challengeResponse);
		return r == uniMagReader.TaskStartRet.SUCCESS;
	}

	public void sendPowerToReader() {
		this._umFwExport.task_start_fwSendPower(this._umtMsg);
	}
}
