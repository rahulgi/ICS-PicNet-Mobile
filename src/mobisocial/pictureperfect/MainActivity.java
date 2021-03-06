package mobisocial.pictureperfect;

import java.util.List;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.musubi.DbIdentity;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.Musubi;
import mobisocial.socialkit.obj.MemObj;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final String TAG = "Locator";
	
	private static final String ACTION_CREATE_FEED = "musubi.intent.action.CREATE_FEED";
	private static final int REQUEST_CREATE_FEED = 1;
	private static final String ACTION_EDIT_FEED = "musubi.intent.action.EDIT_FEED";
	private static final int REQUEST_EDIT_FEED = 2;
	
	private Musubi mMusubi;
	
	private Uri feedUri = null;
	private final String KEY_FEED_URI = "feedUri";
	
	private TextView uriPresenter;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		uriPresenter = (TextView) findViewById(R.id.uriPresenter);
		
		if (!Musubi.isMusubiInstalled(this))
			mMusubi = Musubi.getInstance(this);
		
		String uri = getPreferences(android.content.Context.MODE_PRIVATE).getString(KEY_FEED_URI, null);
		if (uri != null) {
			feedUri = Uri.parse(uri);
			uriPresenter.setText(uri);
		}
	}

	@Override
	public void onResume() {
		IntentFilter iff = new IntentFilter();
		iff.addAction("mobisocial.intent.action.DATA_RECEIVED");
		this.registerReceiver(this.messageReceiver, iff);
		super.onResume();
	}
	
	@Override
	public void onPause() {
		this.unregisterReceiver(this.messageReceiver);
		super.onPause();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.create_feed:
			if (!Musubi.isMusubiInstalled(this)) {
				Log.d(TAG, "Musubi is not installed.");
				return super.onOptionsItemSelected(item);
			}
			Intent intent = new Intent();
			int request;
			if (feedUri == null) {
				intent.setAction(ACTION_CREATE_FEED);
				request = REQUEST_CREATE_FEED;
			} else {
				intent.setAction(ACTION_EDIT_FEED);
				request = REQUEST_EDIT_FEED;
				intent.setData(feedUri);
			}
			startActivityForResult(intent, request);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CREATE_FEED && resultCode == RESULT_OK) {
			if (data == null || data.getData() == null) {
				return;
			}
			
			feedUri = data.getData();
			Log.d(TAG, "feedUri: " + feedUri.toString());
			
			DbFeed feed = mMusubi.getFeed(feedUri);
			uriPresenter.setText(feedUri.toString());
			getPreferences(android.content.Context.MODE_PRIVATE).edit().putString(KEY_FEED_URI, feedUri.toString()).apply();
			if (feed == null) {
				Log.d(TAG, "feed is null?!?");
				return;
			}
			
			List<DbIdentity> members = feed.getMembers();
			for (DbIdentity member: members) {
				Log.d(TAG, "member: " + member.getName() + ", " + member.getId());
			}
			
			JSONObject json = new JSONObject();
			try {
				json.put("can't see this", "invisible");
				json.put(Obj.FIELD_HTML, "hi");
			} catch (JSONException e) {
				Log.e(TAG, "json error", e);
				return;
			}
			feed.postObj(new MemObj("socialkit-locator", json));
			Log.d(TAG, "json obj posted: " + json.toString());
		}
	}

	private BroadcastReceiver messageReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent == null)
				Log.d(TAG, "no intent");
			
			Log.d(TAG, "message received: " + intent);
			
			Uri objUri = (Uri) intent.getParcelableExtra("objUri");
			if (objUri == null) {
				Log.d(TAG, "no object found");
				return;
			}
			Log.d(TAG, "obj uri: " + objUri.toString());
				
			Musubi musubi = Musubi.forIntent(context, intent);
			DbObj obj = musubi.objForUri(objUri);
			
			if (obj == null) {
				Log.d(TAG, "obj is null?");
				return;
			}
			
			if (feedUri == null) {
				feedUri = obj.getContainingFeed().getUri();
				if (feedUri != null) {
					uriPresenter.setText(feedUri.toString());
				}
			}
			
			JSONObject json = obj.getJson();
			if (json == null) {
				Log.d(TAG, "no json attached to obj");
				return;
			}
			Log.d(TAG, "json: " + json);
			
			if (obj.getSender().isOwned()) {
				// do stuff specific to when message is sent by me
				return;
			}
			// Process message
		}
	};
	
}
