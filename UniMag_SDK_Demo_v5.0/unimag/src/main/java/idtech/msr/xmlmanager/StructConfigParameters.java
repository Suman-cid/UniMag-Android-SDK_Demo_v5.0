package idtech.msr.xmlmanager;

import idtech.msr.unimag.uniMagReader;

public class StructConfigParameters
		implements Cloneable {
	static {
		if (ReaderType.values().length !=
				ReaderType.values().length) {
			throw new RuntimeException("ReaderType definition inconsistent");
		}
		if (uniMagReader.SupportStatus.values().length !=
				SupportStatus.values().length) {
			throw new RuntimeException("SupportStatus definition inconsistent");
		}
	}

	private int frequencyInput = 48000;
	private int iRecordReadBufferSize = 0;
	private int iRecordBufferSize = 0;
	private short useVOICE_RECOGNIZITION = 0;
	private int frequencyOutput = 48000;
	private short directionOutputWave = 0;
	private int iWaveDirection = 0;
	private short _Low;
	private short _High;
	private short __Low;
	private short __High;
	private short highThreshold;
	private short lowThreshold;
	private double device_Apm_Base;
	private short min;
	private short max;
	private short preAmbleFactor = 10;
	private int baudRate = 9600;
	private byte shuttleChannel = 48;
	private String strModel = "";
	private uniMagReader.SupportStatus[] supportStatuses = new uniMagReader.SupportStatus[ReaderType.values().length];
	private short powerupWhenSwipe = 0;
	private short powerupLastBeforeCMD = 200;
	private short forceHeadsetPlug = 0;
	private short volumeLevelAdjust = 0;

	public static StructConfigParameters convert(ConfigParameters config) {
		ConfigParameters src = config;
		StructConfigParameters ret = new StructConfigParameters();

		ret.setFrequenceInput(src.getFrequenceInput());
		ret.setRecordReadBufferSize(src.getRecordReadBufferSize());
		ret.setRecordBufferSize(src.getRecordBufferSize());
		ret.setUseVoiceRecognition(src.getUseVoiceRecognition());

		ret.setFrequenceOutput(src.getFrequenceOutput());
		ret.setDirectionOutputWave(src.getDirectionOutputWave());

		ret.setWaveDirection(src.getWaveDirection());
		ret.set_Low(src.get_Low());
		ret.set_High(src.get_High());
		ret.set__Low(src.get__Low());
		ret.set__High(src.get__High());
		ret.sethighThreshold(src.gethighThreshold());
		ret.setlowThreshold(src.getlowThreshold());
		ret.setdevice_Apm_Base(src.getdevice_Apm_Base());
		ret.setMin(src.getMin());
		ret.setMax(src.getMax());
		ret.setPreAmbleFactor(src.getPreAmbleFactor());

		ret.setBaudRate(src.getBaudRate());
		ret.setShuttleChannel(src.getShuttleChannel());

		ret.setModelNumber(src.getModelNumber());
		ret.setSupportStatuses(convertSupportStatus(src));

		ret.setPowerupWhenSwipe(src.getPowerupWhenSwipe());
		ret.setPowerupLastBeforeCMD(src.getPowerupLastBeforeCMD());
		ret.setForceHeadsetPlug(src.getForceHeadsetPlug());
		ret.setVolumeLevelAdjust(src.getVolumeLevelAdjust());

		return ret;
	}

	private static uniMagReader.SupportStatus[] convertSupportStatus(ConfigParameters src) {
		uniMagReader.SupportStatus[] ret =
				new uniMagReader.SupportStatus[ReaderType.values().length];
		for (int i = 0; i < src.getSupportStatuses().length; i++) {
			if (src.getSupportStatuses()[i] != null) {
				switch (src.getSupportStatuses()[i]) {
					case SUPPORTED:
						ret[i] = uniMagReader.SupportStatus.SUPPORTED;
						break;
					case MAYBE_SUPPORTED:
						ret[i] = uniMagReader.SupportStatus.UNSUPPORTED;
						break;
					case UNSUPPORTED:
						ret[i] = uniMagReader.SupportStatus.MAYBE_SUPPORTED;
				}
			} else {
				ret[i] = null;
			}
		}
		return ret;
	}

	private static SupportStatus[] convertSupportStatus(StructConfigParameters src) {
		SupportStatus[] ret =
				new SupportStatus[ReaderType.values().length];
		for (int i = 0; i < src.getSupportStatuses().length; i++) {
			if (src.getSupportStatuses()[i] != null) {
				switch (src.getSupportStatuses()[i]) {
					case SUPPORTED:
						ret[i] = SupportStatus.SUPPORTED;
						break;
					case MAYBE_SUPPORTED:
						ret[i] = SupportStatus.UNSUPPORTED;
						break;
					case UNSUPPORTED:
						ret[i] = SupportStatus.MAYBE_SUPPORTED;
				}
			} else {
				ret[i] = null;
			}
		}
		return ret;
	}

	public int getFrequenceInput() {
		return this.frequencyInput;
	}

	public void setFrequenceInput(int value) {
		this.frequencyInput = value;
	}

	public int getRecordReadBufferSize() {
		return this.iRecordReadBufferSize;
	}

	public void setRecordReadBufferSize(int value) {
		this.iRecordReadBufferSize = value;
	}

	public int getRecordBufferSize() {
		return this.iRecordBufferSize;
	}

	public void setRecordBufferSize(int value) {
		this.iRecordBufferSize = value;
	}

	public short getUseVoiceRecognition() {
		return this.useVOICE_RECOGNIZITION;
	}

	public void setUseVoiceRecognition(short value) {
		this.useVOICE_RECOGNIZITION = value;
	}

	public int getFrequenceOutput() {
		return this.frequencyOutput;
	}

	public void setFrequenceOutput(int value) {
		this.frequencyOutput = value;
	}

	public short getDirectionOutputWave() {
		return this.directionOutputWave;
	}

	public void setDirectionOutputWave(short value) {
		this.directionOutputWave = value;
	}

	public int getWaveDirection() {
		return this.iWaveDirection;
	}

	public void setWaveDirection(int value) {
		this.iWaveDirection = value;
	}

	public short get_Low() {
		return this._Low;
	}

	public void set_Low(short value) {
		this._Low = value;
	}

	public short get_High() {
		return this._High;
	}

	public void set_High(short value) {
		this._High = value;
	}

	public short get__Low() {
		return this.__Low;
	}

	public void set__Low(short value) {
		this.__Low = value;
	}

	public short get__High() {
		return this.__High;
	}

	public void set__High(short value) {
		this.__High = value;
	}

	public short gethighThreshold() {
		return this.highThreshold;
	}

	public short getlowThreshold() {
		return this.lowThreshold;
	}

	public double getdevice_Apm_Base() {
		return this.device_Apm_Base;
	}

	public short getMin() {
		return this.min;
	}

	public void setMin(short value) {
		this.min = value;
	}

	public short getMax() {
		return this.max;
	}

	public void setMax(short value) {
		this.max = value;
	}

	public short getPreAmbleFactor() {
		return this.preAmbleFactor;
	}

	public void setPreAmbleFactor(short value) {
		this.preAmbleFactor = value;
	}

	public int getBaudRate() {
		return this.baudRate;
	}

	public void setBaudRate(int value) {
		this.baudRate = value;
	}

	public byte getShuttleChannel() {
		return this.shuttleChannel;
	}

	public void setShuttleChannel(byte value) {
		this.shuttleChannel = value;
	}

	public String getModelNumber() {
		return this.strModel;
	}

	public void setModelNumber(String value) {
		this.strModel = value;
	}

	public uniMagReader.SupportStatus[] getSupportStatuses() {
		return (uniMagReader.SupportStatus[]) this.supportStatuses.clone();
	}

	public void setSupportStatuses(uniMagReader.SupportStatus[] value) {
		this.supportStatuses = ((uniMagReader.SupportStatus[]) value.clone());
	}

	public short getPowerupWhenSwipe() {
		return this.powerupWhenSwipe;
	}

	public void setPowerupWhenSwipe(short value) {
		this.powerupWhenSwipe = value;
	}

	public short getPowerupLastBeforeCMD() {
		if (this.forceHeadsetPlug == 1) {
			return 600;
		}
		return this.powerupLastBeforeCMD;
	}

	public void setPowerupLastBeforeCMD(short value) {
		this.powerupLastBeforeCMD = value;
	}

	public short getForceHeadsetPlug() {
		return this.forceHeadsetPlug;
	}

	public void setForceHeadsetPlug(short value) {
		this.forceHeadsetPlug = value;
	}

	public short getVolumeLevelAdjust() {
		return this.volumeLevelAdjust;
	}

	public void setVolumeLevelAdjust(short value) {
		this.volumeLevelAdjust = value;
	}

	public void sethighThreshold(short value) {
		this.highThreshold = value;
	}

	public void setlowThreshold(short value) {
		this.lowThreshold = value;
	}

	public void setdevice_Apm_Base(double value) {
		this.device_Apm_Base = value;
	}

	public uniMagReader.SupportStatus querySupportStatus(ReaderType readerType) {
		if (readerType == null) {
			return null;
		}
		if ((readerType == ReaderType.UNKNOWN) || (readerType == ReaderType.UM_OR_PRO)) {
			return null;
		}
		uniMagReader.SupportStatus ret = this.supportStatuses[readerType.ordinal()];
		return ret == null ? uniMagReader.SupportStatus.UNSUPPORTED : ret;
	}

	public StructConfigParameters clone() {
		StructConfigParameters src = this;
		StructConfigParameters ret = new StructConfigParameters();

		ret.setFrequenceInput(src.getFrequenceInput());
		ret.setRecordReadBufferSize(src.getRecordReadBufferSize());
		ret.setRecordBufferSize(src.getRecordBufferSize());
		ret.setUseVoiceRecognition(src.getUseVoiceRecognition());

		ret.setFrequenceOutput(src.getFrequenceOutput());
		ret.setDirectionOutputWave(src.getDirectionOutputWave());

		ret.setWaveDirection(src.getWaveDirection());
		ret.set_Low(src.get_Low());
		ret.set_High(src.get_High());
		ret.set__Low(src.get__Low());
		ret.set__High(src.get__High());
		ret.sethighThreshold(src.gethighThreshold());
		ret.setlowThreshold(src.getlowThreshold());
		ret.setdevice_Apm_Base(src.getdevice_Apm_Base());
		ret.setMin(src.getMin());
		ret.setMax(src.getMax());
		ret.setPreAmbleFactor(src.getPreAmbleFactor());

		ret.setBaudRate(src.getBaudRate());
		ret.setShuttleChannel(src.getShuttleChannel());

		ret.setModelNumber(src.getModelNumber());
		ret.setSupportStatuses(src.getSupportStatuses());

		ret.setPowerupWhenSwipe(src.getPowerupWhenSwipe());
		ret.setPowerupLastBeforeCMD(src.getPowerupLastBeforeCMD());
		ret.setForceHeadsetPlug(src.getForceHeadsetPlug());
		ret.setVolumeLevelAdjust(src.getVolumeLevelAdjust());

		return ret;
	}

	public ConfigParameters convertConfigParameter() {
		StructConfigParameters src = this;
		ConfigParameters ret = new ConfigParameters();

		ret.setFrequenceInput(src.getFrequenceInput());
		ret.setRecordReadBufferSize(src.getRecordReadBufferSize());
		ret.setRecordBufferSize(src.getRecordBufferSize());
		ret.setUseVoiceRecognition(src.getUseVoiceRecognition());

		ret.setFrequenceOutput(src.getFrequenceOutput());
		ret.setDirectionOutputWave(src.getDirectionOutputWave());

		ret.setWaveDirection(src.getWaveDirection());
		ret.set_Low(src.get_Low());
		ret.set_High(src.get_High());
		ret.set__Low(src.get__Low());
		ret.set__High(src.get__High());
		ret.sethighThreshold(src.gethighThreshold());
		ret.setlowThreshold(src.getlowThreshold());
		ret.setdevice_Apm_Base(src.getdevice_Apm_Base());
		ret.setMin(src.getMin());
		ret.setMax(src.getMax());
		ret.setPreAmbleFactor(src.getPreAmbleFactor());

		ret.setBaudRate(src.getBaudRate());
		ret.setShuttleChannel(src.getShuttleChannel());

		ret.setModelNumber(src.getModelNumber());
		ret.setSupportStatuses(convertSupportStatus(src));

		ret.setPowerupWhenSwipe(src.getPowerupWhenSwipe());
		ret.setPowerupLastBeforeCMD(src.getPowerupLastBeforeCMD());
		ret.setForceHeadsetPlug(src.getForceHeadsetPlug());
		ret.setVolumeLevelAdjust(src.getVolumeLevelAdjust());

		return ret;
	}
}
