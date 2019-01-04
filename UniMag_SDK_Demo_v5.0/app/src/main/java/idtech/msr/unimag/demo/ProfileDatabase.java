package idtech.msr.unimag.demo;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.util.Log;

import idtech.msr.xmlmanager.StructConfigParameters;

public class ProfileDatabase {
	private SQLiteDatabase myDb = null;
	private Cursor cursor = null;
	
	static private final String DB_NAME = "IDTECH.AutoConfig";
	static private final String DB_TABLE_PROFILE = "profiles";
	private StructConfigParameters profile = null;
	private Handler handler = new Handler();
	Context context=null;;
	private boolean isUseAutoConfigProfileChecked = false;

	public ProfileDatabase(Context context){this.context=context;}
	
	public void initializeDB() {
   	
        myDb = context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null);
        myDb.execSQL("CREATE TABLE IF NOT EXISTS " + DB_TABLE_PROFILE + "( "+
        		"search_date DATETIME, "+
        		"direction_output_wave INTEGER, "+
        		"input_frequency INTEGER, "+
        		"output_frequency INTEGER, "+
        		"record_buffer_size INTEGER, "+
        		"read_buffer_size INTEGER, "+
        		"wave_direction INTEGER, "+
        		"high_threshold INTEGER, "+
        		"low_threshold INTEGER, "+
        		"min INTEGER, "+
        		"max INTEGER, "+
        		"baud_rate INTEGER, "+
        		"preamble_factor INTEGER, "+
        		"set_default INTEGER,"+
        		"shuttle_channel INTEGER,"+
        		"headset_force_plug INTEGER,"+
         		"use_voice_recognizition INTEGER,"+
         		"volumeLevelAdjust INTEGER)"
       		);
        
        isUseAutoConfigProfileChecked = useAutoConfigProfileAsDefault();
    }
	boolean updateProfileFromDB () {
		try {
	        cursor = myDb.query(DB_TABLE_PROFILE, new String[]{
	        		"search_date", 
	        		"direction_output_wave", 
	        		"input_frequency", 
	        		"output_frequency", 
	        		"record_buffer_size", 
	        		"read_buffer_size", 
	        		"wave_direction", 
	        		"high_threshold", 
	        		"low_threshold", 
	        		"min", 
	        		"max", 
	        		"baud_rate", 
	        		"preamble_factor",
	        		"shuttle_channel",
	        		"headset_force_plug",
	        		"use_voice_recognizition",
	        		"volumeLevelAdjust"
	        		}, null, null, null, null, "search_date desc");
	        
	        if (cursor.moveToFirst()){
	        	profile = new StructConfigParameters();
	        	profile.setDirectionOutputWave((short) cursor.getInt(1));
	        	profile.setFrequenceInput(cursor.getInt(2));
	        	profile.setFrequenceOutput(cursor.getInt(3));
	        	profile.setRecordBufferSize(cursor.getInt(4));
	        	profile.setRecordReadBufferSize(cursor.getInt(5));
	        	profile.setWaveDirection(cursor.getInt(6));
	        	profile.sethighThreshold((short) cursor.getInt(7));
	        	profile.setlowThreshold((short) cursor.getInt(8));
	        	profile.setMin((short) cursor.getInt(9));
	        	profile.setMax((short) cursor.getInt(10));
	        	profile.setBaudRate(cursor.getInt(11));
	        	profile.setPreAmbleFactor((short) cursor.getInt(12));
	        	profile.setShuttleChannel((byte)cursor.getInt(13));
	        	profile.setForceHeadsetPlug((short)cursor.getInt(14));	        	
	        	profile.setUseVoiceRecognition((short)cursor.getInt(15));	        	
	        	profile.setVolumeLevelAdjust((short)cursor.getInt(16));
	        	
	        	Log.d("Demo>>updateProfileFromDB","profile has been loaded from Database. Total of "+cursor.getCount()+" rows stored.");
        		cursor.close();
	        	
	        } else {
	        	return false;
	        }
		} catch (Exception e){
			e.printStackTrace();
			return false;
		}
        return true;
	}
	public void insertResultIntoDB(){
		handler.post(doInsertResultIntoDB);
	}
	Runnable doInsertResultIntoDB = new Runnable() {
		public void run() {
			if (profile == null)
				return;
		
			ContentValues insertValues = new ContentValues();
			
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
			Date date = new Date();
			insertValues.put("search_date", dateFormat.format(date));
			insertValues.put("direction_output_wave", profile.getDirectionOutputWave());
			insertValues.put("input_frequency", profile.getFrequenceInput());
			insertValues.put("output_frequency", profile.getFrequenceOutput());
			insertValues.put("record_buffer_size", profile.getRecordBufferSize());
			insertValues.put("read_buffer_size", profile.getRecordReadBufferSize());
			insertValues.put("wave_direction", profile.getWaveDirection());
			insertValues.put("high_threshold", profile.gethighThreshold());
			insertValues.put("low_threshold", profile.getlowThreshold());
			insertValues.put("min", profile.getMin());
			insertValues.put("max", profile.getMax());
			insertValues.put("baud_rate", profile.getBaudRate());
			insertValues.put("preamble_factor", profile.getPreAmbleFactor());
			insertValues.put("shuttle_channel", profile.getShuttleChannel());
			insertValues.put("headset_force_plug", profile.getForceHeadsetPlug());
			insertValues.put("use_voice_recognizition", profile.getUseVoiceRecognition());
			insertValues.put("volumeLevelAdjust", profile.getVolumeLevelAdjust());
			
			
			if (isUseAutoConfigProfileChecked){
				insertValues.put("set_default", 1);
			} else {
				insertValues.put("set_default", 0);
			}
			
			try {
				// delete all previous profile
				myDb.execSQL("delete from "+DB_TABLE_PROFILE);
				myDb.insert(DB_TABLE_PROFILE, null, insertValues);
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	};
	public void checkOnUseAutoConfigProfile() {
		isUseAutoConfigProfileChecked = true;
		myDb.execSQL("update "+DB_TABLE_PROFILE+" set set_default='1'");
	}
	public void uncheckOnUseAutoConfigProfile() {
		isUseAutoConfigProfileChecked = false;
		myDb.execSQL("update "+DB_TABLE_PROFILE+" set set_default='0'");
	}
	public StructConfigParameters getProfile() {return profile;}
	public void setProfile(StructConfigParameters profile) {this.profile = profile;}
	public boolean getIsUseAutoConfigProfile(){return isUseAutoConfigProfileChecked;}
//	public void setIsUseAutoConfigProfile(boolean isUseAutoConfigProfileChecked){this.isUseAutoConfigProfileChecked = isUseAutoConfigProfileChecked;}
	public boolean isAutoConfigResultInDB () {
        try {
        	cursor = myDb.query(DB_TABLE_PROFILE, new String[]{"set_default"}, null, null, null, null, "search_date desc");
        	if (cursor.moveToFirst()){
        		cursor.close();
        		return true;
        	} else {
        		if (cursor != null)
        			cursor.close();
        		return false;
        	}
        } catch (Exception e){
        	e.printStackTrace();
        }
    	return false;
        
	}
	private boolean useAutoConfigProfileAsDefault() {
		try {
	        cursor = myDb.query(DB_TABLE_PROFILE, new String[]{"set_default"}, null, null, null, null, "search_date desc");
	        if (cursor.moveToFirst()){
	        	if (cursor.getInt(0) == 1) {
		        	cursor.close();
	        		return true;
	        	}
	        	if (cursor != null)
	        		cursor.close();
	        }
		} catch (Exception e){
			e.printStackTrace();
		}
        return false;
	}	
	public void closeDB()
	{
		if (cursor != null)
			cursor.close();
		if (myDb != null)
			myDb.close();
		myDb = null;
		cursor = null;
	}
}
