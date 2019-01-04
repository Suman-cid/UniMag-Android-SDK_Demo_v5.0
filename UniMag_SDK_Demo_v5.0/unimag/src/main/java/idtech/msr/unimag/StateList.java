package idtech.msr.unimag;

public class StateList {
	public static final boolean disconnected = false;
	public static final boolean connected = true;
	public static final boolean setBR_Failed = false;
	public static final boolean setBR_Okay = true;
	public static final boolean isTriedSetBaudRate_N = false;
	public static final boolean isTriedSetBaudRate_Y = true;
	public static final boolean commandToRequire = false;
	public static final boolean commandToRequiredOK = true;
	public static final int commandUnkown = 0;
	public static final int commandConnectInfo = 1;
	public static final int commandGetResult = 2;
	public static final int commandGetData = 3;
	public static final int commandSwipeCard = 4;
	public static final int commandPowerupStr = 5;
	public static final int commandOkay = 6;
	public static final int cmdUnknown = 0;
	public static final int cmdConnect = 1;
	public static final int cmdSetBaudRate = 2;
	public static final int cmdTurnOnTDES = 3;
	public static final int cmdTurnOnAES = 4;
	public static final int cmdGetVersion = 6;
	public static final int cmdGetSetting = 7;
	public static final int cmdSwipeCard = 8;
	public static final int cmdPowerup = 9;
	public static final int cmdCheckHealth = 10;
	public static final int cmdUserDefine = 11;
	public static final int cmdDefaultGeneralSettings = 12;
	public static final int cmdGetSerialNumber = 13;
	public static final int cmdGetNextKSN = 14;
	public static final int cmdEnableErrNotification = 15;
	public static final int cmdDisableErrNotification = 16;
	public static final int cmdEnableExpDate = 17;
	public static final int cmdDisableExpDate = 18;
	public static final int cmdEnableForeceEncryption = 19;
	public static final int cmdDisableForeceEncryption = 20;
	public static final int cmdSetPrePAN = 21;
	public static final int cmdClearBuffer = 22;
	public static final int cmdTestSwipeCard = 23;
	public static final int cmdTestCommand = 24;
	public static final int cmdGetAttachedReaderType = 25;
	public static final int cmdSetBaudRateForAutoCfg = 30;
	public static final int uniMagUnkown = 0;
	public static final int uniMag2G3GPro = 1;
	public static final int uniMagII = 2;
	public static final int uniMagShuttle = 4;
	public static final int uniJack = 5;
	public static final boolean conifg_Failed = false;
	public static final boolean conifg_Okay = true;

	public static final String getInfoFromCommandID(int cmdID) {
		switch (cmdID) {
			case 0:
				return "commandUnkown";
			case 1:
				return "cmdConnect";
			case 2:
				return "cmdSetBaudRate";
			case 3:
				return "cmdTurnOnTDES";
			case 4:
				return "cmdTurnOnAES";
			case 6:
				return "cmdGetVersion";
			case 7:
				return "cmdGetSetting";
			case 8:
				return "cmdSwipeCard";
			case 9:
				return "cmdPowerup";
			case 10:
				return "cmdCheckHealth";
			case 11:
				return "cmdUserDefine";
			case 12:
				return "cmdDefaultGeneralSettings";
			case 13:
				return "cmdGetSerialNumber";
			case 14:
				return "cmdGetNextKSN";
			case 15:
				return "cmdEnableErrNotification";
			case 16:
				return "cmdDisableErrNotification";
			case 17:
				return "cmdEnableExpDate";
			case 18:
				return "cmdDisableExpDate";
			case 19:
				return "cmdEnableForeceEncryption";
			case 20:
				return "cmdDisableForeceEncryption";
			case 21:
				return "cmdSetPrePAN";
			case 22:
				return "cmdClearBuffer";
			case 23:
				return "cmdTestSwipeCard";
			case 24:
				return "cmdTestCommand";
			case 30:
				return "cmdSetBaudRateForAutoCfg";
		}
		return "commandUnkown";
	}
}
