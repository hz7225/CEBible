package com.example.cebible;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.SQLException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class CEBible_MainActivity extends Activity implements OnItemClickListener {
	String TAG = "CEBible";
	
	ListView listViewOT;
	ListView listViewNT;
	ListView listViewChapter;
	ListView listViewVerse;
	
	SharedPreferences prefs;
	int prefsBook;
    int prefsChapter;
    int prefsVerse;
    
	MyDataAdapter adapterOT;
	MyDataAdapter adapterNT;
	MyDataAdapter adapterChapter;
	MyDataAdapter adapterVerse;
	
	List<String> chapterList = new ArrayList<String>();
	List<String> verseList = new ArrayList<String>();
	
	DataBaseHelper BibleDB = new DataBaseHelper(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.activity_main);

    	//Create  Android SQLite database file from downloaded file
    	DataBaseHelper myDbHelper = new DataBaseHelper(this);
    	try {
    		myDbHelper.createDataBase();
    	} catch (IOException ioe) {
    		throw new Error("Unable to create database");
    	} 

    	// Get book names resources from XML
    	String[] ot = getResources().getStringArray(R.array.old_testament);
    	String[] nt = getResources().getStringArray(R.array.new_testament);
    	List<String> OTList = new ArrayList<String>();
    	List<String> NTList = new ArrayList<String>();	   
    	for (int i=0; i<ot.length; i++) {
    		String book = ot[i].substring(0, ot[i].indexOf(","));
    		OTList.add(book);
    	}
    	for (int i=0; i<nt.length; i++) {
    		String book = nt[i].substring(0, nt[i].indexOf(","));
    		NTList.add(book);
    	}
    	
    	// Get Listviews and set onClick listeners
    	listViewOT = (ListView) findViewById(R.id.listViewOT);
    	listViewNT = (ListView) findViewById(R.id.listViewNT);		
    	listViewChapter = (ListView) findViewById(R.id.listViewChapter);
    	listViewVerse = (ListView) findViewById(R.id.listViewVerse);		
    	//Set listeners	
    	listViewOT.setOnItemClickListener(this);
    	listViewNT.setOnItemClickListener(this);
    	listViewChapter.setOnItemClickListener(this);
    	listViewVerse.setOnItemClickListener(this);

    	// Instantiate Listview adapters
    	adapterOT = new MyDataAdapter(this, OTList);
    	adapterNT = new MyDataAdapter(this, NTList);	
    	adapterChapter = new MyDataAdapter(this, chapterList);
    	adapterVerse = new MyDataAdapter(this, verseList);

    	// Get saved preferences
    	prefs  = getSharedPreferences("BiblePreferences", MODE_PRIVATE);		    
    	prefsBook = prefs.getInt("BOOK", 1);
    	prefsChapter = prefs.getInt("CHAPTER", 1);
    	prefsVerse = prefs.getInt("VERSE", 1);

    	//Highlight selected list items from saved preferences
	    if (prefsBook < 40) {  //OT book
	    	adapterOT.selected_item = prefsBook - 1;
	    }
	    else {  //NT book
	    	adapterNT.selected_item = prefsBook - 40;
	    }
	    adapterChapter.selected_item = prefsChapter - 1;
		adapterVerse.selected_item = prefsVerse - 1;
	    
	    //Generate data for Chapter and Verse listviews
	    populateListOfChapterAndVerse();
	    
	    //Set ListView adapters
	    listViewOT.setAdapter(adapterOT);
		listViewNT.setAdapter(adapterNT);				
		listViewChapter.setAdapter(adapterChapter);
		listViewVerse.setAdapter(adapterVerse);	
    }
    
    private void populateListOfChapterAndVerse() {	
		// Find the number of chapters from Bible database
		int c;
		int v;
		//DataBaseHelper BibleDB = new DataBaseHelper(this);
		Log.d(TAG, "prefsBook = " + String.valueOf(prefsBook) + " prefsChapter = " + String.valueOf(prefsChapter) + " prefsVerse = " + String.valueOf(prefsVerse));
		try {
			c = BibleDB.getNumOfChapters(prefsBook);
			v = BibleDB.getNumOfVerses(prefsBook, prefsChapter);
		} catch(SQLException sqle) {	    	
	    	throw sqle;
	    }
		
		chapterList.clear();
		for (int i = 1; i <= c; i++) {
			chapterList.add(String.valueOf(i));
		}
		verseList.clear();
		for (int i = 1; i <= v; i++) {
			verseList.add(String.valueOf(i));
		}
	}
    
    protected void onPause() {
		super.onPause();
		
		Log.d(TAG, "onPause()");
		Editor editor = prefs.edit();
		editor.putInt("BOOK", prefsBook);
		editor.putInt("CHAPTER", prefsChapter);
		editor.putInt("VERSE", prefsVerse);
		editor.commit();
	}
    
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {			

    	if (parent == (ListView) findViewById(R.id.listViewOT)) {
			adapterOT.selected_item = position; //Select OT book
			adapterNT.selected_item = -1; //Unselect NT book
			prefsBook = position + 1; //Set book preferences
			prefsChapter = 1;  //Reset chapter preferences to 1
			prefsVerse = 1;  //Reset verse preferences to 1
			adapterChapter.selected_item = prefsChapter-1;
			adapterVerse.selected_item = prefsVerse-1;
			Log.d(TAG, "position="+String.valueOf(position) + ", id=" + String.valueOf(id) + 
					   " prefsBook = " + String.valueOf(prefsBook));
		}
		if (parent == (ListView) findViewById(R.id.listViewNT)) {
			adapterOT.selected_item = -1;  //Unselect OT book
			adapterNT.selected_item = position;  //Select NT book
			prefsBook = position + 40;  //Set book preferences
			prefsChapter = 1; //Reset chapter preferences to 1
			prefsVerse = 1;  //Reset verse preferences to 1
			adapterChapter.selected_item = prefsChapter-1;
			adapterVerse.selected_item = prefsVerse-1;
			Log.d(TAG, "position="+String.valueOf(position) + ", id=" + String.valueOf(id) + 
					   " prefsBook = " + String.valueOf(prefsBook));
		}		
				
		if (parent == (ListView) findViewById(R.id.listViewChapter)) {
			adapterChapter.selected_item = position;
			prefsChapter = position + 1; 
			prefsVerse = 1; //Reset verse preference to 1
			Log.d(TAG, "position="+String.valueOf(position) + ", id=" + String.valueOf(id) + 
					   " prefsChapter = " + String.valueOf(prefsChapter));
		}
		
		if (parent == (ListView) findViewById(R.id.listViewVerse)) {
			adapterVerse.selected_item = position;
			prefsVerse = position + 1;
			Log.d(TAG, "position="+String.valueOf(position) + ", id=" + String.valueOf(id) + 
					   " prefsVerse = " + String.valueOf(prefsVerse));
			
			//Start DisplayActivity
			launchDisplayActivity(prefsBook, prefsChapter, prefsVerse);
		}
		
		populateListOfChapterAndVerse();
		
		adapterOT.notifyDataSetChanged();
		adapterNT.notifyDataSetChanged();		
		adapterChapter.notifyDataSetChanged();
		adapterVerse.notifyDataSetChanged();	
    }
    
    private void launchDisplayActivity(int book, int chapter, int verse) {
		Intent intent = new Intent(this, DisplayActivity.class);
		intent.putExtra("BOOK", book);
		intent.putExtra("CHAPTER", chapter);
		intent.putExtra("VERSE", verse);
		startActivity(intent);
	}	
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.ce_bible, menu);
		return true;
	}
    
}

