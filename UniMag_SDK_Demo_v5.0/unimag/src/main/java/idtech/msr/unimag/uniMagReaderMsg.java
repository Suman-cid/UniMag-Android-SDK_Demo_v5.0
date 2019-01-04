package idtech.msr.unimag;

import idtech.msr.xmlmanager.StructConfigParameters;

public abstract interface uniMagReaderMsg {
	public static final int typeToPowerupUniMag = 0;
	public static final int typeToUpdateXML = 1;
	public static final int typeToOverwriteXML = 2;
	public static final int typeToTryAutoConfig = 3;
	public static final int typeToReportToIdtech = 4;
	public static final int cmdUnknown = 0;
	public static final int cmdEnableTDES = 1;
	public static final int cmdEnableAES = 2;
	public static final int cmdGetVersion = 3;
	public static final int cmdGetSettings = 4;
	public static final int cmdSwipeCard = 5;
	public static final int cmdDefaultGeneralSetting = 6;
	public static final int cmdGetSerialNumber = 7;
	public static final int cmdGetNextKSN = 8;
	public static final int cmdEnableErrorNotification = 9;
	public static final int cmdDisableErrorNotification = 10;
	public static final int cmdEnableExpDate = 11;
	public static final int cmdDisableExpDate = 12;
	public static final int cmdEnableForceEncryption = 13;
	public static final int cmdDisableForceEncryption = 14;
	public static final int cmdSetPrePAN = 15;
	public static final int cmdClearBuffer = 16;
	public static final int cmdGetAttachedReaderType = 17;
	public static final int cmdTestSwipeCard = 101;
	public static final int cmdTestCommand = 102;
	public static final int cmdCalibrate = 103;
	public static final int cmdGetBatteryLevel = 104;

	public abstract void onReceiveMsgToConnect();

	public abstract void onReceiveMsgConnected();

	public abstract void onReceiveMsgDisconnected();

	public abstract void onReceiveMsgTimeout(String paramString);

	public abstract void onReceiveMsgToSwipeCard();

	public abstract void onReceiveMsgCommandResult(int paramInt, byte[] paramArrayOfByte);

	public abstract void onReceiveMsgCardData(byte paramByte, byte[] paramArrayOfByte);

	public abstract void onReceiveMsgProcessingCardData();

	public abstract void onReceiveMsgToCalibrateReader();

	@Deprecated
	public abstract void onReceiveMsgSDCardDFailed(String paramString);

	public abstract void onReceiveMsgFailureInfo(int paramInt, String paramString);

	public abstract void onReceiveMsgAutoConfigProgress(int paramInt);

	public abstract void onReceiveMsgAutoConfigProgress(int paramInt, double paramDouble, String paramString);

	public abstract void onReceiveMsgAutoConfigCompleted(StructConfigParameters paramStructConfigParameters);

	public abstract boolean getUserGrant(int paramInt, String paramString);
}
