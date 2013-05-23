package edu.ntu.arbor.sbchao.androidlogger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import edu.ntu.arbor.sbchao.androidlogger.scheme.ActivityLog;

public class DiaryListFragment extends Fragment {
	private ListView mListView;
	private TimelineAdapter mAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_diary_list, container,
				false);
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
		for (ActivityLog log : logs) {
			long startTime = log.getStartTime().getTime();
			long endTime = log.getEndTime().getTime();
			if (prevEndTime != -1) {
				double span = startTime - prevEndTime;
				durations.add(span);
			}
			double duration = endTime - startTime;
			durations.add(duration);
			prevEndTime = endTime;
		}

		SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm",
				Locale.TAIWAN);
		// String formattedDateString = dateFormat.format(new java.util.Date());

		// DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
		List<Map<String, Object>> listData = new ArrayList<Map<String, Object>>();
		for (int i = 0; i < durations.size(); i++) {
			Map<String, Object> item = new HashMap<String, Object>();
			item.put("type_icon", (i % 2 == 0) ? getActivityType(logs
					.get(i / 2).getActivityName()) : "");
			item.put("type_text", (i % 2 == 0) ? logs.get(i / 2)
					.getActivityName() : "");
			item.put("activity",
					(i % 2 == 0) ? (logs.get(i / 2).getActivityComment()) : "");
			item.put(
					"duration",
					(i % 2 == 0) ? dateFormat.format(logs.get(i / 2)
							.getStartTime())
							+ " ~ "
							+ dateFormat.format(logs.get(i / 2).getEndTime())
							: "");
			item.put("height", (double) durations.get(i) / 24000);
			listData.add(item);
		}
		mAdapter = new TimelineAdapter(getActivity(), listData,
				R.layout.listitem_diary, new String[] { "type_icon",
						"type_text", "activity", "duration" }, new int[] {
						R.id.imageView_type, R.id.textView_type,
						R.id.textView_activity, R.id.textView_duration });
		mListView.setAdapter(mAdapter);
	}

	private int getActivityType(String activityName) {
		if (activityName.equals("研究"))
			return R.drawable.ic_research;
		else if (activityName.equals("吃飯"))
			return R.drawable.ic_eat;
		else if (activityName.equals("休閒"))
			return R.drawable.ic_relax;
		else if (activityName.equals("運動"))
			return R.drawable.ic_exercise;
		else if (activityName.equals("出遊"))
			return R.drawable.ic_travel;
		else if (activityName.equals("搭交通工具"))
			return R.drawable.ic_commute;
		else
			return R.drawable.ic_research;
	}

	private class TimelineAdapter extends SimpleAdapter {
		private List<? extends Map<String, ?>> data;

		public TimelineAdapter(Context context,
				List<? extends Map<String, ?>> data, int resource,
				String[] from, int[] to) {
			super(context, data, resource, from, to);
			this.data = data;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			double height = (Double) data.get(position).get("height");
			convertView = super.getView(position, convertView, parent);
			if (position % 2 == 1) {
				convertView
						.setLayoutParams(new ListView.LayoutParams(
								RelativeLayout.LayoutParams.MATCH_PARENT,
								(int) height));
				RelativeLayout eventBgLayout = (RelativeLayout) convertView.findViewById(R.id.layout_event_bg);
				eventBgLayout.setBackgroundResource(R.drawable.bg_event_span);
				RelativeLayout eventLayout = (RelativeLayout) convertView.findViewById(R.id.layout_event);
				eventLayout.setVisibility(View.INVISIBLE);
			}

			return convertView;
		}
	}
}