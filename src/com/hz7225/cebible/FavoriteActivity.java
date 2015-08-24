package com.hz7225.cebible;

import java.util.ArrayList;
import java.util.HashMap;
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
import android.util.Log;
import android.view.LayoutInflater;
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

public class FavoriteActivity extends Activity {
	
	String TAG = "FavoriteActivity";
	
	ArrayList<ScriptureData> item;
	ListView listview;	
	MyDataAdapter adapter;
	List<ScriptureData> list;
	List<ScriptureData> sublist;
	LinkedHashMap<String, Integer> hm;
	Spinner sp;

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
		actionBar.setTitle(getString(R.string.myfavorites));
		
    	doSearch();
    }
    
    protected void onResume() {
		super.onResume();
		
		doSearch();
    }	
    
    private void launchDisplayActivity(int book, int chapter, int verse, String language) {
		Intent intent = new Intent(this, DisplayActivity.class);
		intent.putExtra("PARENT", "FavoriteActivity");
		intent.putExtra("BOOK", book);
		intent.putExtra("CHAPTER", chapter);
		intent.putExtra("VERSE", verse);
		intent.putExtra("LANGUAGE", language);
		startActivity(intent);
	}
    
    private void doSearch() {
    	//Log.d(TAG, "doSearch()");
    	
    	//Get the complete list of data from the Notebook database
    	NotebookDbHelper mDbHelper = new NotebookDbHelper(this.getApplicationContext());
    	list = mDbHelper.getScriptureDataList();
    	
    	//Prepare a sublist for displaying the result in a ListView
    	sublist = new ArrayList<ScriptureData>();
    	getSublist(list, "ALL"); //All means result from the whole book
    	
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
    			//Integer new_val = (Integer)hm.get(scripture.getBookName()) + 1;
    			hm.put(scripture.getBookName(), (Integer)hm.get(scripture.getBookName()) + 1);
    		} else {
    			hm.put(scripture.getBookName(), 1);
    		}
    	}   
    	
    	//filter is a list of Strings that consist of book name and the number of found verses in the book
    	//It is used in the list of Spinner for the search result summary
    	List<String> filter = new ArrayList();
    	filter.add(getString(R.string.wholebook) + " (" + list.size() + ")");    
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
    	//return sublist;
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
