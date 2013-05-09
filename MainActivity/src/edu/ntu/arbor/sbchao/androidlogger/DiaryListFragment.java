package edu.ntu.arbor.sbchao.androidlogger;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import edu.ntu.arbor.sbchao.androidlogger.scheme.ActivityLog;

public class DiaryListFragment extends Fragment {
	private ListView mListView;
	private TimelineAdapter mAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_diary_list, container, false);
		mListView = (ListView) view.findViewById(R.id.listView);
		mListView.setAdapter(mAdapter);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	public void update(List<ActivityLog> logs) {
		List<Double> durations = new ArrayList<Double>();
		long prevEndTime = -1;
		double min = Double.MAX_VALUE;
		for (ActivityLog log : logs) {
			long startTime = log.getStartTime().getTime();
			long endTime = log.getEndTime().getTime();
			if (prevEndTime != -1) {
				double span = startTime - prevEndTime;
				if (span < 0) {
					span = 0;
				} else {
					min = Math.min(min, span);
				}
				durations.add(span);
			}
			double duration = endTime - startTime;
			min = Math.min(min, duration);
			durations.add(duration);
			prevEndTime = endTime;
		}

		DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
		List<Map<String, Object>> listData = new ArrayList<Map<String, Object>>();
		for (int i = 0; i < durations.size(); i++) {
			Map<String, Object> item = new HashMap<String, Object>();
			item.put("activity", (i % 2 == 0) ? logs.get(i / 2).getActivityName() : "");
			item.put("duration", (i % 2 == 0) ? dateFormat.format(logs.get(i / 2).getStartTime()) + " ~ "
					+ dateFormat.format(logs.get(i / 2).getEndTime()) : "");
//			Log.d("!!", String.valueOf(durations.get(i)));
			item.put("height", (double) durations.get(i) / (double) min);
			listData.add(item);
		}
		mAdapter = new TimelineAdapter(getActivity(), listData, R.layout.listitem_diary, new String[] {
				"activity", "duration" }, new int[] { R.id.textView_activity, R.id.textView_duration });
		mListView.setAdapter(mAdapter);
	}

	private class TimelineAdapter extends SimpleAdapter {
		private List<? extends Map<String, ?>> data;

		public TimelineAdapter(Context context, List<? extends Map<String, ?>> data, int resource,
				String[] from, int[] to) {
			super(context, data, resource, from, to);
			this.data = data;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
//			if (convertView == null) {
				convertView = super.getView(position, convertView, parent);
				double height = (Double) data.get(position).get("height");
				if (position % 2 == 1) {
					TextView durationTextView = (TextView) convertView.findViewById(R.id.textView_duration);
					TextView activityTextView = (TextView) convertView.findViewById(R.id.textView_activity);
					ImageView lineImageView = (ImageView) convertView.findViewById(R.id.imageView_line);
					durationTextView.setBackgroundColor(Color.rgb(226, 222, 216));
					activityTextView.setBackgroundColor(Color.rgb(226, 222, 216));
					lineImageView.setImageResource(R.drawable.duration_line);
				} else {
					TextView durationTextView = (TextView) convertView.findViewById(R.id.textView_duration);
					TextView activityTextView = (TextView) convertView.findViewById(R.id.textView_activity);
					ImageView lineImageView = (ImageView) convertView.findViewById(R.id.imageView_line);
					durationTextView.setBackgroundColor(Color.rgb(74, 82, 128));
					activityTextView.setBackgroundColor(Color.rgb(229, 216, 171));
					lineImageView.setImageResource(R.drawable.activity_line);
				}
				convertView.setLayoutParams(new ListView.LayoutParams(
						RelativeLayout.LayoutParams.MATCH_PARENT, (int) (height < 1 ? 1 : ((int) (height * 80)))));
//			}
			return convertView;
		}
	}
}