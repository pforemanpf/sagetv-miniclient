package sagex.miniclient.android;

import android.app.Activity;
import android.os.Bundle;

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtil.hideSystemUIOnTV(this);
        setContentView(R.layout.activity_preferences);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppUtil.hideSystemUIOnTV(this);
    }
}
