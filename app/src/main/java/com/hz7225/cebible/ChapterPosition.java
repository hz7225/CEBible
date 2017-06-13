package com.hz7225.cebible;

import android.app.Activity;

/* This is a utility class to calculate Book and Chapter number 
 * from the ViewPager position number which starts from 0 to the 
 * total number of chapters in the whole Bible minus 1. The chapter 
 * number data is taken from the XML resources defined in values.xml
 */

public class ChapterPosition {

	Activity mActivity;
	public int mPosition; // from 0 to 1188
	public int mBook;  // from 1 to 66
	public int mChapter; // from 1 to something
	private int[] num_of_chapters = new int[66]; //Number of chapters for each book
	private int[] total_chapters = new int[66];  //Summed total chapter number
	
	public ChapterPosition(Activity activity, int position) {		
		mActivity = activity;		
		mPosition = position;
		
		populateDataFromXML();
		
		//add offset to start at 1
		int p = position + 1;
				
		if ( p <= total_chapters[0] ) { 
			mBook = 1; mChapter = p;
		}
		for (int i = 0; i < 66; i++) {
			if ( (p > total_chapters[i]) &&  (p <= total_chapters[i+1]) ) {
				mBook = i+2; mChapter = p - total_chapters[i];
			}
		}		
	}
	
	public ChapterPosition(Activity activity, int book, int chapter) {
		mActivity = activity;
		mBook = book;
		mChapter = chapter;
		
		populateDataFromXML();
		
		if (book == 1) {
			mPosition = chapter - 1;
		} else if (book <= 66) {
			mPosition = total_chapters[book-2] + chapter-1;
		} else {
			//error
		}
	}
	
	public int getBook() {
		return mBook;
	}
	
	public int getChapter() {
		return mChapter;
	}
	
	public int getPosition() {
		return mPosition;
	}
	
	/* populate the num_od_chapters[] and total_chapters[] arrays from data defined in XML */
	private void populateDataFromXML() {
		String[] ot;
		String[] nt;

		ot = mActivity.getResources().getStringArray(R.array.old_testament);
		nt = mActivity.getResources().getStringArray(R.array.new_testament);
		
		//populate the num_of_chapters array
		for (int i=0; i<ot.length; i++) {
    		String num = ot[i].substring(ot[i].indexOf(",")+1, ot[i].length());
    		num_of_chapters[i] = Integer.parseInt(num);
    	}
		for (int i=0; i<nt.length; i++) {
    		String num = nt[i].substring(nt[i].indexOf(",")+1, nt[i].length());
    		num_of_chapters[i+39] = Integer.parseInt(num);
    	}
				
		//populate the total chapter number
		total_chapters[0] =  num_of_chapters[0];
		for (int i=1; i<66; i++) {
			total_chapters[i] =  total_chapters[i-1] + num_of_chapters[i];
		}
	}
}
