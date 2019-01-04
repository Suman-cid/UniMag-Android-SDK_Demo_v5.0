package com.idtechproducts.unimagsdk;

import com.idtechproducts.acom.Common;

import java.util.List;

import idtech.msr.xmlmanager.ReaderType;

public enum PUSType {
	INVALID(false, "invalid", null, 0),
	BLM(true, "blm", null, 0),
	BAUD(true, "chan", null, 0),
	MSR1xx(false, "1", ReaderType.UM_OR_PRO, 1),
	MSR2xx(true, "2", ReaderType.UM_II, 2),
	MSR3xx(true, "3", ReaderType.SHUTTLE, 4);

	public final boolean supportsCmd;
	public final String name;
	public final ReaderType readerType;
	public final int stateListVal;

	PUSType(boolean supportsCmd, String name, ReaderType readerType, int stateListVal) {
		this.supportsCmd = supportsCmd;
		this.name = name;
		this.readerType = readerType;
		this.stateListVal = stateListVal;
	}

	public static PUSType parse(byte[] resp) {
		if ((resp == null) || (resp.length == 0)) {
			return INVALID;
		}
		if (Common.containsStr(resp, "SPPMSR1")) {
			return SdkCustomization.CUST == 0 ? MSR1xx : INVALID;
		}
		if (Common.containsStr(resp, "SPPMSR2")) {
			return MSR2xx;
		}
		if (Common.containsStr(resp, "SPPMSR")) {
			return SdkCustomization.CUST == 0 ? MSR3xx : INVALID;
		}
		if ((15 == resp.length) && (6 == resp[0]) && (86 == resp[1])) {
			return BLM;
		}
		if ((2 == resp.length) && (6 == resp[0]) && (86 == resp[1])) {
			return BLM;
		}
		if ((1 == resp.length) && (6 == resp[0])) {
			return BAUD;
		}
		return INVALID;
	}

	public static PUSType parse(List<byte[]> resp) {
		for (byte[] respI : resp) {
			PUSType ret = parse(respI);
			if (ret != INVALID) {
				return ret;
			}
		}
		return INVALID;
	}
}
