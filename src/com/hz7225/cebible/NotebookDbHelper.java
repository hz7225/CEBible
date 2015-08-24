package com.hz7225.cebible;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.util.Log;


public class NotebookDbHelper extends SQLiteOpenHelper {
	
	String TAG = "NotebookDbHelper";

	public static final String TABLE_NOTEBOOK = "notebook";
	public static final String COLUMN_BOOK = "book";
	public static final String COLUMN_CHAPTER = "chapter";
	public static final String COLUMN_VERSE = "verse";
	public static final String COLUMN_SCRIPTURE = "scripture";
	public static final String COLUMN_NOTES = "notes";
	public static final String COLUMN_LINKS = "links";

	private static final String DATABASE_NAME = "notebook.db";
	private static final int DATABASE_VERSION = 1;
	
	private final Context myContext;

	// Database creation sql statement
	private static final String DATABASE_CREATE = "create table "
			+ TABLE_NOTEBOOK + "(" 
			+ COLUMN_BOOK + " INTEGER, " 
			+ COLUMN_CHAPTER + " INTEGER, "
			+ COLUMN_VERSE + " INTEGER, "
			+ COLUMN_SCRIPTURE + " TEXT, "
			+ COLUMN_NOTES + " TEXT, "
			+ COLUMN_LINKS + " TEXT"
			//+ ", unique (book, chapter, verse)"
			+ ");";

	public NotebookDbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		//Log.d(NotebookDbHelper.class.getName(), "NotebookDbHelper object created");
		this.myContext = context;
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		//Log.d(NotebookDbHelper.class.getName(), "NotebookDbHelper:onCreate()");
		database.execSQL(DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		//Log.d(NotebookDbHelper.class.getName(),
		//		"Upgrading database from version " + oldVersion + " to "
		//				+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTEBOOK);
		onCreate(db);
	}
	
	//Return the Scripture field for a matching record of book, chapter, and verse. 
	//If the record doesn't exist, then a null string of length 0 will be returned.
	public String getScripture(int book, int chapter, int verse) throws SQLException {
		// SQL Select Query
		String selectQuery = "SELECT Scripture FROM notebook WHERE Book=" + String.valueOf(book) + 
														" AND Chapter=" + String.valueOf(chapter) +
														" AND Verse=" + String.valueOf(verse);
		//Log.d(TAG, selectQuery); 
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		String str = "";
		if(cursor.getCount() > 0) {
			cursor.moveToFirst();
			str = cursor.getString(0);
		} 
		cursor.close();
		db.close();
		return str;
	}
	
	//Return a list of all scriptures from the notebook database
	public List<ScriptureData> getScriptureDataList() throws SQLException {
		//Log.d(TAG, "getScriptureDataList()");
		List<ScriptureData> scriptureList = new ArrayList<ScriptureData>();
		String selectQuery = "SELECT * FROM notebook ORDER BY book ASC, chapter ASC, verse ASC"; 
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		
		//Log.d(TAG, "cursor.getCount() = " + String.valueOf(cursor.getCount()));
		while (cursor.moveToNext()) {
			//Log.d(TAG, cursor.getString(0) + " " + cursor.getString(1) + ":" + cursor.getString(2) + " " + cursor.getString(3));
			SpannableString ss = new SpannableString(cursor.getString(3));
			scriptureList.add(new ScriptureData(myContext, cursor.getString(0), cursor.getString(1), cursor.getString(2), ss));
		}
		
		cursor.close();
		db.close();
		return scriptureList;
	}
	
	public boolean checkIfRecordExists(int book, int chapter, int verse) {
		if (getScripture(book, chapter, verse) == "") 
			return false;
		else
			return true;
	}
	
	public void insertRecord(int book, int chapter, int verse, String scripture, String notes, String links) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		
		//insert a record
		values.put("book", book);
		values.put("chapter", chapter);
		values.put("verse", verse);
		values.put("scripture", scripture);
		db.insert("notebook", null, values);
		
		db.close();
	}
	
	public void updateRecord(int book, int chapter, int verse, String scripture, String notes, String links) {
		//This function won't do anything if the record doesn't exist
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		
		values.put("scripture", scripture);
		values.put("notes", notes);
		values.put("links", links);
		db.update("notebook", values, "book=" + book + " AND chapter=" + chapter + " AND verse=" + verse, null);	
		
		db.close();
	}
	
	public void deleteRecord(int book, int chapter, int verse) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete("notebook", "book=" + book + " AND chapter=" + chapter + " AND verse=" + verse, null);
		
		db.close();
	}
} 
