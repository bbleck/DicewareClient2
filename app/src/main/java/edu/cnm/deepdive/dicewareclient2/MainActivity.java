package edu.cnm.deepdive.dicewareclient2;

import android.os.AsyncTask;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = "MainActivity";

  private ListView words;
  private Button generate;
  private DicewareService service;
  private ProgressBar progBar;
  private TextInputEditText length;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    initViews();
    setupService();
  }

  private void setupService() {
    Gson gson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .create();
    service = new Retrofit.Builder()
        .baseUrl("http://10.0.2.2:8080/") //this is base url for the emulator to see your machine
//        .baseUrl("http://")             //this is base url for using an attached device
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(DicewareService.class);
  }

  private void initViews() {
    words = findViewById(R.id.lvWords);
    generate = findViewById(R.id.btnGenerate);
    progBar = findViewById(R.id.pbSpinner);
    length = findViewById(R.id.etLength);
    generate.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        new GenerateTask().execute();
        generate.setClickable(false);
      }
    });
    length.addTextChangedListener(new TextWatcher() {
      CharSequence before;
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        before = s;
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {

      }

      @Override
      public void afterTextChanged(Editable s) {
        try{
          Integer.parseInt(s.toString());
        }catch(NumberFormatException e){
          length.removeTextChangedListener(this);
          length.setText(before);
          length.addTextChangedListener(this);
        }
      }
    });
  }

  //async types:  1. what type of info is passed in, 2. what type of info is async going to use to update the UI, 3. what type of info is returning
  private class GenerateTask extends AsyncTask<Void, Void, String[]> {

    @Override
    protected void onPreExecute() {
      progBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected String[] doInBackground(Void... voids) {
      String[] passphrase = null;
      try {
        Call<String[]> call = service.get(Integer.parseInt(length.getText().toString()));
        Response<String[]> response = call.execute();
        if(response.isSuccessful()){//200 range is success, 300 is redirect, 400 is you did something wrong, 500 is server is doing something wrong
          passphrase = response.body();
        }
      } catch (IOException e) {
        Log.d(TAG, "doInBackground: IOException" + e.toString());
        // Do nothing; passphrase is null.
      } finally {
        if(passphrase==null){
          cancel(true);
        }
      }
      return passphrase;
    }

    @Override
    protected void onPostExecute(String[] strings) {
      ArrayAdapter<String> adapter =
          new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, strings);
      progBar.setVisibility(View.INVISIBLE);
      words.setAdapter(adapter);
      generate.setClickable(true);
    }

    @Override
    protected void onCancelled(String[] strings) {
      progBar.setVisibility(View.INVISIBLE);
      generate.setClickable(true);
      Toast.makeText(MainActivity.this, "Unable to obtain passphrase.", Toast.LENGTH_SHORT).show();
    }
  }

}
