package com.hz7225.cebible;

import java.util.List;
import java.util.Locale;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Selection;
import android.text.Spannable;
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
import android.widget.AdapterView;
import android.widget.ListView;
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
	static private int mVerse;
	static private String mVersion;
	static private int mChapter;
	
	ListView listView1;
	ListView listView2;
	ListDataAdapter2 adapter1;
	ListDataAdapter2 adapter2;
	
	String title; //BookName + Chapter # + Verse #
	
	boolean isLeftListEnabled = true;
	boolean isRightListEnabled = true;
	
	TextView mTextView;
	
	TextToSpeech ttobj;
	int selected_listview = 0; //used by TTS to play the text in correct language
	
	public static DisplayPageFragment create(int pageNumber, int book, int verse, String version) {
		//Log.d(TAG, "PageFragment::create(), pageNumber = " + String.valueOf(pageNumber) + " book=" +String.valueOf(book));
        mBook = book;
        //mChapter = chapter;
        mVerse = verse;
              
		DisplayPageFragment fragment = new DisplayPageFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, pageNumber);  
        args.putString(ARG_VERSION, version);
        fragment.setArguments(args);
        return fragment;
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPageNumber = getArguments().getInt(ARG_PAGE);
		mVersion = getArguments().getString(ARG_VERSION);
		//Log.d(TAG, "mPageNumber = " + String.valueOf(mPageNumber));
	}
	
	private List<String> getScriptureFromDB(int book, int chapter, String db) {
		//Log.d(TAG, "getScriptureFromDB: " + db + "ddddddddbbbbbbb");
		DataBaseHelper BibleDB = new DataBaseHelper(getActivity().getApplicationContext(), db);

        //Get the whole chapter of a book from database
        List<String> sl = BibleDB.getChapter(book, chapter);
        for (int i=0; i<sl.size(); i++) {
        	sl.set(i, sl.get(i));
        }
        return sl;
	}

	public void onDestroyView() {
		super.onDestroyView();
		//Log.d(TAG, "onDestroyView()");
		ttobj.shutdown();
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ttobj=new TextToSpeech(getActivity(), 
				new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				if(status != TextToSpeech.ERROR){
					ttobj.setLanguage(Locale.CHINESE);
				}				
			}
		});
		
		//getBookAndChapterFromPosition(mPageNumber);
		
		//Log.d(TAG, "mPageNumber = " + String.valueOf(mPageNumber) + " mChapter = " + String.valueOf(mChapter));
		//Log.d(TAG, "onCreateView(): mPageNumber = " + String.valueOf(mPageNumber) + " mVersion = " + mVersion);		
		
        //Use 2 ListViews in Linear layout
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.listviews, container, false);
        
        //ListView1 for Chinese CUVS
        listView1 = (ListView) rootView.findViewById(R.id.listView1);
        List<String> sl1 = getScriptureFromDB(mBook, mPageNumber+1, "cuvslite.bbl.db");
    	adapter1 = new ListDataAdapter2(this.getActivity(), sl1);     	
	    listView1.setAdapter(adapter1);  //Set ListView adapters	    
    	listView1.setSelection(mVerse);  //Set starting position
    	
    	//ListView2 for English KJV
    	listView2 = (ListView) rootView.findViewById(R.id.listView2);    	
    	List<String> sl2 = getScriptureFromDB(mBook, mPageNumber+1, "EB_kjv_bbl.db");
    	adapter2 = new ListDataAdapter2(this.getActivity(), sl2);    	
    	listView2.setAdapter(adapter2);  //Set ListView adapters    	
    	listView2.setSelection(mVerse);  //Set starting position
    	
    	//Set listener
    	listView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> av, View v, int position, long id) {
				//Log.d(TAG, "LV1: onItemClick");
				title= "";
			}
		});
		
    	//Set long click listener
		listView1.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> av, View v, int position, long id) {
				//Log.d("HZ", "LV1: onItemLongClick selected_listview = 1");
				//Log.d(TAG, "LV1: onItemLongClick");
				TextView tv = (TextView)v.findViewById(R.id.scripture);
				//Log.d(TAG, tv.getText().toString());
				title = getChineseBookName() + String.valueOf(DisplayActivity.mChapter) + ":" + String.valueOf(position+1);
				mTextView = tv;
				mTextView.setTextIsSelectable(true);
				selected_listview = 1;
				setCustomSelectionCAB();
		    	return false; //pass event to onItemClickListener
		    	//return true;  //consume event
			}
		});
		listView2.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> av, View v, int position, long id) {
				//Log.d(TAG, "LV2: onItemLongClick");
				//Log.d("HZ", "LV1: onItemLongClick selected_listview = 2");
				TextView tv = (TextView)v.findViewById(R.id.scripture);
				//Log.d(TAG, tv.getText().toString());
				title = getEnglishBookName() + String.valueOf(DisplayActivity.mChapter) + ":" + String.valueOf(position+1);
				mTextView = tv;
				mTextView.setTextIsSelectable(true);
				selected_listview = 2;
				setCustomSelectionCAB();
		    	return false; //pass event to onItemClickListener
		    	//return true;  //consume event
			}
		});
    	
    	//For CUVS or KJV, only display one ListView, make the other one invisible
    	//Otherwise, display both ListViews 
    	if (mVersion.equals(getString(R.string.cuvs))) {
    		listView2.setVisibility(View.GONE); //To display only one ListView
    		rootView.findViewById(R.id.horizontal_white_space).setVisibility(View.GONE); //Remove the horizontal spacer too
    	} else if (mVersion.equals(getString(R.string.kjv))) {
    		listView1.setVisibility(View.GONE); //To display only one ListView
        	rootView.findViewById(R.id.horizontal_white_space).setVisibility(View.GONE); //Remove the horizontal spacer too
    	}		
    	
		//To synchronize the positions of the two ListViews
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
	
	private String getChineseBookName() {
		// Get book name from XML resource
		String[] ot;
		String[] nt;
		
		ot = getResources().getStringArray(R.array.old_testament_ch);
		nt = getResources().getStringArray(R.array.new_testament_ch);

		String book;
		if (mBook<40) {
			book = ot[mBook-1].substring(0, ot[mBook-1].indexOf(","));
		} else {
			book = nt[mBook-1-39].substring(0, nt[mBook-1-39].indexOf(","));
		}
		
		return book;
	}
	
	private String getEnglishBookName() {
		// Get book name from XML resource
		String[] ot;
		String[] nt;
		
		ot = getResources().getStringArray(R.array.old_testament);
		nt = getResources().getStringArray(R.array.new_testament);

		String book;
		if (mBook<40) {
			book = ot[mBook-1].substring(0, ot[mBook-1].indexOf(","));
		} else {
			book = nt[mBook-1-39].substring(0, nt[mBook-1-39].indexOf(","));
		}
		
		return book;
	}
	
	/*
	private void getBookAndChapterFromPosition(int position) {
		Log.d(TAG, "getBookAndChapterFromPosition");
		ChapterPosition cp = new ChapterPosition(getActivity(), position);
		Log.d(TAG, "getBook " + String.valueOf(cp.getBook()) + " at position " + String.valueOf(position));
		Log.d(TAG, "getChapter " + String.valueOf(cp.getChapter()) + " at position " + String.valueOf(position));
	}
	*/
	
	private void setCustomSelectionCAB() {
    	Selection.selectAll((Spannable) mTextView.getText());
		mTextView.setCustomSelectionActionModeCallback(new Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				// TODO Auto-generated method stub
				//Log.d(TAG, "setCustomSelectionCAB: onCreateActionMode");
				mode.getMenuInflater().inflate(R.menu.contextual_menu, menu);
				mode.setTitle("");
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				// TODO Auto-generated method stub
				//Log.d(TAG, "setCustomSelectionCAB: onPrepareActionMode");
				// Remove the "select all" option
				menu.removeItem(android.R.id.selectAll);
				return true;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode,
					MenuItem item) {
				//Log.d(TAG, "setCustomSelectionCAB: onActionItemClicked");
				switch (item.getItemId()) {
				case R.id.item_play:
					//Log.d(TAG, "Play selected text in " + mVersion);
					//Log.d("HZ", "Play selected text in " + String.valueOf(selected_listview));
					if (selected_listview == 1) {
						ttobj.setLanguage(Locale.CHINESE);
						ttobj.speak(selectedText(), TextToSpeech.QUEUE_FLUSH, null);
					} 					
					else if (selected_listview == 2) {
						ttobj.setLanguage(Locale.US);
						ttobj.speak(selectedText(), TextToSpeech.QUEUE_FLUSH, null);
					}
						
					mode.getMenu().getItem(0).setVisible(false); //Copy
					mode.getMenu().getItem(2).setVisible(true);  //Pause
					mode.getMenu().getItem(3).setVisible(false);  //Share
					return true;
				case R.id.item_pause:
					ttobj.stop();
					return true;
				case R.id.item_share:
					//Log.d(TAG, "mode size = " + String.valueOf(mode.getMenu().size()));
					Intent i=new Intent(android.content.Intent.ACTION_SEND);
					i.setType("text/plain");
					i.putExtra(android.content.Intent.EXTRA_SUBJECT, title);
					i.putExtra(android.content.Intent.EXTRA_TEXT, selectedText() + " (" + title + ")");
					startActivity(Intent.createChooser(i,"Share via"));
					mode.finish();  //Action is executed, close the CAB
					return true;
				default:	
					return false;
				}
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				ttobj.stop();
				//Log.d(TAG, "setCustomSelectionCAB: onDestroyActionMode");
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
