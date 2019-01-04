package com.idtechproducts.acom;

import android.os.Build;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import idtech.msr.xmlmanager.ConfigParameters;
import idtech.msr.xmlmanager.ReaderType;
import idtech.msr.xmlmanager.SupportStatus;

public class AcomXmlParser {
	private static final String TAG = "UmXmlParser";

	private AcomXmlParser() {
	}

	public static UMXmlParseResult
	parseFile(String xmlFileFullPath,
			  String targetManufacturer,
			  String targetModel,
			  boolean readTemplateEntries,
			  boolean useUm2i) {

		UMXmlParseResult ret = new UMXmlParseResult();
		FileInputStream fis = null;
		Exception ex = null;
		try {
			fis = new FileInputStream(xmlFileFullPath);
			ret.fileExists = true;

			DefaultHandler handler =
					new MySaxHandler(ret, targetManufacturer, targetModel, readTemplateEntries, useUm2i);

			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			XMLReader xmlReader = parser.getXMLReader();
			xmlReader.setContentHandler(handler);
			xmlReader.parse(new InputSource(fis));
		} catch (ParserConfigurationException error) {
			ex = error;

			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					ACLog.w("UmXmlParser", "failed to close file");
				}
			}
		} catch (SAXException e) {
			ex = e;


			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e2) {
					ACLog.w("UmXmlParser", "failed to close file");
				}
			}
		} catch (IOException e) {
			ex = e;


			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e1) {
					ACLog.w("UmXmlParser", "failed to close file");
				}
			}
		} catch (RuntimeException e) {
			ex = e;

			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e3) {
					ACLog.w("UmXmlParser", "failed to close file");
				}
			}
		} finally {
			if (fis != null)
				try {
					fis.close();
				} catch (IOException e) {
					ACLog.w("UmXmlParser", "failed to close file");
				}
		}
		if (ex != null) {
			ACLog.w("UmXmlParser", "parsing XML failed due to exception", ex);
		}

		return ret;
	}

	public static class UMXmlParseResult {
		public boolean fileExists = false;

		public String SDKMajorVersion = null;
		public String SDKMinorVersion = null;
		public String XMLVersion = null;
		public ConfigParameters config = null;
		public List<ConfigParameters> templates = null;

		public UMXmlParseResult() {
		}
	}

	private static class MySaxHandler
			extends DefaultHandler {
		private static final String k_vrManufacturerPrefix = "prefix_vr_";

		private static final String kTag_IDTECHPRODUCTS = "IDTECHPRODUCTS";

		private static final String kTag_Summary = "Summary";

		private static final String kTag_Version = "Version";

		private static final String kTag_voice_recognition = "voice-recognition";

		private static final String kTag_special_model_number = "special_model_number";

		private static final String kTag_phone = "phone";

		private static final String kTag_templates = "templates";

		private final String target_manufacturer;

		private final String target_model;

		private final boolean readTemplateEntries;
		private final boolean useUm2i;
		private final AcomXmlParser.UMXmlParseResult result;
		private ConfigParameters profile_nonVr;
		private ConfigParameters profile_voiceRecognition;
		private ConfigParameters profile_specialModel;
		private MainState state_main = MainState.OutSideRoot;
		private ManType state_manType;
		private String state_versionName;
		private String state_versionTextContent;
		private String state_paramName;
		private String state_paramAttr;
		private String state_paramTextContent;
		private int state_level = 0;
		private Integer state_skipOutOfLevel = null;

		public MySaxHandler(AcomXmlParser.UMXmlParseResult result, String targetManufacturer, String targetModel, boolean readTemplateEntries, boolean useUm2i) {

			this.result = result;
			target_manufacturer = targetManufacturer;
			target_model = targetModel;
			this.readTemplateEntries = readTemplateEntries;
			this.useUm2i = useUm2i;

			if (this.readTemplateEntries) {
				result.templates = new ArrayList();
			}
		}

		private static void populateConfigEntry(ConfigParameters cfg, String eName, String eAttr, String enclosedText, boolean useUm2i) {
			try {
				if (eName.equalsIgnoreCase("InputFreq")) {
					cfg.setFrequenceInput(Integer.parseInt(enclosedText));
				} else if (eName.equalsIgnoreCase("OutputFreq")) {
					cfg.setFrequenceOutput(Integer.parseInt(enclosedText));
				} else if (eName.equalsIgnoreCase("ReadRecBuffSize")) {
					cfg.setRecordReadBufferSize(Integer.parseInt(enclosedText));
				} else if (eName.equalsIgnoreCase("RecBuffSize")) {
					cfg.setRecordBufferSize(Integer.parseInt(enclosedText));
				} else if (eName.equalsIgnoreCase("WaveDirct")) {
					cfg.setWaveDirection(Integer.parseInt(enclosedText));
				} else if (eName.equalsIgnoreCase("_Low")) {
					cfg.set_Low(Short.parseShort(enclosedText));
				} else if (eName.equalsIgnoreCase("_High")) {
					cfg.set_High(Short.parseShort(enclosedText));
				} else if (eName.equalsIgnoreCase("__Low")) {
					cfg.set__Low(Short.parseShort(enclosedText));
				} else if (eName.equalsIgnoreCase("__High")) {
					cfg.set__High(Short.parseShort(enclosedText));
				} else if (eName.equalsIgnoreCase("highThreshold")) {
					cfg.sethighThreshold(Short.parseShort(enclosedText));
				} else if (eName.equalsIgnoreCase("lowThreshold")) {
					cfg.setlowThreshold(Short.parseShort(enclosedText));
				} else if (eName.equalsIgnoreCase("device_Apm_Base")) {
					cfg.setdevice_Apm_Base(Double.parseDouble(enclosedText));
				} else if (eName.equalsIgnoreCase("min")) {
					cfg.setMin(Short.parseShort(enclosedText));
				} else if (eName.equalsIgnoreCase("max")) {
					cfg.setMax(Short.parseShort(enclosedText));
				} else if (eName.equalsIgnoreCase("baudRate")) {
					cfg.setBaudRate(Short.parseShort(enclosedText));
				} else if (eName.equalsIgnoreCase("preAmbleFactor")) {
					cfg.setPreAmbleFactor(Short.parseShort(enclosedText));
				} else if (eName.equalsIgnoreCase("directionOutputWave")) {
					cfg.setDirectionOutputWave(Short.parseShort(enclosedText));
					if (eAttr != null) {
						cfg.setVersionToTestOtherDirection(eAttr);
					}
				} else if (eName.equalsIgnoreCase("powerupWhenSwipe")) {
					cfg.setPowerupWhenSwipe(Short.parseShort(enclosedText));
				} else if (eName.equalsIgnoreCase("powerupLastBeforeCMD")) {
					cfg.setPowerupLastBeforeCMD(Short.parseShort(enclosedText));
				} else if (eName.equalsIgnoreCase("support_status")) {
					if (eAttr != null) {
						cfg.setSupportStatuses(readSupportStatusString(eAttr, useUm2i));
					}
				} else if (eName.equalsIgnoreCase("shuttle_channel")) {
					String chanValue = eAttr;
					if (chanValue != null) {
						chanValue = chanValue.trim();
						Byte shuttleChannel = null;
						if (chanValue.equalsIgnoreCase("n")) {
							shuttleChannel = Byte.valueOf((byte) 0);
						} else if (chanValue.equalsIgnoreCase("2")) {
							shuttleChannel = Byte.valueOf((byte) 32);
						} else if (chanValue.equalsIgnoreCase("1")) {
							shuttleChannel = Byte.valueOf((byte) 48);
						} else if (chanValue.equalsIgnoreCase("x")) {
							shuttleChannel = Byte.valueOf((byte) 8);
						}
						if (shuttleChannel != null) {
							cfg.setShuttleChannel(shuttleChannel.byteValue());
						}
					}
				} else if (eName.equalsIgnoreCase("force_headset_plug")) {
					cfg.setForceHeadsetPlug(Short.parseShort(enclosedText));
				} else if (eName.equalsIgnoreCase("volumeLevelAdjust")) {
					cfg.setVolumeLevelAdjust(Short.parseShort(enclosedText));
				} else if (eName.equalsIgnoreCase("useVoiceRecognition")) {
					cfg.setUseVoiceRecognition(Short.parseShort(enclosedText));
				} else if ((eName.equalsIgnoreCase("reverseAudioEvents")) && (Common.ConnectedReader != ReaderType.UNIJACK)) {
					cfg.setReverseAudioEvents(true);
				}

			} catch (NumberFormatException e) {
				ACLog.w("UmXmlParser", "Exception while reading tag <" + eName + ">: " + e.toString());
			}
		}

		private static SupportStatus[] readSupportStatusString(String statusString, boolean useUm2i) {
			SupportStatus[] supportStatuses = new SupportStatus[ReaderType.values().length];


			String[] kvs = statusString.split(";");
			for (String kv : kvs) {
				String[] k_v = kv.split("=");

				if (k_v.length == 2) {

					String um = k_v[0].trim().toLowerCase(Locale.US);
					String sup = k_v[1].trim().toLowerCase(Locale.US);

					ReaderType rt;
					if (um.equalsIgnoreCase("um2g")) {
						rt = ReaderType.UM;
					} else {
						if (um.equalsIgnoreCase("ump")) {
							rt = ReaderType.UM_PRO;
						} else {
							if (um.equalsIgnoreCase(useUm2i ? "um2i" : "um2")) {
								rt = ReaderType.UM_II;
							} else {
								if (um.equalsIgnoreCase("shuttle")) {
									rt = ReaderType.SHUTTLE;
								} else {
									if (!um.equalsIgnoreCase("unijack")) continue;
									rt = ReaderType.UNIJACK;
								}
							}
						}
					}
					SupportStatus ss;
					if (sup.equalsIgnoreCase("u")) {
						ss = SupportStatus.UNSUPPORTED;
					} else {
						if (sup.equalsIgnoreCase("s")) {
							ss = SupportStatus.SUPPORTED;
						} else {
							if (!sup.equalsIgnoreCase("m")) continue;
							ss = SupportStatus.MAYBE_SUPPORTED;
						}
					}


					supportStatuses[rt.ordinal()] = ss;
				}
			}
			return supportStatuses;
		}

		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			state_level += 1;
			if (state_skipOutOfLevel != null) {
				return;
			}
			switch (state_main) {
				case InManufacturer:
					if (localName.equalsIgnoreCase("IDTECHPRODUCTS")) {
						state_main = MainState.InRoot;
					} else {
						throw new SAXException("unexpected root element <" + localName + ">");
					}

					break;
				case InModel:
					if (localName.equalsIgnoreCase("Summary")) {
						state_main = MainState.InSummary;
					} else if (localName.equalsIgnoreCase("voice-recognition")) {
						state_main = MainState.InVoiceRecognition;
					} else {
						boolean isInTargetManufacturer;
						if (localName.equalsIgnoreCase("special_model_number")) {
							state_manType = ManType.SPECIAL;
							isInTargetManufacturer = true;
						} else {
							state_manType = ManType.NORMAL;
							isInTargetManufacturer = localName.equalsIgnoreCase(target_manufacturer);
						}

						if (isInTargetManufacturer) {
							state_main = MainState.InManufacturer;
						} else
							state_skipOutOfLevel = Integer.valueOf(state_level);
					}
					break;

				case InModelParam:
					if (localName.equalsIgnoreCase("Version")) {
						state_main = MainState.InVersion;
					} else {
						state_skipOutOfLevel = Integer.valueOf(state_level);
					}
					break;

				case InRoot:
					state_main = MainState.InVersionParam;
					state_versionName = localName;
					break;


				case InSummary:
					state_skipOutOfLevel = Integer.valueOf(state_level);
					break;
				case InVersion:
					boolean isInTargetManufacturer;
					if (localName.equalsIgnoreCase("templates")) {
						state_manType = ManType.TEMPLATE;
						isInTargetManufacturer = readTemplateEntries;
					} else {
						state_manType = ManType.VR;
						isInTargetManufacturer =
								localName.equalsIgnoreCase("prefix_vr_" + target_manufacturer);
					}

					if (isInTargetManufacturer) {
						state_main = MainState.InManufacturer;
					} else
						state_skipOutOfLevel = Integer.valueOf(state_level);
					break;

				case InVersionParam:
					Boolean isInTargetModel = null;
					String modelName = null;
					switch (state_manType) {
						case NORMAL:
						case SPECIAL:
							modelName = localName;
							isInTargetModel = Boolean.valueOf(modelName.equalsIgnoreCase(target_model));
							String deviceString = attributes.getValue("device");
							if (deviceString != null) {
								isInTargetModel = Boolean.valueOf(false);
								for (String device : deviceString.split(";")) {
									if (device.equalsIgnoreCase(Build.DEVICE)) {
										isInTargetModel = Boolean.valueOf(true);
										break;
									}
								}
							}

							break;
						case TEMPLATE:
							modelName = attributes.getValue("model_number");
							isInTargetModel = Boolean.valueOf(
									(localName.equalsIgnoreCase("phone")) &&
											(modelName != null) &&
											(modelName.equalsIgnoreCase(target_model)));
							break;
						case VR:
							modelName = localName;
							isInTargetModel = Boolean.valueOf(true);
					}


					if (isInTargetModel.booleanValue()) {
						state_main = MainState.InModel;

						ConfigParameters newProfile = new ConfigParameters();
						newProfile.setModelNumber(modelName);

						switch (state_manType) {
							case NORMAL:
								profile_nonVr = newProfile;
								break;
							case SPECIAL:
								profile_voiceRecognition = newProfile;
								newProfile.setUseVoiceRecognition((short) 1);
								break;
							case TEMPLATE:
								profile_specialModel = newProfile;
								break;
							case VR:
								result.templates.add(newProfile);
								newProfile.setUseVoiceRecognition((short) 1);
						}
					} else {
						state_skipOutOfLevel = Integer.valueOf(state_level);
					}
					break;

				case InVoiceRecognition:
					state_main = MainState.InModelParam;
					state_paramName = localName;
					state_paramAttr = attributes.getValue("val");
					break;


				case OutSideRoot:
					state_skipOutOfLevel = Integer.valueOf(state_level);
			}

		}


		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			state_level -= 1;
			if (state_skipOutOfLevel != null) {
				if (state_level >= state_skipOutOfLevel.intValue()) {
					return;
				}

				state_skipOutOfLevel = null;
				return;
			}

			switch (state_main) {
				case InManufacturer:
					break;


				case InModel:
					if (profile_specialModel != null) {
						result.config = profile_specialModel;
					} else if (profile_voiceRecognition != null) {
						result.config = profile_voiceRecognition;
					} else {
						result.config = profile_nonVr;
					}
					state_main = MainState.OutSideRoot;
					break;

				case InModelParam:
					state_main = MainState.InRoot;
					break;

				case InRoot:
					state_main = MainState.InSummary;
					break;

				case InSummary:
					if ("SDKMajorVersion".equalsIgnoreCase(state_versionName)) {
						result.SDKMajorVersion = state_versionTextContent;
					} else if ("SDKMinorVersion".equalsIgnoreCase(state_versionName)) {
						result.SDKMinorVersion = state_versionTextContent;
					} else if ("XMLVersion".equalsIgnoreCase(state_versionName)) {
						result.XMLVersion = state_versionTextContent;
					}


					state_main = MainState.InVersion;
					state_versionName = null;
					state_versionTextContent = null;
					break;

				case InVersion:
					state_main = MainState.InRoot;
					break;

				case InVersionParam:
					switch (state_manType) {
						case NORMAL:
						case TEMPLATE:
							state_main = MainState.InRoot;
							break;
						case SPECIAL:
						case VR:
							state_main = MainState.InVoiceRecognition;
					}


					state_manType = null;
					break;

				case InVoiceRecognition:
					state_main = MainState.InManufacturer;
					break;

				case OutSideRoot:
					ConfigParameters profileToFill = null;
					switch (state_manType) {
						case NORMAL:
							profileToFill = profile_nonVr;
							break;
						case SPECIAL:
							profileToFill = profile_voiceRecognition;
							break;
						case TEMPLATE:
							profileToFill = profile_specialModel;
							break;
						case VR:
							profileToFill = (ConfigParameters) result.templates.get(result.templates.size() - 1);
					}


					populateConfigEntry(profileToFill, state_paramName, state_paramAttr, state_paramTextContent, useUm2i);

					state_main = MainState.InModel;
					state_paramName = null;
					state_paramAttr = null;
					state_paramTextContent = null;
			}

		}


		public void characters(char[] ch, int start, int length)
				throws SAXException {
			if (state_skipOutOfLevel != null) {
				return;
			}
			switch (state_main) {
				case InSummary:
					state_versionTextContent = new String(ch, start, length);
					break;

				case OutSideRoot:
					state_paramTextContent = new String(ch, start, length);
					break;
			}

		}


		private static enum MainState {
			OutSideRoot, InRoot, InSummary, InVersion, InVersionParam, InVoiceRecognition,
			InManufacturer, InModel, InModelParam;
		}


		private static enum ManType {
			NORMAL, VR, SPECIAL, TEMPLATE;
		}
	}
}
