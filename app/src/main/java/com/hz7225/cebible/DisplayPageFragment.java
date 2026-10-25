package com.hz7225.cebible;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
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
import android.content.Context;

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
	/*
	class MyTextView extends TextView {
		public MyTextView(Context context) {
			super(context);
		}
		protected void onSelectionChanged (int selStart, int selEnd) {
			Log.d(TAG, "onSelectedChanged");
		}
	};
	*/
	
	TextView mTextView;
	
	TextToSpeech ttsobj;
	
	int selected_listview = 0; //used by TTS to play the text in correct language
	
	static int in_lv1_or_lv2 = 0; //used in search the correct language database, 1=Chinese, 2 = English
	
	ActionMode amode;
	SpannableString selectedVerseSpan;
	BackgroundColorSpan selectionSpan;

	private boolean isContinuousReading = false;
	private boolean isReadingPaused = false;
	public boolean isContinuousReading() { return isContinuousReading; }
	private int activeReadingVerseIndex = 0;
	private BackgroundColorSpan readingHighlightSpan = new BackgroundColorSpan(0x3000FFFF);
	private SpannableString activeReadingSpan = null;
	private PowerManager.WakeLock wakeLock = null;
	
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
		View.OnTouchListener listTouchListener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN && amode != null) {
					v.performClick();
					dismissCABAndHighlight();
					return true;
				}
				return false;
			}
		};

		listView1.setOnTouchListener(listTouchListener);
		listView2.setOnTouchListener(listTouchListener);

    	listView1.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> av, View v, int position, long id) {
				dismissCABAndHighlight();
			}
		});
    	listView2.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> av, View v, int position, long id) {
				dismissCABAndHighlight();
			}
		});
    	
    	//Set long click listener
		listView1.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> av, View v, int position, long id) {	
				longclickedVerse = position + 1;
				//Log.d(TAG, "setOnItemLongClickListener,listView1, position=" + String.valueOf(position));
				TextView tv = (TextView)v.findViewById(R.id.scripture);

				/*test the following
				for (int i = 0; i < listView1.getChildCount(); i++) {
					if(position == i ){
						listView1.getChildAt(i).setBackgroundColor(Color.YELLOW);
					}else{
						listView1.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
					}
				}
				*/
				/* test
				for (int i=0; i<spl1.size(); i++) {
					SpannableString ss = spl1.get(i);
					if(position == i ) {
						Log.d(TAG, "set color");
						ss.setSpan(new BackgroundColorSpan(0x60ffff00), 0, ss.length(), 0);
						spl1.set(i, ss);
					} else {
						ForegroundColorSpan[] spans=ss.getSpans(0, ss.length(), ForegroundColorSpan.class);
						//for (i = 0; i < ss.length(); i++)
						//	if (ss!=null) ss.removeSpan(spans[i]);
					}
				}
				adapter1.notifyDataSetChanged();
				*/

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
				return true; // consume event so onItemClick is NOT triggered
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
				return true; // consume event so onItemClick is NOT triggered
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
					clearVerseHighlight();
					if (amode != null) {
						amode.finish();
						amode = null;
					}
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
					clearVerseHighlight();
					if (amode != null) {
						amode.finish();
						amode = null;
					}
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
	
	private String getBookNameFor(int bookNum) {
		String[] ot = getResources().getStringArray(R.array.old_testament);
		String[] nt = getResources().getStringArray(R.array.new_testament);

		String book;
		if (bookNum < 40) {
			book = ot[bookNum - 1].substring(0, ot[bookNum - 1].indexOf(","));
		} else {
			book = nt[bookNum - 1 - 39].substring(0, nt[bookNum - 1 - 39].indexOf(","));
		}
		return book;
	}

	private String getBookName() {
		return getBookNameFor(mBook);
	}

	public void clearVerseHighlight() {
		if (selectionSpan != null && selectedVerseSpan != null) {
			selectedVerseSpan.removeSpan(selectionSpan);
			selectionSpan = null;
			selectedVerseSpan = null;
			if (adapter1 != null) adapter1.notifyDataSetChanged();
			if (adapter2 != null) adapter2.notifyDataSetChanged();
		}
		if (mTextView != null && mTextView.getText() instanceof Spannable) {
			Selection.removeSelection((Spannable) mTextView.getText());
		}
	}

	public void dismissCABAndHighlight() {
		clearVerseHighlight();
		if (amode != null) {
			amode.finish();
			amode = null;
		}
	}
	
	private void setCustomSelectionCAB(final boolean longClick) {
		if (mTextView == null || mTextView.getText() == null) return;

		// Finish previous Action Mode and clear previous highlight FIRST
		dismissCABAndHighlight();

		// Apply background selection highlight directly to data model (~37% opacity Light Azure Blue)
		if (selected_listview == 1 && spl1 != null && longclickedVerse > 0 && longclickedVerse <= spl1.size()) {
			selectedVerseSpan = spl1.get(longclickedVerse - 1);
			selectionSpan = new BackgroundColorSpan(0x60007FFF); // 37% opacity Light Azure Blue
			selectedVerseSpan.setSpan(selectionSpan, 0, selectedVerseSpan.length(), 0);
			if (adapter1 != null) adapter1.notifyDataSetChanged();
		} else if (selected_listview == 2 && spl2 != null && longclickedVerse > 0 && longclickedVerse <= spl2.size()) {
			selectedVerseSpan = spl2.get(longclickedVerse - 1);
			selectionSpan = new BackgroundColorSpan(0x60007FFF); // 37% opacity Light Azure Blue
			selectedVerseSpan.setSpan(selectionSpan, 0, selectedVerseSpan.length(), 0);
			if (adapter2 != null) adapter2.notifyDataSetChanged();
		}

		mTextView.setMovementMethod(android.text.method.ArrowKeyMovementMethod.getInstance());
		mTextView.setFocusable(true);
		mTextView.setFocusableInTouchMode(true);
		mTextView.setTextIsSelectable(true);

		final GestureDetector doubleTapBlocker = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onDoubleTap(MotionEvent e) {
				dismissCABAndHighlight();
				return true;
			}
		});

		mTextView.setOnTouchListener(new View.OnTouchListener() {
			private long tapDownTime = 0;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (doubleTapBlocker.onTouchEvent(event)) {
					return true;
				}
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					tapDownTime = SystemClock.uptimeMillis();
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					long duration = SystemClock.uptimeMillis() - tapDownTime;
					if (duration < 350 && amode != null) {
						v.performClick();
						dismissCABAndHighlight();
						return true;
					}
				}
				return false;
			}
		});

		if (mTextView.getText() instanceof Spannable) {
			Selection.selectAll((Spannable) mTextView.getText());
		}

		Callback callback = new Callback() {
			NotebookDbHelper mDbHelper = new NotebookDbHelper(getActivity().getApplicationContext());					
			boolean test = mDbHelper.checkIfRecordExists(mBook, mChapter, longclickedVerse);

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				if (!longClick) {
					return false; // Reject double-tap Action Mode!
				}
				Log.d(TAG, "setCustomSelectionCAB: onCreateActionMode");
				mode.getMenuInflater().inflate(R.menu.contextual_menu, menu);
				mode.setTitle("");
				amode = mode;
				
				//Lollipop MR1 (Build # 22) workaround, onPrepareActionMode didn't get called automatically. So call is manually
				if ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1)
						&& (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M))
				{
					onPrepareActionMode(mode, menu);
				}
				
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				// TODO Auto-generated method stub
				Log.d(TAG, "setCustomSelectionCAB: onPrepareActionMode");

				// Remove all system-injected items (Share, Read Aloud, Translate, Assist, etc.)
				for (int i = menu.size() - 1; i >= 0; i--) {
					MenuItem item = menu.getItem(i);
					int itemId = item.getItemId();
					if (itemId != R.id.item_favorite &&
						itemId != R.id.item_unfavorite &&
						itemId != R.id.item_share &&
						itemId != R.id.item_play &&
						itemId != R.id.item_pause &&
						itemId != R.id.item_note &&
						itemId != android.R.id.copy) {
						menu.removeItem(itemId);
					}
				}

				// Find Favorite and Unfavorite menu items directly by ID
				MenuItem favoriteItem = menu.findItem(R.id.item_favorite);
				MenuItem unfavoriteItem = menu.findItem(R.id.item_unfavorite);

				if (test) {
					// Verse is already favorited -> show Unfavorite, hide Favorite
					if (favoriteItem != null) favoriteItem.setVisible(false);
					if (unfavoriteItem != null) unfavoriteItem.setVisible(true);
				} else {
					// Verse is not favorited -> show Favorite, hide Unfavorite
					if (favoriteItem != null) favoriteItem.setVisible(true);
					if (unfavoriteItem != null) unfavoriteItem.setVisible(false);
				}
				
				return true;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode,
					MenuItem item) {
				//Log.d(TAG, "setCustomSelectionCAB: onActionItemClicked");
				int it = item.getItemId();
				//switch (item.getItemId()) {
				if (it == android.R.id.copy) {
					if (getActivity() != null) {
						ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
						ClipData clip = ClipData.newPlainText("CEBible", selectedText() + " (" + title + ")");
						if (clipboard != null) {
							clipboard.setPrimaryClip(clip);
						}
					}
					Toast.makeText(getActivity(), getString(R.string.copied), Toast.LENGTH_SHORT).show();
					mode.finish();
					return true;
				} else if (it == R.id.item_play) {
					if (isContinuousReading) {
						pauseContinuousReading();
					}
					if (selected_listview == 1) {
						applyAutoVoice(Locale.CHINESE);
						String ssmlText = formatBiblicalSSML(selectedText());
						CEBible_MainActivity.ttsobj.speak(ssmlText, TextToSpeech.QUEUE_FLUSH, null);
					} else if (selected_listview == 2) {
						Locale targetLocale = (mEn_trans != null && mEn_trans.equals(getString(R.string.kjv))) ? Locale.UK : Locale.US;
						applyAutoVoice(targetLocale);
						CEBible_MainActivity.ttsobj.speak(selectedText(), TextToSpeech.QUEUE_FLUSH, null);
					}
					return true;
				} else if (it == R.id.item_pause) {
					if (isContinuousReading) {
						pauseContinuousReading();
					} else if (CEBible_MainActivity.ttsobj != null) {
						CEBible_MainActivity.ttsobj.stop();
					}
					return true;
				} else if (it == R.id.item_share) {
					Intent i=new Intent(android.content.Intent.ACTION_SEND);
					i.setType("text/plain");
					i.putExtra(android.content.Intent.EXTRA_SUBJECT, title);
					i.putExtra(android.content.Intent.EXTRA_TEXT, selectedText() + " (" + title + ")");
					startActivity(Intent.createChooser(i,"Share via"));
					mode.finish();
					return true;
				} else if (it == R.id.item_favorite) {	
					if (test == true) {
						mDbHelper.updateRecord(mBook, mChapter, longclickedVerse, selectedText(), "", "");
					} else {
						mDbHelper.insertRecord(mBook, mChapter, longclickedVerse, selectedText(), "", "");
					}

					SpannableString ss1 = spl1.get(longclickedVerse - 1);
					ss1.setSpan(new BackgroundColorSpan(0x60ffff00), 0, ss1.length(), 0);
					spl1.set(longclickedVerse-1, ss1);
					adapter1.notifyDataSetChanged();
					
					SpannableString ss2 = spl2.get(longclickedVerse - 1);
					ss2.setSpan(new BackgroundColorSpan(0x60ffff00), 0, ss2.length(), 0);
					spl2.set(longclickedVerse-1, ss2);
					adapter2.notifyDataSetChanged();
					
					mode.finish();
					return true;
				} else if (it == R.id.item_unfavorite) {	
					mDbHelper.deleteRecord(mBook, mChapter, longclickedVerse);
					
					SpannableString s1 = spl1.get(longclickedVerse - 1);
					BackgroundColorSpan[] sp1 = s1.getSpans(0, s1.length(), BackgroundColorSpan.class);
					for (int j = 0; j < sp1.length; j++) {
						s1.removeSpan(sp1[j]);
					}
					spl1.set(longclickedVerse-1, s1);
					adapter1.notifyDataSetChanged();
					
					SpannableString s2 = spl2.get(longclickedVerse - 1);
					BackgroundColorSpan[] sp2 = s2.getSpans(0, s2.length(), BackgroundColorSpan.class);
					for (int j = 0; j < sp2.length; j++) {
						s2.removeSpan(sp2[j]);
					}
					spl2.set(longclickedVerse-1, s2);
					adapter2.notifyDataSetChanged();
					
					mode.finish();
					return true;
				}
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				if (!isContinuousReading && CEBible_MainActivity.ttsobj != null) {
					CEBible_MainActivity.ttsobj.stop();
				}
				if (selectionSpan != null && selectedVerseSpan != null) {
					selectedVerseSpan.removeSpan(selectionSpan);
					selectionSpan = null;
					selectedVerseSpan = null;
					if (adapter1 != null) adapter1.notifyDataSetChanged();
					if (adapter2 != null) adapter2.notifyDataSetChanged();
				}
				amode = null;
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
		};

		mTextView.setCustomSelectionActionModeCallback(callback);

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
			amode = mTextView.startActionMode(callback, ActionMode.TYPE_FLOATING);
		} else if (getActivity() != null) {
			amode = getActivity().startActionMode(callback);
		}
	}

	public void toggleContinuousReading() {
		if (isContinuousReading) {
			pauseContinuousReading();
		} else if (isReadingPaused) {
			resumeContinuousReading();
		} else {
			startContinuousReading();
		}
	}

	public void pauseContinuousReading() {
		isContinuousReading = false;
		isReadingPaused = true;

		if (CEBible_MainActivity.ttsobj != null) {
			CEBible_MainActivity.ttsobj.stop();
		}

		if (wakeLock != null) {
			try {
				if (wakeLock.isHeld()) {
					wakeLock.release();
				}
			} catch (Exception e) {
				Log.e(TAG, "Error releasing WakeLock", e);
			}
			wakeLock = null;
		}

		if (getActivity() instanceof DisplayActivity) {
			((DisplayActivity) getActivity()).updateReadAloudIcon(false);
		}
	}

	public void resumeContinuousReading() {
		if (CEBible_MainActivity.ttsobj == null) return;

		isContinuousReading = true;
		isReadingPaused = false;

		if (getActivity() != null) {
			try {
				PowerManager pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
				if (pm != null) {
					wakeLock = pm.newWakeLock(1, "CEBible:TTSWakeLock");
					wakeLock.acquire(15 * 60 * 1000L);
				}
			} catch (Exception e) {
				Log.e(TAG, "Error acquiring WakeLock", e);
			}
		}

		if (getActivity() instanceof DisplayActivity) {
			((DisplayActivity) getActivity()).updateReadAloudIcon(true);
		}

		if (activeReadingVerseIndex == 0) {
			speakChapterHeaderThenVerse();
		} else {
			speakCurrentReadingVerse();
		}
	}

	public void startContinuousReading() {
		if (CEBible_MainActivity.ttsobj == null) return;

		isContinuousReading = true;
		isReadingPaused = false;

		boolean startFromSelectedVerse = false;

		if (selectedVerseSpan != null && longclickedVerse > 0) {
			activeReadingVerseIndex = longclickedVerse - 1;
			startFromSelectedVerse = true;
		} else {
			activeReadingVerseIndex = 0;
		}

		if (amode != null) {
			amode.finish();
			amode = null;
		}

		if (getActivity() != null) {
			try {
				PowerManager pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
				if (pm != null) {
					wakeLock = pm.newWakeLock(1, "CEBible:TTSWakeLock"); // 1 = PARTIAL_WAKELOCK
					wakeLock.acquire(15 * 60 * 1000L); // 15 min max timeout
				}
			} catch (Exception e) {
				Log.e(TAG, "Error acquiring WakeLock", e);
			}
		}

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
			AudioAttributes audioAttributes = new AudioAttributes.Builder()
					.setUsage(AudioAttributes.USAGE_MEDIA)
					.setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
					.build();
			CEBible_MainActivity.ttsobj.setAudioAttributes(audioAttributes);
		}

		if (getActivity() instanceof DisplayActivity) {
			((DisplayActivity) getActivity()).updateReadAloudIcon(true);
		}

		Locale targetLocale = Locale.CHINESE;
		if (mLanguage != null && mLanguage.equals(getString(R.string.en))) {
			if (mEn_trans != null && mEn_trans.equals(getString(R.string.kjv))) {
				targetLocale = Locale.UK;
			} else {
				targetLocale = Locale.US;
			}
		}

		applyAutoVoice(targetLocale);

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
			CEBible_MainActivity.ttsobj.setOnUtteranceProgressListener(new UtteranceProgressListener() {
				@Override
				public void onStart(String utteranceId) {
					if (getActivity() == null) return;
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							highlightReadingVerse(activeReadingVerseIndex);
						}
					});
				}

				@Override
				public void onDone(final String utteranceId) {
					if (!isContinuousReading) return;
					if (getActivity() == null) return;
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if ("chapter_header".equals(utteranceId)) {
								activeReadingVerseIndex = 0;
								speakCurrentReadingVerse();
								return;
							}

							activeReadingVerseIndex++;
							List<SpannableString> currentList = (mLanguage != null && mLanguage.equals(getString(R.string.en))) ? spl2 : spl1;
							if (currentList != null && activeReadingVerseIndex < currentList.size()) {
								speakCurrentReadingVerse();
							} else {
								if (getActivity() instanceof DisplayActivity) {
									boolean advanced = ((DisplayActivity) getActivity()).advanceToNextChapter();
									if (!advanced) {
										stopContinuousReading();
									}
								} else {
									stopContinuousReading();
								}
							}
						}
					});
				}

				@Override
				public void onError(String utteranceId) {
					if (getActivity() != null) {
						getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								stopContinuousReading();
							}
						});
					}
				}
			});
		}

		if (startFromSelectedVerse) {
			speakCurrentReadingVerse();
		} else {
			speakChapterHeaderThenVerse();
		}
	}

	private void speakChapterHeaderThenVerse() {
		int pageNum = getArguments() != null ? getArguments().getInt(ARG_PAGE, 0) : mPageNumber;
		ChapterPosition cp = new ChapterPosition(getActivity(), pageNum);
		int currentBook = cp.getBook();
		int currentChapter = cp.getChapter();

		String realBookName = getBookNameFor(currentBook);

		String chapterHeader;
		if (mLanguage != null && mLanguage.equals(getString(R.string.en))) {
			chapterHeader = realBookName + " Chapter " + currentChapter;
		} else {
			if (currentChapter == 2) {
				chapterHeader = realBookName + " 第<phoneme alphabet=\"pinyin\" ph=\"er4\">二</phoneme>章";
			} else {
				chapterHeader = realBookName + " 第" + currentChapter + "章";
			}
			chapterHeader = formatBiblicalSSML(chapterHeader);
		}

		if (CEBible_MainActivity.ttsobj != null) {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
				CEBible_MainActivity.ttsobj.speak(chapterHeader, TextToSpeech.QUEUE_FLUSH, null, "chapter_header");
			} else {
				HashMap<String, String> params = new HashMap<String, String>();
				params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "chapter_header");
				CEBible_MainActivity.ttsobj.speak(chapterHeader, TextToSpeech.QUEUE_FLUSH, params);
			}
		}
	}

	private String formatBiblicalSSML(String rawText) {
		if (rawText == null || rawText.isEmpty()) return "";

		String text = rawText;

		// Correct common Chinese biblical polyphonic characters (多音字)
		text = text.replace("重新", "<phoneme alphabet=\"pinyin\" ph=\"chong2\">重</phoneme>新");
		text = text.replace("重修", "<phoneme alphabet=\"pinyin\" ph=\"chong2\">重</phoneme>修");
		text = text.replace("重立", "<phoneme alphabet=\"pinyin\" ph=\"chong2\">重</phoneme>立");
		text = text.replace("重建", "<phoneme alphabet=\"pinyin\" ph=\"chong2\">重</phoneme>建");
		text = text.replace("重造", "<phoneme alphabet=\"pinyin\" ph=\"chong2\">重</phoneme>造");

		text = text.replace("长存", "<phoneme alphabet=\"pinyin\" ph=\"chang2\">长</phoneme>存");
		text = text.replace("长远", "<phoneme alphabet=\"pinyin\" ph=\"chang2\">长</phoneme>远");
		text = text.replace("长久", "<phoneme alphabet=\"pinyin\" ph=\"chang2\">长</phoneme>久");
		text = text.replace("成长", "成<phoneme alphabet=\"pinyin\" ph=\"zhang2\">长</phoneme>");
		text = text.replace("族长", "族<phoneme alphabet=\"pinyin\" ph=\"zhang2\">长</phoneme>");
		text = text.replace("长老", "<phoneme alphabet=\"pinyin\" ph=\"zhang2\">长</phoneme>老");

		text = text.replace("音乐", "音<phoneme alphabet=\"pinyin\" ph=\"yue4\">乐</phoneme>");
		text = text.replace("喜乐", "喜<phoneme alphabet=\"pinyin\" ph=\"le4\">乐</phoneme>");
		text = text.replace("快乐", "快<phoneme alphabet=\"pinyin\" ph=\"le4\">乐</phoneme>");

		text = text.replace("传福音", "<phoneme alphabet=\"pinyin\" ph=\"chuan2\">传</phoneme>福音");

		return "<speak>" + text + "</speak>";
	}

	private void speakCurrentReadingVerse() {
		List<SpannableString> currentList = (mLanguage != null && mLanguage.equals(getString(R.string.en))) ? spl2 : spl1;
		if (currentList == null || activeReadingVerseIndex >= currentList.size()) {
			stopContinuousReading();
			return;
		}

		SpannableString verseText = currentList.get(activeReadingVerseIndex);
		String textToSpeak = verseText != null ? verseText.toString() : "";

		// Format as SSML for Chinese
		if (mLanguage == null || !mLanguage.equals(getString(R.string.en))) {
			textToSpeak = formatBiblicalSSML(textToSpeak);
		}

		if (CEBible_MainActivity.ttsobj != null) {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
				CEBible_MainActivity.ttsobj.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "verse_" + activeReadingVerseIndex);
			} else {
				HashMap<String, String> params = new HashMap<String, String>();
				params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "verse_" + activeReadingVerseIndex);
				CEBible_MainActivity.ttsobj.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params);
			}
		}
	}

	private void highlightReadingVerse(int verseIndex) {
		List<SpannableString> currentList = (mLanguage != null && mLanguage.equals(getString(R.string.en))) ? spl2 : spl1;
		ListDataAdapter2 currentAdapter = (mLanguage != null && mLanguage.equals(getString(R.string.en))) ? adapter2 : adapter1;
		ListView currentListView = (mLanguage != null && mLanguage.equals(getString(R.string.en))) ? listView2 : listView1;

		if (currentList == null || verseIndex < 0 || verseIndex >= currentList.size()) return;

		if (activeReadingSpan != null && readingHighlightSpan != null) {
			activeReadingSpan.removeSpan(readingHighlightSpan);
		}

		activeReadingSpan = currentList.get(verseIndex);
		if (activeReadingSpan != null) {
			activeReadingSpan.setSpan(readingHighlightSpan, 0, activeReadingSpan.length(), 0);
		}

		if (currentAdapter != null) {
			currentAdapter.notifyDataSetChanged();
		}

		if (currentListView != null) {
			currentListView.smoothScrollToPosition(verseIndex);
		}
	}

	private void applyAutoVoice(Locale targetLocale) {
		if (CEBible_MainActivity.ttsobj == null || getActivity() == null) return;

		String targetLang = targetLocale != null ? targetLocale.getLanguage().toLowerCase(Locale.US) : "";
		boolean isChinese = targetLang.equals("zh") || targetLang.equals("cmn");

		// Lower pitch and speech rate slightly for Chinese reading
		if (isChinese) {
			CEBible_MainActivity.ttsobj.setPitch(0.80f);      // Slightly deeper pitch
			CEBible_MainActivity.ttsobj.setSpeechRate(0.90f); // Slightly slower speech speed
		} else {
			CEBible_MainActivity.ttsobj.setPitch(1.0f);
			CEBible_MainActivity.ttsobj.setSpeechRate(1.0f);
		}

		CEBible_MainActivity.ttsobj.setLanguage(targetLocale);

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
			try {
				Set<Voice> voices = CEBible_MainActivity.ttsobj.getVoices();
				if (voices == null || voices.isEmpty()) {
					return;
				}

				List<Voice> availableVoices = new ArrayList<Voice>();

				for (Voice voice : voices) {
					if (voice.getLocale() == null) continue;
					String vLang = voice.getLocale().getLanguage().toLowerCase(Locale.US);

					boolean langMatch = false;
					if (targetLang.equals("zh") || targetLang.equals("cmn")) {
						langMatch = vLang.equals("zh") || vLang.equals("cmn");
					} else {
						langMatch = vLang.equals(targetLang);
					}

					if (!langMatch) continue;

					Set<String> features = voice.getFeatures();
					if (features != null && features.contains("notInstalled")) {
						continue; // Skip uninstalled voices
					}
					availableVoices.add(voice);
				}

				if (availableVoices.isEmpty()) {
					return;
				}

				Voice targetVoice = null;

				if (targetLang.equals("zh") || targetLang.equals("cmn")) {
					// High Quality Male voice for Chinese (ccd, cca, cta, male)
					for (Voice v : availableVoices) {
						String name = v.getName().toLowerCase(Locale.US);
						if (name.contains("ccd") || name.contains("cca") || name.contains("cta") || name.contains("m00") || name.contains("male")) {
							targetVoice = v;
							break;
						}
					}
					if (targetVoice == null && !availableVoices.isEmpty()) {
						targetVoice = availableVoices.get(0); // Index 0 is Male in Chinese
					}
				} else {
					// High Quality Female voice for American/British English
					String targetCountry = targetLocale.getCountry().toLowerCase(Locale.US);

					// First pass: match exact country (e.g. US female voices: tpc, tpd, tpf)
					for (Voice v : availableVoices) {
						if (v.getLocale() == null) continue;
						String country = v.getLocale().getCountry().toLowerCase(Locale.US);
						String name = v.getName().toLowerCase(Locale.US);

						boolean countryMatch = country.equals(targetCountry) || name.contains(targetCountry);
						if (countryMatch) {
							if (name.contains("tpc") || name.contains("tpd") || name.contains("tpf") || name.contains("rjd") || name.contains("gba") || name.contains("f00") || name.contains("female")) {
								targetVoice = v;
								break;
							}
						}
					}

					// Second pass: match any US female voice (tpc, tpd, tpf)
					if (targetVoice == null) {
						for (Voice v : availableVoices) {
							String name = v.getName().toLowerCase(Locale.US);
							if (name.contains("tpc") || name.contains("tpd") || name.contains("tpf") || (name.contains("us") && name.contains("female"))) {
								targetVoice = v;
								break;
							}
						}
					}

					// Third pass: match any non-male US voice
					if (targetVoice == null) {
						for (Voice v : availableVoices) {
							String name = v.getName().toLowerCase(Locale.US);
							if (name.contains("us") && !name.contains("sfg") && !name.contains("iom") && !name.contains("fis") && !name.contains("male")) {
								targetVoice = v;
								break;
							}
						}
					}

					if (targetVoice == null && !availableVoices.isEmpty()) {
						targetVoice = availableVoices.get(0);
					}
				}

				if (targetVoice != null) {
					int res = CEBible_MainActivity.ttsobj.setVoice(targetVoice);
					Log.d(TAG, "applyAutoVoice: Successfully set voice " + targetVoice.getName() + " for lang=" + targetLang + " (result=" + res + ")");
				}
			} catch (Exception e) {
				Log.e(TAG, "applyAutoVoice error", e);
			}
		}
	}

	public void stopContinuousReading() {
		isContinuousReading = false;
		isReadingPaused = false;
		activeReadingVerseIndex = 0;
		if (CEBible_MainActivity.ttsobj != null) {
			CEBible_MainActivity.ttsobj.stop();
		}
		if (wakeLock != null) {
			try {
				if (wakeLock.isHeld()) {
					wakeLock.release();
				}
			} catch (Exception e) {
				Log.e(TAG, "Error releasing WakeLock", e);
			}
			wakeLock = null;
		}
		if (activeReadingSpan != null && readingHighlightSpan != null) {
			activeReadingSpan.removeSpan(readingHighlightSpan);
			activeReadingSpan = null;
		}
		if (adapter1 != null) adapter1.notifyDataSetChanged();
		if (adapter2 != null) adapter2.notifyDataSetChanged();
		if (getActivity() instanceof DisplayActivity) {
			((DisplayActivity) getActivity()).updateReadAloudIcon(false);
		}
	}
}
