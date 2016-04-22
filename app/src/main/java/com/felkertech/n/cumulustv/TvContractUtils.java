/*
 * Copyright 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.felkertech.n.cumulustv;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.tv.TvContentRating;
import android.media.tv.TvContract;
import android.media.tv.TvContract.Channels;
import android.media.tv.TvContract.Programs;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.Pair;
import android.util.SparseArray;
import android.widget.Toast;

import com.felkertech.channelsurfer.model.Channel;
import com.felkertech.channelsurfer.model.Program;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Static helper methods for working with {@link android.media.tv.TvContract}.
 */
public class TvContractUtils {
    private static final String TAG = "cumulus:TvContractUtils";
    private static final boolean DEBUG = true;

    private static final SparseArray<String> VIDEO_HEIGHT_TO_FORMAT_MAP =
            new SparseArray<String>();

    static {
        VIDEO_HEIGHT_TO_FORMAT_MAP.put(480, TvContract.Channels.VIDEO_FORMAT_480P);
        VIDEO_HEIGHT_TO_FORMAT_MAP.put(576, TvContract.Channels.VIDEO_FORMAT_576P);
        VIDEO_HEIGHT_TO_FORMAT_MAP.put(720, TvContract.Channels.VIDEO_FORMAT_720P);
        VIDEO_HEIGHT_TO_FORMAT_MAP.put(1080, TvContract.Channels.VIDEO_FORMAT_1080P);
        VIDEO_HEIGHT_TO_FORMAT_MAP.put(2160, TvContract.Channels.VIDEO_FORMAT_2160P);
        VIDEO_HEIGHT_TO_FORMAT_MAP.put(4320, TvContract.Channels.VIDEO_FORMAT_4320P);
    }

    public static void updateChannels(
            Context context, String inputId, List<Channel> channels) {
        // Create a map from original network ID to channel row ID for existing channels.
        SparseArray<Long> mExistingChannelsMap = new SparseArray<Long>();
        Uri channelsUri = TvContract.buildChannelsUriForInput(inputId);
        Log.d(TAG, "For "+inputId);
        Log.d(TAG, "Creating cursor for "+channelsUri.toString());
        Uri all = Uri.parse("content://android.media.tv/channel");
        Cursor cursor = null;
        ContentResolver resolver = context.getContentResolver();
        try {
            cursor = resolver.query(all, null, null, null, null);
            Log.d(TAG, "Found "+cursor.getCount()+" items in allcursor");
            while (cursor != null && cursor.moveToNext()) {
                long rowId = cursor.getLong(cursor.getColumnIndex(Channels._ID));
                int originalNetworkId = cursor.getInt(cursor.getColumnIndex(Channels.COLUMN_ORIGINAL_NETWORK_ID));
                Log.d(TAG, "Seeing oni "+originalNetworkId+" to "+rowId+" "+cursor.getString(cursor.getColumnIndex(Channels.COLUMN_DISPLAY_NAME)));
                mExistingChannelsMap.put(cursor.getInt(cursor.getColumnIndex(Channels.COLUMN_ORIGINAL_NETWORK_ID)), rowId);
                mExistingChannelsMap.put(cursor.getString(cursor.getColumnIndex(Channels.COLUMN_DISPLAY_NUMBER)).hashCode(), rowId);
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage()+"");
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        /*Log.d(TAG, "Now ping 2");
        String[] projection = {Channels._ID, Channels.COLUMN_ORIGINAL_NETWORK_ID, Channels.COLUMN_SERVICE_ID, Channels.COLUMN_DISPLAY_NAME};
        cursor = null;
        resolver = context.getContentResolver();
        try {
            cursor = resolver.query(channelsUri, projection, null, null, null);
            Log.d(TAG, "Found "+cursor.getCount()+" items in cursor");
            while (cursor != null && cursor.moveToNext()) {
                long rowId = cursor.getLong(0);
                int originalNetworkId = cursor.getInt(1);
//                Log.d(TAG, "Assigning oni "+originalNetworkId+" to "+rowId+" "+cursor.getInt(2)+", "+cursor.getString(3));
//                mExistingChannelsMap.put(cursor.getInt(1), rowId);
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }*/

        // If a channel exists, update it. If not, insert a new one.
        ContentValues values = new ContentValues();
        values.put(Channels.COLUMN_INPUT_ID, inputId);
        Map<Uri, String> logos = new HashMap<Uri, String>();
        if(channels == null) {
            Toast.makeText(context, "You have no channels set!", Toast.LENGTH_SHORT).show();
            return; //You have NO channels
        }
        for (Channel channel : channels) {
            Log.d(TAG, "Trying oni "+channel.getOriginalNetworkId()+" "+mExistingChannelsMap.get(channel.getOriginalNetworkId())+" "+channel.getServiceId());
            values.put(Channels.COLUMN_DISPLAY_NUMBER, channel.getNumber());
            values.put(Channels.COLUMN_DISPLAY_NAME, channel.getName());
            Long rowId = mExistingChannelsMap.get(channel.getOriginalNetworkId());
            if(rowId == null) {
                values.put(Channels.COLUMN_ORIGINAL_NETWORK_ID, channel.getOriginalNetworkId());
                if(channel.getNumber() != null) {
                    rowId = mExistingChannelsMap.get(channel.getNumber().toString().hashCode());
                    Log.d(TAG, "Tried " + rowId + " as rowid");
                    if (rowId != null)
                        values.put(Channels.COLUMN_ORIGINAL_NETWORK_ID, rowId);
                } else {
                    throw new NullPointerException("Channel number must exist!");
                }
            } else
                values.put(Channels.COLUMN_ORIGINAL_NETWORK_ID, rowId);
//            values.put(Channels.COLUMN_TRANSPORT_STREAM_ID, channel.transportStreamId);
            values.put(Channels.COLUMN_TRANSPORT_STREAM_ID, 1); //FIXME Hack; can't get ChannelDatabase to output correctly
            values.put(Channels.COLUMN_SERVICE_ID, channel.getServiceId());
            String videoFormat = getVideoFormat(channel.getVideoHeight());
            if (videoFormat != null) {
                values.put(Channels.COLUMN_VIDEO_FORMAT, videoFormat);
            } else {
                values.putNull(Channels.COLUMN_VIDEO_FORMAT);
            }

            Uri uri;
            if (rowId == null) {
                if(channel.getNumber() == null)
                    channel.setNumber("0");
                values.put(Channels.COLUMN_ORIGINAL_NETWORK_ID, channel.getNumber().hashCode());
                Log.d(TAG, "Insert "+values.toString());
                uri = resolver.insert(TvContract.Channels.CONTENT_URI, values);
                if(uri != null) {
                    Log.d(TAG, uri.toString() + " " + uri.getLastPathSegment());
                    values.put(Channels.COLUMN_ORIGINAL_NETWORK_ID, uri.getLastPathSegment());
                    resolver.update(uri, values, null, null);
                    Log.d(TAG, "Changed oni");
                } else {
                    //Hmmm
                }
            } else {
                uri = TvContract.buildChannelUri(rowId);
                Log.d(TAG, "Update " + values.toString());
                Log.d(TAG, uri.toString()+"");
                resolver.update(uri, values, null, null);
                mExistingChannelsMap.remove(channel.getOriginalNetworkId());
                mExistingChannelsMap.remove(Math.round(rowId));
                mExistingChannelsMap.remove(channel.getNumber().hashCode());
            }
            if (!TextUtils.isEmpty(channel.getLogoUrl()) && false) { //FIXME Hack to show title
//                logos.put(TvContract.buildChannelLogoUri(uri), channel.logoUrl);
                Log.d(TAG, "LOGO "+uri.toString()+" "+channel.getLogoUrl());
                logos.put(TvContract.buildChannelLogoUri(uri), channel.getLogoUrl());
            }
            Log.d(TAG, mExistingChannelsMap.toString());
        }
        if (!logos.isEmpty()) {
            new InsertLogosTask(context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, logos);
        }

        // Deletes channels which don't exist in the new feed.
        int size = mExistingChannelsMap.size();
        for(int i = 0; i < size; ++i) {
            Long rowId = mExistingChannelsMap.valueAt(i);
            Log.d(TAG, "Deleting item at "+rowId+" with "+mExistingChannelsMap.keyAt(i));
            try {
                resolver.delete(TvContract.buildChannelUri(rowId), null, null);
            } catch(Exception ignored) {}
        }
    }

    private static String getVideoFormat(int videoHeight) {
        return VIDEO_HEIGHT_TO_FORMAT_MAP.get(videoHeight);
    }

    public static LongSparseArray<Channel> buildChannelMap(ContentResolver resolver,
                                                               String inputId, List<Channel> channels) {
        Uri uri = TvContract.buildChannelsUriForInput(inputId);
        String[] projection = {
                TvContract.Channels._ID,
                TvContract.Channels.COLUMN_DISPLAY_NUMBER
        };

        LongSparseArray<Channel> channelMap = new LongSparseArray<>();
        Cursor cursor = null;
        try {
            cursor = resolver.query(uri, projection, null, null, null);
            if (cursor == null || cursor.getCount() == 0) {
                return null;
            }

            while (cursor.moveToNext()) {
                long channelId = cursor.getLong(0);
                String channelNumber = cursor.getString(1);
                channelMap.put(channelId, getChannelByNumber(channelNumber, channels));
            }
        } catch (Exception e) {
            Log.d(TAG, "Content provider query: " + e.getStackTrace());
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return channelMap;
    }

    public static List<Program> getPrograms(ContentResolver resolver, Uri channelUri) {
        Uri uri = TvContract.buildProgramsUriForChannel(channelUri);
        Cursor cursor = null;
        List<Program> programs = new ArrayList<>();
        try {
            // TvProvider returns programs chronological order by default.
            cursor = resolver.query(uri, null, null, null, null);
            if (cursor == null || cursor.getCount() == 0) {
                return programs;
            }
            while (cursor.moveToNext()) {
                programs.add(Program.fromCursor(cursor));
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to get programs for " + channelUri, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return programs;
    }

    public static long getLastProgramEndTimeMillis(ContentResolver resolver, Uri channelUri) {
        Uri uri = TvContract.buildProgramsUriForChannel(channelUri);
        String[] projection = {Programs.COLUMN_END_TIME_UTC_MILLIS};
        Cursor cursor = null;
        try {
            // TvProvider returns programs chronological order by default.
            cursor = resolver.query(uri, projection, null, null, null);
            if (cursor == null || cursor.getCount() == 0) {
                return 0;
            }
            cursor.moveToLast();
            return cursor.getLong(0);
        } catch (Exception e) {
            Log.w(TAG, "Unable to get last program end time for " + channelUri, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }

    /*public static List<TvManager.PlaybackInfo> getProgramPlaybackInfo(
            ContentResolver resolver, Uri channelUri, long startTimeMs, long endTimeMs,
            int maxProgramInReturn) {
        Uri uri = TvContract.buildProgramsUriForChannel(channelUri, startTimeMs,
                endTimeMs);
        String[] projection = { Programs.COLUMN_START_TIME_UTC_MILLIS,
                Programs.COLUMN_END_TIME_UTC_MILLIS,
                Programs.COLUMN_CONTENT_RATING,
                Programs.COLUMN_INTERNAL_PROVIDER_DATA,
                Programs.COLUMN_CANONICAL_GENRE };
        Cursor cursor = null;
        List<TvManager.PlaybackInfo> list = new ArrayList<>();
        try {
            cursor = resolver.query(uri, projection, null, null, null);
            while (cursor.moveToNext()) {
                long startMs = cursor.getLong(0);
                long endMs = cursor.getLong(1);
                TvContentRating[] ratings = stringToContentRatings(cursor.getString(2));
                Pair<Integer, String> values = parseInternalProviderData(cursor.getString(3));
                String[] genres = Programs.Genres.decode(cursor.getString(4));
                int videoType = values.first;
                String videoUrl = values.second;
                list.add(new TvManager.PlaybackInfo(startMs, endMs, videoUrl, videoType, ratings));
                if (list.size() > maxProgramInReturn) {
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get program playback info from TvProvider.", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return list;
    }*/

    @Deprecated
    public static String convertVideoInfoToInternalProviderData(int videotype, String videoUrl) {
        return videotype + "," + videoUrl;
    }

    //TODO Use this for urls
    @Deprecated
    public static Pair<Integer, String> parseInternalProviderData(String internalData) {
        String[] values = internalData.split(",", 2);
        if (values.length != 2) {
            throw new IllegalArgumentException(internalData);
        }
        return new Pair<>(Integer.parseInt(values[0]), values[1]);
    }

    public static void insertUrl(Context context, Uri contentUri, URL sourceUrl) {
        if (DEBUG) {
            Log.d(TAG, "Inserting " + sourceUrl + " to " + contentUri);
        }
        InputStream is = null;
        OutputStream os = null;
        try {
            is = sourceUrl.openStream();
            os = context.getContentResolver().openOutputStream(contentUri);
            copy(is, os);
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to write " + sourceUrl + "  to " + contentUri, ioe);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore exception.
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    // Ignore exception.
                }
            }
        }
    }

    public static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
    }

    public static String getServiceNameFromInputId(Context context, String inputId) {
        TvInputManager tim = (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE);
        for (TvInputInfo info : tim.getTvInputList()) {
            if (info.getId().equals(inputId)) {
                return info.getServiceInfo().name;
            }
        }
        return null;
    }

    public static TvContentRating[] stringToContentRatings(String commaSeparatedRatings) {
        if (TextUtils.isEmpty(commaSeparatedRatings)) {
            return null;
        }
        String[] ratings = commaSeparatedRatings.split("\\s*,\\s*");
        TvContentRating[] contentRatings = new TvContentRating[ratings.length];
        for (int i = 0; i < contentRatings.length; ++i) {
            contentRatings[i] = TvContentRating.unflattenFromString(ratings[i]);
        }
        return contentRatings;
    }

    public static String contentRatingsToString(TvContentRating[] contentRatings) {
        if (contentRatings == null || contentRatings.length == 0) {
            return null;
        }
        final String DELIMITER = ",";
        StringBuilder ratings = new StringBuilder(contentRatings[0].flattenToString());
        for (int i = 1; i < contentRatings.length; ++i) {
            ratings.append(DELIMITER);
            ratings.append(contentRatings[i].flattenToString());
        }
        return ratings.toString();
    }

    private static Channel getChannelByNumber(String channelNumber,
                                                  List<Channel> channels) {
        for (Channel info : channels) {
            if (info.getNumber().equals(channelNumber)) {
                return info;
            }
        }
        throw new IllegalArgumentException("Unknown channel: " + channelNumber);
    }

    private TvContractUtils() {}

    public static class InsertLogosTask extends AsyncTask<Map<Uri, String>, Void, Void> {
        private final Context mContext;

        InsertLogosTask(Context context) {
            mContext = context;
        }

        @Override
        public Void doInBackground(Map<Uri, String>... logosList) {
            for (Map<Uri, String> logos : logosList) {
                for (Uri uri : logos.keySet()) {
                    try {
                        insertUrl(mContext, uri, new URL(logos.get(uri)));
                    } catch (MalformedURLException e) {
                        Log.e(TAG, "Can't load " + logos.get(uri), e);
                    }
                }
            }
            return null;
        }
    }
}