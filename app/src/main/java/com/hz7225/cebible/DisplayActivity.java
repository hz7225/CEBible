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
import androidx.fragment.app.FragmentActivity;
import androidx.core.app.NavUtils;
import androidx.legacy.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.SearchView;

public class DisplayActivity extends FragmentActivity {
	String TAG = "DisplayActivity";
	
	String parentActivity;
		
	int num_chapters = 1189;  //total number of chapters in the Holy Bible
	
	private ViewPager mPager;
	public PagerAdapter mPagerAdapter;
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
    	if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
    		getActionBar().setHomeAsUpIndicator(R.drawable.ic_home_up_padded);
    	}
    	getActionBar().setIcon(R.drawable.ic_actionbar_icon);
        
        // Get saved preferences
    	prefs  = getSharedPreferences("BiblePreferences", MODE_PRIVATE);		    
    	prefsLanguage = prefs.getString("LANGUAGE", getString(R.string.ch));
    	prefsEN_Trans = prefs.getString("EN_TRANS", getString(R.string.web));
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
		setActionBarTitle15sp(mChapterNameAndNumber);	
		
		// Update action bar after scrolling to a new chapter
		mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				if (!isAutoAdvancingPage) {
					stopActiveReading();
				}
				if (mPagerAdapter != null && mPagerAdapter.currentFragment != null) {
					mPagerAdapter.currentFragment.dismissCABAndHighlight();
				}
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
		if (isFinishing()) {
			stopActiveReading();
		}
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
	
	private void setActionBarTitle15sp(String titleText) {
		ActionBar actionBar = getActionBar();
		if (actionBar != null && titleText != null) {
			SpannableString ss = new SpannableString(titleText);
			ss.setSpan(new AbsoluteSizeSpan(15, true), 0, ss.length(), 0);
			actionBar.setTitle(ss);
		}
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
	
	public class PagerAdapter extends FragmentStatePagerAdapter {
		public DisplayPageFragment currentFragment;
		
        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            if (object instanceof DisplayPageFragment) {
                currentFragment = (DisplayPageFragment) object;
            }
            super.setPrimaryItem(container, position, object);
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
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		updateActionBarMenuItemsVisibility(menu);
		return true;
	}

	public void updateActionBarMenuItemsVisibility(Menu menu) {
		if (menu == null) return;

		MenuItem readItem = menu.findItem(R.id.action_read_aloud);
		MenuItem searchItem = menu.findItem(R.id.action_search);

		boolean isSideBySide = prefsLanguage != null && prefsLanguage.equals(getString(R.string.ch_en));

		if (isSideBySide) {
			if (readItem != null) {
				readItem.setVisible(false);
				if (readItem.getActionView() != null) {
					readItem.getActionView().setVisibility(View.GONE);
				}
			}
			if (searchItem != null) {
				searchItem.setVisible(false);
				if (searchItem.getActionView() != null) {
					searchItem.getActionView().setVisibility(View.GONE);
				}
			}
		} else {
			if (readItem != null) {
				readItem.setVisible(true);
				if (readItem.getActionView() != null) {
					readItem.getActionView().setVisibility(View.VISIBLE);
				}
			}
			if (searchItem != null) {
				searchItem.setVisible(true);
				if (searchItem.getActionView() != null) {
					searchItem.getActionView().setVisibility(View.VISIBLE);
				}
			}
		}
	}

	private void updateLanguageButtonTitle(MenuItem changeItem) {
		if (changeItem == null) return;

		String titleStr;
		if (prefsLanguage.equals(getString(R.string.en))) {
			titleStr = prefsEN_Trans;
		} else if (prefsLanguage.equals(getString(R.string.ch_en))) {
			titleStr = prefsCH_Trans + "/" + prefsEN_Trans;
		} else {
			titleStr = prefsCH_Trans;
		}

		changeItem.setTitle(titleStr);
		if (changeItem.getActionView() instanceof TextView) {
			((TextView) changeItem.getActionView()).setText(titleStr);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.display_activity, menu);
				
		item = menu.findItem(R.id.action_change);  //change item on the action bar
		updateLanguageButtonTitle(item);
		if (item != null && item.getActionView() != null) {
			item.getActionView().setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onOptionsItemSelected(item);
				}
			});
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
		//Add hint to the Display widget window
		//Log.d(TAG, "SearchView prefsLanguage = " + prefsLanguage);
		if (prefsLanguage.equals(getString(R.string.en))) {
			searchView.setQueryHint("String1 & String2 ...");
		} else {
			searchView.setQueryHint("字串1 & 字串2 ...");
		}

		//Configure Read Aloud actionView click listener
		MenuItem readItem = menu.findItem(R.id.action_read_aloud);
		if (readItem != null && readItem.getActionView() != null) {
			View readView = readItem.getActionView();
			readView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mPagerAdapter != null && mPagerAdapter.currentFragment != null) {
						mPagerAdapter.currentFragment.toggleContinuousReading();
					}
				}
			});
		}

		if (searchView != null) {
			searchView.setOnSearchClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					stopActiveReading();
					if (ab_menu != null) {
						MenuItem readItem = ab_menu.findItem(R.id.action_read_aloud);
						if (readItem != null) {
							readItem.setVisible(false);
							if (readItem.getActionView() != null) {
								readItem.getActionView().setVisibility(View.GONE);
							}
						}
					}
				}
			});
			searchView.setOnCloseListener(new SearchView.OnCloseListener() {
				@Override
				public boolean onClose() {
					updateActionBarMenuItemsVisibility(ab_menu);
					return false;
				}
			});
		}

		MenuItem searchMenuItem = menu.findItem(R.id.action_search);
		if (searchMenuItem != null) {
			searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
				@Override
				public boolean onMenuItemActionExpand(MenuItem item) {
					stopActiveReading();
					if (ab_menu != null) {
						MenuItem readItem = ab_menu.findItem(R.id.action_read_aloud);
						if (readItem != null) {
							readItem.setVisible(false);
							if (readItem.getActionView() != null) {
								readItem.getActionView().setVisibility(View.GONE);
							}
						}
					}
					return true;
				}

				@Override
				public boolean onMenuItemActionCollapse(MenuItem item) {
					updateActionBarMenuItemsVisibility(ab_menu);
					return true;
				}
			});
		}

		updateActionBarMenuItemsVisibility(menu);
		return true;
	}

	public void updateReadAloudIcon(boolean isReading) {
		if (ab_menu != null) {
			MenuItem item = ab_menu.findItem(R.id.action_read_aloud);
			if (item != null) {
				if (item.getActionView() instanceof ImageButton) {
					((ImageButton) item.getActionView()).setImageResource(
						isReading ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play
					);
				} else {
					item.setIcon(isReading ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
				}
			}
		}
	}
	
	public boolean isAutoAdvancingPage = false;

	public boolean advanceToNextChapter() {
		if (mPager != null && mPagerAdapter != null) {
			int nextPos = mPager.getCurrentItem() + 1;
			if (nextPos < mPagerAdapter.getCount()) {
				isAutoAdvancingPage = true;
				mPager.setCurrentItem(nextPos, true);
				mPager.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (mPagerAdapter != null && mPagerAdapter.currentFragment != null) {
							mPagerAdapter.currentFragment.startContinuousReading();
						}
						isAutoAdvancingPage = false;
					}
				}, 600);
				return true;
			}
		}
		return false;
	}

	public void stopActiveReading() {
		if (mPagerAdapter != null && mPagerAdapter.currentFragment != null) {
			mPagerAdapter.currentFragment.stopContinuousReading();
		}
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem it) {
        int id = it.getItemId();
        MenuItem item = it;
        SearchView searchView = (ab_menu != null && ab_menu.findItem(R.id.action_search) != null) ? (SearchView) ab_menu.findItem(R.id.action_search).getActionView() : null;
        if (id == android.R.id.home) {
            stopActiveReading();
            finish();
            return true;
        } else if (id == R.id.action_change) {
            stopActiveReading();
        	if (prefsLanguage.equals(getString(R.string.ch))) {
        		prefsLanguage = getString(R.string.en);
        		mySetLocale();
				if (searchView != null) searchView.setQueryHint("String1 & String2 ...");
        	} else if (prefsLanguage.equals(getString(R.string.en))) {
        		prefsLanguage = getString(R.string.ch_en);
        		mySetLocale();
        	} else if (prefsLanguage.equals(getString(R.string.ch_en))) {	
        		prefsLanguage = getString(R.string.ch);
        		mySetLocale();
				if (searchView != null) searchView.setQueryHint("字串1 & 字串2 ...");
        	}

        	updateLanguageButtonTitle(item);
        	invalidateOptionsMenu();

        	Editor editor = prefs.edit();
        	editor.putString("LANGUAGE", prefsLanguage);
        	editor.commit();
        	
    		setActionBarTitle15sp(getBookName() +" "+String.valueOf(mChapter));
    		
    		invalidateOptionsMenu();
    		mPagerAdapter.notifyDataSetChanged();  
        	return true;
        } else if (id == R.id.action_read_aloud) {
        	if (mPagerAdapter != null && mPagerAdapter.currentFragment != null) {
        		mPagerAdapter.currentFragment.toggleContinuousReading();
        	}
        	return true;
        } else if (id == R.id.action_search) {
        	return true;
        }
        return super.onOptionsItemSelected(it);	        	
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
			// We need to get the instance of the LayoutInflater
			LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View layout = inflater.inflate(R.layout.tip_popup, (ViewGroup)findViewById(R.id.popup));
			pwindo = new PopupWindow(layout, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true); //true = PopupWindow has focus
			pwindo.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

			View rootOverlay = layout.findViewById(R.id.tip_popup_root);
			if (rootOverlay != null) {
				rootOverlay.setOnTouchListener(new View.OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						return true; // Consumes touch completely so outside taps NEVER dismiss Usage Tip!
					}
				});
			}

			pwindo.showAtLocation(getWindow().getDecorView(), Gravity.CENTER, 0, 0);

			//Button OK
			btnClosePopup = (Button) layout.findViewById(R.id.btn_close_popup);
			OnClickListener cancel_button_click_listener = new OnClickListener() {
				public void onClick(View v) {
					pwindo.dismiss();
				}
			};
			btnClosePopup.setOnClickListener(cancel_button_click_listener);

			//Show the new new usage popup if the platform release is after M
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
				if (prefsLanguage.equals(getString(R.string.en))) {
					ImageView iv = (ImageView) layout.findViewById(R.id.imageView);
					iv.setImageResource(R.drawable.cab_usage_en);
				} else {
					ImageView iv = (ImageView) layout.findViewById(R.id.imageView);
					iv.setImageResource(R.drawable.cab_usage_ch);
				}
			}

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

