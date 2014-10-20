package com.hz7225.cebible;

import java.io.File;
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
 
    //public static String DB_NAME = "cuvslite.bbl.db";
    public String DB_NAME;

    private SQLiteDatabase myDataBase;  
    private final Context myContext;
    
    boolean update_database = false;
    
    public void setDB(String dbname) {
    	DB_NAME = dbname;
    }
 
    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
     * @param context
     */
    public DataBaseHelper(Context context, String db_name) {
 
    	super(context, db_name, null, 2); //Increment this number when an existing database needs to be updated
    	DB_NAME = db_name;
        this.myContext = context;
    }	
 
  /**
     * Creates a empty database on the system and rewrites it with your own database.
     * */
    public void createDataBase() throws IOException{
    	//Log.d(TAG, "createDatabase(): update_database = " + String.valueOf(update_database));
    	
    	boolean dbExist = checkDataBase();
    	
    	if(dbExist && !update_database){
    		//do nothing - database already exist and doesn't need to be updated
    		//Log.d(TAG, "database already exist and doesn't need to be updated");
    	}else{ 
    		//By calling this method and empty database will be created into the default system path
            //of your application so we are gonna be able to overwrite that database with our database.
    		//if (update_database) Log.d(TAG, "Updating databases");
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
    
    public void deleteDataBase() {
    	//Log.d(TAG, "deleteDatabase()");
    	File file = new File(DB_PATH + DB_NAME);
    	if(file.exists())
    	{
    		file.delete();
    	}
    }
 
    @Override
	public synchronized void close() { 
    	    if(myDataBase != null)
    		    myDataBase.close();
 
    	    super.close(); 
	}
 
	@Override
	public void onCreate(SQLiteDatabase db) {
		//Log.d(TAG, "Database onCreate, db.getVersion = " + String.valueOf(db.getVersion()));
	}
 
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		//Log.d(TAG, "Database onUpgrade, old = " + String.valueOf(oldVersion) + " new = " + String.valueOf(newVersion));
		if (newVersion > oldVersion)
        {
              //Log.d(TAG, "Set update_database to true");
              update_database = true;
              //deleteDataBase();
              /*
              try {
            	  createDataBase();
              } catch (IOException ioe) {
            	  throw new Error("Unable to create database");
              } 
              */
        }
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
		String selectQuery = "SELECT Verse FROM Bible WHERE Book=" + String.valueOf(book) + 
													" AND Chapter=" + String.valueOf(chapter);

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
	
	public List<ScriptureData> searchWholeBible(String searchStr) throws SQLException {
		List<ScriptureData> scriptureList = new ArrayList<ScriptureData>();
		// SQL Select Query
		String selectQuery = "SELECT * FROM Bible WHERE Scripture LIKE " + "\'%" + searchStr + "%\'";
		//Log.d(TAG, "searchBible: " + selectQuery); 
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		
		while (cursor.moveToNext()) {			
			//Log.d(TAG, cursor.getString(0) + " " + cursor.getString(1) + ":" + cursor.getString(2) + " " + cursor.getString(3));
			scriptureList.add(new ScriptureData(myContext, cursor.getString(0), cursor.getString(1),cursor.getString(2), cursor.getString(3)));
		}
		
		return scriptureList;
	}
	
	public List<ScriptureData> searchOneBibleBook(String book, String searchStr) throws SQLException {
		List<ScriptureData> scriptureList = new ArrayList<ScriptureData>();
		// SQL Select Query
		String selectQuery = "SELECT * FROM Bible WHERE Book = " + book + " AND Scripture LIKE " + "\'%" + searchStr + "%\'";
		//Log.d(TAG, "searchBibleBook " + book + ": " + selectQuery); 
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		
		while (cursor.moveToNext()) {			
			//Log.d(TAG, cursor.getString(0) + " " + cursor.getString(1) + ":" + cursor.getString(2) + " " + cursor.getString(3));
			scriptureList.add(new ScriptureData(myContext, cursor.getString(0), cursor.getString(1),cursor.getString(2), cursor.getString(3)));
		}
		
		return scriptureList;
	}
 
}
