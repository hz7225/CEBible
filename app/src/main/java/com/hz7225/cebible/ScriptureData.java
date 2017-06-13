package com.hz7225.cebible;

import android.app.Activity;
import android.content.Context;
import android.text.SpannableString;

public class ScriptureData {
	//private Activity activity;
	private final Context mContext;
	private String book;  //number String between "1" and "66"
	private String chapter;
	private String verse;
	private SpannableString scripture;
	private String bookName;
	
	public ScriptureData(Context context, String b, String c, String v, SpannableString s) {
		mContext = context;
		book = b;
		chapter = c;
		verse = v;
		scripture = s;

		String[] ot = mContext.getResources().getStringArray(R.array.old_testament);
		String[] nt = mContext.getResources().getStringArray(R.array.new_testament);

		int bookNum = Integer.valueOf(book);
		if (bookNum<40) {
			bookName = ot[bookNum-1].substring(0, ot[bookNum-1].indexOf(","));
		} else {
			bookName = nt[bookNum-1-39].substring(0, nt[bookNum-1-39].indexOf(","));
		}
	}
	
	public void setBookName(String bn) {
		bookName = bn;
	}
	
	public String getBookName() {
		return bookName;
	}
	
	public void setBook(String b) {
		book = b;
	}

	public String getBook() {
		return book;
	}
	
	public void setChapter(String c) {
		chapter = c;
	}

	public String getChapter() {
		return chapter;
	}
	
	public void setVerse(String v) {
		verse = v;
	}

	public String getVerse() {
		return verse;
	}
	
	public void setScripture(SpannableString s) {
		scripture = s;
	}

	public SpannableString getScripture() {
		return scripture;
	}
	
	
}
