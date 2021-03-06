package com.hz7225.cebible;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
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
import android.widget.TextView;
import android.widget.Toast;

public class DisplayPageFragment extends Fragment {
	
	static String TAG = "PageFragment";
	
	/**
     * The argument key for the page number this fragment represents.
     */
    public static final String ARG_PAGE = "page";
    public static final String ARG_VERSION = "version";
	
	private int mPageNumber; //This is the page position number of ScrollView
	static private int mBook; //book number passed in from the DisplayActivity via user selection,from 1 to 66
	static private int mChapter; //chapter number passed in from the DisplayActivity via user selection, start from 1
	static private int mVerse;  //verse number passed in from the DisplayActivity via user selection, start from 1, used for setting the starting position of the ListView
	static private int longclickedVerse; //highlighted verse number via long click, used for creating a database record for favorite scriptures
	static private String mLanguage;
	static private String mEn_trans;
	static private String mCh_trans;
	private String english_db;
	private String chinese_db;
		
	ListView listView1;
	ListView listView2;
	List<SpannableString> spl1;
	List<SpannableString> spl2;
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
	
	NotebookDbHelper mDbHelper;
	
	//Msg types
	private final int EVENT_HIGHLIGHT = 1;
	
	public static DisplayPageFragment create(int pageNumber, int book, int chapter, int verse, String lang, String en_trans, String ch_trans) {
        mBook = book;
        mChapter = chapter;
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
	
	int _book;
	int _chapter;
	public void onResume() {
		super.onResume();
				
		mPageNumber = getArguments().getInt(ARG_PAGE);
		ChapterPosition cp = new ChapterPosition(getActivity(), mPageNumber);
		_book = cp.getBook();
		_chapter = cp.getChapter();
		
		//Log.d(TAG, "DisplayPageFragment::onResume(), book=" + String.valueOf(_book) + " chapter=" + String.valueOf(_chapter));
		
		//After the scriptures are displayed, send an event to the Activity itself 
		//to start highlighting favored verses
		Thread thBackground = new Thread(run);
    	thBackground.start();
	}
	
	Runnable run = new Runnable() {    	
		public synchronized void run() {
			//Log.d(TAG, "Runable() executed");

			//Set an event to search for favored verses and highlight them
			Message msg = handler.obtainMessage(EVENT_HIGHLIGHT);
			handler.sendMessage(msg);
		}
	};
    	
	private final Handler handler = new Handler() {
		public void handleMessage(Message m) {
			super.handleMessage(m);

			switch(m.what) {
			case EVENT_HIGHLIGHT:
				//Search notebook database and highlight the favored verses
				if (mDbHelper == null) mDbHelper = new NotebookDbHelper(getActivity().getApplicationContext());
				for (int i=0; i<spl1.size(); i++) {
					SpannableString ss = spl1.get(i);
					boolean test = mDbHelper.checkIfRecordExists(_book, _chapter, i+1);
					if (test == true) {
		        		ss.setSpan(new BackgroundColorSpan(0x60ffff00), 0, ss.length(), 0);
		        	}
					spl1.set(i, ss);
				}
				adapter1.notifyDataSetChanged();
				for (int i=0; i<spl2.size(); i++) {
					SpannableString ss = spl2.get(i);
					boolean test = mDbHelper.checkIfRecordExists(_book, _chapter, i+1);
					if (test == true) {
		        		ss.setSpan(new BackgroundColorSpan(0x60ffff00), 0, ss.length(), 0);
		        	}
					spl2.set(i, ss);
				}
				adapter2.notifyDataSetChanged();

				break;
			}
		}
	};	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mPageNumber = getArguments().getInt(ARG_PAGE);

		//Log.d(TAG, "mPageNumber = " + String.valueOf(mPageNumber));
		
		// initialize mBook and mChapter from ViewPager position number
		ChapterPosition cp = new ChapterPosition(getActivity(), mPageNumber);
		int book = cp.getBook();
		int chapter = cp.getChapter();
		int verse = mVerse;
		
		//3 PageViews will be created, 1 to the left and 1 to the right of the selected chapter.
		//Set verse to 1 to the two budding ViewPages so that they start at verse 1 when turning to.
		if (chapter != mChapter) verse = 1;
		
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
		
		//Log.d(TAG, "onCreateView(): mPageNumber = " + String.valueOf(mPageNumber) + " mChapter = " + String.valueOf(mChapter) + " mVerse = " + String.valueOf(mVerse));
        
		//Use 2 ListViews in Linear layout for displaying Chinese and English Bibles respectively
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.listviews, container, false);
        
        //ListView1 for Chinese translation
        listView1 = (ListView) rootView.findViewById(R.id.listView1); 
        spl1 = getSpannableScriptureFromDB(book, chapter, chinese_db);
    	adapter1 = new ListDataAdapter2(this.getActivity(), spl1);     	
	    listView1.setAdapter(adapter1);  //Set ListView adapters	    
    	listView1.setSelection(verse-1);  //Set starting position
    	/* Test to make the verse number clickable. Doesn't work because tvn is null
    	TextView tvn = (TextView)listView1.findViewById(R.id.scripture_number);
    	tvn.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				// TODO Auto-generated method stub
				return false;
			}    		
    	});
    	*/
    	
    	//ListView2 for English translation
    	listView2 = (ListView) rootView.findViewById(R.id.listView2);    	
    	spl2 = getSpannableScriptureFromDB(book, chapter, english_db);
    	adapter2 = new ListDataAdapter2(this.getActivity(), spl2);    	
    	listView2.setAdapter(adapter2);  //Set ListView adapters    	
    	listView2.setSelection(verse-1);  //Set starting position
    	
    	//Set listener
    	listView1.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> av, View v, int position, long id) {
				//delete action mode
				//amode.finish();
			}
		});
    	listView2.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> av, View v, int position, long id) {
				//delete action mode
				//amode.finish();
			}
		});
    	
    	//Set long click listener
		listView1.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> av, View v, int position, long id) {	
				longclickedVerse = position + 1;
				//Log.d(TAG, "setOnItemLongClickListener,listView1, position=" + String.valueOf(position));
				TextView tv = (TextView)v.findViewById(R.id.scripture);
				mTextView = tv;
				if (in_lv1_or_lv2 != 0) {
					//title = getBookName() + String.valueOf(DisplayActivity.mChapter) + ":" + String.valueOf(position+1);
					title = getBookName() + String.valueOf(mChapter) + ":" + String.valueOf(position+1);
					//mTextView.setTextIsSelectable(true);
					selected_listview = 1;
					setCustomSelectionCAB(true);
					in_lv1_or_lv2 = 1; 
				}
				else {					
					mTextView.setTextIsSelectable(false); //Disable Text Selection in side-by-side view 
				}
				return false; //pass event to onItemClickListener
				//return true;  //consume event
			}
		});
		listView2.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> av, View v, int position, long id) {
				longclickedVerse = position + 1;
				//Log.d(TAG, "setOnItemLongClickListener,listView2, position=" + String.valueOf(position));
				TextView tv = (TextView)v.findViewById(R.id.scripture);
				mTextView = tv;
				if (in_lv1_or_lv2 != 0) {
					//title = getBookName() + String.valueOf(DisplayActivity.mChapter) + ":" + String.valueOf(position+1);
					title = getBookName() + String.valueOf(mChapter) + ":" + String.valueOf(position+1);
					//mTextView.setTextIsSelectable(true);
					selected_listview = 2;
					setCustomSelectionCAB(true);
					in_lv1_or_lv2 = 2;
				}
				else {					
					mTextView.setTextIsSelectable(false); //Disable Text Selection in side-by-side view 
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

        return sl;
	}
	
	private List<SpannableString> getSpannableScriptureFromDB(int book, int chapter, String db) {
		//Log.d(TAG, "getSpannableScriptureFromDB: " + db + " mChapter = " + String.valueOf(chapter));
		DataBaseHelper BibleDB = new DataBaseHelper(getActivity().getApplicationContext(), db);

        //Get the whole chapter of a book from database
        List<String> sl = BibleDB.getChapter(book, chapter);
        
        //Construct a SpannableString list
        //Log.d(TAG, "sl size = " + String.valueOf(sl.size()));
        List<SpannableString> spl = new ArrayList<SpannableString>();
        if (mDbHelper == null) mDbHelper = new NotebookDbHelper(getActivity().getApplicationContext());	
        for (int i=0; i<sl.size(); i++) {
        	SpannableString ss = new SpannableString(sl.get(i));
        	/* Do this after first displaying the verses for speed consideration. See EVENT_HIGHLIGHT
        	boolean test = mDbHelper.checkIfRecordExists(book, chapter, i+1);
        	if (test == true) {
        		//ss.setSpan(new BackgroundColorSpan(Color.YELLOW), 0, ss.length(), 0);
        		ss.setSpan(new BackgroundColorSpan(0x60ffff00), 0, ss.length(), 0);
        	}
        	*/
        	spl.add(ss);
        }
        return spl;
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
		Selection.selectAll((Spannable) mTextView.getText());	
    	
		mTextView.setCustomSelectionActionModeCallback(new Callback() {
			NotebookDbHelper mDbHelper = new NotebookDbHelper(getActivity().getApplicationContext());					
			boolean test = mDbHelper.checkIfRecordExists(mBook, mChapter, longclickedVerse);

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				// TODO Auto-generated method stub
				//Log.d(TAG, "setCustomSelectionCAB: onCreateActionMode");				
				mode.getMenuInflater().inflate(R.menu.contextual_menu, menu);
				mode.setTitle("");
				amode = mode;
				
				//Lollipop MR1 (Build # 22) workaround, onPrepareActionMode didn't get called automatically. So call is manually
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
					onPrepareActionMode(mode, menu);
				}
				
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				// TODO Auto-generated method stub
				//Log.d(TAG, "setCustomSelectionCAB: onPrepareActionMode");
				
				// Remove the "select all" option
				//menu.removeItem(android.R.id.selectAll);
				
				// Just make the select all option invisiable
				mode.getMenu().getItem(0).setVisible(false);
				
				//Don't show Share icon untill SelectAll is clicked
				//mode.getMenu().getItem(4).setVisible(false);
				
				//Show Favorite or Unfavorite depends on if the verse is already highlighted or not
				if (test == true) {
					mode.getMenu().getItem(7).setVisible(true);  //Make Unfavorite visible
					mode.getMenu().getItem(6).setVisible(false);
				} else {
					mode.getMenu().getItem(6).setVisible(true);  //Make Favorite visible
					mode.getMenu().getItem(7).setVisible(false);
				}
				
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
				case android.R.id.selectAll:
					mode.getMenu().getItem(4).setVisible(true); //Make Share icon visible
					mode.getMenu().getItem(0).setVisible(false); //Make SelectAll invisible
					mode.getMenu().getItem(8).setVisible(false);  //Make Search invisible
					//See if the clicked verse is already a favorite verse or not					
					if (test == true) {
						mode.getMenu().getItem(7).setVisible(true);  //Make Unfavorite visible
					} else {
						mode.getMenu().getItem(6).setVisible(true);  //Make Favorite visible
					}
					return false;
				case android.R.id.copy:
					Toast.makeText(getActivity(), getString(R.string.copied), Toast.LENGTH_SHORT).show();
					return false;
				case R.id.item_play:
					//Log.d(TAG, "Play selected text in " + mVersion);
					if (selected_listview == 1) {
						CEBible_MainActivity.ttsobj.setLanguage(Locale.CHINESE);						
						CEBible_MainActivity.ttsobj.speak(selectedText(), TextToSpeech.QUEUE_FLUSH, null);
					} 					
					else if (selected_listview == 2) {
						if (mEn_trans.equals(getString(R.string.kjv))) {
							CEBible_MainActivity.ttsobj.setLanguage(Locale.UK);
						}
						else {
							CEBible_MainActivity.ttsobj.setLanguage(Locale.US);
						}
						CEBible_MainActivity.ttsobj.speak(selectedText(), TextToSpeech.QUEUE_FLUSH, null);
					}
						
					mode.getMenu().getItem(0).setVisible(false); //Select All
					mode.getMenu().getItem(1).setVisible(false); //Copy
					mode.getMenu().getItem(3).setVisible(true);  //Pause
					mode.getMenu().getItem(4).setVisible(false);  //Share
					mode.getMenu().getItem(5).setVisible(false);  //Note					
					mode.getMenu().getItem(6).setVisible(false);  //Favorite
					mode.getMenu().getItem(7).setVisible(false);  //Unfavorite
					mode.getMenu().getItem(8).setVisible(false);  //Search
					return true;
				case R.id.item_pause:
					CEBible_MainActivity.ttsobj.stop();
					return true;
				case R.id.item_share:
					//Log.d(TAG, "mode size = " + String.valueOf(mode.getMenu().size()));
					//Select the text of the whole verse to share
					//Selection.selectAll((Spannable) mTextView.getText());
					
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
				case R.id.item_favorite:	
					//Log.d(TAG, "Favorite icon pressed");
					//Log.d(TAG, "mBook=" + String.valueOf(mBook) + " mChapter=" + String.valueOf(mChapter) + " mVerse=" + String.valueOf(mVerse) + " longclickedVerse=" + String.valueOf(longclickedVerse));
					
					if (test == true) {
						//Log.d(TAG, "update existing DB record");
						mDbHelper.updateRecord(mBook, mChapter, longclickedVerse, selectedText(), "", "");
					} else {
						//Log.d(TAG, "inserting new DB record");
						mDbHelper.insertRecord(mBook, mChapter, longclickedVerse, selectedText(), "", "");
					}

					//Log.d(TAG, "Favorite clicked, longclickedVerse=" + String.valueOf(longclickedVerse));
					
					//Highlight the text via Spannable string for ListView1
					SpannableString ss1 = spl1.get(longclickedVerse - 1);
					ss1.setSpan(new BackgroundColorSpan(0x60ffff00), 0, ss1.length(), 0);
					spl1.set(longclickedVerse-1, ss1);
					adapter1.notifyDataSetChanged();
					
					//Highlight the text via Spannable string for ListView2
					SpannableString ss2 = spl2.get(longclickedVerse - 1);
					ss2.setSpan(new BackgroundColorSpan(0x60ffff00), 0, ss2.length(), 0);
					spl2.set(longclickedVerse-1, ss2);
					adapter2.notifyDataSetChanged();
					
					return true;
				case R.id.item_unfavorite:	
					//Log.d(TAG, "Unfavorite icon pressed");	
					mDbHelper.deleteRecord(mBook, mChapter, longclickedVerse);
					
					//Clear the Span for ListView1
					SpannableString s1 = spl1.get(longclickedVerse - 1);
					BackgroundColorSpan[] sp1 = s1.getSpans(0, s1.length(), BackgroundColorSpan.class);
					for (int j = 0; j < sp1.length; j++) {
						s1.removeSpan(sp1[j]);
					}
					spl1.set(longclickedVerse-1, s1);
					adapter1.notifyDataSetChanged();
					
					//Clear the Span for ListView2
					SpannableString s2 = spl2.get(longclickedVerse - 1);
					BackgroundColorSpan[] sp2 = s2.getSpans(0, s2.length(), BackgroundColorSpan.class);
					for (int j = 0; j < sp2.length; j++) {
						s2.removeSpan(sp2[j]);
					}
					spl2.set(longclickedVerse-1, s2);
					adapter2.notifyDataSetChanged();
					
					return true;
				default:	
					return false;
				}
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				CEBible_MainActivity.ttsobj.stop();
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
