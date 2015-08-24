package com.hz7225.cebible;

import java.util.Locale;

import com.hz7225.cebible.DisplayPageFragment;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.PopupWindow;
import android.widget.SearchView;

public class DisplayActivity extends FragmentActivity {
	String TAG = "DisplayActivity";
	
	String parentActivity;
		
	int num_chapters = 1189;  //total number of chapters in the Holy Bible
	
	private ViewPager mPager;
	private PagerAdapter mPagerAdapter;
	private int mBook;
	public int mChapter;
	private int mVerse;
	public String mChapterNameAndNumber;
	
	SharedPreferences prefs;
    String prefsLanguage;
    String prefsEN_Trans;
    String prefsCH_Trans;
    Boolean showUsageTip;
    
    private int page_fragment_scrolled; //show usage tip popup only after scrolling several pages
    private Boolean usage_tip_showed = false; //flag to make sure it will be showed only once
    
    public static boolean eng_ch_compare = false;
    
    DataBaseHelper BibleDB;
    //Action bar menu
  	Menu ab_menu;
          
  	Locale myLocale;
	
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
        
        // To allow Up navigation with the app icon in the action bar
    	getActionBar().setDisplayHomeAsUpEnabled(true);
        
        // Get saved preferences
    	prefs  = getSharedPreferences("BiblePreferences", MODE_PRIVATE);		    
    	prefsLanguage = prefs.getString("LANGUAGE", getString(R.string.ch));
    	prefsEN_Trans = prefs.getString("EN_TRANS", getString(R.string.kjv));
    	prefsCH_Trans = prefs.getString("CH_TRANS", getString(R.string.cuvs));
    	showUsageTip = prefs.getBoolean("SHOW_USAGE_TIP", true);
    	
    	//Log.d(TAG, "Preferences: Lang=" + prefsLanguage + " EN_trans=" + prefsEN_Trans + " CH_Trans=" + prefsCH_Trans);
        
        Intent intent = getIntent();
        mBook = intent.getIntExtra("BOOK", 1);
        mChapter = intent.getIntExtra("CHAPTER", 1);
        mVerse = intent.getIntExtra("VERSE", 1);
        parentActivity = intent.getStringExtra("PARENT");
        
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
				//Log.d(TAG, "PageFragment position = " + String.valueOf(position));
				
				page_fragment_scrolled++; //counter used for deciding when to the usage tip PopupWindow
				
				mVerse = 1; // Reset mVerse after turning to new page
				ChapterPosition cp = new ChapterPosition(DisplayActivity.this, position);
				int previous_book = mBook; 
				mBook = cp.getBook();
				if (mBook != previous_book) {
					//Log.d(TAG, "Book changed");
					Editor editor = prefs.edit();
					//editor.putInt("BOOK", mBook);
					editor.putInt("CHAPTER_"+String.valueOf(previous_book-1), mChapter);
					//editor.putInt("VERSE", mVerse);
					editor.commit();
				}
				mChapter = cp.getChapter();
				mChapterNameAndNumber = getBookName() +" "+String.valueOf(mChapter);
				ActionBar actionBar = getActionBar();
				actionBar.setTitle(mChapterNameAndNumber);
				
				//show Usage Tip PopupWindow after 5th scrolling of PageFragments and only show it once
				//Log.d(TAG, "showUsageTip="+showUsageTip.toString() + " usage_tip_showed=" + usage_tip_showed.toString() + " " + String.valueOf(page_fragment_scrolled));
				if ((showUsageTip == true) && (usage_tip_showed == false) && (page_fragment_scrolled == 5)) {
					startPopupWindow();
					usage_tip_showed = true;
				}
			}
		});
	}		
	
	public void onPause() {
		super.onPause();
		//Log.d(TAG, "onPause()");	
		
		Editor editor = prefs.edit();
		
    	//Save preferences only if the activity is started from the main activity
		if ((parentActivity!=null) &&parentActivity.equals("MainActivity")) {
			//Log.d(TAG, "onPause()" + " ParentActivity=" + parentActivity + ", mChapter=" + String.valueOf(mChapter) + " mVerse=" + String.valueOf(mVerse));
			editor.putInt("BOOK", mBook);
			editor.putInt("CHAPTER_"+String.valueOf(mBook-1), mChapter);
			editor.putInt("VERSE", mVerse);
			editor.putBoolean("SHOW_USAGE_TIP", showUsageTip);
		}	
		
		//Restore Language preference back to Chinese if it's currently set to side by side Chinese-English
    	if (prefsLanguage.equals(getString(R.string.ch_en))) {
    		prefsLanguage = getString(R.string.ch);    		
    		editor.putString("LANGUAGE", prefsLanguage);       	
    	}
    	
    	editor.commit();
	}	
	
	private String getBookName() {
		// Get book name from XML resource
		String[] ot;
		String[] nt;

		ot = getResources().getStringArray(R.array.old_testament);
		nt = getResources().getStringArray(R.array.new_testament);
		//Log.d(TAG, " ************** mBook=" + String.valueOf(mBook) + " mChapter="+String.valueOf(mChapter));
		String book;
		if (mBook<=39) {
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
        	ChapterPosition cp = new ChapterPosition(DisplayActivity.this, position); 
        	//Log.d(TAG, "PageFragment.create, position="+String.valueOf(position) + " mBook=" + String.valueOf(mBook) +
        	//		   " mChapter=" + String.valueOf(mChapter) + " chapter=" + String.valueOf(chapter));

        	return DisplayPageFragment.create(position, mBook, mChapter, mVerse, prefsLanguage, prefsEN_Trans, prefsCH_Trans);
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
			if (prefsCH_Trans.equals(getString(R.string.cuvs))) {
    			item.setTitle(getString(R.string.cuvs));
    		} else if (prefsCH_Trans.equals(getString(R.string.cuvt))) {
    			item.setTitle(getString(R.string.cuvt));
    		}
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
        		prefsLanguage = getString(R.string.en);
        		mySetLocale();
        		item.setTitle(prefsEN_Trans);
        		//Enable the Search menu        		
        		ab_menu.getItem(1).setVisible(true);
        	} else if (prefsLanguage.equals(getString(R.string.en))) {
        		//Change to display Chinese-English side by side
        		prefsLanguage = getString(R.string.ch_en);
        		mySetLocale();
        		item.setTitle(getString(R.string.cuv) + "/" + prefsEN_Trans);
        		//Disable the Search menu        		
        		ab_menu.getItem(1).setVisible(false);
        	} else if (prefsLanguage.equals(getString(R.string.ch_en))) {	
        		//Change to display Chinese only
        		prefsLanguage = getString(R.string.ch);
        		mySetLocale();
        		//item.setTitle(getString(R.string.cuv));
        		if (prefsCH_Trans.equals(getString(R.string.cuvs))) {
        			item.setTitle(getString(R.string.cuvs));
        		} else if (prefsCH_Trans.equals(getString(R.string.cuvt))) {
        			item.setTitle(getString(R.string.cuvt));
        		}
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
	
	//PopupWindow for displaying usage tip
	private PopupWindow pwindo;
	Button btnClosePopup;
	CheckBox ckBox;
	private void startPopupWindow() {
		//Log.d(TAG, "startPopupWindow()");
		try {
			//Get screen density because PopupWindow uses pixel in width and height parameters
			DisplayMetrics dm = getResources().getDisplayMetrics();
			int densityDpi = dm.densityDpi;
			//Log.d(TAG, "screen density = " + String.valueOf(densityDpi));

			// We need to get the instance of the LayoutInflater
			LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View layout = inflater.inflate(R.layout.tip_popup, (ViewGroup)findViewById(R.id.popup));
			pwindo = new PopupWindow(layout, (int)(1.75*densityDpi), (int)(2.15*densityDpi), true); //true = PopupWindow has focus
			pwindo.showAtLocation(layout, Gravity.CENTER, 0, 0);

			//Button OK
			btnClosePopup = (Button) layout.findViewById(R.id.btn_close_popup);
			OnClickListener cancel_button_click_listener = new OnClickListener() {
				public void onClick(View v) {
					pwindo.dismiss();
				}
			};
			btnClosePopup.setOnClickListener(cancel_button_click_listener);

			//"Don't Show Again" CheckBox listener
			ckBox = (CheckBox) layout.findViewById(R.id.checkBox);
			ckBox.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					if (((CheckBox) v).isChecked()) {
						//Log.d(TAG, "Don't Show Again checked");
						showUsageTip = false;
						//usage_tip_showed = false; //reset this variable
					}	else {
						//Log.d(TAG, "unchecked");
						showUsageTip = true;
					}
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

