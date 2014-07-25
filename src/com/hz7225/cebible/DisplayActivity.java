package com.hz7225.cebible;

import java.util.ArrayList;
import java.util.List;

import com.hz7225.cebible.DisplayPageFragment;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class DisplayActivity extends FragmentActivity {
	String TAG = "DisplayActivity";
	
	
	int num_chapters;
	private ViewPager mPager;
	private PagerAdapter mPagerAdapter;
	private int mBook;
	private int mChapter;
	private int mVerse;
	
	SharedPreferences prefs;
    String prefsLanguage;
    String prefsVersion;
    
    DataBaseHelper BibleDB;
    //Action bar menu
  	Menu ab_menu;
          
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_pager);
        
        // To allow Up navigation with the app icon in the action bar
    	getActionBar().setDisplayHomeAsUpEnabled(true);
        
        // Get saved preferences
    	prefs  = getSharedPreferences("BiblePreferences", MODE_PRIVATE);		    
    	prefsLanguage = prefs.getString("LANGUAGE", getString(R.string.ch));
    	prefsVersion = prefs.getString("VERSION", getString(R.string.cuvs));
        
        Intent intent = getIntent();
        int book = intent.getIntExtra("BOOK", 1);
        int chapter = intent.getIntExtra("CHAPTER", 1);
        int verse = intent.getIntExtra("VERSE", 1);
        
        mBook = book;
        mChapter = chapter;
        mVerse = verse;
        
        //Get number of chapters of a book
        BibleDB = new DataBaseHelper(this, "cuvslite.bbl.db");
        num_chapters = BibleDB.getNumOfChapters(book);        

        mPager = (ViewPager) findViewById(R.id.pager);       
        mPagerAdapter = new PagerAdapter(getFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(mChapter-1);  
        
        // Set action bar title with book name and chapter number
		ActionBar actionBar = getActionBar();
		actionBar.setTitle(getBookName() +" "+String.valueOf(mChapter));
		
		
		// Update action bar after scrolling to a new chapter
		mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				ActionBar actionBar = getActionBar();
				actionBar.setTitle(getBookName() +" "+String.valueOf(position+1));
				//Save the position
				mChapter = position + 1;
			}
		});
	}
	
	protected void onResume() {
		super.onResume();
		
		prefsLanguage = prefs.getString("LANGUAGE", getString(R.string.ch));
		Editor editor = prefs.edit();
   
		/*
		if (prefsLanguage.equals(getString(R.string.en))) {
			//Log.d(TAG, "DiaplayActivity::onResume(), prefsLanguage = 'EN'");
			editor.putString("VERSION", getString(R.string.kjv));
			//item.setTitle("KJV");
			//BibleDB.setDB("EB_kjv_bbl.db");
		} else {
			//Log.d(TAG, "DiaplayActivity::onResume(), prefsLanguage = 'CH'");
			editor.putString("VERSION", getString(R.string.cuvs));
			//item.setTitle("CUVS");
			//BibleDB.setDB("cuvslite.bbl.db");            	
		}
		editor.commit();
		*/
	}
	
	private String getBookName() {
		// Get book name from XML resource
		String[] ot;
		String[] nt;
		if (prefsLanguage.equals(getString(R.string.ch))) {
			ot = getResources().getStringArray(R.array.old_testament_ch);
			nt = getResources().getStringArray(R.array.new_testament_ch);
		} else {
			ot = getResources().getStringArray(R.array.old_testament);
			nt = getResources().getStringArray(R.array.new_testament);
		}
		//Log.d(TAG, " ************** mBook=" + String.valueOf(mBook) + " mChapter="+String.valueOf(mChapter));
		String book;
		if (mBook<40) {
			book = ot[mBook-1].substring(0, ot[mBook-1].indexOf(","));
		} else {
			book = nt[mBook-1-39].substring(0, nt[mBook-1-39].indexOf(","));
		}
		
		return book;
	}
	
	private class PagerAdapter extends FragmentStatePagerAdapter {
        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
        	//Log.d(TAG, "PageFragment.create, position="+String.valueOf(position));
            return DisplayPageFragment.create(position, mBook, mVerse, prefsVersion);
        }

        @Override
        public int getCount() {
        	return num_chapters;
        }
        
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
		Editor editor = prefs.edit();
		if (prefsLanguage.equals(getString(R.string.ch))) {
			item.setTitle(getString(R.string.cuvs));
			editor.putString("VERSION", getString(R.string.cuvs));
		}
		else {
			item.setTitle(getString(R.string.kjv));
			editor.putString("VERSION", getString(R.string.kjv));
		}
		editor.commit();
		//Save the menu
		ab_menu = menu;
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
        	if (prefsVersion.equals(getString(R.string.cuvs))) {
        		prefsLanguage = getString(R.string.en);
        		prefsVersion = getString(R.string.kjv);
        		editor.putString("LANGUAGE", prefsLanguage);
        		editor.putString("VERSION", prefsVersion);
        		item.setTitle(getString(R.string.kjv));
                //BibleDB.setDB("EB_kjv_bbl.db");
        	} else if (prefsVersion.equals(getString(R.string.kjv))) {
        		prefsLanguage = getString(R.string.ch);
        		prefsVersion = getString(R.string.cuvs_kjv);
        		editor.putString("LANGUAGE", prefsLanguage);
        		editor.putString("VERSION", prefsVersion);
        		item.setTitle(getString(R.string.cuvs_kjv));
        	} else if (prefsVersion.equals(getString(R.string.cuvs_kjv))) {	
        		prefsLanguage = getString(R.string.ch);
        		prefsVersion = getString(R.string.cuvs);
        		editor.putString("LANGUAGE", prefsLanguage);
        		editor.putString("VERSION", prefsVersion);
        		item.setTitle(getString(R.string.cuvs));
        	}
        	editor.commit();
        	
        	// Update action bar title with book name and chapter number
    		ActionBar actionBar = getActionBar();
    		actionBar.setTitle(getBookName() +" "+String.valueOf(mChapter));
    		
    		// Update the ViewPager's content on the screen immediately
    		mPagerAdapter.notifyDataSetChanged();
        	
        	// Save the change in the Preferences
        	//Editor editor = prefs.edit();
        	//editor.putString("LANGUAGE", prefsLanguage);
        	//editor.commit();
            
        	return true;
        case R.id.action_search: 
        	
        	return true;	
        default:
            return super.onOptionsItemSelected(it);	        	
        }
	}   

}

