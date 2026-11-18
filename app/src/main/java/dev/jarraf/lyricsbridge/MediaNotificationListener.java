package dev.jarraf.lyricsbridge;

import android.media.MediaMetadata;
import android.media.session.MediaSessionManager;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.content.ComponentName;
import android.util.Base64;
import android.util.Log;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class MediaNotificationListener extends NotificationListenerService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile Socket socket;
    private volatile OutputStream output;
    private volatile MediaController selected;
    private final Runnable poller = new Runnable() {
        @Override public void run() {
            try { updateActiveSession(); sendCurrent(); } catch (Exception e) { Log.e("LyricsBridge", "Poll error", e); closeSocket(); }
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onListenerConnected() {
        super.onListenerConnected();
        Log.i("LyricsBridge", "LISTENER CONNECTED: callback fired");
        handler.removeCallbacks(poller);
        handler.post(poller);
    }

    @Override public void onListenerDisconnected() {
        handler.removeCallbacks(poller);
        closeSocket();
        super.onListenerDisconnected();
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(poller);
        closeSocket();
        super.onDestroy();
    }

    private void updateActiveSession() {
        MediaSessionManager manager = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
        List<MediaController> sessions = manager.getActiveSessions(new ComponentName(this, MediaNotificationListener.class));
        Log.i("LyricsBridge", "Active media sessions: " + (sessions == null ? "null" : sessions.size()));
        MediaController best = null;
        if (sessions != null) {
            for (MediaController c : sessions) {
                PlaybackState state = c.getPlaybackState();
                if (state != null && state.getState() == PlaybackState.STATE_PLAYING) { best = c; break; }
                if (best == null && c.getMetadata() != null) best = c;
            }
        }
        selected = best;
        Log.i("LyricsBridge", "Selected media: " + (best == null ? "none" : best.getPackageName()));
    }

    private void sendCurrent() throws Exception {
        MediaController c = selected;
        if (c == null || c.getMetadata() == null) return;
        MediaMetadata m = c.getMetadata();
        PlaybackState p = c.getPlaybackState();
        String title = m.getString(MediaMetadata.METADATA_KEY_TITLE);
        String artist = m.getString(MediaMetadata.METADATA_KEY_ARTIST);
        String album = m.getString(MediaMetadata.METADATA_KEY_ALBUM);
        if (title == null) title = "";
        if (artist == null) artist = "";
        if (album == null) album = "";
        Log.i("LyricsBridge", "MEDIA: title=" + title + " | artist=" + artist + " | album=" + album);
        long duration = m.getLong(MediaMetadata.METADATA_KEY_DURATION);
        long position = p == null ? 0 : Math.max(0, p.getPosition());
        boolean playing = p != null && p.getState() == PlaybackState.STATE_PLAYING;
        String line = enc(title) + "\t" + enc(artist) + "\t" + enc(album) + "\t" + duration + "\t" + position + "\t" + (playing ? "1" : "0") + "\n";
        ensureConnected();
        if (output != null) { output.write(line.getBytes(StandardCharsets.UTF_8)); output.flush(); }
    }

    private static String enc(String s) {
        return Base64.encodeToString(s.getBytes(StandardCharsets.UTF_8), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    private synchronized void ensureConnected() {
        try {
            if (socket != null && socket.isConnected() && !socket.isClosed()) return;
            closeSocket();
            Socket s = new Socket();
            s.connect(new InetSocketAddress("127.0.0.1", 39871), 500);
            s.setTcpNoDelay(true);
            socket = s;
            output = s.getOutputStream(); Log.i("LyricsBridge", "BRIDGE CONNECTED: 127.0.0.1:39871");
        } catch (Exception e) { Log.e("LyricsBridge", "Bridge connection failed", e); closeSocket(); }
    }

    private synchronized void closeSocket() {
        try { if (output != null) output.close(); } catch (Exception ignored) { }
        try { if (socket != null) socket.close(); } catch (Exception ignored) { }
        output = null;
        socket = null;
    }
}
