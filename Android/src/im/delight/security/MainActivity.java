package im.delight.security;

/**
 * Copyright 2015 www.delight.im <info@delight.im>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import im.delight.apprater.AppRater;
import java.util.ArrayList;
import android.widget.ImageView;
import android.content.Intent;
import android.widget.AdapterView;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.app.ListActivity;
import android.content.res.Resources;
import java.util.List;
import android.widget.TextView;
import android.os.Bundle;

public class MainActivity extends ListActivity {

	private class AppAdapter extends ArrayAdapter<App> {

		public AppAdapter(final Context context, final int resource, final List<App> objects) {
			super(context, resource, objects);
		}

		@Override
		public View getView(final int position, View convertView, final ViewGroup parent) {
			if (convertView == null) {
				convertView = View.inflate(MainActivity.this, R.layout.row_apps, null);
			}

			final App app = getItem(position);
			final int numSecurityTips = app.hasTips() ? app.getTips().size() : 0;

			((ImageView) convertView.findViewById(R.id.icon)).setImageBitmap(app.getIcon());
			((TextView) convertView.findViewById(R.id.title)).setText(app.getTitle());
			((TextView) convertView.findViewById(R.id.body)).setText(app.getPackageName());
			((TextView) convertView.findViewById(R.id.footer)).setText(mResources.getQuantityString(R.plurals.x_security_tips, numSecurityTips, numSecurityTips));
			convertView.findViewById(R.id.system_app).setVisibility(app.isSystemApp() ? View.VISIBLE : View.GONE);

			return convertView;
		}

	}

	private Resources mResources;
	private ListView mListView;
	private AppAdapter mAdapter;
	private ArrayList<App> mList;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mResources = getApplicationContext().getResources();

		mList = new ArrayList<App>();

		mAdapter = new AppAdapter(this, R.layout.row_apps, mList);

		mListView = getListView();
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
				final Intent intent = new Intent(MainActivity.this, DetailsActivity.class);
				intent.putExtra(DetailsActivity.EXTRA_APP, mAdapter.getItem(position));
				startActivity(intent);
			}

		});

		if (savedInstanceState == null) {
			reloadApps(Config.DEBUG);
		}
		else {
			List<App> restoredList = savedInstanceState.<App>getParcelableArrayList("mList");
			if (restoredList == null) {
				reloadApps(Config.DEBUG);
			}
			else {
				mList.clear();
				mList.addAll(restoredList);
				mAdapter.notifyDataSetChanged();
			}
		}
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList("mList", mList);
	}

	private void reloadApps(final boolean includeEmpty) {
		new Thread() {

			@Override
			public void run() {
				final List<App> apps = App.getList(getApplicationContext(), mResources, includeEmpty);

				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						onAppsLoaded(apps);
					}

				});
			}

		}.start();
	}

	private void onAppsLoaded(final List<App> apps) {
		mList.clear();
		mList.addAll(apps);
		mAdapter.notifyDataSetChanged();

		findViewById(R.id.loading).setVisibility(View.GONE);
		findViewById(R.id.security_ok).setVisibility(View.VISIBLE);

		new AppRater(this).show();
	}

}
