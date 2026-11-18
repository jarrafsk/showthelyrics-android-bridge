package dev.jarraf.lyricsbridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 48, 32, 32);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView title = new TextView(this);
        title.setText("Show the Lyrics — Android Bridge");
        title.setTextSize(22);
        TextView info = new TextView(this);
        info.setText("Enable Notification Access below. This lets the bridge read media-session metadata (song title, artist and playback position). It does not record audio. Start YouTube playback, then launch Minecraft with the original mod and the Fabric bridge addon.");
        info.setTextSize(16);
        Button settings = new Button(this);
        settings.setText("Open Notification Access settings");
        settings.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        layout.addView(title);
        layout.addView(info);
        layout.addView(settings);
        setContentView(layout);
    }
}
