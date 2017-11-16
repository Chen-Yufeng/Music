package com.ifchan.music.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.ifchan.music.MainActivity;
import com.ifchan.music.R;
import com.ifchan.music.entity.Music;

import java.io.IOException;
import java.util.ArrayList;


public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer
                .OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {

    public static final String MODE_INITIALIZE_OR_PLAY_NEW = "MODE_INITIALIZE_OR_PLAY_NEW";
    public static final String INTENT_RENEW_MAIN_ACTIVITY_LRC = "INTENT_RENEW_MAIN_ACTIVITY_LRC";
    private final String TAG = "@vir MediaPlayerService";
    public static final String ACTION_PLAY = "com.valdioveliu.valdio.audioplayer.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.valdioveliu.valdio.audioplayer.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "com.valdioveliu.valdio.audioplayer" +
            ".ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "com.valdioveliu.valdio.audioplayer.ACTION_NEXT";
    public static final String ACTION_STOP = "com.valdioveliu.valdio.audioplayer.ACTION_STOP";
    public final static String INITIALIZE_OR_PLAY_NEW = "INITIALIZE_OR_PLAY_NEW";
    private final IBinder iBinder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private ArrayList<Music> mMusicList;
    private int position;
    //path to the audio file
    private int resumePosition = 0;

    //MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;

    //AudioPlayer notification ID
    private static final int NOTIFICATION_ID = 101;
    boolean isPaused = false;
    private int playingFlag = 0;

    public enum PlaybackStatus {
        PLAYING,
        PAUSED
    }

    private static boolean getDuration = false;
    private int time = -1;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mMusicList = (ArrayList<Music>) intent.getSerializableExtra(MainActivity
                .INTENT_MEDIA);
        position = intent.getIntExtra("position", 0);
        try {
            initMediaSession();
            buildNotification(PlaybackStatus.PLAYING);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (!requestAudioFocus()) {
            stopSelf();
        }
        if (mMusicList != null) {
            initMediaPlayer();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter intentFilter = new IntentFilter(INITIALIZE_OR_PLAY_NEW);

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mMusicList = (ArrayList<Music>) intent.getSerializableExtra(MainActivity
                        .INTENT_MEDIA);
                switch (intent.getIntExtra(MODE_INITIALIZE_OR_PLAY_NEW, 0)) {
                    case 1:
                        break;
                    case 2:
                        position = intent.getIntExtra("position", 0);
                        stopMedia();
                        initMediaPlayer();
                        break;
                }
            }
        }, intentFilter);
// TODO: 11/3/17 完成他
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction(ACTION_PAUSE);
        intentFilter2.addAction(ACTION_PLAY);
        intentFilter2.addAction(ACTION_NEXT);
        intentFilter2.addAction(ACTION_PREVIOUS);
        registerReceiver(new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                int todo = intent.getIntExtra("todo", 0);
                switch (todo) {
                    case 1:
                        if (isPaused) {
                            playMedia();
                            isPaused = false;
                        } else {
                            pauseMedia();
                            isPaused = true;
                        }
                        break;
                    case 2:
                        skipToNext();
                        break;
                    case 3:
                        skipToPrevious();
                        break;
                }
            }
        }, intentFilter2);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
        }
        removeAudioFocus();
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnInfoListener(this);
        //Reset so that the MediaPlayer is not pointing to another data source
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mediaPlayer.setDataSource(mMusicList.get(position).getPath());
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }

        mediaPlayer.prepareAsync();
        getDuration = false;
        time = -1;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    // TODO: 11/4/17 complete playing control here
    @Override
    public void onCompletion(MediaPlayer mp) {
        stopMedia();
        switch (playingFlag) {
            case 0:
                skipToNext();
                break;
            case 1:
                playByRandom();
                break;
            case 2:
                playByList();
                break;
            default:
                break;
//        stopSelf();
        }
        Intent intent = new Intent(INTENT_RENEW_MAIN_ACTIVITY_LRC);
        sendBroadcast(intent);
    }

    public void setPlayMode(int mode) {
        playingFlag = mode;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        playMedia();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mediaPlayer == null) {
                    initMediaPlayer();
                } else if (!mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                }
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.setVolume(0.1f, 0.1f);
                }
                break;
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " +
                        extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA_ERROR_UNKNOWN" + extra);
                break;
        }
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {

    }

    // TODO: 10/31/17 use?
    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    public void pauseOrPlay() {
        if (isPaused) {
            playMedia();
            isPaused = false;
        } else {
            pauseMedia();
            isPaused = true;
        }
    }

    public void playMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public void stopMedia() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    public void pauseMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
        }
    }

    public void resumeMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
        }
    }

    public void setResumePosition(int resumePosition) {
        this.resumePosition = resumePosition;
    }

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            return true;
        return false;
    }

    private boolean removeAudioFocus() {
        //use newer later
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocus(this);
    }

    class ServiceBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }

    private void initMediaSession() throws RemoteException {
        if (mediaSessionManager != null) return; //mediaSessionManager exists

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        // Create a new MediaSession
        mediaSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");
        //Get MediaSessions transport controls
        transportControls = mediaSession.getController().getTransportControls();
        //set MediaSession -> ready to receive media commands
        mediaSession.setActive(true);
        //indicate that the MediaSession handles transport control commands
        // through its MediaSessionCompat.Callback.
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        //Set mediaSession's MetaData
        updateMetaData();

        // Attach Callback to receive MediaSession updates
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            // Implement callbacks
            @Override
            public void onPlay() {
                super.onPlay();
                resumeMedia();
//                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onPause() {
                super.onPause();
                pauseMedia();
//                buildNotification(PlaybackStatus.PAUSED);
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                skipToNext();
                updateMetaData();
//                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                skipToPrevious();
                updateMetaData();
//                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                //Stop the service
                stopSelf();
            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }
        });
    }

    private void updateMetaData() {
//        Bitmap albumArt = BitmapFactory.decodeResource(getResources(),
//                R.drawable.image); //replace with medias albumArt
        // Update the current metadata
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory
                        .decodeResource(getResources(),
                                R.mipmap.ic_launcher))
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mMusicList.get(position)
                        .getSinger())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "artist")
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, mMusicList.get(position)
                        .getName())
                .build());
    }

    public void skipToNext() {
        if (position == mMusicList.size() - 1) {
            position = 0;
        } else {
            position += 1;
        }
        stopMedia();
        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();
    }

    public void playByList() {
        if (position < mMusicList.size() - 1) {
            position += 1;
            stopMedia();
            //reset mediaPlayer
            mediaPlayer.reset();
            initMediaPlayer();
            return;
        }
    }

    public void playByRandom() {
        position = (int) (Math.random() * mMusicList.size());
        stopMedia();
        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();
    }


    public void skipToPrevious() {

        if (position == 0) {
            //if first in playlist
            //set index to the last of mMusicList
            position = mMusicList.size() - 1;
        } else {
            //get previous in playlist
            position -= 1;
        }

        stopMedia();
        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();
    }

    private void buildNotification(PlaybackStatus playbackStatus) {

        int notificationAction = android.R.drawable.ic_media_pause;//needs to be initialized
        PendingIntent play_pauseAction = null;

        //Build a new notification according to the current state of the MediaPlayer
        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = android.R.drawable.ic_media_pause;
            //create the pause action
            play_pauseAction = playbackAction(1);
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = android.R.drawable.ic_media_play;
            //create the play action
            play_pauseAction = playbackAction(0);
        }

        // Create a new Notification
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new
                NotificationCompat.Builder(this)
                .setShowWhen(false)
                // Set the Notification style
                .setStyle(new NotificationCompat.MediaStyle()
                        // Attach our MediaSession token
                        .setMediaSession(mediaSession.getSessionToken())
                        // Show our playback controls in the compact notification view.
                        .setShowActionsInCompactView(0, 1, 2))
                // Set the Notification color
                .setColor(getResources().getColor(R.color.colorPrimary))
                // Set the large and small icons
//                .setLargeIcon(largeIcon)
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                // Set Notification content information
//                .setContentText(mMusicList.get(position).getArtist())
                .setContentTitle("Lite Music")
//                .setContentInfo(activeAudio.getTitle())
                // Add playback actions
                .addAction(android.R.drawable.ic_media_previous, "previous", playbackAction(3))
                .addAction(notificationAction, "pause", play_pauseAction)
                .addAction(android.R.drawable.ic_media_next, "next", playbackAction(2));

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify
                (NOTIFICATION_ID, notificationBuilder.build());
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context
                .NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent();
        switch (actionNumber) {
            case 0:
                // Play
                playbackAction.setAction(ACTION_PLAY);
                playbackAction.putExtra("todo", 0);
                return PendingIntent.getBroadcast(this, actionNumber, playbackAction, 0);
            case 1:
                // Pause
                playbackAction.setAction(ACTION_PAUSE);
                playbackAction.putExtra("todo", 1);
                return PendingIntent.getBroadcast(this, actionNumber, playbackAction, 0);
            case 2:
                // Next track
                playbackAction.setAction(ACTION_NEXT);
                playbackAction.putExtra("todo", 2);
                return PendingIntent.getBroadcast(this, actionNumber, playbackAction, 0);
            case 3:
                // Previous track
                playbackAction.setAction(ACTION_PREVIOUS);
                playbackAction.putExtra("todo", 3);
                return PendingIntent.getBroadcast(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    public int getDurationInMilliseconds() {
        if (mediaPlayer.isPlaying() && !getDuration) {
            time = mediaPlayer.getDuration();
            getDuration = true;
        }
        return time;
    }

    public int getCurrentPosition() {
        if (mediaPlayer.isPlaying()) {
            return mediaPlayer.getCurrentPosition();
        } else {
            return resumePosition;
        }
    }

    public boolean myIsPlaying() {
        return mediaPlayer.isPlaying();
    }

    public void mySeekTo(int mesc) {
        pauseMedia();
        setResumePosition(mesc);
        resumeMedia();
    }

}
