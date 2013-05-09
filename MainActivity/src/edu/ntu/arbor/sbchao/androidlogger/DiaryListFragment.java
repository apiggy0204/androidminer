package edu.ntu.arbor.sbchao.androidlogger;

import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.model.Marker;

import edu.ntu.arbor.sbchao.androidlogger.scheme.ActivityLog;

public class DiaryListFragment extends Fragment implements OnInfoWindowClickListener {
	private Activity mActivity;
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
		mActivity = getActivity();
	}

	public void update(List<ActivityLog> logs) {
		for (ActivityLog log : logs) {
			Date startTime = log.getStartTime();
	    	Date endTime = log.getEndTime();
		}
		mAdapter = new TimelineAdapter(logs);
		mListView.setAdapter(new TimelineAdapter(logs));
	}

	private class TimelineAdapter implements ListAdapter {
		private List<ActivityLog> logs;

		public TimelineAdapter(List<ActivityLog> _logs) {
			logs = _logs;
		}
		@Override
		public int getCount() {
			return 2 * logs.size() - 1 ;
		}

		@Override
		public Object getItem(int position) {
			return logs.get(position);
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getItemViewType(int arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View arg1, ViewGroup arg2) {
			LayoutInflater inflater = mActivity.getLayoutInflater();
			View listItem = (View) inflater.inflate(R.layout.listitem_event, null);

			listItem.setMinimumHeight(10);
			TextView hourTextView = (TextView) listItem.findViewById(R.id.textView_hour);
			if (((position >= 0) && (position <= 2)) || ((position >= 15) && (position <= 23)))
				hourTextView.setText(String.valueOf((position + 9) % 24) + " am");
			else hourTextView.setText(String.valueOf((position + 9) % 24) + " pm");

			LinearLayout eventlLayout = (LinearLayout) listItem.findViewById(R.id.layout_event);
			if (position % 2 == 0) {
				TextView eventTextView = new TextView(mActivity);
				eventTextView.setText(logs.get(position/2).getActivityName());
				eventlLayout.addView(eventTextView);
			} else {
				listItem.setMinimumHeight(20);
			}
			

//			TextView eventTextView = new TextView(mActivity);
//			eventTextView.setText("!!!");
//			eventlLayout.addView(eventTextView);

			return listItem;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public void registerDataSetObserver(DataSetObserver arg0) {
		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver arg0) {
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int arg0) {
			return false;
		}
	}
	
	@Override
    public void onInfoWindowClick(Marker marker) {
		marker.hideInfoWindow();
    }
}