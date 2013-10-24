package info.mzimmermann.xposed.cputempstatusbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import android.os.Bundle;
import android.app.ActionBar;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class DebugActivity extends Activity {

	private TextView mTextView = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_debug);

		ActionBar actionBar = getActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);

		mTextView = (TextView) findViewById(R.id.textView1);
		mTextView.setText(Utils.readLog());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.debug, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.action_refresh:
			mTextView.setText(Utils.readLog());
			Toast.makeText(this, "Successfully Refreshed", Toast.LENGTH_SHORT)
					.show();
			return false;
		case android.R.id.home:
			finish();
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onClick(View v) {
		try {
			InputStream in = Runtime.getRuntime()
					.exec("busybox find /sys -type f -name *temp*")
					.getInputStream();
			BufferedReader inBuffered = new BufferedReader(
					new InputStreamReader(in));

			String result = "";

			String line = null;
			while ((line = inBuffered.readLine()) != null) {
				try {
					File f = new File(line.trim());
					String permissions = f.canRead() ? "r" : "-";

					result += line.trim()
							+ "("
							+ permissions
							+ "): '"
							+ Utils.convertStreamToString(new FileInputStream(f))
							+ "'";
				} catch (Exception e) {
					result+=Log.getStackTraceString(e);
				}
			}

			Utils.log(result);
			mTextView.setText(mTextView.getText() + result);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
