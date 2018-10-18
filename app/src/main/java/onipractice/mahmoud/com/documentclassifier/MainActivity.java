package onipractice.mahmoud.com.documentclassifier;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    CardView historycardId;
    CardView biocardId;
    CardView politicscardId;
    CardView geocardId;
    LinearLayout linearLayout;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        historycardId = (CardView) findViewById(R.id.historycardId);
        biocardId = (CardView) findViewById(R.id.biocardId);
        politicscardId = (CardView) findViewById(R.id.politicscardId);
        geocardId = (CardView) findViewById(R.id.geocardId);
        historycardId.setOnClickListener(this);
        biocardId.setOnClickListener(this);
        geocardId.setOnClickListener(this);
        politicscardId.setOnClickListener(this);
        linearLayout = (LinearLayout) findViewById(R.id.linearLayout);
        linearLayout.setBackgroundColor(getColor(R.color.bg));
    }

    @Override
    public void onClick(View view) {

        Intent intent;

        switch (view.getId()){

            case R.id.historycardId:
                intent = new Intent(this, ClassiferActivity.class);
                intent.putExtra("History", "History");
                startActivity(intent);
                break;
            case R.id.politicscardId:
                intent = new Intent(this, ClassiferActivity.class);
                intent.putExtra("Politics", "Politics");
                startActivity(intent);
                break;
            case R.id.biocardId:
                intent = new Intent(this, ClassiferActivity.class);
                intent.putExtra("Biology", "Biology");
                startActivity(intent);
                break;
            case R.id.geocardId:
                intent = new Intent(this, ClassiferActivity.class);
                intent.putExtra("Geography", "Geography");
                startActivity(intent);

        }
    }
}























