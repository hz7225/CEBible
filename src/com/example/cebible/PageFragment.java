package com.example.cebible;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class PageFragment extends Fragment {
	
	static String TAG = "PageFragment";
	
	/**
     * The argument key for the page number this fragment represents.
     */
    public static final String ARG_PAGE = "page";
	
	private int mPageNumber;
	static private DataBaseHelper mBibleDB;
	static private int mBook;
	//static private int mChapter;
	
	//public static PageFragment create(int pageNumber, DataBaseHelper BibleDB, int book, int chapter) {
	public static PageFragment create(int pageNumber, DataBaseHelper BibleDB, int book) {
		Log.d(TAG, "pageNumber = " + String.valueOf(pageNumber) + " book=" +String.valueOf(book));
		mBibleDB = BibleDB;
        mBook = book;
        //mChapter = chapter;
        
		PageFragment fragment = new PageFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, pageNumber);        
        fragment.setArguments(args);
        return fragment;
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPageNumber = getArguments().getInt(ARG_PAGE);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "mPageNumber = " + String.valueOf(mPageNumber) + " mChapter = " + String.valueOf(mChapter));
		Log.d(TAG, "mPageNumber = " + String.valueOf(mPageNumber));
		
        //Get the whole chapter of a book from database
		int chapter = mPageNumber + 1;
        List<String> sl = mBibleDB.getChapter(mBook, chapter);
        
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
	}

	public int getPageNumber() {
        return mPageNumber;
    }
}
