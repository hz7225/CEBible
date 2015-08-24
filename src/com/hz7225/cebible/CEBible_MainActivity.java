package com.hz7225.cebible;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;

import android.database.SQLException;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
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
	int prefsBook; //1-66
    int[] prefsChapter; //low limit is 1
    int prefsVerse; //low limit is 1
    String prefsLanguage;
    String prefsEN_Trans;
    String prefsCH_Trans;
    
	ListDataAdapter adapterOT;
	ListDataAdapter adapterNT;
	ListDataAdapter adapterChapter;
	ListDataAdapter adapterVerse;
	
	List<String> OTList = new ArrayList<String>();
	List<String> NTList = new ArrayList<String>();	
	List<String> chapterList = new ArrayList<String>();
	List<String> verseList = new ArrayList<String>();
	
	SetLocale myLocale;
	
	String swVersion;
	
	static public TextToSpeech ttsobj;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.activity_main); 
    	
    	//Get the sw version number from the AndroidManifest.XML file
    	try {
    		swVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
    		//Log.d(TAG, "sw version v" + swVersion);
    	} catch (NameNotFoundException e) {
    		//Log.e("tag", e.getMessage());
    	}
    	
    	//Log.d(TAG, "Create ttsobj");
        ttsobj=new TextToSpeech(this, new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				if(status != TextToSpeech.ERROR){
					ttsobj.setLanguage(Locale.CHINESE);
				}				
			}
		});
    	
    	/*
    	//Delete the old databases files. 
    	File file = new File("/data/data/com.hz7225.cebible/databases/cuvslite.bbl.db");
    	file.delete();
    	File file2 = new File("/data/data/com.hz7225.cebible/databases/cuvslite.bbl.db-journal");
    	file2.delete();
    	File file3 = new File("/data/data/com.hz7225.cebible/databases/cuvtlite.bbl.db");
    	file3.delete();
    	File file4 = new File("/data/data/com.hz7225.cebible/databases/cuvtlite.bbl.db-journal");
    	file4.delete(); 
    	*/    	 
    	
    	//Log.d(TAG, "Device API level = " + String.valueOf(android.os.Build.VERSION.SDK_INT));

    	//Create  Android SQLite database files from downloaded Bible databases
    	DataBaseHelper myDbHelper = new DataBaseHelper(this, "EB_kjv_bbl.db");    	
    	
    	myDbHelper.setDB("EB_kjv_bbl.db");
    	try {
    		myDbHelper.createDataBase();
    	} catch (IOException ioe) {
    		throw new Error("Unable to create database");
    	}     	
    	myDbHelper.setDB("EB_web_bbl.db");
    	try {
    		myDbHelper.createDataBase();
    	} catch (IOException ioe) {
    		throw new Error("Unable to create database");
    	} 
    	myDbHelper.setDB("CB_cuvslite.bbl.db");
    	try {
    		myDbHelper.createDataBase();
    	} catch (IOException ioe) {
    		throw new Error("Unable to create database");
    	} 
    	myDbHelper.setDB("CB_cuvtlite.bbl.db");
    	try {
    		myDbHelper.createDataBase();
    	} catch (IOException ioe) {
    		throw new Error("Unable to create database");
    	} 
    	
    	//Initialize prefsChapter array
    	prefsChapter = new int[66];
    	
    	// Get saved preferences
    	prefs  = getSharedPreferences("BiblePreferences", MODE_PRIVATE);		    
    	prefsBook = prefs.getInt("BOOK", 1);
    	for (int i=0; i<66; i++) {
    		prefsChapter[i] = prefs.getInt("CHAPTER_"+String.valueOf(i), 1);
    	}
    	prefsVerse = prefs.getInt("VERSE", 1);
    	prefsLanguage = prefs.getString("LANGUAGE", getString(R.string.ch));
    	prefsEN_Trans = prefs.getString("EN_TRANS", getString(R.string.kjv));
    	prefsCH_Trans = prefs.getString("CH_TRANS", getString(R.string.cuvs));
    	
    	//Log.d(TAG, "Preferences: Lang=" + prefsLanguage + " EN_trans=" + prefsEN_Trans + " CH_Trans=" + prefsCH_Trans);
    	
    	//Set Locale according to saved preferences
    	myLocale = new SetLocale(getApplicationContext());
    	myLocale.set();  
    	
    	//Set the correct app name according to language
    	setTitle(getResources().getString(R.string.app_name));

    	// Get book names resources from XML and initialize OTList and NTList
    	populateListOfBooknames();
    	
    	// Get Listviews and set onClick listeners
    	listViewOT = (ListView) findViewById(R.id.listViewOT);
    	listViewNT = (ListView) findViewById(R.id.listViewNT);		
    	listViewChapter = (ListView) findViewById(R.id.listViewChapter);
    	listViewVerse = (ListView) findViewById(R.id.listViewVerse);
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
	    if (prefsBook <= 39) {  //OT book
	    	//Log.d(TAG, "OT, prefsBook = " + String.valueOf(prefsBook));
	    	adapterOT.selected_item = prefsBook - 1;
	    	listViewOT.setSelection(adapterOT.selected_item);  //Set starting position
	    }
	    else {  //NT book
	    	//Log.d(TAG, "NT, prefsBook = " + String.valueOf(prefsBook));
	    	adapterNT.selected_item = prefsBook - 40; //-39-1
	    	listViewNT.setSelection(adapterNT.selected_item);  //Set starting position
	    }
	    adapterChapter.selected_item = prefsChapter[prefsBook-1] - 1;
		adapterVerse.selected_item = prefsVerse - 1;

	    //Generate data for Chapter and Verse ListViews, chapterList and verseList
	    populateListOfChapterAndVerse();
	    
	    //Set ListView adapters
	    listViewOT.setAdapter(adapterOT);
		listViewNT.setAdapter(adapterNT);				
		listViewChapter.setAdapter(adapterChapter);
		listViewVerse.setAdapter(adapterVerse);	
		
		//Set the starting positions of ListViews to make sure 
		//the selected items are visible on the screen
		if (prefsBook <= 39) {  //OT book
	    	listViewOT.setSelection(prefsBook - 1);  
	    }
	    else {  //NT book
	    	listViewNT.setSelection(prefsBook - 40);  
	    }
		listViewChapter.setSelection(prefsChapter[prefsBook-1] - 1); 
		listViewVerse.setSelection(prefsVerse - 1); 
		
		//Show titles (OT, NT, Chapter, Verse) in preferred language
		displayTitles();
    }
    
	protected void onResume() {
		super.onResume();
		
		prefsBook = prefs.getInt("BOOK", 1);
		for (int i=0; i<66; i++) {
			prefsChapter[i] = prefs.getInt("CHAPTER_"+String.valueOf(i), 1);
		}
    	prefsVerse = prefs.getInt("VERSE", 1);
    	if (prefsBook <= 39) {
    		adapterOT.selected_item = prefsBook-1;
        	adapterOT.notifyDataSetChanged();
    	} else {
    		adapterNT.selected_item = prefsBook-40;
        	adapterNT.notifyDataSetChanged();
    	}
    	populateListOfChapterAndVerse(); //re-calculate list of chapters and verses in case they are changed
    	adapterChapter.selected_item = prefsChapter[prefsBook-1]-1;
    	adapterChapter.notifyDataSetChanged();
    	adapterVerse.selected_item = prefsVerse-1;
    	adapterVerse.notifyDataSetChanged();
		//Log.d(TAG, "onResume(), prefsChapter=" + String.valueOf(prefsChapter) + " prefsVerse=" + String.valueOf(prefsVerse));
		
		//Set the starting position
		listViewChapter.setSelection(prefsChapter[prefsBook-1] - 1); 
		listViewVerse.setSelection(prefsVerse - 1);
		
		prefsLanguage = prefs.getString("LANGUAGE", getString(R.string.ch));
    	prefsEN_Trans = prefs.getString("EN_TRANS", getString(R.string.kjv));
    	prefsCH_Trans = prefs.getString("CH_TRANS", getString(R.string.cuvs));
    	
    	//Set Locale
    	myLocale.set();
    	
    	//Set the correct app name according to language
    	setTitle(getResources().getString(R.string.app_name));
    		
		displayTitles();  // Set book, chapter, and verse titles
		
		if (item != null) {
			item.setTitle(prefsLanguage);  //Set Action bar text
			// Change the lists on the display
			populateListOfBooknames();
			adapterNT.notifyDataSetChanged();
			adapterOT.notifyDataSetChanged();
		}
		
		this.invalidateOptionsMenu(); // Update the ActionBar menu with the correct language
	}

    protected void onPause() {
		super.onPause();		
		//Log.d(TAG, "onPause()");
		Editor editor = prefs.edit();
		editor.putInt("BOOK", prefsBook);
		for (int i=0; i<66; i++) {
			editor.putInt("CHAPTER_"+String.valueOf(i), prefsChapter[i]);
		}	
		editor.putInt("VERSE", prefsVerse);
		editor.putString("LANGUAGE", prefsLanguage);
		editor.commit();
	}
    
    public void onDestroy() {
		super.onDestroy();		
		//Log.d(TAG, "onDestroy(): shutdown ttsobj");
				
		//Shut down the TTS engine
		ttsobj.shutdown();
	}
    
	private void displayTitles() {		
		//Log.d(TAG, "displayTitle()");
		((TextView)findViewById(R.id.ot)).setText(R.string.ot);
		((TextView)findViewById(R.id.nt)).setText(R.string.nt);
		((TextView)findViewById(R.id.chapter)).setText(R.string.chapter);
		((TextView)findViewById(R.id.verse)).setText(R.string.verse);
	}
    
    private void populateListOfBooknames() {
    	String[] ot;
    	String[] nt;

    	ot = getResources().getStringArray(R.array.old_testament);
    	nt = getResources().getStringArray(R.array.new_testament);
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

		//Log.d(TAG, "prefsBook = " + String.valueOf(prefsBook) + " prefsChapter = " + String.valueOf(prefsChapter[prefsBook-1]) + " prefsVerse = " + String.valueOf(prefsVerse));
		DataBaseHelper BibleDB = new DataBaseHelper(this, "CB_cuvslite.bbl.db");
		try {
			c = BibleDB.getNumOfChapters(prefsBook);
			v = BibleDB.getNumOfVerses(prefsBook, prefsChapter[prefsBook-1]);
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
    
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {			
    	if (parent == (ListView) findViewById(R.id.listViewOT)) {
			if (adapterOT.selected_item == position) {
				launchDisplayActivity(prefsBook, prefsChapter[prefsBook-1], prefsVerse, prefsLanguage);
			} else {
    		adapterOT.selected_item = position; //Select OT book
			adapterNT.selected_item = -1; //Unselect NT book
			prefsBook = position + 1; //Set book preferences
			//prefsChapter[prefsBook-1] = 1;  //Reset chapter preferences to 1
			prefsVerse = 1;  //Reset verse preferences to 1
			adapterChapter.selected_item = prefsChapter[prefsBook-1]-1;
			adapterVerse.selected_item = prefsVerse-1; 
			//Log.d(TAG, "position="+String.valueOf(position) + ", id=" + String.valueOf(id) + " prefsBook = " + String.valueOf(prefsBook));
			}
		}
		if (parent == (ListView) findViewById(R.id.listViewNT)) {
			if (adapterNT.selected_item == position) {
				launchDisplayActivity(prefsBook, prefsChapter[prefsBook-1], prefsVerse, prefsLanguage);
			} else {
			adapterOT.selected_item = -1;  //Unselect OT book
			adapterNT.selected_item = position;  //Select NT book			
			prefsBook = position + 40;  //Set book preferences
			//prefsChapter[prefsBook-1] = 1; //Reset chapter preferences to 1
			prefsVerse = 1;  //Reset verse preferences to 1
			adapterChapter.selected_item = prefsChapter[prefsBook-1]-1;
			adapterVerse.selected_item = prefsVerse-1;
			//Log.d(TAG, "position="+String.valueOf(position) + ", id=" + String.valueOf(id) + " prefsBook = " + String.valueOf(prefsBook));
			}
		}		
				
		if (parent == (ListView) findViewById(R.id.listViewChapter)) {
			if (adapterChapter.selected_item == position) {
				launchDisplayActivity(prefsBook, prefsChapter[prefsBook-1], prefsVerse, prefsLanguage);
			} else {
			adapterChapter.selected_item = position;
			prefsChapter[prefsBook-1] = position + 1; 
			prefsVerse = 1; //Reset verse preference to 1
			//Log.d(TAG, "position="+String.valueOf(position) + ", id=" + String.valueOf(id) + " prefsChapter = " + String.valueOf(prefsChapter));
			}
		}
		
		if (parent == (ListView) findViewById(R.id.listViewVerse)) {
			adapterVerse.selected_item = position;
			prefsVerse = position + 1;
			//Log.d(TAG, "position="+String.valueOf(position) + ", id=" + String.valueOf(id) + " prefsVerse = " + String.valueOf(prefsVerse));
			
			//Start DisplayActivity
			launchDisplayActivity(prefsBook, prefsChapter[prefsBook-1], prefsVerse, prefsLanguage);
		}
		
		populateListOfChapterAndVerse();
		
		adapterOT.notifyDataSetChanged();
		adapterNT.notifyDataSetChanged();		
		adapterChapter.notifyDataSetChanged();
		adapterVerse.notifyDataSetChanged();
		
		//Set the starting position
		listViewChapter.setSelection(prefsChapter[prefsBook-1] - 1); 
		listViewVerse.setSelection(prefsVerse - 1);
    }
    
    private void launchDisplayActivity(int book, int chapter, int verse, String language) {
		Intent intent = new Intent(this, DisplayActivity.class);
		intent.putExtra("PARENT", "MainActivity");
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
		item.setTitle(prefsLanguage);
		
		//Set the version number
		menu.findItem(R.id.action_about).setTitle(getString(R.string.action_about) + swVersion);
		
		//Make the Favorite icon invisible if the Notebook DB has no entry yet
		NotebookDbHelper mDbHelper = new NotebookDbHelper(this.getApplicationContext());
    	if (mDbHelper.getScriptureDataList().size() == 0) {
    		item = menu.findItem(R.id.action_favorite);
    		item.setVisible(false);
    	}	
		
		return true;
	}
    
    @Override
    public boolean onOptionsItemSelected(MenuItem it) {
        // Handle presses on the action bar items
        switch (it.getItemId()) 
        {	
        case R.id.action_favorite:
        	Intent intent2 = new Intent(this.getApplicationContext(), FavoriteActivity.class);
    		startActivity(intent2);
        	return true;
        case R.id.action_language:        	
        	if (prefsLanguage.equals(getString(R.string.ch))) {
        		prefsLanguage = getString(R.string.en);
        	} else if (prefsLanguage.equals(getString(R.string.en))){
        		prefsLanguage = getString(R.string.ch);
        	}
        	
        	// Save the change in the Preferences
        	Editor editor = prefs.edit();
        	editor.putString("LANGUAGE", prefsLanguage);
        	editor.commit();
        	
        	//Set the Locale resources
        	myLocale.set();     
        	
        	//Set the correct app name according to language
        	setTitle(getResources().getString(R.string.app_name));
        	
        	item.setTitle(prefsLanguage);  // Change text on the action bar
        	
        	// Change the lists on the display
        	populateListOfBooknames();
        	adapterNT.notifyDataSetChanged();
        	adapterOT.notifyDataSetChanged();
        	displayTitles();  // Set book, chapter, and verse titles
        	this.invalidateOptionsMenu(); // Update the ActionBar menu with the correct language
        	return true;
        case R.id.action_settings: 
        	Intent intent = new Intent(this, SettingsActivity.class);
    		startActivity(intent);
        	return true;
        case R.id.action_about: 
        	return true;	
        default:
            return super.onOptionsItemSelected(it);	        	
        }
	}   
}

