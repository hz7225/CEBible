package com.hz7225.cebible;

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.callback.Callback;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Selection;
import android.text.Spannable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ListDataAdapter extends BaseAdapter {
	String TAG = "MyDataAdapter";
	
	private List<String> listData;
	private LayoutInflater layoutInflater;
	public int selected_item;
	Context mContext;
	Drawable orig_color;
	
	public ListDataAdapter(Context context, List<String> listData) {
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
		//Log.d(TAG, "vg = " + vg);
		
		ViewHolder holder;
		if (v == null) {
			//Log.d(TAG, "create new view at position = " + String.valueOf(position));
			holder = new ViewHolder();
			if (mContext instanceof com.hz7225.cebible.DisplayActivity) {
				v = layoutInflater.inflate(R.layout.list_item, null);
				holder.textView = (TextView) v.findViewById(R.id.text);
				holder.textView.setTextIsSelectable(true);
				holder.textView.setTextSize(18);
			}
			if (mContext instanceof com.hz7225.cebible.CEBible_MainActivity) {
				v = layoutInflater.inflate(R.layout.list_item, null);
				holder.textView = (TextView) v.findViewById(R.id.text);
				holder.textView.setTextSize(15);
			}
			v.setTag(holder);
		} else {
			//Log.d(TAG, "use recycled view at position = " + String.valueOf(position));
			holder = (ViewHolder) v.getTag();
		}
		
		holder.textView.setText(listData.get(position));
		
		//Log.d(TAG, "position = " + String.valueOf(position) + " selected_item = " + String.valueOf(this.selected_item) + "  this=" + String.valueOf(this));
		
		// Set color for selected item
		if (position == this.selected_item ) {
			if (mContext instanceof com.hz7225.cebible.DisplayActivity) {
				//Log.d(TAG, "set TextIsSelectable to true");
			} else {
				v.setBackgroundColor(0xff398eb5);
				holder.textView.setTextColor(Color.WHITE);
			}
		} else {
			if (mContext instanceof com.hz7225.cebible.DisplayActivity) {
				//Log.d(TAG, "set TextIsSelectable to false");
			} else {
				v.setBackground(orig_color);
				holder.textView.setTextColor(Color.BLACK);
			}	
		}
		
		
		return v;
	}
	
	static class ViewHolder {
		
			TextView textView;
	
	}
	
}

