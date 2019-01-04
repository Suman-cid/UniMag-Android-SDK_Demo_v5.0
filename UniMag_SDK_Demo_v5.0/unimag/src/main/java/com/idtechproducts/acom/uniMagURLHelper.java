package com.idtechproducts.acom;

import idtech.msr.unimag.uniMagReaderMsg;

public class uniMagURLHelper {
    private static final int TIMEOUT_VALUE_VERSION_CHECK = 2000;
    private static final int TIMEOUT_VALUE = 5000;
    private uniMagReaderMsg _cbMagReaderMsg;

    public uniMagURLHelper(uniMagReaderMsg _callback) {
        this._cbMagReaderMsg = _callback;
    }

    public String getLastXMLVersion() {
        return "5.0.13";

//		String strXMLVerName_URL = "http://sdk.idtechproducts.com/sdk_x_m_l_file/";
//		String strXMLVerName = "uniMagAndroidSDKInfoForXML.txt";
//		try {
//			URL url = new URL(strXMLVerName_URL + strXMLVerName);
//			URLConnection ucon = url.openConnection();
//			ucon.setConnectTimeout(2000);
//			ucon.setReadTimeout(2000);
//			InputStream is = ucon.getInputStream();
//			BufferedInputStream bis = new BufferedInputStream(is);
//			ByteArrayBuffer baf = new ByteArrayBuffer(50);
//			int current = 0;
//			while ((current = bis.read()) != -1) {
//				baf.append((byte) current);
//			}
//			return new String(baf.toByteArray());
//		} catch (SocketTimeoutException e) {
//			this._cbMagReaderMsg.onReceiveMsgFailureInfo(7, "Can't download the XML file. Please make sure the network is accessible.");
//			e.printStackTrace();
//			return null;
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return null;
    }

    public boolean DownloadFromUrl(String xmlVersion, String fileName) {
        return true;

//		String strXMLFileName_URL = "http://sdk.idtechproducts.com/sdk_x_m_l_file/";
//		String strXMLFileName = "IDT_uniMagCfg_" + xmlVersion + ".zip";
//		try {
//			URL url = new URL(strXMLFileName_URL + strXMLFileName);
//			URLConnection ucon = url.openConnection();
//			ucon.setConnectTimeout(5000);
//			ucon.setReadTimeout(5000);
//			InputStream is = ucon.getInputStream();
//			BufferedInputStream bis = new BufferedInputStream(is);
//			ByteArrayBuffer baf = new ByteArrayBuffer(50);
//			int current = 0;
//			while ((current = bis.read()) != -1) {
//				baf.append((byte) current);
//			}
//			String compressedFileName = fileName.replace(".xml", ".zip");
//			File compressedFile = new File(compressedFileName);
//			FileOutputStream fos = new FileOutputStream(compressedFile);
//			fos.write(baf.toByteArray());
//			fos.close();
//
//			ZipInputStream zis = new ZipInputStream(new FileInputStream(compressedFileName));
//			ZipEntry ze = zis.getNextEntry();
//
//			byte[] buffer = new byte[Integer.MAX_VALUE];
//
//			if (ze != null) {
//				File newFile = new File(Common.getSDRootFilePath() + File.separator + "IDT_uniMagCfg.xml");
//				fos = new FileOutputStream(newFile);
//
//				int len;
//				while ((len = zis.read(buffer)) > 0)
//					fos.write(buffer, 0, len);
//
//				fos.close();
//			}
//			compressedFile.delete();
//
//			zis.closeEntry();
//			zis.close();
//
//			return true;
//		} catch (SocketTimeoutException e) {
//			this._cbMagReaderMsg.onReceiveMsgFailureInfo(7, "Can't download the XML file. Please make sure the network is accessible.");
//			e.printStackTrace();
//			return false;
//		} catch (IOException e) {
//		}
//		return false;
    }
}
