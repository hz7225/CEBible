package com.example.cebible;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ListDataAdapter extends BaseAdapter {
	String TAG = "MyDataAdapter";
	
	//private String[] listData;
	private List<String> listData;
	private LayoutInflater layoutInflater;
	//public int position; //selected position
	public int selected_item;
	
	public ListDataAdapter(Context context, List<String> listData) {
	//public MyDataAdapter(Context context, String[] listData) {
		this.listData = listData;
		layoutInflater = LayoutInflater.from(context);
		selected_item = -1; // -1 means unselected
	}

	@Override
	public int getCount() {
		//return listData.length;
		return listData.size();
	}

	@Override
	public Object getItem(int position) {
		//return listData[position];
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
			v = layoutInflater.inflate(R.layout.list_item, null);
			holder = new ViewHolder();
			holder.textView = (TextView) v.findViewById(R.id.text);			
			v.setTag(holder);
		} else {
			//Log.d(TAG, "use recycled view at position = " + String.valueOf(position));
			holder = (ViewHolder) v.getTag();
		}
		
		//holder.textView.setText(listData[position]);
		holder.textView.setText(listData.get(position));
		
		//Log.d(TAG, "position = " + String.valueOf(position) + " selected_item = " + String.valueOf(this.selected_item) + "  this=" + String.valueOf(this));
		
		// Set color for selected item
		if (position == this.selected_item ) {
			v.setBackgroundColor(0xFF00FF00);
		} else {
			v.setBackgroundColor(0xFFFFFFFF);
		}
		
		
		return v;
	}
	
	static class ViewHolder {
		TextView textView;
	}
}

