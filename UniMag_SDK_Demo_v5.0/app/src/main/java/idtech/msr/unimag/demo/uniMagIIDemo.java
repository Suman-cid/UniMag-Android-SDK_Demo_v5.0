package idtech.msr.unimag.demo;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import IDTech.MSR.XMLManager.StructConfigParameters;
import IDTech.MSR.uniMag.Common;
import IDTech.MSR.uniMag.Demo.R;
import IDTech.MSR.uniMag.StateList;
import IDTech.MSR.uniMag.uniMagReader;
import IDTech.MSR.uniMag.uniMagReaderMsg;
import IDTech.MSR.uniMag.UniMagTools.uniMagReaderToolsMsg;
import IDTech.MSR.uniMag.UniMagTools.uniMagSDKTools;
import IDTech.MSR.uniMag.uniMagReader.ReaderType;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnKeyListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.BufferType;
/*
 * 
 * File name: 	uniMagIIDemo.java
 * Author:		Eric.Yang
 * Time:		2011.10.21
 * 
 * Modified by Jimmy Mo on 2012.09.21
 * to make demo application more readable
 * 
 */

// interface uniMagReaderMsg should be implemented
// if firmware download is supported, uniMagReaderToolsMsg also needs to be implemented 
public class uniMagIIDemo extends Activity implements  uniMagReaderMsg ,uniMagReaderToolsMsg{

	// declaring the instance of the uniMagReader;
	private uniMagReader myUniMagReader = null;
	private uniMagSDKTools firmwareUpdateTool = null;
	
	private TextView connectStatusTextView; // displays status of UniMag Reader: Connected / Disconnected
	private TextView headerTextView; // short description of data displayed below
	private TextView textAreaTop;
	private EditText textAreaBottom;
	private Button btnCommand;
	private Button btnSwipeCard;
	private boolean isReaderConnected = false;
	private boolean isExitButtonPressed = false;
	private boolean isWaitingForCommandResult=false;
	private boolean isSaveLogOptionChecked = false;
	private boolean isUseAutoConfigProfileChecked = false;
//	private boolean isConnectWithCommand = true;
	private int readerType = -1; // 0: UniMag, 1: UniMag II
	
	//update the powerup status
	private int percent = 0;
	private long beginTime = 0;
	private long beginTimeOfAutoConfig = 0;
	private byte[] challengeResponse = null;

	private String popupDialogMsg = null;
	private boolean enableSwipeCard =false;
	private boolean autoconfig_running = false;	

	private String strMsrData = null;
	private byte[] msrData = null;
	private String statusText = null;
	private int challengeResult = 0;
	
	

	/*****************************************	
	CREATE TABLE profiles ( 
		search_date DATETIME,
		direction_output_wave INTEGER,
		input_frequency INTEGER,
		output_frequency INTEGER,
		record_buffer_size INTEGER,
		read_buffer_size INTEGER,
		wave_direction INTEGER,
		_low INTEGER,
		_high INTEGER,
		__low INTEGER,
		__high INTEGER,
		high_threshold INTEGER,
		low_threshold INTEGER,
		device_apm_base INTEGER,
		min INTEGER,
		max INTEGER,
		baud_rate INTEGER,
		preamble_factor INTEGER,
		set_default INTEGER)
		
	)
	*****************************************/
	
	static private final int REQUEST_GET_XML_FILE = 1;
	static private final int REQUEST_GET_BIN_FILE = 2;
	static private final int REQUEST_GET_ENCRYPTED_BIN_FILE = 3;
	
	//property for the menu item.
	static final private int START_SWIPE_CARD 	= Menu.FIRST;
	static final private int SETTINGS_ITEM 		= Menu.FIRST + 2;
	static final private int SUB_SAVE_LOG_ITEM 	= Menu.FIRST + 3;
	static final private int SUB_USE_AUTOCONFIG_PROFILE = Menu.FIRST + 4;
	static final private int SUB_SELECT_READER = Menu.FIRST + 5;
	static final private int SUB_LOAD_XML 		= Menu.FIRST + 6;
	static final private int SUB_LOAD_BIN 		= Menu.FIRST + 7;
	static final private int SUB_START_AUTOCONFIG= Menu.FIRST + 8;
	static final private int SUB_STOP_AUTOCONFIG = Menu.FIRST + 10;
	static final private int SUB_ATTACHED_TYPE 	= Menu.FIRST + 103;
	static final private int SUB_SUPPORT_STATUS	= Menu.FIRST + 104;
	static final private int DELETE_LOG_ITEM 	= Menu.FIRST + 11;   
	static final private int ABOUT_ITEM 		= Menu.FIRST + 12;  
	static final private int EXIT_IDT_APP 		= Menu.FIRST + 13;
	static final private int SUB_LOAD_ENCRYPTED_BIN = Menu.FIRST + 14;
    
	private MenuItem itemStartSC = null;
	private MenuItem itemSubSaveLog = null;
	private MenuItem itemSubUseAutoConfigProfile = null;
	private MenuItem itemSubSelectReader = null;
	private MenuItem itemSubLoadXML = null;
	private MenuItem itemSubStartAutoConfig = null;
	private MenuItem itemSubStopAutoConfig = null;
	private MenuItem itemDelLogs = null;   
	private MenuItem itemAbout = null;
	private MenuItem itemExitApp = null;
	
	private SubMenu sub = null;
    
	private UniMagTopDialog dlgTopShow = null ;
	private UniMagTopDialog dlgSwipeTopShow = null ;
	private UniMagTopDialogYESNO dlgYESNOTopShow = null ;

	private StructConfigParameters profile = null;
	private ProfileDatabase profileDatabase = null;
	private Handler handler = new Handler();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.main);
    	profileDatabase = new ProfileDatabase(this);
    	profileDatabase.initializeDB();
    	isUseAutoConfigProfileChecked = profileDatabase.getIsUseAutoConfigProfile();

    	openReaderSelectDialog();

    	initializeUI();
//    	initializeReader();
	
    	// to prevent screen timeout
    	getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
    
    void openReaderSelectDialog(){
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Select a reader:");
    	builder.setCancelable(false);
    	builder.setItems(R.array.reader_type, new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				
				switch(which) {
				case 0:
					readerType = 0;
					initializeReader(ReaderType.UM_OR_PRO);
					Toast.makeText(getApplicationContext(), "UniMag / UniMag Pro selected", Toast.LENGTH_SHORT).show();
					break;
				case 1:
					readerType = 1;
					initializeReader(ReaderType.SHUTTLE);
					Toast.makeText(getApplicationContext(), "UniMag II / Shuttle selected", Toast.LENGTH_SHORT).show();
					break;
				default:
					readerType = 1;
					initializeReader(ReaderType.SHUTTLE);
					Toast.makeText(getApplicationContext(), "UniMag II / Shuttle selected", Toast.LENGTH_SHORT).show();
					break;
				}
	        	showAboutInfo();		
			}
		});
    	builder.create().show();
    }

	@Override
	protected void onPause() {
		if(myUniMagReader!=null)
		{
			//stop swipe card when the application go to background
			myUniMagReader.stopSwipeCard();
		}
		hideTopDialog();
		hideSwipeTopDialog();
		super.onPause();
	}
	@Override
	protected void onResume() {
		if(myUniMagReader!=null)
		{
			if(isSaveLogOptionChecked==true)
				myUniMagReader.setSaveLogEnable(true);
			else
				myUniMagReader.setSaveLogEnable(false);
		}
		if(itemStartSC!=null)
			itemStartSC.setEnabled(true); 
		isWaitingForCommandResult=false;
		super.onResume();
	}
	@Override
    protected void onDestroy() {
		if (myUniMagReader != null)
			myUniMagReader.release();
		profileDatabase.closeDB();
		super.onDestroy();
		if (isExitButtonPressed)
		{
			android.os.Process.killProcess(android.os.Process.myPid());
		}
    }
	
    public synchronized void onActivityResult(final int requestCode, int resultCode, final Intent data) {

        if (resultCode == Activity.RESULT_OK) {

        	String strTmpFileName = data.getStringExtra(FileDialog.RESULT_PATH);;
            if (requestCode == REQUEST_GET_XML_FILE) {
	    		
	    		if(!isFileExist(strTmpFileName))
	    		{ 
	    			headerTextView.setText("Warning");
	    			textAreaTop.setText("Please copy the XML file 'IDT_uniMagCfg.xml' into root path of SD card.");
	    			textAreaBottom.setText("");
	    			return  ;
	    		}
	    		if (!strTmpFileName.endsWith(".xml")){
	    			headerTextView.setText("Warning");
	    			textAreaTop.setText("Please select a file with .xml file extension.");
	    			textAreaBottom.setText("");
	    			return  ;
	    		}
	    		
	    		/////////////////////////////////////////////////////////////////////////////////
	    		// loadingConfigurationXMLFile() method may connect to server to download xml file.
	    		// Network operation is prohibited in the UI Thread if target API is 11 or above.
	    		// If target API is 11 or above, please use AsyncTask to avoid errors.
	    	    myUniMagReader.setXMLFileNameWithPath(strTmpFileName);
	    	    if (myUniMagReader.loadingConfigurationXMLFile(false)) {
		    	    headerTextView.setText("Command Info");
		    	    textAreaTop.setText("Reload XML file succeeded.");
		    	    textAreaBottom.setText("");
	    	    }
	    	    else {
	    			headerTextView.setText("Warning");
	    			textAreaTop.setText("Please select a correct file and try again.");
	    			textAreaBottom.setText("");
	    	    }
            } 
            else if (requestCode == REQUEST_GET_BIN_FILE)
            {
 	    		if(!isFileExist(strTmpFileName))
	    		{ 
 	    			headerTextView.setText("Warning");
 	    			textAreaTop.setText("Please copy the BIN file into the SD card root path.");
 	    			textAreaBottom.setText("");
	    			return  ;
	    		} 
				//set BIN file
		        if(true==firmwareUpdateTool.setFirmwareBINFile(strTmpFileName))
		        {
		        	headerTextView.setText("Command Info");
		        	textAreaTop.setText("Set the BIN file succeeded.");
		        	textAreaBottom.setText("");
	    		}
		        else
		        {
		        	headerTextView.setText("Command Info");
		        	textAreaTop.setText("Failed to set the BIN file, please check the file format.");
		        	textAreaBottom.setText("");
		        }
            }
            else if(requestCode == REQUEST_GET_ENCRYPTED_BIN_FILE)
            {

 	    		if(!isFileExist(strTmpFileName))
	    		{ 
 	    			headerTextView.setText("Warning");
 	    			textAreaTop.setText("Please copy the BIN file into the SD card root path.");
 	    			textAreaBottom.setText("");
	    			return  ;
	    		} 
				//set BIN file
		        if(true==firmwareUpdateTool.setFirmwareEncryptedBINFile(strTmpFileName))
		        {
		        	headerTextView.setText("Command Info");
		        	textAreaTop.setText("Set the Encrypted BIN file succeeded.");
		        	textAreaBottom.setText("");
	    		}
		        else
		        {
		        	headerTextView.setText("Command Info");
		        	textAreaTop.setText("Failed to set the Encrypted BIN file, please check the file format.");
		        	textAreaBottom.setText("");
		        }
            }
        } 
    }

	private void initializeUI()
	{
		btnSwipeCard = (Button)findViewById(R.id.btn_swipeCard);
		btnCommand = (Button)findViewById(R.id.btn_command);
		textAreaTop = (TextView)findViewById(R.id.text_area_top);
		textAreaBottom = (EditText)findViewById(R.id.text_area_bottom);
		connectStatusTextView = (TextView)findViewById(R.id.status_text);
		headerTextView = (TextView)findViewById(R.id.header_text);
	
		headerTextView.setText("MSR Data");
		connectStatusTextView.setText("DISCONNECTED");
		getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN, WindowManager.LayoutParams. FLAG_FULLSCREEN);

		// Set Listener for "Swipe Card" Button
		btnSwipeCard.setOnClickListener(new OnClickListener(){  
			public void onClick(View v) {
				if (myUniMagReader!=null)
				{	
					if (!isWaitingForCommandResult) 
					{
						beginTime = getCurrentTime();

						if(myUniMagReader.startSwipeCard())
						{
							headerTextView.setText("MSR Data");
							textAreaTop.setText("Please wait for reader to be ready");
							textAreaBottom.setText("");
							Log.d("","to startSwipeCard");
						}
						else
							Log.d("","cannot startSwipeCard");
					}
				}
			}  
		});  

		// Set Listener for "Command" Button
		btnCommand.setOnClickListener(new OnClickListener(){  
			public void onClick(View v) {
				if (!isWaitingForCommandResult)
				{
					DlgSettingOption myDlg = new DlgSettingOption (uniMagIIDemo.this,myUniMagReader);
					myDlg.DisplayDlg();
				}
			}  
		});  
	}
	
	private void initializeReader(ReaderType type)
	{
		if(myUniMagReader!=null){
			myUniMagReader.unregisterListen();
			myUniMagReader.release();
			myUniMagReader = null;
		}
		myUniMagReader = new uniMagReader(this,this,type);

		if (myUniMagReader == null)
			return;
		
		myUniMagReader.setVerboseLoggingEnable(true);
        myUniMagReader.registerListen();
        
        //load the XML configuratin file
        String fileNameWithPath = getConfigurationFileFromRaw();
        if(!isFileExist(fileNameWithPath)) { 
        	fileNameWithPath = null; 
        }

        if (isUseAutoConfigProfileChecked) {
			if (profileDatabase.updateProfileFromDB()) {
				this.profile = profileDatabase.getProfile();
				Toast.makeText(this, "AutoConfig profile has been loaded.", Toast.LENGTH_LONG).show();
				handler.post(doConnectUsingProfile);
			}
			else {
				Toast.makeText(this, "No profile found. Please run AutoConfig first.", Toast.LENGTH_LONG).show();
			}
        } else {
	        /////////////////////////////////////////////////////////////////////////////////
			// Network operation is prohibited in the UI Thread if target API is 11 or above.
			// If target API is 11 or above, please use AsyncTask to avoid errors.
	        myUniMagReader.setXMLFileNameWithPath(fileNameWithPath);
	        myUniMagReader.loadingConfigurationXMLFile(true);
		    /////////////////////////////////////////////////////////////////////////////////
        }
        //Initializing SDKTool for firmware update
        firmwareUpdateTool = new uniMagSDKTools(this,this);
        firmwareUpdateTool.setUniMagReader(myUniMagReader);
        myUniMagReader.setSDKToolProxy(firmwareUpdateTool.getSDKToolProxy());		
		
	}
	

	private void initializeReader()
	{
/*
		if(myUniMagReader!=null){
			myUniMagReader.unregisterListen();
			myUniMagReader.release();
			myUniMagReader = null;
		}
//		if (isConnectWithCommand)
			myUniMagReader = new uniMagReader(this,this,true);
//		else 
//		myUniMagReader = new uniMagReader(this,this);
*/
		if (myUniMagReader == null)
			return;
		
		myUniMagReader.setVerboseLoggingEnable(true);
        myUniMagReader.registerListen();
        
        //load the XML configuratin file
        String fileNameWithPath = getConfigurationFileFromRaw();
        if(!isFileExist(fileNameWithPath)) { 
        	fileNameWithPath = null; 
        }

        if (isUseAutoConfigProfileChecked) {
			if (profileDatabase.updateProfileFromDB()) {
				this.profile = profileDatabase.getProfile();
				Toast.makeText(this, "AutoConfig profile has been loaded.", Toast.LENGTH_LONG).show();
				handler.post(doConnectUsingProfile);
			}
			else {
				Toast.makeText(this, "No profile found. Please run AutoConfig first.", Toast.LENGTH_LONG).show();
			}
        } else {
	        /////////////////////////////////////////////////////////////////////////////////
			// Network operation is prohibited in the UI Thread if target API is 11 or above.
			// If target API is 11 or above, please use AsyncTask to avoid errors.
	        myUniMagReader.setXMLFileNameWithPath(fileNameWithPath);
	        myUniMagReader.loadingConfigurationXMLFile(true);
		    /////////////////////////////////////////////////////////////////////////////////
        }
        //Initializing SDKTool for firmware update
        firmwareUpdateTool = new uniMagSDKTools(this,this);
        firmwareUpdateTool.setUniMagReader(myUniMagReader);
        myUniMagReader.setSDKToolProxy(firmwareUpdateTool.getSDKToolProxy());
	}
	private String getConfigurationFileFromRaw( ){
		return getXMLFileFromRaw("idt_unimagcfg_default.xml");
		}
	private String getAutoConfigProfileFileFromRaw( ){
		//share the same copy with the configuration file
		return getXMLFileFromRaw("idt_unimagcfg_default.xml");
		}
	    
	// If 'idt_unimagcfg_default.xml' file is found in the 'raw' folder, it returns the file path.
	private String getXMLFileFromRaw(String fileName ){
		//the target filename in the application path
		String fileNameWithPath = null;
		fileNameWithPath = fileName;
	
		try {
			InputStream in = getResources().openRawResource(R.raw.idt_unimagcfg_default);
			int length = in.available();
			byte [] buffer = new byte[length];
			in.read(buffer);    	   
			in.close();
			deleteFile(fileNameWithPath);
			FileOutputStream fout = openFileOutput(fileNameWithPath, MODE_PRIVATE);
			fout.write(buffer);
			fout.close();
    	   
			// to refer to the application path
			File fileDir = this.getFilesDir();
			fileNameWithPath = fileDir.getParent() + java.io.File.separator + fileDir.getName();
			fileNameWithPath += java.io.File.separator+"idt_unimagcfg_default.xml";
	   	   
		} catch(Exception e){
			e.printStackTrace();
			fileNameWithPath = null;
		}
		return fileNameWithPath;
	}
	
    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if ((keyCode == KeyEvent.KEYCODE_BACK||KeyEvent.KEYCODE_HOME==keyCode||KeyEvent.KEYCODE_SEARCH==keyCode)){
			return false;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
   	if ((keyCode == KeyEvent.KEYCODE_BACK||KeyEvent.KEYCODE_HOME==keyCode||KeyEvent.KEYCODE_SEARCH==keyCode)){
    		 
			return false;
		}	return super.onKeyMultiple(keyCode, repeatCount, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
    	if ((keyCode == KeyEvent.KEYCODE_BACK||KeyEvent.KEYCODE_HOME==keyCode||KeyEvent.KEYCODE_SEARCH==keyCode)){
 			return false;
		}
    	return super.onKeyUp(keyCode, event);
	}

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId())
	    {
       		// when the 'swipe card' menu item clicked
	    	case (START_SWIPE_CARD):
	    	{
	    		headerTextView.setText("MSR Data");
	    		textAreaTop.setText("");
	    		textAreaBottom.setText("");
        		//itemStartSC.setEnabled(false); 
        	
        		if(myUniMagReader!=null)
        			myUniMagReader.startSwipeCard();
	    		break;
	    	}
	    	// when the 'exit' menu item clicked
	    	case (EXIT_IDT_APP):
	    	{
	    		isExitButtonPressed = true;
        		if(myUniMagReader!=null)
        		{
        			myUniMagReader.unregisterListen();
        			myUniMagReader.stopSwipeCard();
        			myUniMagReader.release();
        		}
        		finish();
	    		break;
	    	}
	    	// If save log option is already enabled, put a check mark whenever settings menu is clicked.   
	    	case (SETTINGS_ITEM):
	    	{
	    		if(itemSubSaveLog!=null)
	    			itemSubSaveLog.setChecked(isSaveLogOptionChecked);
	    		if(itemSubUseAutoConfigProfile!=null)
	    			itemSubUseAutoConfigProfile.setChecked(isUseAutoConfigProfileChecked);
//	    		if(itemSubUseCommandToConnect!=null)
//	    			itemSubUseCommandToConnect.setChecked(isConnectWithCommand);
	    		break;
	    	}
	    	// deleting log files in the sd card.
	    	case (DELETE_LOG_ITEM):
	    	{
        		if(myUniMagReader!=null)
        			myUniMagReader.deleteLogs();
	    		break;		    		
	    	}
	    	// showing manufacturer, model number, SDK version, and OS Version information if clicked.
	    	case (ABOUT_ITEM):
	    	{
	    		showAboutInfo();
	    		break;		    		
	    	}
	    	// user can manually load a configuration file (xml), which should be located in the sd card.  
	    	case (SUB_LOAD_XML):
	    	{
	    		String strTmpFileName = getMyStorageFilePath();
	    		if (strTmpFileName == null)
	    		{
	    			headerTextView.setText("Warning");
	    			textAreaTop.setText("Please insert SD card.");
	    			textAreaBottom.setText("");
	    			return false;
	    		}
            	FileDialog fileDialog = new FileDialog();
            	Intent intent = new Intent( getBaseContext(), fileDialog.getClass());
				intent.putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory().getPath());
				startActivityForResult(intent, REQUEST_GET_XML_FILE);
	    		break;
	    	}
	    	// in order to update firmware of reader, user needs to set a firmware file (.bin) first.
	    	// this menu allows to user to update firmware from v1.x to later version (v2.x or v3.x).
	    	case (SUB_LOAD_BIN):
	    	{
	    		headerTextView.setText("Command Info");

	    		String strTmpFileName = getMyStorageFilePath();
	    		if (strTmpFileName == null)
	    		{
	    			headerTextView.setText("Warning");
	    			textAreaTop.setText("Please insert SD card.");
	    			textAreaBottom.setText("");
	    			return false;
	    		}
            	FileDialog fileDialog = new FileDialog();
            	Intent intent = new Intent( getBaseContext(), fileDialog.getClass());
				intent.putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory().getPath());
				startActivityForResult(intent, REQUEST_GET_BIN_FILE);
	    		break;
	    	}
	    	// Bin file should be encrypted in order to update from v2.x or v3.x to later version. 
	    	case (SUB_LOAD_ENCRYPTED_BIN):
	    	{
	    		headerTextView.setText("Command Info");

	    		String strTmpFileName = getMyStorageFilePath();
	    		if (strTmpFileName == null)
	    		{
	    			headerTextView.setText("Warning");
	    			textAreaTop.setText("Please insert SD card.");
	    			textAreaBottom.setText("");
	    			return false;
	    		}
            	FileDialog fileDialog = new FileDialog();
            	Intent intent = new Intent( getBaseContext(), fileDialog.getClass());
				intent.putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory().getPath());
				startActivityForResult(intent, REQUEST_GET_ENCRYPTED_BIN_FILE);
	    		break;
	    	}
	    	case (SUB_START_AUTOCONFIG):
	    	{
	    		String fileNameWithPath = getAutoConfigProfileFileFromRaw();
    	        if(!isFileExist(fileNameWithPath)) {
    	        	fileNameWithPath = null; 
    	        }  
	    		boolean startAcRet = myUniMagReader.startAutoConfig(fileNameWithPath,true);
	    		if (startAcRet)
	    		{
    	    		strProgressInfo=null;
    	    		handler.post(doUpdateAutoConfigProgressInfo);
    	    		percent = 0;
    	    		beginTime = getCurrentTime();  
    	    		autoconfig_running = true;
	    		}
	    		break;
	    	}
	    	case (SUB_STOP_AUTOCONFIG):
	    	{
	    		if(autoconfig_running==true)
	    		{
		    		myUniMagReader.stopAutoConfig();
		    		myUniMagReader.unregisterListen();
		    		myUniMagReader.release();                       
	
		    		percent = 0;
		    		// Reinitialize the reader if AutoConfig has been stopped.
		    		switch (readerType)	{
						case 0:
							readerType = 0;
							initializeReader(ReaderType.UM_OR_PRO);
							Toast.makeText(getApplicationContext(), "UniMag / UniMag Pro selected", Toast.LENGTH_SHORT).show();
							break;
						case 1:
							readerType = 1;
							initializeReader(ReaderType.SHUTTLE);
							Toast.makeText(getApplicationContext(), "UniMag II / Shuttle selected", Toast.LENGTH_SHORT).show();
							break;
						default:
							readerType = 1;
							initializeReader(ReaderType.SHUTTLE);
							Toast.makeText(getApplicationContext(), "UniMag II / Shuttle selected", Toast.LENGTH_SHORT).show();
							break;
		    		}
		        	showAboutInfo();		

//		    		initializeReader();
		    		autoconfig_running = false;
	    		}
	    		break;
	    	}  
	    	// when the 'save option' menu item clicked 
	    	case (SUB_SAVE_LOG_ITEM):
	    	{
	    		if(item.isChecked())
	    		{
	    			myUniMagReader.setSaveLogEnable(false);		   
	    			item.setChecked(false);
	    			isSaveLogOptionChecked = false;

	    		}
	    		else
	    		{
	    			//cannot enable the item when you are swiping the card.
	    			if(myUniMagReader.isSwipeCardRunning()==true)
	    			{
	    				item.setChecked(true);
	    				myUniMagReader.setSaveLogEnable(true);
	    				isSaveLogOptionChecked = true;
	    			}
	    		} 
	    		break;
	    	}
	    	
	    	case (SUB_USE_AUTOCONFIG_PROFILE):
	    	{
	    		if (!isReaderConnected) {
		    		if (item.isChecked()) {
		    			item.setChecked(false);
		    			isUseAutoConfigProfileChecked = false;
		    			
		    			profileDatabase.uncheckOnUseAutoConfigProfile();
		    			// change back to default profile
	    				initializeReader();
	    				
		    		} else {
		    			if (profileDatabase.updateProfileFromDB()) {
		    				this.profile = profileDatabase.getProfile();
		    				item.setChecked(true);
		    				isUseAutoConfigProfileChecked = true;
		    				profileDatabase.checkOnUseAutoConfigProfile();
		    			} else {
		    				AlertDialog.Builder builder = new AlertDialog.Builder(this);
		    				builder.setTitle("Warning");
		    				builder.setMessage("No profile found. Please run AutoConfig first.");
		    				builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
								}
							});
		    				AlertDialog alert = builder.create();		
		    				alert.show();	 		    				
		    			}
		    		}
	    		} else {
    				AlertDialog.Builder builder = new AlertDialog.Builder(this);
    				builder.setTitle("Warning");
    				builder.setMessage("Please detach the reader in order to change a profile.");
    				builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
						}
					});
    				AlertDialog alert = builder.create();		
    				alert.show();		    			
	    		}
	    		
	    		break;
	    	}
	    	case (SUB_SELECT_READER):
	    	{
	    		openReaderSelectDialog();
/*
	    		
	    		
	    		
	    		if (!isReaderConnected) {
	    			if (item.isChecked()) {
	    				isConnectWithCommand = false;
	    				initializeReader();
	    			} else {
//	    				isConnectWithCommand = true;
//	    				initializeReader();
	    				
	    				AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    				builder.setTitle("Caution");
	    				builder.setMessage("Please note that older generation of UniMag Readers (UniMag & UniMag Pro) won't be connected if this option checked.");
	    				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
							}
						});
	    				
	    				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							
							public void onClick(DialogInterface dialog, int which) {
			    				isConnectWithCommand = true;
			    				initializeReader();
							}
						});
	    				AlertDialog alert = builder.create();		
	    				alert.show();
	    			}
	    		} else {
    				AlertDialog.Builder builder = new AlertDialog.Builder(this);
    				builder.setTitle("Information");
    				builder.setMessage("Please detach the reader in order to change the setting.");
    				builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
						}
					});

    				AlertDialog alert = builder.create();		
    				alert.show();	    			
	    		}
*/
	    		
		    	break;
	    	}
	    	// displays attached reader type
	    	case SUB_ATTACHED_TYPE:
	    		ReaderType art = myUniMagReader.getAttachedReaderType();
	    		
	    		Log.e("Attached reader","Returned reader type: "+art);
	    		
	    		if (art == null)
	    		{
		    		textAreaTop.setText("Please connect a reader first.");
		    		textAreaBottom.setText("");
	    		}
	    		else if(art==ReaderType.UNKNOWN)
	    		{
		    		textAreaTop.setText("To get Attached Reader type, waiting for response.");
		    		textAreaBottom.setText("");
 	    		}
	    		else
	    		{
		    		textAreaTop.setText("Attached Reader:\n   "+getReaderName(art));
		    		textAreaBottom.setText("");
	    		}
	    		break;
	    	// displays support status of all ID Tech readers  
	    	case SUB_SUPPORT_STATUS:
	    		//print a list of reader:supported status pairs
	    		textAreaTop.setText("Reader support status from cfg:\n");
	    		for (ReaderType rt : ReaderType.values()) {
	    			if (rt == ReaderType.UM || rt == ReaderType.UM_PRO || rt == ReaderType.UM_II || rt == ReaderType.SHUTTLE)
	    				textAreaTop.append(getReaderName(rt)+" : "+myUniMagReader.getSupportStatus(rt)+"\n");
	    		}
	    		textAreaBottom.setText("");
	    		break;
    	}
       	return super.onOptionsItemSelected(item);
	}

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		itemStartSC = menu.add(0,START_SWIPE_CARD, Menu.NONE, "Swipe Card");
		itemStartSC.setEnabled(true); 
		sub = menu.addSubMenu(0,SETTINGS_ITEM,Menu.NONE,"Settings");
		itemSubSaveLog = sub.add(0,SUB_SAVE_LOG_ITEM,Menu.NONE,"Save Log option");
		itemSubUseAutoConfigProfile = sub.add(1, SUB_USE_AUTOCONFIG_PROFILE, Menu.NONE, "Use AutoConfig profile");
		itemSubSelectReader = sub.add(1, SUB_SELECT_READER, Menu.NONE, "Change reader type");
		itemSubLoadXML = sub.add(1,SUB_LOAD_XML,Menu.NONE,"Reload XML");
		itemSubStartAutoConfig = sub.add(4,SUB_START_AUTOCONFIG,Menu.NONE,"Start AutoConfig");
		itemSubStopAutoConfig = sub.add(6,SUB_STOP_AUTOCONFIG,Menu.NONE,"Stop AutoConfig");
		sub.add(Menu.NONE,SUB_ATTACHED_TYPE,Menu.NONE,"Get attached type");
		sub.add(Menu.NONE,SUB_SUPPORT_STATUS,Menu.NONE,"Get support status");
		itemSubSaveLog.setCheckable(true);
		itemSubUseAutoConfigProfile.setCheckable(true);
		itemSubLoadXML.setEnabled(true); 
		itemSubStartAutoConfig.setEnabled(true); 
		itemSubStopAutoConfig.setEnabled(true); 
		itemDelLogs = menu.add(0,DELETE_LOG_ITEM,Menu.NONE,"Delete Logs");
		itemDelLogs.setEnabled(true); 
		itemAbout = menu.add(0,ABOUT_ITEM,Menu.NONE,"About");
		itemAbout.setEnabled(true); 
		itemExitApp = menu.add(0,EXIT_IDT_APP,Menu.NONE,"Exit");
		itemExitApp.setEnabled(true); 
		return super.onCreateOptionsMenu(menu);
	}

    // Returns reader name based on abbreviations 
    private String getReaderName(ReaderType rt){
    	switch(rt){
    	case UM:
    		return "UniMag";
    	case UM_PRO:
    		return "UniMag Pro";
    	case UM_II:
    		return "UniMag II";
    	case SHUTTLE:
    		return "Shuttle";
    	case UM_OR_PRO:
    		return "UniMag or UniMag Pro";
    	}
    	return "Unknown";
    	
    }
    //for uniMagReader.getAttachedReaderType()
    public ReaderType getAttachedReaderType(int uniMagUnit) {
    	switch (uniMagUnit) {
    	case StateList.uniMag2G3GPro:
    		return ReaderType.UM_OR_PRO;
    	case StateList.uniMagII:
    		return ReaderType.UM_II;
    	case StateList.uniMagShuttle:
    		return ReaderType.SHUTTLE;
    	case StateList.uniMagUnkown:
    	default:
    		return ReaderType.UNKNOWN;
    	}
    }
    private void showAboutInfo()
    {
		String strManufacture = myUniMagReader.getInfoManufacture();
		String strModel = myUniMagReader.getInfoModel();
		String strDevice = android.os.Build.DEVICE;
		String strSDKVerInfo = myUniMagReader.getSDKVersionInfo();
		String strXMLVerInfo = myUniMagReader.getXMLVersionInfo();
		String selectedReader;
		if (readerType == 0)
			selectedReader = "UniMag/UniMag Pro";
		else if (readerType == 1)
			selectedReader = "UniMag II/Shuttle";
		else
			selectedReader = "Unknown";
		
		headerTextView.setText("SDK Info");
		textAreaBottom.setText("");
		String strOSVerInfo = android.os.Build.VERSION.RELEASE;
//		Log.e("Device", android.os.Build.DEVICE);
//		Log.e("Product", android.os.Build.PRODUCT);
//		Log.e("Brand", android.os.Build.BRAND);
//		Log.e("Board", android.os.Build.BOARD);
//		Log.e("Manufacturer", android.os.Build.MANUFACTURER);
//		Log.e("Model", android.os.Build.MODEL);
//		Log.e("ID", android.os.Build.ID);
    	textAreaTop.setText("Phone: "+strManufacture+"\n"+"Model: "+strModel+"\nDevice: "+strDevice+"\nSDK Ver: "+strSDKVerInfo+"\nXML Ver: "+strXMLVerInfo+"\nOS Version: "+strOSVerInfo+"\nReader Type: "+selectedReader);

    }    
	private Runnable doShowTimeoutMsg = new Runnable()
	{
		public void run()
		{
			if(itemStartSC!=null&&enableSwipeCard==true)
				itemStartSC.setEnabled(true); 
			enableSwipeCard = false;
			showDialog(popupDialogMsg);
		}

	};
	// shows messages on the popup dialog
	private void showDialog(String strTitle)
	{
		try
		{
	        AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        builder.setTitle("UniMag");
	        builder.setMessage(strTitle);
	        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	
	            public void onClick(DialogInterface dialog, int which) {
	                dialog.dismiss();
	            }
	        });
	        builder.create().show();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	};	
 
	private Runnable doShowTopDlg = new Runnable()
	{
		public void run()
		{
			showTopDialog(popupDialogMsg);
		}
	};	
	private Runnable doHideTopDlg = new Runnable()
	{
		public void run()
		{
			hideTopDialog( );
		}

	};	
	private Runnable doShowSwipeTopDlg = new Runnable()
	{
		public void run()
		{
			showSwipeTopDialog( );
		}
	};	
	private Runnable doShowYESNOTopDlg = new Runnable()
	{
		public void run()
		{
			showYesNoDialog( );
		}
	};	
	private Runnable doHideSwipeTopDlg = new Runnable()
	{
		public void run()
		{
			hideSwipeTopDialog( );
		}
	};	
	// displays result of commands, autoconfig, timeouts, firmware update progress and results.
	private Runnable doUpdateStatus = new Runnable()
	{
		public void run()
		{
			try
			{
				textAreaTop.setText(statusText);
				headerTextView.setText("Command Info");
	    		if(msrData!=null)
	    		{
	            StringBuffer hexString = new StringBuffer();
	            
	            hexString.append("<");
	            String fix = null;
	            for (int i = 0; i < msrData.length; i++) {
	            	fix = Integer.toHexString(0xFF & msrData[i]);
	            	if(fix.length()==1)
	            		fix = "0"+fix;
	                hexString.append(fix);
	                if((i+1)%4==0&&i!=(msrData.length-1))
	                	hexString.append(' ');
	            }
	            hexString.append(">");
	            textAreaBottom.setText(hexString.toString());
	    		}
	    		else
	    			textAreaBottom.setText("");
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
				
			}
		}
	};
	// displays result of commands, autoconfig, timeouts, firmware update progress and results.
	private Runnable doUpdateAutoConfigProgress = new Runnable()
	{
		public void run()
		{
			try
			{
				textAreaTop.setText(statusText);
				headerTextView.setText("Command Info");
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
				
			}
		}
	};
	String strProgressInfo = "";
	// displays result of commands, autoconfig, timeouts, firmware update progress and results.
	private Runnable doUpdateAutoConfigProgressInfo = new Runnable()
	{
		public void run()
		{
			try
			{

	    		if(strProgressInfo!=null)
	    		{
	            textAreaBottom.setText(strProgressInfo);
	    		}
	    		else
	    			textAreaBottom.setText("");
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
				
			}
		}
	};
	// displays result of get challenge command 
	private Runnable doUpdateChallengeData = new Runnable()
	{
		public void run()
		{
			try
			{
				textAreaTop.setText(statusText);
				headerTextView.setText("Command Info");
	    		if(cmdGetChallenge_Succeed_WithChallengData==challengeResult)
	    		{
	    			textAreaBottom.setText("");
					textAreaBottom.setText( textAreaBottom.getText(), BufferType.EDITABLE);
					textAreaBottom.setEnabled(true);
					textAreaBottom.setClickable(true);
					textAreaBottom.setFocusable(true);
				}
	    		else if (cmdGetChallenge_Succeed_WithFileVersion==challengeResult)
	    		{
	    			textAreaBottom.setText("");
	    			textAreaBottom.setText( textAreaBottom.getText(), BufferType.EDITABLE);
	    			textAreaBottom.setEnabled(true);
	    			textAreaBottom.setClickable(true);
	    			textAreaBottom.setFocusable(true);
				}
	    		else
	    			textAreaBottom.setText("");
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
				
			}
		}
	};
	// displays data from card swiping
	private Runnable doUpdateTVS = new Runnable()
	{
		public void run()
		{
			try
			{
//				CardData cd = new CardData(msrData);
				if(itemStartSC!=null)
					itemStartSC.setEnabled(true); 
				textAreaTop.setText(strMsrData);
				
	            StringBuffer hexString = new StringBuffer();
	            hexString.append("<");
	            String fix = null;
	            for (int i = 0; i < msrData.length; i++) {
	            	fix = Integer.toHexString(0xFF & msrData[i]);
	            	if(fix.length()==1)
	            		fix = "0"+fix;
	                hexString.append(fix);
	                if((i+1)%4==0&&i!=(msrData.length-1))
	                	hexString.append(' ');
	            }
	            hexString.append(">");
	            textAreaBottom.setText(hexString.toString());//+"\n\n"+cd.toString());
				adjustTextView();
				myUniMagReader.WriteLogIntoFile(hexString.toString());
//				myUniMagReader.WriteLogIntoFile(cd.toString());				
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
			}
		}
	};
	private void adjustTextView()
	{
		int height = (textAreaTop.getHeight()+ textAreaBottom.getHeight())/2;
		textAreaTop.setHeight(height);
		textAreaBottom.setHeight(height);
	}	
	// displays a connection status of UniMag reader
	private Runnable doUpdateTV = new Runnable()
	{
		public void run()
		{
			if(!isReaderConnected)
				connectStatusTextView.setText("DISCONNECTED");
			else
				connectStatusTextView.setText("CONNECTED");
		}
	};
	private Runnable doUpdateToast = new Runnable()
	{
		public void run()
		{
			try{
				Context context = getApplicationContext();
				String msg = null;//"To start record the mic.";
				if(isReaderConnected)
				{
					msg = "<<CONNECTED>>";	
					int duration = Toast.LENGTH_SHORT ;
					Toast.makeText(context, msg, duration).show();
					if(itemStartSC!=null)
						itemStartSC.setEnabled(true); 
				}
			}catch(Exception ex)
			{
				ex.printStackTrace();
			}
		}
	};
	private Runnable doConnectUsingProfile = new Runnable()
	{
		public void run() {
			if (myUniMagReader != null)
			{
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				myUniMagReader.connectWithProfile(profile);
			}
		}
	};

	/***
	 * Class: UniMagTopDialog
	 * Author: Eric Yang
	 * Date: 2010.10.12
	 * Function: to show the dialog on the top of the desktop.
	 * 
	 * *****/
	private class UniMagTopDialog extends Dialog{

		public UniMagTopDialog(Context context) {
			super(context);
		}

	    @Override
		public boolean onKeyDown(int keyCode, KeyEvent event) {
	    	if ((keyCode == KeyEvent.KEYCODE_BACK||KeyEvent.KEYCODE_HOME==keyCode||KeyEvent.KEYCODE_SEARCH==keyCode)){
				return false;
			}
			return super.onKeyDown(keyCode, event);
		}

		@Override
		public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
	    	if ((keyCode == KeyEvent.KEYCODE_BACK||KeyEvent.KEYCODE_HOME==keyCode||KeyEvent.KEYCODE_SEARCH==keyCode)){
	    		 
				return false;
			}	return super.onKeyMultiple(keyCode, repeatCount, event);
		}

		@Override
		public boolean onKeyUp(int keyCode, KeyEvent event) {
	    	if ((keyCode == KeyEvent.KEYCODE_BACK||KeyEvent.KEYCODE_HOME==keyCode||KeyEvent.KEYCODE_SEARCH==keyCode)){
	 			return false;
			}
	    	return super.onKeyUp(keyCode, event);
		}
	}
	private class UniMagTopDialogYESNO extends Dialog{

		public UniMagTopDialogYESNO(Context context) {
			super(context);
		}

	    @Override
		public boolean onKeyDown(int keyCode, KeyEvent event) {
	    	if ((keyCode == KeyEvent.KEYCODE_BACK||KeyEvent.KEYCODE_HOME==keyCode||KeyEvent.KEYCODE_SEARCH==keyCode)){
				return false;
			}
			return super.onKeyDown(keyCode, event);
		}

		@Override
		public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
	    	if ((keyCode == KeyEvent.KEYCODE_BACK||KeyEvent.KEYCODE_HOME==keyCode||KeyEvent.KEYCODE_SEARCH==keyCode)){
	    		 
				return false;
			}	return super.onKeyMultiple(keyCode, repeatCount, event);
		}

		@Override
		public boolean onKeyUp(int keyCode, KeyEvent event) {
	    	if ((keyCode == KeyEvent.KEYCODE_BACK||KeyEvent.KEYCODE_HOME==keyCode||KeyEvent.KEYCODE_SEARCH==keyCode)){
	 			return false;
			}
	    	return super.onKeyUp(keyCode, event);
		}
	}
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    
	    if (newConfig.orientation ==
	        Configuration.ORIENTATION_LANDSCAPE)
	    {
	    	//you can make sure if you would change it
	    }
	    if (newConfig.orientation ==
	        Configuration.ORIENTATION_PORTRAIT)
	    {
	    	//you can make sure if you would change it
	    }
	    if (newConfig.keyboardHidden ==
	        Configuration.KEYBOARDHIDDEN_NO)
	    {
	    	//you can make sure if you need change it
	    }
		super.onConfigurationChanged(newConfig);
	}

	private void showTopDialog(String strTitle)
	{
		hideTopDialog();
		if(dlgTopShow==null)
			dlgTopShow = new UniMagTopDialog(this);
		try
		{
			Window win = dlgTopShow.getWindow();
			win.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
			dlgTopShow.setTitle("UniMag");
			dlgTopShow.setContentView(R.layout.dlgtopview );
			TextView myTV = (TextView)dlgTopShow.findViewById(R.id.TView_Info);
			
			myTV.setText(popupDialogMsg);
			dlgTopShow.setOnKeyListener(new OnKeyListener(){
				public boolean onKey(DialogInterface dialog, int keyCode,
						KeyEvent event) {
					if ((keyCode == KeyEvent.KEYCODE_BACK)){
						return false;
					}
					return true;
				}
			});
	        dlgTopShow.show();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			dlgTopShow = null;
		}
	};	
	private void hideTopDialog( )
	{
		if(dlgTopShow!=null)
		{
			try{
 			dlgTopShow.hide();
 			dlgTopShow.dismiss();
			}
			catch(Exception ex)
			{
			
				ex.printStackTrace();
			}
 			dlgTopShow = null;
		}
	};	
	
	private void showSwipeTopDialog( )
	{
		hideSwipeTopDialog();
		try{
			
			if(dlgSwipeTopShow==null)
				dlgSwipeTopShow = new UniMagTopDialog(this);
			
			Window win = dlgSwipeTopShow.getWindow();
			win.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
			dlgSwipeTopShow.setTitle("UniMag");
			dlgSwipeTopShow.setContentView(R.layout.dlgswipetopview );
			TextView myTV = (TextView)dlgSwipeTopShow.findViewById(R.id.TView_Info);
			Button myBtn = (Button)dlgSwipeTopShow.findViewById(R.id.btnCancel);
			
			myTV.setText(popupDialogMsg);
			myBtn.setOnClickListener(new Button.OnClickListener()
			{
				public void onClick(View v) {
					if(itemStartSC!=null)
						itemStartSC.setEnabled(true); 
					//stop swipe
					myUniMagReader.stopSwipeCard();
					if (readerType == 2)
						isWaitingForCommandResult = true;

					if (dlgSwipeTopShow != null) {
						statusText = "Swipe card cancelled.";
						msrData = null;
						handler.post(doUpdateStatus);
						dlgSwipeTopShow.dismiss();
					}
				}
			});
			dlgSwipeTopShow.setOnKeyListener(new OnKeyListener(){
				public boolean onKey(DialogInterface dialog, int keyCode,
						KeyEvent event) {
					if ((keyCode == KeyEvent.KEYCODE_BACK)){
						return false;
					}
					return true;
				}
			});
			dlgSwipeTopShow.show();	 
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	};	
	private void showYesNoDialog( )
	{
		hideSwipeTopDialog();
		try{
			
			if(dlgYESNOTopShow==null)
				dlgYESNOTopShow = new UniMagTopDialogYESNO(this);
			
			Window win = dlgYESNOTopShow.getWindow();
			win.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
			dlgYESNOTopShow.setTitle("Warning");
			 
			dlgYESNOTopShow.setContentView(R.layout.dlgtopview2bnt );
			TextView myTV = (TextView)dlgYESNOTopShow.findViewById(R.id.TView_Info);
			myTV.setTextColor(0xFF0FF000);
			Button myBtnYES = (Button)dlgYESNOTopShow.findViewById(R.id.btnYes);
			Button myBtnNO = (Button)dlgYESNOTopShow.findViewById(R.id.btnNo);
			
		//	myTV.setText("Warrning, Now will Update Firmware if you press 'YES' to update, or 'No' to cancel");
			myTV.setText("Upgrading the firmware might cause the device to not work properly. \nAre you sure you want to continue? ");
			myBtnYES.setOnClickListener(new Button.OnClickListener()
			{
				public void onClick(View v) {
					updateFirmware_exTools();
					dlgYESNOTopShow.dismiss();
				}
			});
			myBtnNO.setOnClickListener(new Button.OnClickListener()
			{
				public void onClick(View v) {
					dlgYESNOTopShow.dismiss();
				}
			});
			dlgYESNOTopShow.setOnKeyListener(new OnKeyListener(){
				public boolean onKey(DialogInterface dialog, int keyCode,
						KeyEvent event) {
					if ((keyCode == KeyEvent.KEYCODE_BACK)){
						return false;
					}
					return true;
				}
			});
			dlgYESNOTopShow.show();	 
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	};	
	private void hideSwipeTopDialog( )
	{
		try
		{
			if(dlgSwipeTopShow!=null)
			{
				dlgSwipeTopShow.hide();
				dlgSwipeTopShow.dismiss();
				dlgSwipeTopShow = null;	
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	};

	// implementing a method onReceiveMsgCardData, defined in uniMagReaderMsg interface
	// receiving card data here
	public void onReceiveMsgCardData(byte flagOfCardData,byte[] cardData) {
		
		if (cardData.length > 5)
			if (cardData[0] == 0x25 && cardData[1] == 0x45) {
				statusText = "Swipe error. Please try again.";
				msrData = new byte[cardData.length];
				System.arraycopy(cardData, 0, msrData, 0, cardData.length);
				enableSwipeCard = true;
				handler.post(doHideSwipeTopDlg);
				handler.post(doUpdateStatus);
				return;
			}
		
		byte flag = (byte) (flagOfCardData&0x04);
//		Log.d(" onReceive flagOfCardData="+flagOfCardData,"CardData="+ getHexStringFromBytes(cardData));

		if(flag==0x00)
			strMsrData = new String (cardData);
		if(flag==0x04)
		{
			//You need to decrypt the data here first.
			strMsrData = new String (cardData);
		}
		msrData = new byte[cardData.length];
		System.arraycopy(cardData, 0, msrData, 0, cardData.length);
		enableSwipeCard = true;
		handler.post(doHideTopDlg);
		handler.post(doHideSwipeTopDlg);
		handler.post(doUpdateTVS);
	}
	
	// implementing a method onReceiveMsgConnected, defined in uniMagReaderMsg interface
	// receiving a message that the uniMag device has been connected	
	public void onReceiveMsgConnected() {
		
		isReaderConnected = true;
		if(percent==0)
		{
			if(profile!=null)
			{
				if(profile.getModelNumber().length()>0)
					statusText = "Now the UniMag Unit is connected.("+getTimeInfoMs(beginTime)+"s, with profile "+profile.getModelNumber()+")";
				else statusText = "Now the UniMag Unit is connected.("+getTimeInfoMs(beginTime)+"s)";
			}
			else
				statusText = "Now the UniMag Unit is connected."+" ("+getTimeInfoMs(beginTime)+"s)";
		}
		else
		{
			if(profile!=null)
				statusText = "Now the UniMag Unit is connected.("+getTimeInfoMs(beginTime)+"s, "+"Profile found at "+percent +"% named "+profile.getModelNumber()+",auto config last " +getTimeInfoMs(beginTimeOfAutoConfig)+"s)";
			else
				statusText = "Now the UniMag Unit is connected."+" ("+getTimeInfoMs(beginTime)+"s, "+"Profile found at "+percent +"%,auto config last " +getTimeInfoMs(beginTimeOfAutoConfig)+"s)";
			percent = 0;
		}		
		handler.post(doHideTopDlg);
		handler.post(doHideSwipeTopDlg);
		handler.post(doUpdateTV);
		handler.post(doUpdateToast);
		msrData = null;
		handler.post(doUpdateStatus);	
		handler.post(doUpdateAutoConfigProgressInfo);

	}

	// implementing a method onReceiveMsgDisconnected, defined in uniMagReaderMsg interface
	// receiving a message that the uniMag device has been disconnected		
	public void onReceiveMsgDisconnected() {
		percent=0;
		strProgressInfo=null;
		isReaderConnected = false;
		isWaitingForCommandResult=false;
		autoconfig_running=false;
		handler.post(doHideTopDlg);
		handler.post(doHideSwipeTopDlg);
		handler.post(doUpdateTV);
		showAboutInfo();
	}
	// implementing a method onReceiveMsgTimeout, defined in uniMagReaderMsg inteface
	// receiving a timeout message for powering up or card swipe		
	public void onReceiveMsgTimeout(String strTimeoutMsg) {
		isWaitingForCommandResult=false;
		enableSwipeCard = true;
		handler.post(doHideTopDlg);
		handler.post(doHideSwipeTopDlg);
		statusText = strTimeoutMsg+"("+getTimeInfo(beginTime)+")";
		msrData = null;
		handler.post(doUpdateStatus);
	}
	// implementing a method onReceiveMsgToConnect, defined in uniMagReaderMsg interface
	// receiving a message when SDK starts powering up the UniMag device		
	public void onReceiveMsgToConnect(){ 
		beginTime = System.currentTimeMillis();
		handler.post(doHideTopDlg);
		handler.post(doHideSwipeTopDlg);
		popupDialogMsg = "Powering up uniMag...";
		handler.post(doShowTopDlg);
	}
	// implementing a method onReceiveMsgToSwipeCard, defined in uniMagReaderMsg interface
	// receiving a message when SDK starts recording, then application should ask user to swipe a card		
	public void onReceiveMsgToSwipeCard() {
		textAreaTop.setText("");
		popupDialogMsg = "Please swipe card ("+getTimeInfoMs(beginTime)+"ms)";
		handler.post(doHideTopDlg);
		handler.post(doHideSwipeTopDlg);
		handler.post(doShowSwipeTopDlg);
	}
	// implementing a method onReceiveMsgProcessingCardData, defined in uniMagReaderMsg interface
	// receiving a message when SDK detects data coming from the UniMag reader
	// The main purpose is to give early notification to user to wait until SDK finishes processing card data.
	public void onReceiveMsgProcessingCardData() {
		statusText = "Card data is being processed. Please wait.";
		msrData = null;
		handler.post(doUpdateStatus);
	}
	
	public void onReceiveMsgToCalibrateReader() {
		statusText = "Reader needs to be calibrated. Please wait.";
		msrData = null;
		handler.post(doUpdateStatus);
	}
	// this method has been depricated, and will not be called in this version of SDK. 
	public void onReceiveMsgSDCardDFailed(String strSDCardFailed)
	{
		popupDialogMsg = strSDCardFailed;
		handler.post(doHideTopDlg);
		handler.post(doHideSwipeTopDlg);		
		handler.post(doShowTimeoutMsg);
	}
	// Setting a permission for user	
	public boolean getUserGrant(int type, String strMessage) {
		Log.d(" getUserGrant:",strMessage);
		boolean getUserGranted = false;
		switch(type)
		{
		case uniMagReaderMsg.typeToPowerupUniMag:
			//pop up dialog to get the user grant
			getUserGranted = true;
			break;
		case uniMagReaderMsg.typeToUpdateXML:
			//pop up dialog to get the user grant
			getUserGranted = true;
			break;
		case uniMagReaderMsg.typeToOverwriteXML:
			//pop up dialog to get the user grant
			getUserGranted = true;
			break;
		case uniMagReaderMsg.typeToReportToIdtech:
			//pop up dialog to get the user grant
			getUserGranted = true;
			break;
		default:
			getUserGranted = false;
			break;
		}
		return getUserGranted;
	}
	// implementing a method onReceiveMsgFailureInfo, defined in uniMagReaderMsg interface
	// receiving a message when SDK could not find a profile of the phone	
	public void onReceiveMsgFailureInfo(int index, String strMessage) {
		isWaitingForCommandResult = false;
		
		// If AutoConfig found a profile before and saved into db, then retreive it and connect.
		if (profileDatabase.updateProfileFromDB()) {
			this.profile = profileDatabase.getProfile();
    		showAboutInfo();
			handler.post(doConnectUsingProfile);
		} else {
			statusText = "Failure index: "+index+", message: "+strMessage;
			msrData = null;
			handler.post(doUpdateStatus);
		}
		//Cannot support current phone in the XML file.
		//start to Auto Config the parameters
		if(myUniMagReader.startAutoConfig(false)==true)
		{
			beginTime = getCurrentTime();
		}
	}
	// implementing a method onReceiveMsgCommandResult, defined in uniMagReaderMsg interface
	// receiving a message when SDK is able to parse a response for commands from the reader
	public void onReceiveMsgCommandResult(int commandID, byte[] cmdReturn) {
		Log.d(" onReceive commandID="+commandID,",cmdReturn="+ getHexStringFromBytes(cmdReturn));
		isWaitingForCommandResult = false;
		
		if (cmdReturn.length > 1){
			if (6==cmdReturn[0]&&(byte)0x56==cmdReturn[1])
			{
				statusText = "Failed to send command. Attached reader is in boot loader mode. Format:<"+getHexStringFromBytes(cmdReturn)+">";
				handler.post(doUpdateStatus);
				return;
			}
		}
		
		switch(commandID)
		{
		case uniMagReaderMsg.cmdGetNextKSN:
			if(0==cmdReturn[0])
				statusText = "Get Next KSN timeout.";
			else if(6==cmdReturn[0])
				statusText = "Get Next KSN Succeed.";
			else
				statusText = "Get Next KSN failed.";
			break;
		case uniMagReaderMsg.cmdEnableAES:
			if(0==cmdReturn[0])
				statusText = "Turn on AES timeout.";
			else if(6==cmdReturn[0])
				statusText = "Turn on AES Succeed.";
			else
				statusText = "Turn on AES failed.";
			break;
		case uniMagReaderMsg.cmdEnableTDES:
			if(0==cmdReturn[0])
				statusText = "Turn on TDES timeout.";
			else if(6==cmdReturn[0])
				statusText = "Turn on TDES Succeed.";
			else
				statusText = "Turn on TDES failed.";
			break;
		case uniMagReaderMsg.cmdGetVersion:
			if(0==cmdReturn[0])
				statusText = "Get Version timeout.";
			else if (cmdReturn.length <= 3)
				statusText = "Get Version: "+new String(cmdReturn);
			else if(6==cmdReturn[0]&&2==cmdReturn[1]&&3==cmdReturn[cmdReturn.length-2])
			{
				statusText = null;
				byte cmdDataX[]  = new byte[cmdReturn.length-4];
				System.arraycopy(cmdReturn, 2, cmdDataX, 0, cmdReturn.length-4);
				statusText = "Get Version:"+new String(cmdDataX);
			}
			else
			{
				statusText = "Get Version failed, Error Format:<"+ getHexStringFromBytes(cmdReturn)+">";
			}
			break;
		case uniMagReaderMsg.cmdGetSerialNumber:
			if(0==cmdReturn[0])
				statusText = "Get Serial Number timeout.";
			else if (cmdReturn.length <= 3)
				statusText = "Get SerialNumber: "+new String(cmdReturn);
			else if(cmdReturn.length>3 && 6==cmdReturn[0]&&2==cmdReturn[1]&&3==cmdReturn[cmdReturn.length-2])
			{
				statusText = null;
				byte cmdDataX[]  = new byte[cmdReturn.length-4];
				System.arraycopy(cmdReturn, 2, cmdDataX, 0, cmdReturn.length-4);
				statusText = "Get Serial Number:"+new String(cmdDataX);
			}
			else
			{
				statusText = "Get Serial Number failed, Error Format:<"+ getHexStringFromBytes(cmdReturn)+">";
			}
			break;
		case uniMagReaderMsg.cmdGetAttachedReaderType:
			int readerType = cmdReturn[0];
			ReaderType art = getAttachedReaderType(readerType);
			statusText = "Attached Reader:\n   "+getReaderName(art) ;
			msrData = null;
			handler.post(doUpdateStatus);
			return;
		
		case uniMagReaderMsg.cmdGetSettings:
			if(0==cmdReturn[0])
				statusText = "Get Setting timeout.";
			else if (cmdReturn.length <= 3)
				statusText = "Get Settings: "+new String(cmdReturn);
			else if(6==cmdReturn[0]&&2==cmdReturn[1]&&3==cmdReturn[cmdReturn.length-2])
			{
				byte cmdDataX[]  = new byte[cmdReturn.length-4];
				System.arraycopy(cmdReturn, 2, cmdDataX, 0, cmdReturn.length-4);
				statusText = "Get Setting:"+ getHexStringFromBytes(cmdDataX);
				cmdDataX=null;
			}
			else
			{
				statusText = "Get Setting failed, Error Format:<"+ getHexStringFromBytes(cmdReturn)+">";
			}
			break;
		case uniMagReaderMsg.cmdCalibrate:
			if (6==cmdReturn[0] && (cmdReturn[1] & 0xFF) != 0xEE)
				statusText = "Calibration succeeded.";
			else 
				statusText = "Calibration failed.";
			break;
		case uniMagReaderMsg.cmdGetBatteryLevel:
			if (cmdReturn.length >=3 && cmdReturn[0] == 6)
				statusText = "Battery Status: "+Common.getHexStringFromBytes(new byte[]{cmdReturn[1], cmdReturn[2]});
			else 
				statusText = "Failed to check battery level";
			break;
		default:
			break;
		}
		msrData = null;
		msrData = new byte[cmdReturn.length];
		System.arraycopy(cmdReturn, 0, msrData, 0, cmdReturn.length);
		handler.post(doUpdateStatus);
	}
	// implementing a method onReceiveMsgChallengeResult, defined in uniMagReaderToolsMsg interface
	// receiving a message when SDK is able to parse a response for get challenge command from the reader
	public void onReceiveMsgChallengeResult(int returnCode,byte[] data) {
		isWaitingForCommandResult = false;
		switch(returnCode)
		{
		case uniMagReaderToolsMsg.cmdGetChallenge_Succeed_WithChallengData:
			challengeResult = cmdGetChallenge_Succeed_WithChallengData;
			//show the challenge data and enable edit the hex text view
			
			if (readerType == 2 && 6==data[0]&&2==data[1]&&3==data[data.length-2]){
				byte cmdChallengeData[]  = new byte[8];
				System.arraycopy(data, 2, cmdChallengeData, 0, 8);
				statusText = "Challenge Data:<"+ 
						getHexStringFromBytes(cmdChallengeData)+"> "+"\n"+  
						"please enter "+firmwareUpdateTool.getRequiredChallengeResponseLength()+"-byte challenge response below, as hex, then update firmware.";
			}
			
			else if(6==data[0]&&2==data[1]&&3==data[data.length-2])
			{
				statusText = null;
				byte cmdChallengeData[]  = new byte[8];
				System.arraycopy(data, 2, cmdChallengeData, 0, 8);
				byte cmdChallengeData_encyption[]  = new byte[8];
				System.arraycopy(data, 2, cmdChallengeData_encyption, 0, 8);
				
				byte cmdChallengeData_KSN[]  = new byte[10];
				System.arraycopy(data, 10, cmdChallengeData_KSN, 0, 10);
				statusText = "Challenge Data:<"+ 
							getHexStringFromBytes(cmdChallengeData)+"> "+"\n"+"KSN:<"+  
							getHexStringFromBytes(cmdChallengeData_KSN)+">"+"\n"+
							"please enter "+firmwareUpdateTool.getRequiredChallengeResponseLength()+"-byte challenge response below, as hex, then update firmware.";
			} 
			else {
				statusText = "Get Challenge failed, Error Format:<"+ getHexStringFromBytes(data)+">";
			}

			break;
		case uniMagReaderToolsMsg.cmdGetChallenge_Succeed_WithFileVersion:
			challengeResult = cmdGetChallenge_Succeed_WithFileVersion;
			if(6==data[0]&&((byte)0x56)==data[1] )
			{
				statusText = null;
				byte cmdFileVersion[]  = new byte[2];
				System.arraycopy(data, 2, cmdFileVersion, 0, 2);
				char fileVersionHigh=(char) cmdFileVersion[0];
				char fileVersionLow=(char) cmdFileVersion[1];
				
				statusText = "Already in boot load mode, and the file version is "+fileVersionHigh+"."+fileVersionLow+"\n" +
								"Please update firmware directly.";
			} else
			{
				statusText = "Get Challenge failed, Error Format:<"+ getHexStringFromBytes(data)+">";
			}

			break;
		case uniMagReaderToolsMsg.cmdGetChallenge_Failed:
			statusText = "Get Challenge failed, please try again.";

			break;
		case uniMagReaderToolsMsg.cmdGetChallenge_NeedSetBinFile:
			statusText = "Get Challenge failed, need to set BIN file first.";
			break;
		case uniMagReaderToolsMsg.cmdGetChallenge_Timeout:
			statusText = "Get Challenge timeout.";
			break;
		default:
			break;
		}
		msrData = null;
		handler.post(doUpdateChallengeData);
 		
	}
	// implementing a method onReceiveMsgUpdateFirmwareProgress, defined in uniMagReaderToolsMsg interface
	// receiving a message of firmware update progress	
	public void onReceiveMsgUpdateFirmwareProgress(int progressValue) {
		Log.d("UpdateFirmwareProgress" ,"v = "+progressValue);
		statusText = "Updating firmware, "+progressValue+"% finished.";
		msrData = null;
		handler.post(doUpdateStatus);
		
	}
	// implementing a method onReceiveMsgUpdateFirmwareResult, defined in uniMagReaderToolsMsg interface
	// receiving a message when firmware update has been finished	
	public void onReceiveMsgUpdateFirmwareResult(int result) {
		isWaitingForCommandResult = false;		

		switch(result)
		{
		case uniMagReaderToolsMsg.cmdUpdateFirmware_Succeed:
			statusText = "Update firmware succeed.";
			break;
		case uniMagReaderToolsMsg.cmdUpdateFirmware_NeedSetBinFile:
			statusText = "Update firmware failed, need to set BIN file first";
			break;
		case uniMagReaderToolsMsg.cmdUpdateFirmware_NeedGetChallenge:
			statusText = "Update firmware failed, need to get challenge first.";
			break;
		case uniMagReaderToolsMsg.cmdUpdateFirmware_Need8BytesData:
			statusText = "Update firmware failed, need input 8 bytes data.";
			break;
		case uniMagReaderToolsMsg.cmdUpdateFirmware_Need24BytesData:
			statusText = "Update firmware failed, need input 24 bytes data.";
			break;
		case uniMagReaderToolsMsg.cmdUpdateFirmware_EnterBootloadModeFailed:
			statusText = "Update firmware failed, cannot enter boot load mode.";
			break;
		case uniMagReaderToolsMsg.cmdUpdateFirmware_DownloadBlockFailed:
			statusText = "Update firmware failed, cannot download block data.";
			break;
		case uniMagReaderToolsMsg.cmdUpdateFirmware_EndDownloadBlockFailed:
			statusText = "Update firmware failed, cannot end download block.";
			break;
		case uniMagReaderToolsMsg.cmdUpdateFirmware_Timeout:
			statusText = "Update firmware timeout.";
			break;
		}
		Log.d(" UpdateFirmwareResult" ,"v = "+result);
		msrData = null;
		handler.post(doUpdateStatus);
			
	}
	// implementing a method onReceiveMsgAutoConfigProgress, defined in uniMagReaderMsg interface
	// receiving a message of Auto Config progress	
	public void onReceiveMsgAutoConfigProgress(int progressValue) {
		Log.d(" AutoConfigProgress" ,"v = "+progressValue);
		percent = progressValue;
		statusText = "Searching the configuration automatically, "+progressValue+"% finished."+"("+getTimeInfo(beginTime)+")";
		msrData = null;
		beginTimeOfAutoConfig = beginTime;
		handler.post(doUpdateAutoConfigProgress);
	}
	public void onReceiveMsgAutoConfigProgress(int percent, double result,
			String profileName) {
		if(strProgressInfo==null)
			strProgressInfo="("+profileName+ ") <"+percent+"%>,Result="+Common.getDoubleValue(result);
		else
			strProgressInfo+="\n("+profileName+ ") <"+percent+"%>,Result="+Common.getDoubleValue(result);
    	Log.d("**__@__**","demo = "+strProgressInfo);
		handler.post(doUpdateAutoConfigProgressInfo);
	}

	public void onReceiveMsgAutoConfigCompleted(StructConfigParameters profile) {
		Log.d(" AutoConfigCompleted" ,"A profile has been found, trying to connect...");
		autoconfig_running = false;
		beginTimeOfAutoConfig = beginTime;
		this.profile = profile;
		profileDatabase.setProfile(profile);
		profileDatabase.insertResultIntoDB();
		handler.post(doConnectUsingProfile);
	}

	public void getChallenge()
	{
		getChallenge_exTools();
	}
	public void updateFirmware()
	{
		if (isReaderConnected)
			handler.post(doShowYESNOTopDlg);
		else 
			Toast.makeText(this, "Please connect a reader first.", Toast.LENGTH_SHORT).show();
	}
	private void getChallenge_exTools()
	{
		if (firmwareUpdateTool != null)
		{
			if (firmwareUpdateTool.getChallenge() == true)
			{
				isWaitingForCommandResult = true;
				// show to get challenge
				statusText = " To Get Challenge, waiting for response.";
				msrData = null;
				handler.post(doUpdateStatus);
			}
		}
	}	
	private void updateFirmware_exTools()
	{
	
		if (firmwareUpdateTool != null)
		{
			String strData = textAreaBottom.getText().toString();
			
			if(strData.length()>0)
			{
				challengeResponse = getBytesFromHexString(strData);
				if(challengeResponse==null)
				{
					statusText = "Invalidate challenge data, please input hex data.";
					msrData = null;
					handler.post(doUpdateStatus);				
					return;
				}
			}
			else
				challengeResponse=null;

			isWaitingForCommandResult = true;
			if (firmwareUpdateTool.updateFirmware(challengeResponse) == true)
			{
				statusText = " To Update Firmware, waiting for response.";
				msrData = null;
				handler.post(doUpdateStatus);				
			}
		}
	}	
	public void prepareToSendCommand(int cmdID)
	{
		isWaitingForCommandResult = true;
		switch(cmdID)
		{
		case uniMagReaderMsg.cmdGetNextKSN:
			statusText = " To Get Next KSN, wait for response.";
			break;
		case uniMagReaderMsg.cmdEnableAES:
			statusText = " To Turn on AES, wait for response.";
			break;
		case uniMagReaderMsg.cmdEnableTDES:
			statusText = " To Turn on TDES, wait for response.";
			break;
		case uniMagReaderMsg.cmdGetVersion:
			statusText = " To Get Version, wait for response.";
			break;
		case uniMagReaderMsg.cmdGetSettings:
			statusText = " To Get Setting, wait for response.";
			break;
		case uniMagReaderMsg.cmdGetSerialNumber:
			statusText = " To Get Serial Number, wait for response.";
			break;
		case uniMagReaderMsg.cmdGetBatteryLevel:
			statusText = " To Check battery level, wait for response.";
			break;

		default:
			break;
		}
		msrData = null;
		handler.post(doUpdateStatus);
	}
	private String getHexStringFromBytes(byte []data)
    {
		if(data.length<=0) 
			return null;
		StringBuffer hexString = new StringBuffer();
		String fix = null;
		for (int i = 0; i < data.length; i++) {
			fix = Integer.toHexString(0xFF & data[i]);
			if(fix.length()==1)
				fix = "0"+fix;
			hexString.append(fix);
		}
		fix = null;
		fix = hexString.toString();
		return fix;
    }
    public byte[] getBytesFromHexString(String strHexData)
	{
	    if (1==strHexData.length()%2) {
	    	return null;
	    }
	    byte[] bytes = new byte[strHexData.length()/2];
	    try{
		    for (int i=0;i<strHexData.length()/2;i++) {
		    	bytes[i] = (byte) Integer.parseInt(strHexData.substring(i*2, (i+1)*2) , 16);
		    }
	    }
	    catch(Exception ex)
	    {
	    	ex.printStackTrace();
	    	return null;
	    }
	    return bytes;
	}
	static private String getMyStorageFilePath( ) {
		String path = null;
		if(isStorageExist())
			path = Environment.getExternalStorageDirectory().toString();
		return path;
	}
	private boolean isFileExist(String path) {
    	if(path==null)
    		return false;
	    File file = new File(path);
	    if (!file.exists()) {
	      return false ;
	    }
	    return true;
    }
	static private boolean isStorageExist() {
		//if the SD card exists
		boolean sdCardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
		return sdCardExist;
	}
	private long getCurrentTime(){
		return System.currentTimeMillis();
	}
	private String getTimeInfo(long timeBase){
		int time = (int)(getCurrentTime()-timeBase)/1000;
		int hour = (int) (time/3600);
		int min = (int) (time/60);
		int sec= (int) (time%60);
		return  hour+":"+min+":"+sec;
	}
	private String getTimeInfoMs(long timeBase){
		float time = (float)(getCurrentTime()-timeBase)/1000;
		String strtime = String.format("%03f",time);
		return  strtime;
	}
}