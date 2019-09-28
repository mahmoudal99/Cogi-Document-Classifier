package onipractice.mahmoud.com.documentclassifier;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import static android.widget.Toast.LENGTH_SHORT;

public class ChooseFileActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_STORAGE = 1000;
    private static final int READ_REQUEST_CODE = 42;

    FloatingActionButton loadFileFA;
    String pathToDocChosenByUser;
    Intent intent;
    String categoryChosen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_file);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_STORAGE);
        }

        setupWidgets();
        init();
    }

    //Initialize variables used in the layout
    private void setupWidgets() {
        loadFileFA = findViewById(R.id.load);
        getIncomingIntent();
    }

    private void init() {
        //When clicked user is given option to choose file they want to classify
        loadFileFA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Function Call
                performFileSearch();
            }
        });
    }

    //Search for file
    private void performFileSearch() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }


    //Permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission Denied", LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                pathToDocChosenByUser = uri.getPath();
                pathToDocChosenByUser = pathToDocChosenByUser.substring(pathToDocChosenByUser.indexOf(":") + 1);
                Intent intent = new Intent(ChooseFileActivity.this, ClassiferActivity.class);
                intent.putExtra(categoryChosen, categoryChosen);
                intent.putExtra("path", pathToDocChosenByUser);
                startActivity(intent);
            }
        }
    }


    //Receives the option chosen by the user through an intent
    private void getIncomingIntent() {
        intent = getIntent();
        if (intent.hasExtra("History")) {
            categoryChosen = intent.getStringExtra("History");
        } else if (intent.hasExtra("Biology")) {
            categoryChosen = intent.getStringExtra("Biology");
        } else if (intent.hasExtra("Geography")) {
            categoryChosen = intent.getStringExtra("Geography");
        }
    }
}





























