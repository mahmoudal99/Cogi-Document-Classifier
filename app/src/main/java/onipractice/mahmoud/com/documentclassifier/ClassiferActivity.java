package onipractice.mahmoud.com.documentclassifier;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static android.widget.Toast.LENGTH_SHORT;

public class ClassiferActivity extends AppCompatActivity {

    //Widgets
    Button start;
    FloatingActionButton process;
    TextView result;

    //Variables
    double probability;
    int totalNumOfWordsInCategory;
    int numberOfUniqueWordsInAllDocs;
    String currentSubCategory;
    String categoryChosen;
    int subCategoryIndex;
    String pathToDocChosenByUser;
    double highestProbability;
    String correctFile;

    //SharedPreferences
    SharedPreferences.Editor editor;
    SharedPreferences prefs;

    Set<String> wordsOfCat;
    Set<String> wordsToRemove;
    Map<String, Integer> frequencyOfWordsInCategory;
    Map<String, Double> probsOfDocGivenSubCat;
    ArrayList<String> topic;
    ArrayList<String> history;
    ArrayList<String> politics;
    ArrayList<String> geography;
    ArrayList<String> biology;


    //BufferReader
    BufferedReader reader;
    BufferedReader readWordsToRemove;

    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calssifier_layout);

        setupWidgets();
        setHistory();
        setBiology();
        setGeography();
        getIncomingIntent();

        //Initialzing the shared preferences
        prefs = getSharedPreferences("Categories", Context.MODE_PRIVATE);
        editor = prefs.edit();


        saveArrayList(history, "History");
        saveArrayList(biology, "Biology");
        saveArrayList(geography, "Geography");

        topic.addAll(getArrayList(categoryChosen));

        startAsyncTask();
        init();
    }

    private void init(){

        process.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                subCategoryIndex = 0;
                //Loop through sub categories of a Category and get their probability
                while (subCategoryIndex < topic.size())
                {
                    currentSubCategory = topic.get(subCategoryIndex);
                    loadSubCategory(currentSubCategory);
                    classify(pathToDocChosenByUser);
                    subCategoryIndex++;
                }

                highestProbability = (Collections.max(probsOfDocGivenSubCat.values()));

                //Loop through all probabilities of choose the highest
                for(String word : probsOfDocGivenSubCat.keySet())
                {
                    String key = word.toString();
                    double value = probsOfDocGivenSubCat.get(word);

                    if(value == highestProbability)
                    {
                        correctFile = key;
                    }
                }
                result.setText("Document belongs to " + correctFile + " " + highestProbability);
            }
        });

    }

    private static class loadRemovableWordsTask extends AsyncTask<Integer, Integer, String> {

        private WeakReference<ClassiferActivity> activityWeakReference;

        loadRemovableWordsTask(ClassiferActivity activity) {
            activityWeakReference = new WeakReference<ClassiferActivity>(activity);
        }

        @Override
        protected String doInBackground(Integer... integers) {

            ClassiferActivity activity = activityWeakReference.get();
            if (activity == null || activity.isFinishing()) {
                return "Done";
            }

            activity.wordsToRemove = new HashSet<String>();

            //Read the text file of the chosen category using a BufferedReader
            try {
                activity.readWordsToRemove = new BufferedReader(new InputStreamReader(activity.getAssets().open("WordsToRemove.txt")));
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                String current = activity.readWordsToRemove.readLine();
                while (current != null) {

                    if (!current.trim().equals("")) {
                        String[] words = current.split(" ");
                        for (String word : words) {
                            if (word == null || word.trim().equals("")) {
                                continue;
                            }
                            activity.wordsToRemove.add(word);
                        }
                    }
                    current = activity.readWordsToRemove.readLine();
                }

                Log.d("MAIN", "Loaded All!!!!!");

            } catch (IOException e) {
                e.printStackTrace();
            }
            return "Loaded";
        }
    }

    //Initialize variables used in the layout
    private void setupWidgets(){

        topic = new ArrayList<>();
        history = new ArrayList<>();
        geography = new ArrayList<>();
        politics = new ArrayList<>();
        biology = new ArrayList<>();
        start = (Button)findViewById(R.id.start);
        probsOfDocGivenSubCat = new HashMap<>();
        process = (FloatingActionButton) findViewById(R.id.process);
        result = (TextView) findViewById(R.id.result);

    }

    //Adding the possible categories into the History Array List
    public void setHistory()
    {
        history.add("MongolEmpire");
        history.add("WorldWar2");
    }

    //Adding the possible categories into the Biology Array List
    public void setBiology()
    {
        biology.add("Eye");
        biology.add("Heart");

    }

    public void setGeography(){

        geography.add("Volcanoes");
        geography.add("NitrogenCycle");

    }

    //Receives the option chosen by the user through an intent
    private void getIncomingIntent()
    {
        intent = getIntent();

        if (intent.hasExtra("History"))
        {
            categoryChosen = intent.getStringExtra("History");
            pathToDocChosenByUser = intent.getStringExtra("path");

        }
        else if(intent.hasExtra("Biology")){

            categoryChosen = intent.getStringExtra("Biology");
            pathToDocChosenByUser = intent.getStringExtra("path");
        }
        else if(intent.hasExtra("Geography")){

            categoryChosen = intent.getStringExtra("Geography");
            pathToDocChosenByUser = intent.getStringExtra("path");
        }
    }

    //Saves the array list of the categories to shared preferences
    public void saveArrayList(ArrayList<String> list, String key){

        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString(key, json);
        editor.apply();

    }

    //Loads array list based on category chosen
    public ArrayList<String> getArrayList(String key){

        Gson gson = new Gson();
        String json = prefs.getString(key, null);
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        return gson.fromJson(json, type);

    }

    public void startAsyncTask() {

        loadRemovableWordsTask task1 = new loadRemovableWordsTask(ClassiferActivity.this);
        task1.execute(10);
    }

    //Loads the Category chosen by the user
    private void loadSubCategory(String subCategory)
    {

        //Read the text file of the chosen category using a BufferedReader
        try {
            reader = new BufferedReader(new InputStreamReader(getAssets().open(subCategory + ".txt")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Calling the Classifier Function
        loadCurrentSubCategory(reader);

    }


    //Loads a subcategory text file and gets the frequency of each word in the text file
    // The frequency is stored in a Map
    private void loadCurrentSubCategory(BufferedReader reader)
    {

        //Stores the total number of all words in the sub Category
        totalNumOfWordsInCategory = 0;
        numberOfUniqueWordsInAllDocs = 0;

        //Stores the word and its frequency in the subCategory
        frequencyOfWordsInCategory = new HashMap<>();

        try {

            String line = reader.readLine();

            while (line != null) {

                if (!line.trim().equals("")) {

                    String[] words = line.split(" ");

                    for (String word : words) {

                        if (word == null || word.trim().equals("")) {

                            continue;

                        }

                        String processed = word.toLowerCase();
                        processed = processed.replace(",", "");
                        totalNumOfWordsInCategory++;

                        if(frequencyOfWordsInCategory.containsKey(processed)) {

                            frequencyOfWordsInCategory.put(processed, frequencyOfWordsInCategory.get(processed) + 1);

                        } else {

                            frequencyOfWordsInCategory.put(processed, 1);

                        }
                    }
                }

                line = reader.readLine();

            }

            //Stores number of all words in all docs
            numberOfUniqueWordsInAllDocs += totalNumOfWordsInCategory;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Classifies Document based on the highest probability given a certain sub category of a Topic
    private void classify(String documentChosenByUser)
    {
        probability = 0.0;


        File file = new File(Environment.getExternalStorageDirectory(), documentChosenByUser);

        wordsOfCat = new HashSet<>();

        try {

            BufferedReader readFile = new BufferedReader(new FileReader(file));
            String line = readFile.readLine();

            while(line != null) {

                if(!line.trim().equals("")) {

                    String [] words = line.split(" ");

                    for(String word : words) {

                        if(word == null || word.trim().equals("")) {
                            continue;
                        }

                        String processed = word.toLowerCase();
                        processed = processed.replace(",", "");

                        wordsOfCat.add(processed);

                    }

                }

                wordsOfCat.removeAll(wordsToRemove);

                Map<String, Integer> newMap = new HashMap<>();

                for(String word : wordsOfCat)
                {

                    if(frequencyOfWordsInCategory.containsKey(word))
                    {
                        newMap.put(word, frequencyOfWordsInCategory.get(word));
                    }
                }

                for (String name: newMap.keySet()){

                    probability += (double) (newMap.get(name) + 1) / (totalNumOfWordsInCategory + numberOfUniqueWordsInAllDocs);

                }

                line = reader.readLine();
                probsOfDocGivenSubCat.put(currentSubCategory, probability);

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}





























