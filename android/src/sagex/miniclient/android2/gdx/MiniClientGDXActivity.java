package sagex.miniclient.android2.gdx;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.squareup.otto.DeadEvent;
import com.squareup.otto.Subscribe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;
import sagex.miniclient.MACAddressResolver;
import sagex.miniclient.MiniClient;
import sagex.miniclient.ServerInfo;
import sagex.miniclient.android2.AppUtil;
import sagex.miniclient.android2.MiniclientApplication;
import sagex.miniclient.android2.NavigationFragment;
import sagex.miniclient.android2.R;
import sagex.miniclient.android2.events.BackPressedEvent;
import sagex.miniclient.android2.events.CloseAppEvent;
import sagex.miniclient.android2.events.HideKeyboardEvent;
import sagex.miniclient.android2.events.HideNavigationEvent;
import sagex.miniclient.android2.events.HideSystemUIEvent;
import sagex.miniclient.android2.events.ShowKeyboardEvent;
import sagex.miniclient.android2.events.ShowNavigationEvent;
import sagex.miniclient.prefs.PrefStore;
import sagex.miniclient.uibridge.EventRouter;

import static sagex.miniclient.android2.AppUtil.confirmExit;
import static sagex.miniclient.android2.AppUtil.hideSystemUI;

/**
 * Created by seans on 20/09/15.
 */
public class MiniClientGDXActivity extends AndroidApplication implements MACAddressResolver {
    public static final String ARG_SERVER_INFO = "server_info";
    private static final Logger log = LoggerFactory.getLogger(MiniClientGDXActivity.class);
    @Bind(R.id.surface)
    FrameLayout uiFrameHolder;

    @Bind(R.id.video_surface)
    SurfaceView videoHolder;

    @Bind(R.id.waitforit)
    View pleaseWait = null;

    @Bind(R.id.pleaseWaitText)
    TextView plaseWaitText = null;

    MiniClient client;
    MiniClientRenderer mgr;

    private View miniClientView;

    public MiniClientGDXActivity() {
    }

    @Override
    protected void onResume() {
        log.debug("MiniClient UI onResume() called");

        // setup to handle events
        client.eventbus().register(this);

        MiniClientKeyListener keyListener = new MiniClientKeyListener(client);

        try {
            miniClientView.setOnTouchListener(new MiniclientTouchListener(this, client));
            miniClientView.setOnKeyListener(keyListener);
        } catch (Throwable t) {
            log.error("Failed to restore the key and touch handlers");
        }

        try {
            log.debug("Telling SageTV to repaint {}x{}", mgr.uiSize.getWidth(), mgr.uiSize.getHeight());
            client.getCurrentConnection().postRepaintEvent(0, 0, mgr.uiSize.getWidth(), mgr.uiSize.getHeight());
        } catch (Throwable t) {
            log.warn("Failed to do a repaint event on refresh");
        }

        hideSystemUI(this);

        super.onResume();
    }

    @Override
    protected void onPause() {
        // remove ourself from handling events
        client.eventbus().unregister(this);

        try {
            miniClientView.setOnTouchListener(null);
            miniClientView.setOnKeyListener(null);
        } catch (Throwable t) {
        }

        log.debug("MiniClient UI onPause() called");
        try {
            // pause video if we are leaving the app
            if (client.getCurrentConnection() != null && client.getCurrentConnection().getMediaCmd() != null) {
                if (client.getCurrentConnection().getMediaCmd().getPlaya() != null) {
                    log.info("We are leaving the App, Make sure Video is stopped.");
                    client.getCurrentConnection().getMediaCmd().getPlaya().pause();
                    EventRouter.post(client, EventRouter.MEDIA_STOP);
                }
            }
        } catch (Throwable t) {
            log.debug("Failed why attempting to pause media player");
        }
        try {
            if (client.properties().getBoolean(PrefStore.Keys.app_destroy_on_pause)) {
                try {
                    client.closeConnection();
                } catch (Throwable t) {
                }
                finishAndRemoveTask();
            } else {
                // TODO: Try to free up memory, clear caches, etc
            }
        } catch (Throwable t) {
            log.debug("Failed to close client connection");
        }
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            hideSystemUI(this);

            setContentView(R.layout.miniclientgl_layout);
            ButterKnife.bind(this);

            AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
            //cfg.useGL20 = false;
            // we need to change the default pixel format - since it does not include an alpha channel
            // we need the alpha channel so the camera preview will be seen behind the GL scene
            cfg.r = 8;
            cfg.g = 8;
            cfg.b = 8;
            cfg.a = 8;

            client = MiniclientApplication.get().getClient();

            mgr = new MiniClientRenderer(this, client);
            miniClientView = initializeForView(mgr, cfg);

            if (graphics.getView() instanceof SurfaceView) {
                log.debug("Setting Translucent View");
                GLSurfaceView glView = (GLSurfaceView) graphics.getView();
                // This is needed or else we won't see OSD over video
                glView.setZOrderOnTop(true);
                // This is needed or else we will not see the video playing behind the OSD
                glView.getHolder().setFormat(PixelFormat.RGBA_8888);
            }

            miniClientView.setFocusable(true);
            miniClientView.setFocusableInTouchMode(true);
            miniClientView.setOnTouchListener(null);
            miniClientView.setOnClickListener(null);
            miniClientView.setOnKeyListener(null);
            miniClientView.setOnDragListener(null);
            miniClientView.setOnFocusChangeListener(null);
            miniClientView.setOnGenericMotionListener(null);
            miniClientView.setOnHoverListener(null);
            miniClientView.setOnTouchListener(null);
            //miniClientView.setBackgroundColor(Color.TRANSPARENT);
            uiFrameHolder.addView(miniClientView);
            //uiFrameHolder.setBackgroundColor(Color.TRANSPARENT);
            miniClientView.requestFocus();

            ServerInfo si = (ServerInfo) getIntent().getSerializableExtra(ARG_SERVER_INFO);
            if (si == null) {
                log.error("Missing SERVER INFO in Intent: {}", ARG_SERVER_INFO);
                finish();
            }

            //setupNavigationDrawer();

            plaseWaitText.setText("Connecting to " + si.address + "...");
            setConnectingIsVisible(true);

            startMiniClient(si);
        } catch (Throwable t) {
            log.error("Failed to start/create the Main Activity for the MiniClient UI", t);
            throw new RuntimeException("Unable to start Activity", t);
        }
    }

    public void startMiniClient(final ServerInfo si) {
        Thread t = new Thread("ANDROID-MINICLIENT") {
            @Override
            public void run() {
                try {
                    // cannot make network connections on the main thread
                    client.connect(si, MiniClientGDXActivity.this);
                } catch (final IOException e) {
                    MiniClientGDXActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MiniClientGDXActivity.this, "Unable to connect to server: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        };
        t.start();
    }

    @Override
    public void onBackPressed() {
        // hide system ui, in case keyboard is visible
        hideSystemUI(this);
        //EventRouter.post(client, EventRouter.BACK);
        //confirmExit(this);
    }

    @Override
    protected void onDestroy() {
        log.debug("Closing MiniClient Connection");

        try {
            client.closeConnection();
        } catch (Throwable t) {
            log.error("Error shutting down client", t);
        }
        super.onDestroy();
    }

    public void setConnectingIsVisible(final boolean connectingIsVisible) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pleaseWait.setVisibility((connectingIsVisible) ? View.VISIBLE : View.GONE);
            }
        });
    }

    public Surface getVideoSurface() {
        return videoHolder.getHolder().getSurface();
    }

    @Override
    public String getMACAddress() {
        return AppUtil.getMACAddress(this);
    }

    public SurfaceView getVideoView() {
        return videoHolder;
    }

    public void showHideKeyboard(final boolean visible) {

        miniClientView.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (visible) {
                    log.debug("Showing Keyboard");
                    miniClientView.requestFocus();
                    miniClientView.requestFocusFromTouch();
                    im.showSoftInput(miniClientView, InputMethodManager.SHOW_FORCED);
                } else {
                    im.hideSoftInputFromWindow(miniClientView.getWindowToken(), 0);
                }
            }
        }, 200);
    }

    public void showHideSoftRemote(boolean visible) {
        if (visible) {
            showNavigationDialog();
        } else {
            hideNavigationDialog();
        }
    }

    void showNavigationDialog() {
        log.debug("Showing Navigation");
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("nav");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = new NavigationFragment(client);

        newFragment.show(ft, "nav");
    }

    public void leftEdgeSwipe(MotionEvent event) {
        log.debug("Left Edge Swipe");
    }

    public View getRootView() {
        return miniClientView;
    }

    @Subscribe
    public void handleOnShowKeyboard(ShowKeyboardEvent event) {
        showHideKeyboard(true);
    }

    @Subscribe
    public void handleOnHideKeyboard(HideKeyboardEvent event) {
        showHideKeyboard(false);
    }

    @Subscribe
    public void handleOnHideSystemUI(HideSystemUIEvent event) {
        hideSystemUI(this);
    }

    @Subscribe
    public void handleOnShowNavigation(ShowNavigationEvent event) {
        try {
            log.debug("MiniClient built-in Naviation is visible");
            showHideSoftRemote(true);
        } catch (Throwable t) {
            log.debug("Failed to show navigation");
        }
    }

    @Subscribe
    public void handleOnHideNavigation(HideNavigationEvent event) {
        try {
            log.debug("MiniClient built-in Naviation is hidden");
            showHideSoftRemote(false);
            hideSystemUI(this);
        } catch (Throwable t) {
            log.debug("Failed to hide navigation");
        }
    }

    @Subscribe
    public void handleOnCloseApp(CloseAppEvent event) {
        confirmExit(this, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
    }

    boolean hideNavigationDialog() {
        log.debug("Hiding Navigation");
        // remove nav OSD
        Fragment prev = getFragmentManager().findFragmentByTag("nav");
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        boolean hidingOSD = false;
        if (prev != null) {
            try {
                DialogFragment f = (DialogFragment) prev;
                f.dismiss();
            } catch (Throwable t) {
            }
            hidingOSD = true;
            try {
                ft.remove(prev);
            } catch (Throwable t) {
            }
        }
        ft.commit();

        // return true if the remote was actually hidden
        return hidingOSD;
    }

    @Subscribe
    public void handleOnBackPressed(BackPressedEvent event) {
        hideSystemUI(this);

        log.debug("on back pressed");

        if (!hideNavigationDialog()) {
            log.debug("Navigation wasn't visible so will process normal back");
            if (client.isVideoPlaying()) {
                log.debug("Sending Media Stop");
                EventRouter.post(client, EventRouter.MEDIA_STOP);
            } else {
                log.debug("Sending Back");
                EventRouter.post(client, EventRouter.BACK);
            }
        } else {
            log.debug("Just hiding navigation");
        }
    }

    @Subscribe
    public void onDeadEvent(DeadEvent event) {
        log.debug("Unhandled Event: {}", event);
    }
}
