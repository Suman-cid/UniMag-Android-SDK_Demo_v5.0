package idtech.msr.unimag.unimagtools;

public abstract interface uniMagReaderToolsMsg {
	public static final int cmdGetChallenge_Succeed_WithChallengData = 201;
	public static final int cmdGetChallenge_Failed = 202;
	public static final int cmdGetChallenge_NeedSetBinFile = 203;
	public static final int cmdGetChallenge_Timeout = 204;
	public static final int cmdGetChallenge_Succeed_WithFileVersion = 205;
	public static final int cmdUpdateFirmware_Succeed = 301;
	public static final int cmdUpdateFirmware_EnterBootloadModeFailed = 302;
	public static final int cmdUpdateFirmware_DownloadBlockFailed = 303;
	public static final int cmdUpdateFirmware_EndDownloadBlockFailed = 304;
	public static final int cmdUpdateFirmware_Timeout = 305;
	public static final int cmdUpdateFirmware_NeedSetBinFile = 306;
	public static final int cmdUpdateFirmware_NeedGetChallenge = 307;
	public static final int cmdUpdateFirmware_Need8BytesData = 308;
	public static final int cmdUpdateFirmware_Need24BytesData = 309;
	public static final int CMD_UPDATE_FIRMWARE_NEED_WAIT_2MINUTE = 401;
	public static final int CMD_UPDATE_FIRMWARE_SUCCEED_1ST_STEP = 402;

	public abstract void onReceiveMsgUpdateFirmwareProgress(int paramInt);

	public abstract void onReceiveMsgUpdateFirmwareResult(int paramInt);

	public abstract void onReceiveMsgChallengeResult(int paramInt, byte[] paramArrayOfByte);
}
