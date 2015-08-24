package com.hz7225.cebible;

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.callback.Callback;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ListDataAdapter2 extends BaseAdapter {
	String TAG = "MyDataAdapter2";
	
	private List<SpannableString> listData;
	private LayoutInflater layoutInflater;
	public int selected_item;
	Context mContext;
	Drawable orig_color;
	
	public ListDataAdapter2(Context context, List<SpannableString> listData) {
		mContext = context;
		this.listData = listData;
		layoutInflater = LayoutInflater.from(context);
		View v = layoutInflater.inflate(R.layout.list_item, null);
		orig_color = v.getBackground();
		selected_item = -1; // -1 means unselected
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
		//Log.d(TAG, "vg = " + vg + " position=" + String.valueOf(position));
		
		ViewHolder holder;
		if (v == null) {
			//Log.d(TAG, "create new view at position = " + String.valueOf(position));
			holder = new ViewHolder();
			v = layoutInflater.inflate(R.layout.list_item2, null);
			holder.tvScripture = (TextView) v.findViewById(R.id.scripture);
			//holder.tvScripture.setTextIsSelectable(true);
			holder.tvScriptureNum = (TextView) v.findViewById(R.id.scripture_number);
			v.setTag(holder);
		} else {
			//Log.d(TAG, "use recycled view at position = " + String.valueOf(position));
			holder = (ViewHolder) v.getTag();
		}
		
		holder.tvScriptureNum.setText(String.valueOf(position+1));
		holder.tvScripture.setText(listData.get(position));
			
		return v;
	}
	
	static class ViewHolder {
			TextView tvScriptureNum;
			TextView tvScripture;	
	}
	
}
