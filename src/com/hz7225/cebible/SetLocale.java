package com.hz7225.cebible;

import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

public class SetLocale {

	Context context;
	private String lang;
	private String country;
	
	public SetLocale(Context c) {
		context = c;
		lang = "en";
		country = "";
	}
		
	public void set() {

		SharedPreferences prefs = context.getSharedPreferences("BiblePreferences", context.MODE_PRIVATE);
		String prefsLanguage = prefs.getString("LANGUAGE", context.getString(R.string.ch));
    	String prefsCH_Trans = prefs.getString("CH_TRANS", context.getString(R.string.cuvt));
		
		if (prefsLanguage.equals(context.getString(R.string.en))) {
			lang = "en";
			country = "";
		} else if (prefsLanguage.equals(context.getString(R.string.ch))) {
    		if (prefsCH_Trans.equals(context.getString(R.string.cuvs))) {
    			lang = "zh";
    			country = "CN";
    		} else if (prefsCH_Trans.equals(context.getString(R.string.cuvt))) {
    			lang = "zh";
    			country = "TW";
    		}
    	}
		
		Locale myLocale = new Locale(lang, country);
		Resources res = context.getResources();
		DisplayMetrics dm = res.getDisplayMetrics();
		Configuration conf = res.getConfiguration();
		conf.locale = myLocale;
		res.updateConfiguration(conf, dm);
	}	
	
}
