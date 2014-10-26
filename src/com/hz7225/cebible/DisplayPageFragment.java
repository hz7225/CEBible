package com.hz7225.cebible;

import java.util.List;
import java.util.Locale;

import android.app.Fragment;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Selection;
import android.text.Spannable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ActionMode.Callback;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

public class DisplayPageFragment extends Fragment {
	
	static String TAG = "PageFragment";
	
	/**
     * The argument key for the page number this fragment represents.
     */
    public static final String ARG_PAGE = "page";
    public static final String ARG_VERSION = "version";
	
	private int mPageNumber;
	static private int mBook;
	static private int mChapter;
	static private int mVerse;
	static private String mLanguage;
	static private String mEn_trans;
	static private String mCh_trans;
	private String english_db;
	private String chinese_db;
		
	ListView listView1;
	ListView listView2;
	ListDataAdapter2 adapter1;
	ListDataAdapter2 adapter2;
	
	String title; //BookName + Chapter # + Verse #
	
	boolean isLeftListEnabled = true;
	boolean isRightListEnabled = true;
	
	TextView mTextView;
	
	TextToSpeech ttsobj;
	int selected_listview = 0; //used by TTS to play the text in correct language
	
	static int in_lv1_or_lv2 = 0; //used in search the correct language database, 1=Chinese, 2 = English
	ActionMode amode;
	
	public static DisplayPageFragment create(int pageNumber, int book, int verse, String lang, String en_trans, String ch_trans) {
        mBook = book;
        mVerse = verse;
        mLanguage = lang;
        mEn_trans = en_trans;
        mCh_trans = ch_trans;
                     
		DisplayPageFragment fragment = new DisplayPageFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, pageNumber);  
        fragment.setArguments(args);
        return fragment;
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	public void onDestroyView() {
		super.onDestroyView();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mPageNumber = getArguments().getInt(ARG_PAGE);

		//Log.d(TAG, "mPageNumber = " + String.valueOf(mPageNumber));
		
		// initialize mBook and mChapter from ViewPager position number
		ChapterPosition cp = new ChapterPosition(getActivity(), mPageNumber);
		mBook = cp.getBook();
		mChapter = cp.getChapter();
		
		//Log.d(TAG, "mBook="+String.valueOf(mBook) + " mChapter"+String.valueOf(mChapter));
		
		if (mCh_trans.equals(getString(R.string.cuvs))) {
        	chinese_db = "CB_cuvslite.bbl.db";
        } else if (mCh_trans.equals(getString(R.string.cuvt))) {
        	chinese_db = "CB_cuvtlite.bbl.db";
        }
		
		if (mEn_trans.equals(getString(R.string.kjv))) {
        	english_db = "EB_kjv_bbl.db";
        } else if (mEn_trans.equals(getString(R.string.web))) {
        	english_db = "EB_web_bbl.db";
        }
		
		//Log.d(TAG, "onCreateView(): mPageNumber = " + String.valueOf(mPageNumber) + " mChapter = " + String.valueOf(mChapter));
		//Log.d(TAG, "onCreateView(): mPageNumber = " + String.valueOf(mPageNumber) + " mVersion = " + mVersion);		
        //Use 2 ListViews in Linear layout for displaying Chinese and English Bibles respectively
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.listviews, container, false);
        
        //ListView1 for Chinese translation
        listView1 = (ListView) rootView.findViewById(R.id.listView1);
        List<String> sl1 = getScriptureFromDB(mBook, mChapter, chinese_db);
    	adapter1 = new ListDataAdapter2(this.getActivity(), sl1);     	
	    listView1.setAdapter(adapter1);  //Set ListView adapters	    
    	listView1.setSelection(mVerse-1);  //Set starting position
    	
    	//ListView2 for English translation
    	listView2 = (ListView) rootView.findViewById(R.id.listView2);    	
    	List<String> sl2 = getScriptureFromDB(mBook, mChapter, english_db);
    	adapter2 = new ListDataAdapter2(this.getActivity(), sl2);    	
    	listView2.setAdapter(adapter2);  //Set ListView adapters    	
    	listView2.setSelection(mVerse-1);  //Set starting position

    	
    	//Set listener
    	listView1.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> av, View v, int position, long id) {
				//Log.d(TAG, "LV1: onItemClick");
				//delete action mode
				//amode.finish();
			}
		});
    	listView2.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> av, View v, int position, long id) {
				//Log.d(TAG, "LV1: onItemClick");
				//delete action mode
				//amode.finish();
			}
		});
		
    	//Set long click listener
		listView1.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> av, View v, int position, long id) {	
				//Log.d(TAG, "LV1: onItemLongClick selected_listview = 1");
				if (in_lv1_or_lv2 != 0) {
				TextView tv = (TextView)v.findViewById(R.id.scripture);
				title = getBookName() + String.valueOf(DisplayActivity.mChapter) + ":" + String.valueOf(position+1);
				mTextView = tv;
				mTextView.setTextIsSelectable(true);
				selected_listview = 1;
				if (in_lv1_or_lv2 != 0) setCustomSelectionCAB(true);
				in_lv1_or_lv2 = 1; 
				}
		    	return false; //pass event to onItemClickListener
		    	//return true;  //consume event
			}
		});
		listView2.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> av, View v, int position, long id) {
				//Log.d(TAG, "LV1: onItemLongClick selected_listview = 2");
				if (in_lv1_or_lv2 != 0) {
				TextView tv = (TextView)v.findViewById(R.id.scripture);
				title = getBookName() + String.valueOf(DisplayActivity.mChapter) + ":" + String.valueOf(position+1);
				mTextView = tv;
				mTextView.setTextIsSelectable(true);
				selected_listview = 2;
				setCustomSelectionCAB(true);
				in_lv1_or_lv2 = 2;
				}
		    	return false; //pass event to onItemClickListener
		    	//return true;  //consume event
			}
		});
		
    	
		//Display only one ListView, make the other one invisible
		//when mLanguage is either Chinese or English. Otherwise,
		//both ListViews will be displayed side-by-side
		if (mLanguage.equals(getString(R.string.en))) {
			listView1.setVisibility(View.GONE); //To display only one ListView
			rootView.findViewById(R.id.horizontal_white_space).setVisibility(View.GONE); //Remove the horizontal spacer too
			in_lv1_or_lv2 = 2;
		}
		else if (mLanguage.equals(getString(R.string.ch))) {
			listView2.setVisibility(View.GONE); //To display only one ListView
			rootView.findViewById(R.id.horizontal_white_space).setVisibility(View.GONE); //Remove the horizontal spacer too
			in_lv1_or_lv2 = 1;
		} else {
			in_lv1_or_lv2 = 0; //to indicate the side-by-side view
		}
		
		//To synchronize the positions of the two ListViews
		//This method has some warning messages in logcat but scrolls smoothly
		listView1.setOverScrollMode(ListView.OVER_SCROLL_NEVER);
		listView2.setOverScrollMode(ListView.OVER_SCROLL_NEVER);
		listView1.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// onScroll will be called and there will be an infinite loop.
				// That's why i set a boolean value
				if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
					isRightListEnabled = false;
				} else if (scrollState == SCROLL_STATE_IDLE) {
					isRightListEnabled = true;
				}
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,                
					int totalItemCount) {
				View c = view.getChildAt(0);
				if (c != null && isLeftListEnabled) {
					listView2.setSelectionFromTop(firstVisibleItem, c.getTop());
				}
			}
		});    
		listView2.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// onScroll will be called and there will be an infinite loop.
				// That's why i set a boolean value
				if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
					isLeftListEnabled = false;
				} else if (scrollState == SCROLL_STATE_IDLE) {
					isLeftListEnabled = true;
				}
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,                
					int totalItemCount) {
				View c = view.getChildAt(0);
				if (c != null && isRightListEnabled) {
					listView1.setSelectionFromTop(firstVisibleItem, c.getTop());
				}
			}
		}); 
		        
		return rootView;				
	}

	public int getPageNumber() {
        return mPageNumber;
    }
	
	private List<String> getScriptureFromDB(int book, int chapter, String db) {
		//Log.d(TAG, "getScriptureFromDB: " + db + " mChapter = " + String.valueOf(chapter));
		DataBaseHelper BibleDB = new DataBaseHelper(getActivity().getApplicationContext(), db);

        //Get the whole chapter of a book from database
        List<String> sl = BibleDB.getChapter(book, chapter);
        for (int i=0; i<sl.size(); i++) {
        	sl.set(i, sl.get(i));
        }
        return sl;
	}
	
	private String getBookName() {
		// Get book name from XML resource
		
		String[] ot = getResources().getStringArray(R.array.old_testament);
		String[] nt = getResources().getStringArray(R.array.new_testament);

		String book;
		if (mBook<40) {
			book = ot[mBook-1].substring(0, ot[mBook-1].indexOf(","));
		} else {
			book = nt[mBook-1-39].substring(0, nt[mBook-1-39].indexOf(","));
		}
		
		return book;
	}
	
	private void setCustomSelectionCAB(boolean longClick) {
		//Select all text
		//Selection.selectAll((Spannable) mTextView.getText());	
    	
		mTextView.setCustomSelectionActionModeCallback(new Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				// TODO Auto-generated method stub
				//Log.d(TAG, "setCustomSelectionCAB: onCreateActionMode");				
				mode.getMenuInflater().inflate(R.menu.contextual_menu, menu);
				mode.setTitle("");
				amode = mode;
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				// TODO Auto-generated method stub
				//Log.d(TAG, "setCustomSelectionCAB: onPrepareActionMode");
				
				// Remove the "select all" option
				//menu.removeItem(android.R.id.selectAll);
				// Just make the select all option invisiable
				//mode.getMenu().getItem(0).setVisible(false);
				
				//Don't show the TTS Play icon for API level older than 17 (Jelly Bean MR1)
				if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
					menu.getItem(2).setVisible(false);
				}
				return true;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode,
					MenuItem item) {
				//Log.d(TAG, "setCustomSelectionCAB: onActionItemClicked");
				switch (item.getItemId()) {
				case R.id.item_play:
					//Log.d(TAG, "Play selected text in " + mVersion);
					if (selected_listview == 1) {
						DisplayActivity.ttsobj.setLanguage(Locale.CHINESE);						
						DisplayActivity.ttsobj.speak(selectedText(), TextToSpeech.QUEUE_FLUSH, null);
					} 					
					else if (selected_listview == 2) {
						if (mEn_trans.equals(getString(R.string.kjv))) {
							DisplayActivity.ttsobj.setLanguage(Locale.UK);
						}
						else {
							DisplayActivity.ttsobj.setLanguage(Locale.US);
						}
						DisplayActivity.ttsobj.speak(selectedText(), TextToSpeech.QUEUE_FLUSH, null);
					}
						
					mode.getMenu().getItem(0).setVisible(false); //Select All
					mode.getMenu().getItem(1).setVisible(false); //Copy
					mode.getMenu().getItem(3).setVisible(true);  //Pause
					mode.getMenu().getItem(4).setVisible(false);  //Share
					mode.getMenu().getItem(5).setVisible(false);  //Note
					mode.getMenu().getItem(6).setVisible(false);  //Search
					return true;
				case R.id.item_pause:
					DisplayActivity.ttsobj.stop();
					return true;
				case R.id.item_share:
					//Log.d(TAG, "mode size = " + String.valueOf(mode.getMenu().size()));
					//Select the text of the whole verse to share
					Selection.selectAll((Spannable) mTextView.getText());
					
					Intent i=new Intent(android.content.Intent.ACTION_SEND);
					i.setType("text/plain");
					i.putExtra(android.content.Intent.EXTRA_SUBJECT, title);
					i.putExtra(android.content.Intent.EXTRA_TEXT, selectedText() + " (" + title + ")");
					startActivity(Intent.createChooser(i,"Share via"));
					mode.finish();  //Action is executed, close the CAB
					return true;
				case R.id.item_search:
					//do Search with the selected text and 
					//launch SearchResultsActivity to display results
					Intent intent = new Intent(getActivity(), SearchActivity.class);
					intent.putExtra("SEARCH_STR", selectedText());
		    		startActivity(intent);
					
					return true;
				default:	
					return false;
				}
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				DisplayActivity.ttsobj.stop();
				//Log.d(TAG, "setCustomSelectionCAB: onDestroyActionMode" + mTextView.getText());
				//mTextView.setTextIsSelectable(false); //Investigate, app crashes
			}
			
			private String selectedText() {
				int min = 0;
				int max = mTextView.getText().length();
				if (mTextView.isFocused()) {
					final int selStart = mTextView.getSelectionStart();
					final int selEnd = mTextView.getSelectionEnd();

					min = Math.max(0, Math.min(selStart, selEnd));
					max = Math.max(0, Math.max(selStart, selEnd));
				}
				// Perform your definition lookup with the selected text
				final CharSequence text = mTextView.getText().subSequence(min, max);
				//Log.d(TAG, "selectedText: " + text);
				return text.toString();
			}
		});
	}	
}
