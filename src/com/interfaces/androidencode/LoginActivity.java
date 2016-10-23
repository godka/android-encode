package com.interfaces.androidencode;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_main);
		final EditText iptext = (EditText) findViewById(R.id.editText1);
		// final CheckBox checkbox = (CheckBox)findViewById(R.id.checkBox1);

		// final boolean isComp = false;
		iptext.setText("rtmp://192.168.31.193/live/stream");
		Button checkbutton = (Button) findViewById(R.id.button1);
		checkbutton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				// boolean isComp = checkbox.isChecked();
				Intent intent = new Intent();
				Bundle bundle = new Bundle();
				bundle.putString("rtmplink", iptext.getText().toString());
				// bundle.putBoolean("Comp", isComp);
				intent.putExtras(bundle);
				intent.setClass(LoginActivity.this, MainActivity.class);
				startActivity(intent);
			}
		});
	}
}
