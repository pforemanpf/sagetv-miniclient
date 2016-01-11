package sagex.miniclient.android2;

import android.view.KeyEvent;

import sagex.miniclient.util.StaticFieldMapper;

/**
 * Created by seans on 22/11/15.
 */
public class AndroidKeyEventMapper extends StaticFieldMapper<Integer> {
    public AndroidKeyEventMapper() {
        super(KeyEvent.class, "KEYCODE", true);
    }
}
