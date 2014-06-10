package com.example.cebible;

import java.util.ArrayList;
import java.util.List;

import com.example.cebible.PageFragment;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
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
	
	DataBaseHelper BibleDB = new DataBaseHelper(this);
	int num_chapters;
	private ViewPager mPager;
	private PagerAdapter mPagerAdapter;
	private int mBook;
	private int mChapter;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_pager);
        
        Intent intent = getIntent();
        int book = intent.getIntExtra("BOOK", 1);
        int chapter = intent.getIntExtra("CHAPTER", 1);
        int verse = intent.getIntExtra("VERSE", 1);
        
        mBook = book;
        mChapter = chapter;
        
        //Get number of chapters of a book
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
			}
		});
	}
	
	private String getBookName() {
		// Get book name from XML resource
		String[] ot = getResources().getStringArray(R.array.old_testament);
		String[] nt = getResources().getStringArray(R.array.new_testament);
		Log.d(TAG, " ************** mBook=" + String.valueOf(mBook) + " mChapter="+String.valueOf(mChapter));
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
        	Log.d(TAG, "PageFragment.create, position="+String.valueOf(position));
            return PageFragment.create(position, BibleDB, mBook);
        }

        @Override
        public int getCount() {
        	return num_chapters;
        }
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.display_activity, menu);
		return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem it) {
        // Handle presses on the action bar items
		Log.d(TAG, "Action bar item clicked");
		ActionBar ab = getActionBar(); 
        switch (it.getItemId()) 
        {		
        case R.id.action_change:
        	Log.d(TAG, "Action bar change button clicked"); 
        	ab.setTitle("KJV");
        	return true;
        case R.id.action_search:
        	Log.d(TAG, "Action bar search button clicked");  
        	ab.setTitle("CUVS");
        	return true;	
        default:
            return super.onOptionsItemSelected(it);	        	
        }
	}   

}

