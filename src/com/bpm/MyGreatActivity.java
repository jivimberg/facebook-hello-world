package com.bpm;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import org.json.JSONException;
import org.json.JSONObject;

public class MyGreatActivity extends Activity {

    private String username;
    private TextView userNameView;
    private SharedPreferences mPrefs;
    private Handler mHandler;
    private Button loginButton;
    private Toast toast;
    private ImageView userPicView;

    public MyGreatActivity() {
        mHandler = new Handler();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.bpm.R.layout.main);

        /*
        * Get existing access_token if any
        */
        mPrefs = getPreferences(MODE_PRIVATE);
        String access_token = mPrefs.getString("access_token", null);
        long expires = mPrefs.getLong("access_expires", 0);
        final Facebook facebook = ((MyApplication) getApplication()).facebook;
        if(access_token != null) {
            facebook.setAccessToken(access_token);
        }
        if(expires != 0) {
            facebook.setAccessExpires(expires);
        }

        /*
        * Only call authorize if the access_token has expired.
        */
        if(!facebook.isSessionValid()) {

            facebook.authorize(this, new String[] {"email", "user_about_me", "user_location","publish_actions"}, new Facebook.DialogListener() {
                @Override
                public void onComplete(Bundle values) {
                    SharedPreferences.Editor editor = mPrefs.edit();
                    editor.putString("access_token", facebook.getAccessToken());
                    editor.putLong("access_expires", facebook.getAccessExpires());
                    editor.commit();
                }

                @Override
                public void onFacebookError(FacebookError error) {}

                @Override
                public void onError(DialogError e) {}

                @Override
                public void onCancel() {}
            });
        }

        userNameView = (TextView)this.findViewById(R.id.user_name);
        userPicView = (ImageView)this.findViewById(R.id.user_pic);

        //open graph
        Bundle params = new Bundle();
        int screenLayoutSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        if (screenLayoutSize == Configuration.SCREENLAYOUT_SIZE_LARGE ||
                screenLayoutSize == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            params.putString("fields", "name, picture.type(large)");
        } else {
            params.putString("fields", "name, picture.type(normal)");
        }

        ((MyApplication)getApplication()).asyncRunner.request("me",params, new UserDataRequestListener());
        userNameView.setText(username);


        loginButton = (Button)findViewById(R.id.login_with_facebook);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle params = new Bundle();
                params.putString("meeting", "https://sultry-oasis-3294.herokuapp.com/meeting.php");
                /*if(userMessage.getText() != null && userMessage.getText().length() > 0) {
                    params.putString("message", userMessage.getText().toString());
                }*/
                ((MyApplication)getApplication()).asyncRunner.request("me/com-bpm:attend", params,
                        "POST", new AfterMeetingListener(), 1);
            }
        });
        toast = Toast.makeText(getApplicationContext(), "POST made succesfully", 2000);

    }

    /*
    * Callback for fetching current user's name, picture, uid.
    */
    private class UserDataRequestListener extends BaseRequestListener {

        public void onComplete(final String response, final Object state) {
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(response);

                username = jsonObject.getString("name");
                final String picURL = jsonObject.getJSONObject("picture").getJSONObject("data").getString("url");
                /* At this point we are in the facebook sdk asyncrunner thread,
                     * the UI must however be updated in the UI thread.
                     */
                mHandler.post(new Runnable() {
                    public void run() {
                        userNameView.setText(username);
                        new DownloadPictureTask().execute(picURL, null, null);

                    }
                });

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private class DownloadPictureTask extends AsyncTask<String, Void, Bitmap> {

        protected Bitmap doInBackground(String... urls) {
            return Utility.getBitmap(urls[0]);
        }

        protected void onProgressUpdate(Void... progress) {
        }

        protected void onPostExecute(Bitmap result) {
//            userPic = result;
            userPicView.setImageBitmap(result);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        ((MyApplication)getApplication()).facebook.authorizeCallback(requestCode, resultCode, data);
    }

    private class AfterMeetingListener extends BaseRequestListener {
        @Override
        public void onComplete(String s, Object o) {
            toast.show();
        }
    }
}

