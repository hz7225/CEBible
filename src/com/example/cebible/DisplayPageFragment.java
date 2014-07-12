package com.example.cebible;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.ActionBar.LayoutParams;
import android.app.Fragment;
import android.app.ListFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

//public class PageFragment extends ListFragment {  //for simple list fragment
public class DisplayPageFragment extends Fragment implements OnItemLongClickListener {
	
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
	//static private int mChapter;
	
	ListView listView1;
	ListView listView2;
	ListDataAdapter adapter1;
	ListDataAdapter adapter2;
	
	boolean isLeftListEnabled = true;
	boolean isRightListEnabled = true;
	
	public static DisplayPageFragment create(int pageNumber, int book, int verse, String version) {
		Log.d(TAG, "PageFragment::create(), pageNumber = " + String.valueOf(pageNumber) + " book=" +String.valueOf(book));
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
	}
	
	private List<String> getScriptureFromDB(int book, int chapter, String db) {
		Log.d(TAG, "getScriptureFromDB: " + db + "ddddddddbbbbbbb");
		//DataBaseHelper BibleDB = new DataBaseHelper(getActivity().getApplicationContext() );
		DataBaseHelper BibleDB = new DataBaseHelper(getActivity().getApplicationContext(), db);
		//BibleDB.setDB(db);
        //Get the whole chapter of a book from database
        List<String> sl = BibleDB.getChapter(mBook, chapter);
        for (int i=0; i<sl.size(); i++) {
        	sl.set(i, "[" + String.valueOf(i+1) + "]" + sl.get(i));
        }
        return sl;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "mPageNumber = " + String.valueOf(mPageNumber) + " mChapter = " + String.valueOf(mChapter));
		Log.d(TAG, "onCreateView(): mPageNumber = " + String.valueOf(mPageNumber) + " mVersion = " + mVersion);
		
        /*//Use TextView
        //Build the String and display it in TextView
        StringBuilder str = new StringBuilder();
		for (int i =0; i<sl.size(); i++) {
			//Log.d(TAG, "["+String.valueOf(i+1)+"]" + sl.get(i));
			str.append("["+String.valueOf(i+1)+"]" + sl.get(i) + "\n");
		}		
		
		// Inflate the layout containing a title and body text.
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.activity_display, container, false);
		((TextView) rootView.findViewById(R.id.display_text)).setText(str);
		//((TextView) rootView.findViewById(R.id.display_text)).setText("Page " + String.valueOf(mPageNumber));
		
		return rootView;
		*/
        
        /*
        //Use 1 simple ListView
        for (int i=0; i<sl.size(); i++) {
        	sl.set(i, "[" + String.valueOf(i+1) + "]" + sl.get(i));
        }
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
				R.layout.textview, sl);
		setListAdapter(adapter);
		
		ListView rootView = (ListView) inflater.inflate(R.layout.listview, container, false);
		
		return rootView;
		*/
		
        //Use 2 ListViews in Linear layout
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.listviews, container, false);
        
        //ListView1 for Chinese CUVS
        listView1 = (ListView) rootView.findViewById(R.id.listView1);
        listView1.setOnItemLongClickListener((OnItemLongClickListener) this); //Set listener
        List<String> sl1 = getScriptureFromDB(mBook, mPageNumber+1, "cuvslite.bbl.db");
    	adapter1 = new ListDataAdapter(this.getActivity(), sl1); 
    	//Set ListView adapters
	    listView1.setAdapter(adapter1);
	    //Set starting position
    	listView1.setSelection(mVerse);
        
    	//ListView2 for English KJV
    	listView2 = (ListView) rootView.findViewById(R.id.listView2);    	
    	listView2.setOnItemLongClickListener((OnItemLongClickListener) this);
    	List<String> sl2 = getScriptureFromDB(mBook, mPageNumber+1, "EB_kjv_bbl.db");
    	adapter2 = new ListDataAdapter(this.getActivity(), sl2);
    	//Set ListView adapters
    	listView2.setAdapter(adapter2);
    	//Set starting position
    	listView2.setSelection(mVerse);
    	
    	/*
    	//Test: Change the ListViews layout_weight parameter
    	LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
    		    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    	params.weight = 1.0f;
    	listView2.setLayoutParams(params);
    	*/
    	
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
	
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {	
		//Toast.makeText(getActivity(), "Clicked", Toast.LENGTH_SHORT).show();
		return true;  //return true consumes the event so it won't be picked up by the normal onItemClick
	}
}
