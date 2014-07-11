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
import android.widget.TextView;

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
    String prefsLanguage;
    String prefsVersion;
    
	ListDataAdapter adapterOT;
	ListDataAdapter adapterNT;
	ListDataAdapter adapterChapter;
	ListDataAdapter adapterVerse;
	
	List<String> OTList = new ArrayList<String>();
	List<String> NTList = new ArrayList<String>();
	
	List<String> chapterList = new ArrayList<String>();
	List<String> verseList = new ArrayList<String>();
	
	DataBaseHelper BibleDB = new DataBaseHelper(this, "cuvslite.bbl.db");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.activity_main);    	
    	Log.d(TAG, "onCreate()");

    	//Create  Android SQLite database file from downloaded file
    	DataBaseHelper myDbHelper = new DataBaseHelper(this, "cuvslite.bbl.db");
    	//myDbHelper.setDB("cuvslite.bbl.db");
    	try {
    		myDbHelper.createDataBase();
    	} catch (IOException ioe) {
    		throw new Error("Unable to create database");
    	} 
    	DataBaseHelper myDbHelper2 = new DataBaseHelper(this, "EB_kjv_bbl.db");
    	//myDbHelper.setDB("EB_kjv_bbl.db");
    	try {
    		myDbHelper2.createDataBase();
    	} catch (IOException ioe) {
    		throw new Error("Unable to create database");
    	} 
    	
    	// Get saved preferences
    	prefs  = getSharedPreferences("BiblePreferences", MODE_PRIVATE);		    
    	prefsBook = prefs.getInt("BOOK", 1);
    	prefsChapter = prefs.getInt("CHAPTER", 1);
    	prefsVerse = prefs.getInt("VERSE", 1);
    	prefsLanguage = prefs.getString("LANGUAGE", getString(R.string.ch));
    	prefsVersion = prefs.getString("VERSION", getString(R.string.cuvs));

    	// Get book names resources from XML and initialize OTList and NTList
    	populateListOfBooknames();
    	
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
    	adapterOT = new ListDataAdapter(this, OTList);
    	adapterNT = new ListDataAdapter(this, NTList);	
    	adapterChapter = new ListDataAdapter(this, chapterList);
    	adapterVerse = new ListDataAdapter(this, verseList);

    	//Highlight selected list items from saved preferences
	    if (prefsBook < 40) {  //OT book
	    	adapterOT.selected_item = prefsBook - 1;
	    }
	    else {  //NT book
	    	adapterNT.selected_item = prefsBook - 40;
	    }
	    adapterChapter.selected_item = prefsChapter - 1;
		adapterVerse.selected_item = prefsVerse - 1;
	    
	    //Generate data for Chapter and Verse ListViews, chapterList and verseList
	    populateListOfChapterAndVerse();
	    
	    //Set ListView adapters
	    listViewOT.setAdapter(adapterOT);
		listViewNT.setAdapter(adapterNT);				
		listViewChapter.setAdapter(adapterChapter);
		listViewVerse.setAdapter(adapterVerse);	
		
		//Show titles (OT, NT, Chapter, Verse) in selected language
		displayTitles();
    }
    
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume()");
		prefsLanguage = prefs.getString("LANGUAGE", getString(R.string.ch));
		displayTitles();
		
		Editor editor = prefs.edit();		   
		if (prefsLanguage.equals(getString(R.string.en))) {
			editor.putString("VERSION", getString(R.string.kjv));
		} else {
			editor.putString("VERSION", getString(R.string.cuvs));     	
		}
		editor.commit();
		
		if (item != null) {
			// Change text on the action bar
			//if (prefsLanguage != getString(R.string.ch_en)) {
				item.setTitle(prefsLanguage);
			//}				
			// Change the lists on the display
			populateListOfBooknames();
			adapterNT.notifyDataSetChanged();
			adapterOT.notifyDataSetChanged();
		}
	}
	
	private void displayTitles() {
		TextView tv1 = (TextView)findViewById(R.id.ot);
		TextView tv2 = (TextView)findViewById(R.id.nt);
		TextView tv3 = (TextView)findViewById(R.id.chapter);
		TextView tv4 = (TextView)findViewById(R.id.verse);
		if (prefsLanguage.equals(getString(R.string.en))) {
			tv1.setText(R.string.ot);
			tv2.setText(R.string.nt);
			tv3.setText(R.string.chapter);
			tv4.setText(R.string.verse);
		} else {
			tv1.setText(R.string.ot_ch);
			tv2.setText(R.string.nt_ch);
			tv3.setText(R.string.chapter_ch);
			tv4.setText(R.string.verse_ch);
		}
	}
    
    private void populateListOfBooknames() {
    	String[] ot;
    	String[] nt;
    	if (prefsLanguage.equals(getString(R.string.en))) {
    		ot = getResources().getStringArray(R.array.old_testament);
        	nt = getResources().getStringArray(R.array.new_testament);
    	} else {
    		ot = getResources().getStringArray(R.array.old_testament_ch);
    		nt = getResources().getStringArray(R.array.new_testament_ch);
    	}	
    	OTList.clear();
    	NTList.clear();
    	for (int i=0; i<ot.length; i++) {
    		String book = ot[i].substring(0, ot[i].indexOf(","));
    		OTList.add(book);
    	}
    	for (int i=0; i<nt.length; i++) {
    		String book = nt[i].substring(0, nt[i].indexOf(","));
    		NTList.add(book);
    	}
    }
    
    private void populateListOfChapterAndVerse() {	
		// Find the number of chapters from Bible database
		int c;
		int v;

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
		editor.putString("LANGUAGE", prefsLanguage);
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
			prefsVerse = position;
			Log.d(TAG, "position="+String.valueOf(position) + ", id=" + String.valueOf(id) + 
					   " prefsVerse = " + String.valueOf(prefsVerse));
			
			//Start DisplayActivity
			launchDisplayActivity(prefsBook, prefsChapter, prefsVerse, prefsLanguage);
		}
		
		populateListOfChapterAndVerse();
		
		adapterOT.notifyDataSetChanged();
		adapterNT.notifyDataSetChanged();		
		adapterChapter.notifyDataSetChanged();
		adapterVerse.notifyDataSetChanged();	
    }
    
    private void launchDisplayActivity(int book, int chapter, int verse, String language) {
		Intent intent = new Intent(this, DisplayActivity.class);
		intent.putExtra("BOOK", book);
		intent.putExtra("CHAPTER", chapter);
		intent.putExtra("VERSE", verse);
		intent.putExtra("LANGUAGE", language);
		startActivity(intent);
	}
    
    //Action bar menu
  	MenuItem item;
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds s to the action bar if it is present.
		getMenuInflater().inflate(R.menu.ce_bible, menu);
		item = menu.findItem(R.id.action_language);  //change item on the action bar
		//if (prefsLanguage != getString(R.string.ch_en)) {
			item.setTitle(prefsLanguage);
		//}		
		return true;
	}
    
    @Override
    public boolean onOptionsItemSelected(MenuItem it) {
        // Handle presses on the action bar items
        switch (it.getItemId()) 
        {		
        case R.id.action_language:
        	Log.d(TAG, "Language clicked");	
        	if (prefsLanguage.equals(getString(R.string.ch))) {
        		prefsLanguage = getString(R.string.en);
        	} else if (prefsLanguage.equals(getString(R.string.en))){
        		prefsLanguage = getString(R.string.ch);
        	}
        	// Save the change in the Preferences
        	Editor editor = prefs.edit();
        	editor.putString("LANGUAGE", prefsLanguage);
        	editor.commit();
        	// Change text on the action bar
        	item.setTitle(prefsLanguage);
        	// Change the lists on the display
        	populateListOfBooknames();
        	adapterNT.notifyDataSetChanged();
        	adapterOT.notifyDataSetChanged();
        	//Update Titles
        	displayTitles();
        	return true;
        case R.id.action_settings: 
        	return true;	
        default:
            return super.onOptionsItemSelected(it);	        	
        }
	}   

}

