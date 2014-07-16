package com.hz7225.cebible;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DataBaseHelper extends SQLiteOpenHelper{
	
	String TAG = "DatabaseHElper";
	 
    //The Android's default system path of your application database.
    private static String DB_PATH = "/data/data/com.hz7225.cebible/databases/";
 
    //private static String DB_NAME = "cuvslite.bbl.db";
    //private static String DB_NAME = "EB_kjv_bbl.db";
 
    //public static String DB_NAME = "cuvslite.bbl.db";
    public String DB_NAME;

    private SQLiteDatabase myDataBase;  
    private final Context myContext;
    
    public void setDB(String dbname) {
    	DB_NAME = dbname;
    }
 
    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
     * @param context
     */
    public DataBaseHelper(Context context, String db_name) {
 
    	//super(context, DB_NAME, null, 1);
    	super(context, db_name, null, 1);
    	DB_NAME = db_name;
        this.myContext = context;
    }	
 
  /**
     * Creates a empty database on the system and rewrites it with your own database.
     * */
    public void createDataBase() throws IOException{
 
    	boolean dbExist = checkDataBase();
 
    	if(dbExist){
    		//do nothing - database already exist
    	}else{
 
    		//By calling this method and empty database will be created into the default system path
               //of your application so we are gonna be able to overwrite that database with our database.
        	this.getReadableDatabase();
 
        	try {
 
    			copyDataBase();
 
    		} catch (IOException e) {
 
        		throw new Error("Error copying database");
 
        	}
    	}
 
    }
 
    /**
     * Check if the database already exist to avoid re-copying the file each time you open the application.
     * @return true if it exists, false if it doesn't
     */
    private boolean checkDataBase(){
 
    	SQLiteDatabase checkDB = null;
 
    	try{
    		String myPath = DB_PATH + DB_NAME;
    		checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
 
    	}catch(SQLiteException e){
 
    		//database does't exist yet.
 
    	}
 
    	if(checkDB != null){
 
    		checkDB.close();
 
    	}
 
    	return checkDB != null ? true : false;
    }
 
    /**
     * Copies your database from your local assets-folder to the just created empty database in the
     * system folder, from where it can be accessed and handled.
     * This is done by transfering bytestream.
     * */
    private void copyDataBase() throws IOException{
 
    	//Open your local db as the input stream
    	InputStream myInput = myContext.getAssets().open(DB_NAME);
 
    	// Path to the just created empty db
    	String outFileName = DB_PATH + DB_NAME;
 
    	//Open the empty db as the output stream
    	OutputStream myOutput = new FileOutputStream(outFileName);
 
    	//transfer bytes from the inputfile to the outputfile
    	byte[] buffer = new byte[1024];
    	int length;
    	while ((length = myInput.read(buffer))>0){
    		myOutput.write(buffer, 0, length);
    	}
 
    	//Close the streams
    	myOutput.flush();
    	myOutput.close();
    	myInput.close();
 
    }
 
    public void openDataBase() throws SQLException{
    	//Open the database
        String myPath = DB_PATH + DB_NAME;
    	myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
    	//Log.d(TAG, "openDataBase() path = " + myPath);
    }
 
    @Override
	public synchronized void close() {
 
    	    if(myDataBase != null)
    		    myDataBase.close();
 
    	    super.close();
 
	}
 
	@Override
	public void onCreate(SQLiteDatabase db) {
 
	}
 
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
 
	}
 
	// Add your public helper methods to access and get content from the database.
	// You could return cursors by doing "return myDataBase.query(....)" so it'd be easy
	// to you to create adapters for your views.
	
	public int getNumOfChapters(int book) throws SQLException {
		// SQL Select Query
		String selectQuery = "SELECT Chapter FROM Bible WHERE Book=" + String.valueOf(book);

		//Log.d(TAG, selectQuery); 
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		cursor.moveToLast();
		int num = cursor.getInt(0);
		cursor.close();
		return num;
	}   

	public int getNumOfVerses(int book, int chapter) throws SQLException {
		// SQL Select Query
		String selectQuery = "SELECT Verse FROM Bible WHERE Book=" + String.valueOf(book) + " AND Chapter=" + String.valueOf(chapter);

		//Log.d(TAG, selectQuery); 
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		cursor.moveToLast();
		int num = cursor.getInt(0);
		cursor.close();
		return num;
	}
	
	public String getVerse(int book, int chapter, int verse) throws SQLException {
		// SQL Select Query
		String selectQuery = "SELECT Scripture FROM Bible WHERE Book=" + String.valueOf(book) + 
													" AND Chapter=" + String.valueOf(chapter) +
													" AND Verse=" + String.valueOf(verse);
		//Log.d(TAG, selectQuery); 
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		cursor.moveToFirst();
		String str = cursor.getString(0);
		cursor.close();
		return str;
	}
	
	public List<String> getChapter(int book, int chapter) throws SQLException {
		List<String> scriptureList = new ArrayList<String>();
		// SQL Select Query
		String selectQuery = "SELECT Scripture FROM Bible WHERE Book=" + String.valueOf(book) + 
															" AND Chapter=" + String.valueOf(chapter);
		//Log.d(TAG, "getChapter: " + selectQuery); 
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		//cursor.moveToFirst();
		while (cursor.moveToNext()) {
			String str = cursor.getString(0);
			//Log.d(TAG, str);
			scriptureList.add(str);
		}
		
		cursor.close();
		return scriptureList;
	}
 
}
