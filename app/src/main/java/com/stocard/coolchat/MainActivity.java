package com.stocard.coolchat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ListView chatListView;
    private EditText editText;
    private ProgressDialog dialog;

    @Nullable
    private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chatListView = findViewById(R.id.chat_list);
        editText = findViewById(R.id.message_input);
        Button buttonSend = findViewById(R.id.send_button);

        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String input = editText.getText().toString();
                send(input);
                editText.setText(null);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchMessages();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        name = prefs.getString("name", null);

        if (name == null) {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        }
    }

    private void send(String message) {
        new PostMessageTask().execute("https://android-hackschool.herokuapp.com/message", name, message);
    }

    private void fetchMessages() {
        new GetMessagesTask().execute("https://android-hackschool.herokuapp.com");
    }

    private void showMessages(List<Message> messages) {
        List<String> messageItems = new ArrayList<>();
        for (Message message : messages) {
            messageItems.add(message.getName() + ": " + message.getMessage());
        }
        chatListView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messageItems));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Refresh").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        fetchMessages();
        return true;
    }

    /**
     * Borrowed form https://stackoverflow.com/a/37525989/570168.
     */
    private class GetMessagesTask extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            super.onPreExecute();

            dialog = new ProgressDialog(MainActivity.this);
            dialog.setMessage("Fetching messages, please wait");
            dialog.setCancelable(false);
            dialog.show();
        }

        protected String doInBackground(String... params) {

            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                Thread.sleep(100); // for some more drama on fast networks ;-)
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();


                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                    Log.d("Response: ", "> " + line);
                }

                return buffer.toString();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            Moshi moshi = new Moshi.Builder().build();
            Type type = Types.newParameterizedType(List.class, Message.class);
            JsonAdapter<List<Message>> jsonAdapter = moshi.adapter(type);
            List<Message> messages = null;
            try {
                messages = jsonAdapter.fromJson(result);
            } catch (IOException e) {
                e.printStackTrace();
            }
            showMessages(messages);
        }
    }

    private class PostMessageTask extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            super.onPreExecute();

            dialog = new ProgressDialog(MainActivity.this);
            dialog.setMessage("Sending, please wait");
            dialog.setCancelable(false);
            dialog.show();
        }

        protected String doInBackground(String... params) {

            HttpURLConnection connection = null;

            try {
                Thread.sleep(100); // for some more drama on fast networks ;-)
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.connect();

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("name", params[1]);
                jsonParam.put("message", params[2]);

                DataOutputStream os = new DataOutputStream(connection.getOutputStream());
                os.writeBytes(jsonParam.toString());

                os.flush();
                os.close();

                Log.i("STATUS", String.valueOf(connection.getResponseCode()));
                Log.i("MSG", connection.getResponseMessage());

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            fetchMessages();
        }
    }
}
