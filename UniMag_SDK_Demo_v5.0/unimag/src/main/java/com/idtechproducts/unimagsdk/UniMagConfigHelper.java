
package com.idtechproducts.unimagsdk;

import android.content.Context;
import android.os.Build;
import com.idtechproducts.acom.AcomXmlParser;
import com.idtechproducts.acom.Common;
import com.idtechproducts.acom.uniMagURLHelper;
import com.idtechproducts.unimagsdk.SdkCustomization;
import java.io.File;
import java.util.Locale;

import idtech.msr.unimag.uniMagReaderMsg;
import idtech.msr.xmlmanager.ConfigParameters;

public final class UniMagConfigHelper {
	private ConfigParameters myConfigParams;
	private String _strFileName = null;
	private final String _strManufacture;
	private final String _strMP;
	private String _concatenatedXmlVersion;
	private final uniMagReaderMsg _cbMagReaderMsg;
	private final Context mContext;
	private uniMagURLHelper urlHelper;

	public UniMagConfigHelper(uniMagReaderMsg _callback, Context ctx) {
		this._cbMagReaderMsg = _callback;
		this.mContext = ctx;
		this.urlHelper = new uniMagURLHelper(this._cbMagReaderMsg);
		String temp = Build.MANUFACTURER;
		this._strManufacture = temp = temp.toLowerCase(Locale.ENGLISH).replaceAll("\\s*", "");
		temp = Build.MODEL;
		this._strMP = temp = temp.toLowerCase(Locale.ENGLISH).replaceAll("\\s*", "");
	}

	public void setPathFileName(String strFileName) {
		this._strFileName = strFileName;
	}

	public String getManufacture() {
		return this._strManufacture;
	}

	public String getModel() {
		return this._strMP;
	}

	public ConfigParameters getConfigParams() {
		return this.myConfigParams;
	}

	public boolean loadingXMLFile(boolean bAuto) {
		if (this._strFileName == null && !bAuto) {
			this._cbMagReaderMsg.onReceiveMsgFailureInfo(3, "Wrong XML file name, please set the filename or enable the auto update.");
			return false;
		}
		return this.loadingXMLFile(this._strFileName, bAuto);
	}

	public String getLoadedXmlVersion() {
		return this._concatenatedXmlVersion;
	}

	/*
	 * Enabled force condition propagation
	 * Lifted jumps to return sites
	 */
	private boolean downloadXMLFile() {
		if (!Common.isOnline(this.mContext)) {
			return false;
		}
		String verXML = this.urlHelper.getLastXMLVersion();
		if (verXML != null) {
			if (Common.isStorageExist()) {
				String strTmpFileName = String.valueOf(Common.getSDRootFilePath()) + File.separator + "IDT_uniMagCfg.xml";
				boolean getUserGrant = true;
				if (Common.isFileExist(strTmpFileName) && !(getUserGrant = this._cbMagReaderMsg.getUserGrant(2, "Checking if user grants overwrite XML to the latest version."))) return false;
				if (!this.urlHelper.DownloadFromUrl(verXML, strTmpFileName)) return false;
				boolean xmlDownloadedAndLoadedOK = this.loadingXMLFileAfterDownload(strTmpFileName);
				String strTmpAppFileName = String.valueOf(Common.getApplicationPath(this.mContext)) + File.separator + "IDT_uniMagCfg.xml";
				try {
					Common.copyFile(strTmpFileName, strTmpAppFileName);
					return xmlDownloadedAndLoadedOK;
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				return xmlDownloadedAndLoadedOK;
			}
			String strTmpAppFileName = String.valueOf(Common.getApplicationPath(this.mContext)) + File.separator + "IDT_uniMagCfg.xml";
			boolean getUserGrant = true;
			if (Common.isFileExist(strTmpAppFileName) && !(getUserGrant = this._cbMagReaderMsg.getUserGrant(2, "Checking if user grants overwrite XML to the latest version."))) return false;
			if (!this.urlHelper.DownloadFromUrl(verXML, strTmpAppFileName)) return false;
			return this.loadingXMLFileAfterDownload(strTmpAppFileName);
		}
		this._cbMagReaderMsg.onReceiveMsgFailureInfo(7, "Can't download the XML file. Please make sure the network is accessible.");
		return false;
	}

	private boolean loadingXMLFile(String strXMLFileNameWithPath, boolean bAuto) {
		AcomXmlParser.UMXmlParseResult r;
		boolean bReadyToReadXML = false;
		if (Common.isFileExist(strXMLFileNameWithPath)) {
			r = AcomXmlParser.parseFile(strXMLFileNameWithPath, this._strManufacture, this._strMP, false, SdkCustomization.CUST == 1);
			if (this.ReadXMLCfgByModel(r)) {
				return true;
			}
		} else if (!bAuto) {
			this._cbMagReaderMsg.onReceiveMsgFailureInfo(4, "The XML file does not exist and the auto update disabled.");
			return false;
		}
		if (!bReadyToReadXML) {
			String strTmpFileName;
			if (Common.isStorageExist()) {
				strTmpFileName = String.valueOf(Common.getSDRootFilePath()) + File.separator + "IDT_uniMagCfg.xml";
				if (Common.isFileExist(strTmpFileName)) {
					strXMLFileNameWithPath = strTmpFileName;
					bReadyToReadXML = true;
				}
			} else {
				strTmpFileName = String.valueOf(Common.getApplicationPath(this.mContext)) + File.separator + "IDT_uniMagCfg.xml";
				if (Common.isFileExist(strTmpFileName)) {
					strXMLFileNameWithPath = strTmpFileName;
					bReadyToReadXML = true;
				}
			}
		}
		if (bReadyToReadXML) {
			r = AcomXmlParser.parseFile(strXMLFileNameWithPath, this._strManufacture, this._strMP, false, SdkCustomization.CUST == 1);
			if (this.ReadXMLCfgByModel(r)) {
				return true;
			}
			boolean bAutoUpdate = this._cbMagReaderMsg.getUserGrant(1, "Checking if user grants downloading latest XML from Server.");
			if (bAutoUpdate && bAuto) {
				return this.downloadXMLFile();
			}
		} else {
			boolean bAutoUpdate = this._cbMagReaderMsg.getUserGrant(1, "Checking if user grants downloading latest XML from Server.");
			if (bAutoUpdate && bAuto) {
				return this.downloadXMLFile();
			}
			this._cbMagReaderMsg.onReceiveMsgFailureInfo(4, "The XML file does not exist and the auto update disabled.");
		}
		return false;
	}

	private boolean ReadXMLCfgByModel(AcomXmlParser.UMXmlParseResult r) {
		if (r == null) {
			return false;
		}
		if (r.config == null) {
			return false;
		}
		this.myConfigParams = r.config;
		this._concatenatedXmlVersion = r.SDKMajorVersion != null && r.SDKMinorVersion != null && r.XMLVersion != null ? String.valueOf(r.SDKMajorVersion) + "." + r.SDKMinorVersion + "." + r.XMLVersion : "";
		return true;
	}

	private boolean loadingXMLFileAfterDownload(String strXMLFileNameWithPath) {
		AcomXmlParser.UMXmlParseResult r;
		if (Common.isFileExist(strXMLFileNameWithPath) && this.ReadXMLCfgByModel(r = AcomXmlParser.parseFile(strXMLFileNameWithPath, this._strManufacture, this._strMP, false, SdkCustomization.CUST == 1))) {
			return true;
		}
		this._cbMagReaderMsg.onReceiveMsgFailureInfo(2, "This phone model is not supported by the current SDK. Please contact supporter for assistance.");
		return false;
	}
}