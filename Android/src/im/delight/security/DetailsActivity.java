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

import android.widget.ListView;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;
import android.view.Menu;
import android.widget.Toast;
import android.net.Uri;
import android.view.MenuItem;
import android.content.Intent;
import android.os.Bundle;
import android.app.ListActivity;

public class DetailsActivity extends ListActivity {

	private class DetailsAdapter extends ArrayAdapter<String> {

		public DetailsAdapter(final Context context, final int resource, final List<String> objects) {
			super(context, resource, objects);
		}

		@Override
		public View getView(final int position, View convertView, final ViewGroup parent) {
			if (convertView == null) {
				convertView = View.inflate(DetailsActivity.this, R.layout.row_details, null);
			}

			((TextView) convertView.findViewById(R.id.title)).setText(getItem(position));
			convertView.findViewById(R.id.done).setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					final String tip = mAdapter.getItem(position);
					mApp.markTipAsRead(DetailsActivity.this, tip);
					mAdapter.remove(tip);
				}

			});

			return convertView;
		}

	}

	public static final String EXTRA_APP = "app";
	private App mApp;
	private ListView mListView;
	private DetailsAdapter mAdapter;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_details);

		getActionBar().setDisplayHomeAsUpEnabled(true);

		if (savedInstanceState != null) {
			mApp = (App) savedInstanceState.getParcelable("mApp");
		}
		else {
			mApp = (App) getIntent().getParcelableExtra(EXTRA_APP);
		}

		setTitle(mApp.getTitle());
		((TextView) findViewById(R.id.security_ok_title)).setText(mApp.getTitle());

		if (mApp.hasTips()) {
			mAdapter = new DetailsAdapter(this, R.layout.row_details, mApp.getTips());

			mListView = getListView();
			mListView.setAdapter(mAdapter);
		}
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable("mApp", mApp);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.menu_details, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_open_in_store:
				if (!openAppSettings(mApp.getPackageName())) {
					openAppInStore(mApp.getPackageName());
				}
				return true;
			case R.id.action_launch_app:
				if (!launchApp(mApp.getPackageName())) {
					Toast.makeText(DetailsActivity.this, R.string.app_cannot_be_launched, Toast.LENGTH_SHORT).show();
				}
				return true;
			default:
				startActivity(new Intent(this, MainActivity.class)); // go one step up in Activity hierarchy
				finish(); // destroy this Activity so that the user does not immediately come back if they press "Back"
				return true;
		}
	}

	private void openAppInStore(final String packageName) {
		Intent intent;

		try {
			intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
		}
		catch (Exception e) {
			intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
		}

		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		startActivity(intent);
	}

	private boolean launchApp(final String packageName) {
		final Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);

		if (intent != null) {
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);

			return true;
		}
		else {
			return false;
		}
	}

	private boolean openAppSettings(final String packageName) {
		try {
		    final Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
		    intent.setData(Uri.parse("package:" + packageName));
		    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		    startActivity(intent);

		    return true;
		}
		catch (Exception e) {
			return false;
		}
	}

}
