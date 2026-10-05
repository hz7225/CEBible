package com.hz7225.cebible;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.ActionBar;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

public class SearchActivity extends Activity {
	
	String TAG = "SearchResultsActivity";
	
	ArrayList<ScriptureData> item;
	ListView listview;	
	MyDataAdapter adapter;
	List<ScriptureData> list;
	List<ScriptureData> sublist;
	LinkedHashMap<String, Integer> hm;
	Spinner sp;
	
	SharedPreferences prefs;
	String prefsLanguage;
    String prefsEN_Trans;
    String prefsCH_Trans;

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
       	//Log.d(TAG, "onCreate");
       	setContentView(R.layout.search_result_layout);    
       	listview = (ListView) findViewById(R.id.listViewSearch);
       	
       	listview.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				//Log.d(TAG, "Searched results long clicked position: " + String.valueOf(position));
				return true;
			}			    		
    	});
    	
    	listview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				//Log.d(TAG, "Searched Results short clicked position: " + String.valueOf(position));
				launchDisplayActivity(Integer.parseInt(sublist.get(position).getBook()), 
									Integer.parseInt(sublist.get(position).getChapter()), 
									Integer.parseInt(sublist.get(position).getVerse()), 
									getString(R.string.ch));
			}			    		
    	});
       	
       	sp = (Spinner) findViewById(R.id.spinner1);
       	
       	ActionBar actionBar = getActionBar();
		if (actionBar != null) {
			actionBar.setTitle(getString(R.string.searchresult));
			actionBar.setDisplayHomeAsUpEnabled(true);
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
				actionBar.setHomeAsUpIndicator(R.drawable.ic_home_up_padded);
			}
			actionBar.setIcon(R.drawable.ic_actionbar_icon);
		}
		
		// Get saved preferences
    	prefs  = getSharedPreferences("BiblePreferences", MODE_PRIVATE);
    	prefsLanguage = prefs.getString("LANGUAGE", getString(R.string.ch));
    	prefsEN_Trans = prefs.getString("EN_TRANS", getString(R.string.kjv));
    	prefsCH_Trans = prefs.getString("CH_TRANS", getString(R.string.cuvs));
       	
        handleIntent(getIntent());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
    	//Log.d(TAG, "onNewIntent");   	
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
    	//Log.d(TAG, "intent = " + intent);    	
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            //This Intent is sent from the SearchView widget createdin the DisplayActivity
            String query = intent.getStringExtra(SearchManager.QUERY);        
            //use the query to search your data somehow
            //Log.d(TAG, "Intent.ACTION_SEARCH query extra = " + query);
            doSearch(query);
        }
        else if (intent.getComponent().getClassName().equalsIgnoreCase("com.hz7225.cebible.SearchActivity")) {
            //This Intent is sent from the DisplayPageFragment class for the selected text
            String query = intent.getStringExtra("SEARCH_STR");        
            //use the query to search your data somehow
            //Log.d(TAG, "Intent SearchActivity query extra = " + query);
            doSearch(query); 
        }        
    }
    
    private void launchDisplayActivity(int book, int chapter, int verse, String language) {
		Intent intent = new Intent(this, DisplayActivity.class);
		intent.putExtra("PARENT", "SearchActivity");
		intent.putExtra("BOOK", book);
		intent.putExtra("CHAPTER", chapter);
		intent.putExtra("VERSE", verse);
		intent.putExtra("LANGUAGE", language);
		startActivity(intent);
	}

	//This method parses the '&' separated input String into a List of Strings
	private List<String> parseSearchString(String str) {

		List<String> strList = new ArrayList<String>();;

		//Find out the total number of search phrases separated by '&'
		int num = 1;
		String temp = str;
		int i = temp.indexOf("&");
		while (i != -1) {
			temp = temp.substring(i+1, temp.length());
			i = temp.indexOf("&");
			num += 1;
		}
		Log.d("TAG", "Total number of search phrases = " + String.valueOf(num));

		//Parse the input search string into a List of Strings
		i = str.indexOf("&");
		while (i != -1) {
			//ss = search_str.substring(0, i);
			strList.add(str.substring(0, i).trim());  //The trim() removes the leading and trailing spaces
			str = str.substring(i+1, str.length());
			i = str.indexOf("&");
		}
		strList.add(str.trim()); //Don't forget to add this one

		return strList;
	}

    private void doSearch(String queryStr) {
    	//Log.d(TAG, "doSearch = " + queryStr);
    	
    	//Save search queries in content provider for later suggestions
    	SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                SearchSuggestionProvider.AUTHORITY, SearchSuggestionProvider.MODE);
        suggestions.saveRecentQuery(queryStr, null);
    	
    	ActionBar actionBar = getActionBar();
    	actionBar.setTitle(getString(R.string.searchresult) + " (" + queryStr.trim() + ")");
    	
    	//Set the correct database for search
    	prefsEN_Trans = prefs.getString("EN_TRANS", getString(R.string.kjv));
    	prefsCH_Trans = prefs.getString("CH_TRANS", getString(R.string.cuvs));
    	String db;
    	if (DisplayPageFragment.in_lv1_or_lv2 == 1) {
    		if (prefsCH_Trans.equals(getString(R.string.cuvs))) 
    			db = "CB_cuvslite.bbl.db";
    		else 
    			db = "CB_cuvtlite.bbl.db";
    	} else {
    		if (prefsEN_Trans.equals(getString(R.string.kjv))) 
    			db = "EB_kjv_bbl.db";
    		else 
    			db = "EB_web_bbl.db";
    	}
    	DataBaseHelper BibleDB = new DataBaseHelper(this, db);
    	
    	//Perform search
    	//list = BibleDB.searchWholeBible(queryStr);
		list = BibleDB.searchWholeBible2(parseSearchString(queryStr));
    	
    	//Prepare a sublist for displaying the result in a ListView
    	sublist = new ArrayList<ScriptureData>();
    	getSublist(list, "ALL"); //All means result from the whole book
    	
    	//Span the query string in the searched result
    	for (ScriptureData scripture : sublist) {
    		SpannableString str = scripture.getScripture();

			List<String> qlist = parseSearchString(queryStr);
			for (String qstr: qlist) {
				int first = str.toString().indexOf(qstr);
				str.setSpan(new BackgroundColorSpan(0x30007FFF), first, first + qstr.length(), 0);
				scripture.setScripture(str);
			}

    		//int first = str.toString().indexOf(queryStr);
			//if (first < 0) {
				/*The search result of db.rawQuery(selectQuery, null) method in DataBaseHelper.java is case insensitive.
				  So the index 'first' can be -1, which will cause the Activity to crash in the following
				  str.setSpan() call. The real fix is to make the SQL search case sensitive. I have tried to add
				  'COLLATE SQL_Latin1_General_CP1_CS_AS' at the end of the search string, but it didn't work.
				*/
				//Log.d(TAG, "index=" + String.valueOf(first)+ ": " + "'" + queryStr + "'" + " not found in: " + str);
			//} else {
			//	str.setSpan(new BackgroundColorSpan(0x30007FFF), first, first + queryStr.length(), 0);
			//	scripture.setScripture(str);
			//}
    	}
    	
    	//Display the detail search result in the ListView
    	adapter = new MyDataAdapter(this, sublist);
    	listview.setAdapter(adapter); 

    	// Use the LinkedHashMap in the filter Spinner for narrowing the search result
    	hm = new LinkedHashMap();
    	
    	// This for loop works the same as the one below, just the traditional way
    	//for (int i=0; i < list.size(); i++) {
    	//}
    	for (ScriptureData scripture : list) {
    		//Log.d(TAG, "Found: " + scripture.getBookName() + " " + scripture.getChapter() + ":" + scripture.getVerse() + " " + scripture.getScripture());    		
    		if (hm.containsKey(scripture.getBookName())) {
    			Integer new_val = (Integer)hm.get(scripture.getBookName()) + 1;
    			hm.put(scripture.getBookName(), (Integer)hm.get(scripture.getBookName()) + 1);
    		} else {
    			hm.put(scripture.getBookName(), 1);
    		}
    	} 
    	
    	//
    	String searchedBible = "";
    	if (prefsLanguage.equals(getString(R.string.en))) {
			if (prefsEN_Trans.equals(getString(R.string.kjv))) {
				searchedBible=getString(R.string.kjv);
			} 	else if (prefsEN_Trans.equals(getString(R.string.web))) {
				searchedBible=getString(R.string.web);
			} 		
		} else {	
			//item.setTitle(getString(R.string.cuv));
			if (prefsCH_Trans.equals(getString(R.string.cuvs))) {
				searchedBible=getString(R.string.cuvs);
    		} else if (prefsCH_Trans.equals(getString(R.string.cuvt))) {
    			searchedBible=getString(R.string.cuvt);
    		}
		}
    	
    	//filter is a list of Strings that consist of book name and the number of found verses in the book
    	//It is used in the list of Spinner for the search result summary
    	List<String> filter = new ArrayList();
    	filter.add(searchedBible + " " + getString(R.string.wholebook) + " (" + list.size() + ")");    
        Set set = hm.entrySet();  // Get a set of the entries        
        Iterator i = set.iterator();  // Get an iterator        
        while(i.hasNext()) {  // Display elements
           Map.Entry me = (Map.Entry)i.next();           
           filter.add(me.getKey() + " (" + me.getValue() + ")");
           //Log.d(TAG, "filter: " + me.getKey() + " (" + me.getValue() + ")");
        }
        
        //Display the summary of search result in the Spinner which also used for setting display filter
        ArrayAdapter<String> adapter_spinner = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, filter);
        adapter_spinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapter_spinner);    
        sp.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				String key;
				if (position == 0) {
					key = "ALL";
				} else {	
					key = (new ArrayList<String>(hm.keySet())).get(position - 1);
				}	
				//Log.d(TAG, "Spinner position = " + String.valueOf(position) + " key = " + key);
				getSublist(list, key);
				adapter.notifyDataSetChanged();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub				
			}        	
        });
        
	
    }
    
    // Get a sublist of the searched scriptures in a particular book
    // The 2nd parameter can be either book number or book name
    //private List<ScriptureData> getSublist(List<ScriptureData> list, String book) {
    private void getSublist(List<ScriptureData> list, String book) {
    	//List<ScriptureData> sublist = new ArrayList(); 
    	sublist.clear();
    	for (ScriptureData scripture : list) {
    		if (book.equals("ALL")) {
    			sublist.add(scripture);
    		}
    		else if (scripture.getBook().equals(book) || scripture.getBookName().equals(book)) {
    			sublist.add(scripture);
    		}
    	}    	
    }
    
    private class MyDataAdapter extends BaseAdapter {

    	private List<ScriptureData> listData;
    	private LayoutInflater layoutInflater;

    	public MyDataAdapter(Context context, List<ScriptureData> listData) {
    		this.listData = listData;
    		layoutInflater = LayoutInflater.from(context);
    	}

    	@Override
    	public int getCount() {
    		return listData.size();
    	}

    	@Override
    	public Object getItem(int position) {
    		return listData.get(position);
    	}

    	@Override
    	public long getItemId(int position) {
    		return position;
    	}

    	@Override
    	public View getView(int position, View v, ViewGroup vg) {
    		ViewHolder holder;
    		if (v == null) {
    			v = layoutInflater.inflate(R.layout.search_result_listitem, null);
    			holder = new ViewHolder();
    			holder.titleView = (TextView) v.findViewById(R.id.search_result_title);
    			holder.scriptureView = (TextView) v.findViewById(R.id.search_result_scripture);

    			v.setTag(holder);
    		} else {
    			holder = (ViewHolder) v.getTag();
    		}

    		holder.titleView.setText(listData.get(position).getBookName() + " " + listData.get(position).getChapter() + ":" + listData.get(position).getVerse());
    		holder.titleView.setTypeface(Typeface.DEFAULT_BOLD);
    		holder.scriptureView.setText(listData.get(position).getScripture());

    		return v;
    	}

    	private class ViewHolder {
    		TextView titleView;
    		TextView scriptureView;
    	}
    }
}
