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

import android.graphics.Bitmap;
import java.util.Arrays;
import android.util.Base64;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.content.res.Resources;
import java.util.LinkedList;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.Context;
import java.util.List;

public class App implements Parcelable {

	private static final char PACKAGE_SEPARATOR = '.';
	private static final char PACKAGE_SEPARATOR_REPLACEMENT = '$';
	private static final char TIP_IDENTIFIER_SEPARATOR = '§';
	private static final String RESOURCE_APP_INFO_PREFIX = "app_info_";
	private static final String RESOURCE_APP_INFO_TYPE = "array";
	private static final String PREFS_FILE_NAME = "tips_accepted";
	private static final String HASH_ALGORITHM = "SHA-1";
	private static final String CHARSET = "UTF-8";
	private static PackageManager mPackageManager;
	private static String mSelfName;
	private final String mPackageName;
	private final String mTitle;
	private final Bitmap mIcon;
	private final boolean mSystemApp;
	private final List<String> mTips;

	public App(final String packageName, final String title, final Bitmap icon, final boolean systemApp, final List<String> tips) {
		mPackageName = packageName;
		mTitle = title;
		mIcon = icon;
		mSystemApp = systemApp;
		mTips = tips;
	}

	public String getPackageName() {
		return mPackageName;
	}

	public String getTitle() {
		return mTitle;
	}

	public Bitmap getIcon() {
		return mIcon;
	}

	public boolean isSystemApp() {
		return mSystemApp;
	}

	public List<String> getTips() {
		return mTips;
	}

	public boolean hasTips() {
		return mTips != null && mTips.size() > 0;
	}

	public void markTipAsRead(final Context context, final String tip) {
		final SharedPreferences prefs = getPrefs(context);
		final String identifier = createTipIdentifier(mPackageName, tip);
		prefs.edit().putBoolean(identifier, true).commit();
	}

	private static SharedPreferences getPrefs(final Context context) {
		return context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE);
	}

	private static String createTipIdentifier(final String packageName, final String tip) {
		return base64Encode(sha1(packageName + TIP_IDENTIFIER_SEPARATOR + tip));
	}

	private static String base64Encode(final byte[] data) {
		final byte[] encoded = Base64.encode(data, Base64.URL_SAFE | Base64.NO_WRAP);

		try {
			return new String(encoded, CHARSET);
		}
		catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private static byte[] sha1(final String text) {
		final MessageDigest md;

		try {
			md = MessageDigest.getInstance(HASH_ALGORITHM);
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		try {
			md.update(text.getBytes(CHARSET), 0, text.length());
		}
		catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		return md.digest();
	}

	public static List<App> getList(final Context context, final Resources resources) {
		return getList(context, resources, false);
	}

	public static List<App> getList(final Context context, final Resources resources, final boolean includeEmpty) {
		final List<ApplicationInfo> apps = getInstalledPackages(context);
		final SharedPreferences prefs = getPrefs(context);

		final List<App> out = new LinkedList<App>();
		App app;
		List<String> tips;
		int numTips;
		String tipIdentifier;
		for (ApplicationInfo appInfo : apps) {
			if (!appInfo.enabled) {
				continue;
			}

			tips = getAppTips(context, resources, appInfo.packageName);

			// remove tips that have already been marked as read
			if (!Config.DEBUG) {
				if (tips != null) {
					numTips = tips.size();
					for (int i = numTips - 1; i >= 0; i--) {
						tipIdentifier = createTipIdentifier(appInfo.packageName, tips.get(i));
						if (prefs.getBoolean(tipIdentifier, false)) {
							tips.remove(i);
						}
					}
				}
			}

			app = new App(appInfo.packageName, getAppTitle(context, appInfo), getAppIcon(context, appInfo), isSystemApp(appInfo), tips);

			if (app.hasTips()) {
				out.add(0, app);
			}
			else {
				if (includeEmpty) {
					out.add(app);
				}
			}
		}

		return out;
	}

	public static void launchApp(final Context context, final String packageName) {
		final Intent intent = getPackageManager(context).getLaunchIntentForPackage(packageName);

		if (intent == null) {
			throw new RuntimeException("Package with specified name is not launchable");
		}

		context.startActivity(intent);
	}

	private static String createPackageIdentifier(final String packageName) {
		return packageName.replace(PACKAGE_SEPARATOR, PACKAGE_SEPARATOR_REPLACEMENT);
	}

	private static List<String> getAppTips(final Context context, final Resources resources, final String packageName) {
		if (mSelfName == null) {
			mSelfName = context.getPackageName();
		}

		final String resourceName = RESOURCE_APP_INFO_PREFIX + createPackageIdentifier(packageName);
		final int resourceId = resources.getIdentifier(resourceName, RESOURCE_APP_INFO_TYPE, mSelfName);

		try {
			return new LinkedList<String>(Arrays.asList(resources.getStringArray(resourceId)));
		}
		catch (Exception e) {
			return null;
		}
	}

	private static boolean isSystemApp(final ApplicationInfo packageInfo) {
		if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM) {
			return true;
		}

		if ((packageInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) {
			return true;
		}

		return false;
	}

	private static PackageManager getPackageManager(final Context context) {
		if (mPackageManager == null) {
			mPackageManager = context.getPackageManager();
		}

		return mPackageManager;
	}

	private static List<ApplicationInfo> getInstalledPackages(final Context context) {
		return getPackageManager(context).getInstalledApplications(PackageManager.GET_META_DATA);
	}

	private static String getAppTitle(final Context context, final ApplicationInfo packageInfo) {
		try {
			return getPackageManager(context).getApplicationLabel(packageInfo).toString();
		}
		catch  (Exception e) {
			return null;
		}
	}

	private static Bitmap getAppIcon(final Context context, final ApplicationInfo packageInfo) {
		try {
			return Images.Bitmaps.fromDrawable(getPackageManager(context).getApplicationIcon(packageInfo));
		}
		catch  (Exception e) {
			return null;
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Parcelable.Creator<App> CREATOR = new Parcelable.Creator<App>() {

		@Override
		public App createFromParcel(Parcel in) {
			return new App(in);
		}

		@Override
		public App[] newArray(int size) {
			return new App[size];
		}

	};

	private App(final Parcel in) {
		mPackageName = in.readString();
		mTitle = in.readString();
		mIcon = in.<Bitmap>readParcelable(Bitmap.class.getClassLoader());
		mSystemApp = in.readByte() == 1;
		mTips = in.createStringArrayList();
	}

	@Override
	public void writeToParcel(final Parcel out, final int flags) {
		out.writeString(mPackageName);
		out.writeString(mTitle);
		out.writeParcelable(mIcon, flags);
		out.writeByte((byte) (mSystemApp ? 1 : 0));
		out.writeStringList(mTips);
	}

}
