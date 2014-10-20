package com.hz7225.cebible;

import java.util.Locale;

import com.hz7225.cebible.DisplayPageFragment;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.SearchManager;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;

public class DisplayActivity extends FragmentActivity {
	String TAG = "DisplayActivity";
		
	int num_chapters = 1189;  //total number of chapters in the Holy Bible
	
	private ViewPager mPager;
	private PagerAdapter mPagerAdapter;
	private int mBook;
	public static int mChapter;
	private int mVerse;
	public String mChapterNameAndNumber;
	
	SharedPreferences prefs;
    String prefsLanguage;
    String prefsEN_Trans;
    String prefsCH_Trans;
    
    public static boolean eng_ch_compare = false;
    
    DataBaseHelper BibleDB;
    //Action bar menu
  	Menu ab_menu;
          
  	Locale myLocale;
  	
  	static public TextToSpeech ttsobj;
	
  	// This method hides the system bars and resize the content
  	private void hideSystemUI() {
  		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
  				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
  				| View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
  				// remove the following flag for version < API 19
  				| View.SYSTEM_UI_FLAG_IMMERSIVE); 
  	} 
  	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_pager);
        
        //hideSystemUI();
        
        ttsobj=new TextToSpeech(this, 
				new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				if(status != TextToSpeech.ERROR){
					ttsobj.setLanguage(Locale.CHINESE);
				}				
			}
		});

        // To allow Up navigation with the app icon in the action bar
    	getActionBar().setDisplayHomeAsUpEnabled(true);
        
        // Get saved preferences
    	prefs  = getSharedPreferences("BiblePreferences", MODE_PRIVATE);		    
    	prefsLanguage = prefs.getString("LANGUAGE", getString(R.string.ch));
    	prefsEN_Trans = prefs.getString("EN_TRANS", getString(R.string.kjv));
    	prefsCH_Trans = prefs.getString("CH_TRANS", getString(R.string.cuvt));
    	
    	//Log.d(TAG, "Preferences: Lang=" + prefsLanguage + " EN_trans=" + prefsEN_Trans + " CH_Trans=" + prefsCH_Trans);
        
        Intent intent = getIntent();
        mBook = intent.getIntExtra("BOOK", 1);
        mChapter = intent.getIntExtra("CHAPTER", 1);
        mVerse = intent.getIntExtra("VERSE", 1);
        
        //Log.d(TAG, "Intent: mBook=" + mBook + " mChapter=" + mChapter + " mVerse=" + mVerse);

        mPager = (ViewPager) findViewById(R.id.pager);       
        mPagerAdapter = new PagerAdapter(getFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        ChapterPosition cp = new ChapterPosition(this, mBook, mChapter);
        mPager.setCurrentItem(cp.getPosition());
        
        // Set action bar title with book name and chapter number
        mChapterNameAndNumber = getBookName() +" "+String.valueOf(mChapter);
		ActionBar actionBar = getActionBar();
		actionBar.setTitle(mChapterNameAndNumber);		
		
		// Update action bar after scrolling to a new chapter
		mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				//Log.d(TAG, "position = " + String.valueOf(position));
				ChapterPosition cp = new ChapterPosition(DisplayActivity.this, position);
				mBook = cp.getBook();
				mChapter = cp.getChapter();
				mChapterNameAndNumber = getBookName() +" "+String.valueOf(mChapter);
				ActionBar actionBar = getActionBar();
				actionBar.setTitle(mChapterNameAndNumber);	
			}
		});
	}
	
	public void onDestroy() {
		super.onDestroy();
		//Log.d(TAG, "DisplayActivity: onDestroy()");
		
		//Shut down the TTS engine
		ttsobj.shutdown();
	}
	
	private String getBookName() {
		// Get book name from XML resource
		String[] ot;
		String[] nt;

		ot = getResources().getStringArray(R.array.old_testament);
		nt = getResources().getStringArray(R.array.new_testament);
		//Log.d(TAG, " ************** mBook=" + String.valueOf(mBook) + " mChapter="+String.valueOf(mChapter));
		String book;
		if (mBook<40) {
			book = ot[mBook-1].substring(0, ot[mBook-1].indexOf(","));
		} else {
			book = nt[mBook-1-39].substring(0, nt[mBook-1-39].indexOf(","));
		}
		
		return book;
	}
	
	private void mySetLocale() {
    	if (prefsLanguage.equals(getString(R.string.en))) {
    		setLocale("en", "");
    	} 
    	else if (prefsLanguage.equals(getString(R.string.ch))) {
    		if (prefsCH_Trans.equals(getString(R.string.cuvs))) {
    			setLocale("zh", "CN");
    		} else if (prefsCH_Trans.equals(getString(R.string.cuvt))) {
    			setLocale("zh", "TW");
    		}
    	} else if (prefsLanguage.equals(getString(R.string.ch_en))) {
    		if (prefsCH_Trans.equals(getString(R.string.cuvs))) {
    			setLocale("zh", "CN");
    		} else if (prefsCH_Trans.equals(getString(R.string.cuvt))) {
    			setLocale("zh", "TW");
    		}
    	}
    }
	
	public void setLocale(String lang, String country) {
		//Log.d(TAG, "setLcale, land = " + lang + " country = " + country);
		myLocale = new Locale(lang, country);
		Resources res = getResources();
		DisplayMetrics dm = res.getDisplayMetrics();
		Configuration conf = res.getConfiguration();
		conf.locale = myLocale;
		res.updateConfiguration(conf, dm);
		
		//Call this function so the Titles of the Menu Items are updated with the correct language
		onConfigurationChanged(conf);
	}
	
	private class PagerAdapter extends FragmentStatePagerAdapter {
		
        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
        	//Log.d(TAG, "PageFragment.create, position="+String.valueOf(position) + " prefsVersion=" + prefsVersion);
        	return DisplayPageFragment.create(position, mBook, mVerse, prefsLanguage, prefsEN_Trans, prefsCH_Trans);
        }

        @Override
        public int getCount() {
        	return num_chapters;
        }
        
        //This method is implemented to refresh the ViewPager, not in a way for good performance
        public int getItemPosition(Object object) {
    	    return POSITION_NONE;
    	}
    }
	
	//Action bar menu
	MenuItem item;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.display_activity, menu);
				
		item = menu.findItem(R.id.action_change);  //change item on the action bar
		if (prefsLanguage.equals(getString(R.string.en))) {
			if (prefsEN_Trans.equals(getString(R.string.kjv))) {
				item.setTitle(getString(R.string.kjv));
			} 	else if (prefsEN_Trans.equals(getString(R.string.web))) {
				item.setTitle(getString(R.string.web));
			} 		
		} else {	
			item.setTitle(getString(R.string.cuv));
		}
		
		//Save the menu
		ab_menu = menu;	
		
		// Associate searchable configuration with the SearchView
	    SearchManager searchManager =
	           (SearchManager) getSystemService(Context.SEARCH_SERVICE);
	    SearchView searchView =
	            (SearchView) menu.findItem(R.id.action_search).getActionView();
	    searchView.setSearchableInfo(
	            searchManager.getSearchableInfo(getComponentName()));
		
		return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem it) {
		Editor editor = prefs.edit();
        // Handle presses on the action bar items
        switch (it.getItemId()) 
        {		
        case android.R.id.home:
            NavUtils.navigateUpFromSameTask(this);
            return true;
        case R.id.action_change:
        	//Log.d(TAG, "onOptionsItemSelected: R.id.action_change");
        	if (prefsLanguage.equals(getString(R.string.ch))) {
        		//Change to display English only
        		//setLocale("en", "");        		
        		prefsLanguage = getString(R.string.en);
        		mySetLocale();
        		item.setTitle(prefsEN_Trans);
        		//Enable the Search menu        		
        		ab_menu.getItem(1).setVisible(true);
        	} else if (prefsLanguage.equals(getString(R.string.en))) {
        		//Change to display Chinese-English side by side
        		//setLocale("zh", "CN");
        		prefsLanguage = getString(R.string.ch_en);
        		mySetLocale();
        		item.setTitle(getString(R.string.cuv) + "/" + prefsEN_Trans);
        		//Disable the Search menu        		
        		ab_menu.getItem(1).setVisible(false);
        	} else if (prefsLanguage.equals(getString(R.string.ch_en))) {	
        		//Change to display Chinese only
        		//setLocale("zh", "CN");
        		prefsLanguage = getString(R.string.ch);
        		mySetLocale();
        		item.setTitle(getString(R.string.cuv));
        		//Enable the Search menu        		
        		ab_menu.getItem(1).setVisible(true);
        	}    	
        	
        	editor.putString("LANGUAGE", prefsLanguage);
        	editor.commit();
        	
        	// Update action bar title with book name and chapter number
    		ActionBar actionBar = getActionBar();
    		actionBar.setTitle(getBookName() +" "+String.valueOf(mChapter));
    		
    		// Update the ViewPager's content on the screen immediately
    		mPagerAdapter.notifyDataSetChanged();  
        	
        	return true;
        case R.id.action_search: 
        	return true;
        /*
        case R.id.action_read: //read the whole Bible	
        	//Testing: to go to the next page
        	//mPager.setCurrentItem(mPager.getCurrentItem()+1);        	
        	return true;
        */		
        default:
            return super.onOptionsItemSelected(it);	        	
        }
	}  
	
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    //Log.d(TAG, "onConfigurationChanged");
	    MenuItem it = ab_menu.findItem(R.id.action_search);
	    it.setTitle(R.string.action_search);
	}    
}

