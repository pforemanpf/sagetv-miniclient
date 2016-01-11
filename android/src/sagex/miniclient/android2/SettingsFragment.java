package sagex.miniclient.android2;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.text.Html;

import sagex.miniclient.Version;
import sagex.miniclient.prefs.PrefStore;


/**
 * Created by seans on 24/10/15.
 */
public class SettingsFragment extends PreferenceFragment {
    PrefStore prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);

        try {
            prefs = MiniclientApplication.get(getActivity()).getClient().properties();

            //prefs.setEnabled(this, Prefs.Key.use_log_to_sdcard, !getResources().getBoolean(R.bool.istv));

            Preference p = findPreference(PrefStore.Keys.use_log_to_sdcard);
            p.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    AppUtil.initLogging(SettingsFragment.this.getActivity(), (Boolean) newValue);
                    return true;
                }
            });

            final Preference loglevel = findPreference(PrefStore.Keys.log_level);
            loglevel.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    AppUtil.setLogLevel((String) newValue);
                    updateSummary(preference, R.string.summary_list_loglevels_preference, newValue);
                    return true;
                }
            });

            final Preference streammode = findPreference(PrefStore.Keys.log_level);
            streammode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    updateSummary(preference, R.string.summary_list_streaming_mode_preference, newValue);
                    return true;
                }
            });

            updateSummary(loglevel, R.string.summary_list_loglevels_preference, prefs.getString(PrefStore.Keys.log_level, "debug"));
            updateSummary(streammode, R.string.summary_list_streaming_mode_preference, prefs.getString(PrefStore.Keys.streaming_mode, "dynamic"));

            final Preference sendlog = findPreference("sendlog");
            sendlog.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(getActivity().getBaseContext(), SendLogActivity.class));
                    return true;
                }
            });

            final Preference version = findPreference("version");
            version.setSummary(Version.VERSION);
            final Preference clientid = findPreference("clientid");
            clientid.setSummary(AppUtil.getMACAddress(this.getActivity()));

            final Preference decoders = findPreference("decoders");
            decoders.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showHardwareDecoderInfo();
                    return true;
                }

            });


        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void showHardwareDecoderInfo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.hardware_decoders);
        StringBuilder sb = new StringBuilder();
        sb.append("<B>").append("Video Decoders").append("</B><br/>\n");
        for (String mi : AppUtil.getVideoDecoders()) {
            sb.append(mi).append("<br/>\n");
        }
        sb.append("\n<br/><B>").append("Audio Decoders").append("</B><br/>\n");
        for (String mi : AppUtil.getAudioDecoders()) {
            sb.append(mi).append("<br/>\n");
        }
        builder.setMessage(Html.fromHtml(sb.toString()));
        builder.setCancelable(true);
        builder.show();
    }
    private void updateSummary(Preference pref, int resId, Object value) {
        //pref.setSummary(getResources().getString(resId, value));
    }
}
