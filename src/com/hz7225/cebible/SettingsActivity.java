package com.hz7225.cebible;

import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {	
	String TAG = "SettingsActivity";
	
	static final String PREFS_NAME = "CEBIBLE";
	
	String[] ch_translations = new String[] {
	        "简体和合本",
	        "繁體和合本",
	    };
	String[] en_translations = new String[] {
	        "King James Version",
	        "World English Bible",
	    };
	
	SharedPreferences prefs;
    String prefsLanguage;
    String prefsEN_Trans;
    String prefsCH_Trans;
    
    Locale myLocale;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getActionBar().setTitle(R.string.action_settings);
		
		setContentView(R.layout.activity_settings);
		
		prefs  = getSharedPreferences("BiblePreferences", MODE_PRIVATE);
		prefsEN_Trans = prefs.getString("EN_TRANS", getString(R.string.kjv));
    	prefsCH_Trans = prefs.getString("CH_TRANS", getString(R.string.cuvt));
		
    	//Listview1 is for Chinese
        ListView listView1 = (ListView) findViewById(R.id.lv_ch_translations); 
        ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice,ch_translations); 
        listView1.setAdapter(adapter1);
        //check the appropriate Chinese translation according to saved preferences
        if (prefsCH_Trans.equals(getString(R.string.cuvs))) {
        	//Log.d(TAG, "Settings: default to simplified Chinese");
        	listView1.setItemChecked(0, true);
		} else if (prefsCH_Trans.equals(getString(R.string.cuvt))) {
			//Log.d(TAG, "Settings: default to traditional Chinese");
			listView1.setItemChecked(1, true);
		}			        
        
        //Listview2 is for English
        ListView listView2 = (ListView) findViewById(R.id.lv_en_translations); 
        ArrayAdapter<String> adapte2 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, en_translations);
        listView2.setAdapter(adapte2);
        //check the appropriate English translation according to saved preferences
        if (prefsEN_Trans.equals(getString(R.string.kjv))) {
        	listView2.setItemChecked(0, true);
		} else if (prefsEN_Trans.equals(getString(R.string.web))) {
			listView2.setItemChecked(1, true);
		}
        
        listView1.setOnItemClickListener(new OnItemClickListener() {
        	@Override
        	public void onItemClick(AdapterView<?> parent, View view,int position, long id) {
        		//Log.d(TAG, "LV1: onItemClicked, position " + String.valueOf(position));
        		Editor editor = prefs.edit();
        		switch (position) {
        		case 0:        
        			setLocale("zh", "CN");
        			prefsCH_Trans = getString(R.string.cuvs);
        			editor.putString("CH_TRANS", prefsCH_Trans);
        			break;
        		case 1:
        			setLocale("zh", "TW");
        			prefsCH_Trans = getString(R.string.cuvt);
        			editor.putString("CH_TRANS", prefsCH_Trans);
        			break;
        		}
        		editor.commit();
        		getActionBar().setTitle(R.string.action_settings); // Update the ActionBar menu with the correct language
        		//Log.d(TAG, "menu size = " + String.valueOf(mMenu.size()));
        		TextView tv = (TextView)findViewById(R.id.tv_ch_translations);
        		tv.setText(R.string.select_chinese_translation);
        	}
        }); 
        listView2.setOnItemClickListener(new OnItemClickListener() {
        	@Override
        	public void onItemClick(AdapterView<?> parent, View view,int position, long id) {
        		//Log.d(TAG, "LV2: onItemClicked, position " + String.valueOf(position));
        		Editor editor = prefs.edit();
        		switch (position) {
        		case 0:        			
        			prefsEN_Trans = getString(R.string.kjv);
        			editor.putString("EN_TRANS", prefsEN_Trans);
        			break;        		
        		case 1:
        			prefsEN_Trans = getString(R.string.web);
        			editor.putString("EN_TRANS", prefsEN_Trans);
        			break;	
        		}
        		editor.commit();
        	}
        }); 
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}
	
	public void setLocale(String lang, String country) {
		myLocale = new Locale(lang, country);
		Resources res = getResources();
		DisplayMetrics dm = res.getDisplayMetrics();
		Configuration conf = res.getConfiguration();
		conf.locale = myLocale;
		res.updateConfiguration(conf, dm);
	}
}
