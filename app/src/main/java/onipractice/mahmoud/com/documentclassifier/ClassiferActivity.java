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

    private static final int PERMISSION_REQUEST_STORAGE = 1000;
    private static final int READ_REQUEST_CODE = 42;

    //Widgets
    Button start;
    String fileName;
    String text;
    FloatingActionButton loadFileFA;
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

    String[] extraKeywords;
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
    InputStream inputStream;

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

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_STORAGE);
        }

        writeFile();
        loadRemovableWords();
        init();
    }

    private void init(){

        //When clicked user is given option to choose file they want to classify
        loadFileFA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Function Call
                performFileSearch();
            }
        });

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

    //Loads a list of pronouns and determiners which are removed from the document picked by user
    private void loadRemovableWords(){

        wordsToRemove = new HashSet<String>();

        //Read the text file of the chosen category using a BufferedReader
        try {
            readWordsToRemove = new BufferedReader(new InputStreamReader(getAssets().open("WordsToRemove.txt")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {

            String current = readWordsToRemove.readLine();

            while (current != null) {

                if (!current.trim().equals("")) {

                    String[] words = current.split(" ");

                    for (String word : words) {

                        if (word == null || word.trim().equals("")) {

                            continue;

                        }

                        wordsToRemove.add(word);
                    }
                }

                current = readWordsToRemove.readLine();

            }

        } catch (IOException e) {
            e.printStackTrace();
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
        loadFileFA = (FloatingActionButton) findViewById(R.id.load);
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

        }
        else if(intent.hasExtra("Biology")){

            categoryChosen = intent.getStringExtra("Biology");
        }
        else if(intent.hasExtra("Geography")){

            categoryChosen = intent.getStringExtra("Geography");
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

    //Creates a text file and saves it into phone storage
    public void writeFile(){

        fileName = "Volcanoes.txt";
        text = "The most common perception of a volcano is of a conical mountain, " +
                "spewing lava and poisonous gases from a crater at its summit; however, " +
                "this describes just one of the many types of volcano. The features of volcanoes are much more complicated and their structure and" +
                " behavior depends on a number of factors. Some volcanoes have rugged peaks formed by lava domes rather than a summit crater while " +
                "others have landscape features such as massive plateaus. Vents that issue volcanic material (including lava and ash) " +
                "and gases (mainly steam and magmatic gases) can develop anywhere on the landform and may give rise to smaller cones such as Puʻu ʻŌʻō on a " +
                "flank of Hawaii's Kīlauea. Other types of volcano include cryovolcanoes (or ice volcanoes), particularly on some moons of Jupiter, " +
                "Saturn, and Neptune; and mud volcanoes, which are formations often not associated with known magmatic activity. " +
                "Active mud volcanoes tend to involve " +
                "temperatures much lower than those of igneous volcanoes except when the mud volcano is actually a vent of an igneous volcano. ";

        if(isExternalStorageWritable() && checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)){

            File path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);

            File textFile = new File(path, fileName);
            try{
                FileOutputStream fos = new FileOutputStream(textFile);
                fos.write(text.getBytes());
                fos.close();

                Toast.makeText(this, "File Saved.", LENGTH_SHORT).show();
            }catch (IOException e){
                e.printStackTrace();
            }
        }else{
            Toast.makeText(this, "Cannot Write to External Storage.", LENGTH_SHORT).show();
        }
    }

    //Search for file
    private void performFileSearch()
    {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
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

    //Permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == PERMISSION_REQUEST_STORAGE){

            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "Permission Granted", LENGTH_SHORT).show();
            }else {
                Toast.makeText(this, "Permission Denied", LENGTH_SHORT).show();
                finish();
            }
        }
    }

    public boolean checkPermission(String permission){
        int check = ContextCompat.checkSelfPermission(this, permission);
        return (check == PackageManager.PERMISSION_GRANTED);
    }

    private boolean isExternalStorageWritable(){
        if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
            Log.i("State","Yes, it is writable!");
            return true;
        }else{
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if(requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK){

            if(data != null)
            {
                Uri uri = data.getData();
                pathToDocChosenByUser = uri.getPath();
                pathToDocChosenByUser = pathToDocChosenByUser.substring(pathToDocChosenByUser.indexOf(":") + 1);

            }

        }
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

                            frequencyOfWordsInCategory.put(processed,

                                    frequencyOfWordsInCategory.get(processed) + 1);

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





























