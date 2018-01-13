package ru.garaz.flaqquiz;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public static final String CHOICES = "pref_numberOfChoices";
    public static final String REGIONS = "pref_regionsToInclude";

    private boolean phoneDevice = true; //Включение портретного режима
    private boolean preferenceChanged = true; //Изменились ли настройки


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Задание значений по умолчанию
        PreferenceManager.setDefaultValues(this, R.xml.preference, false);

        //Регистрация слушателя для изменений
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        //Определение размерв экрана
        int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

        //Проверка плашет или телефон, для планшета уст false
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE || screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            phoneDevice = false;
        }

        //Для телефона разрешена только портретная ориентация
        if (phoneDevice == true) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        //FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        //fab.setOnClickListener(new View.OnClickListener() {
        //  @Override
        //public void onClick(View view) {
        //     Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
        //             .setAction("Action", null).show();
        //}
        //}
        // )
        ;
    }

    @Override
    protected void OnStart() {
        super.onStart();

        if (preferenceChanged) {
            MainActivityFragment quizFragment = (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.quizFragment);
            quizFragment.updateGuessRows(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.updateRegions(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.resetQuiz();
            preferenceChanged = false;

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        } else
            return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent preferenceIntent = new Intent(this, SettingsActivity.class);
        startActivity(preferenceIntent);
        return super.onOptionsItemSelected(item);
    }
}

private OnSharedPreferenceChangeListener preferenceChangeListener = new OnSharedPreferenceChangeListener(){
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        preferenceChanged = true;

        MainActivityFragment quizFragment = (MainActivityFragment)getSupportFragmentManager().findFragmentById(R.id.quizFragment);

        if(key.equals(CHOICES)) {  // Если изменилась сложность т.е кол-во вариантов
            quizFragment.updateGuessRows(sharedPreferences);
            quizFragment.resetQuiz();
        }
        else if (key.equals(REGIONS)) {  // Изменился регион
            Set<String> regions = sharedPreferences.getStringSet(REGIONS, null);// Берём список реогиионов записанный в файле настройки


            if ( regions != null && regions.size() > 0){// Если регионы выбраны делаем апдейт.
                quizFragment.updateRegions(sharedPreferences);
                quizFragment.resetQuiz();

            }

            else { // Если ничего не выбрано - режим по умолчанию
                //Хотя бы один регион по умолчанию Северная Америка
                SharedPreferences.Editor editor = sharedPreferences.edit(); // Загружаем наш файл в редактор
                regions.add(getString(R.string.default_region)); // Добавляем в переменную regions значение default_region
                editor.putStringSet(REGIONS, regions);// Записываем в файл с ключом REGIONS
                editor.apply();

                Toast.makeText(MainActivity.this, R.string.default_region_message, Toast.LENGTH_SHORT).show();
            }
        }
            Toast.makeText(MainActivity.this, R.string.restarting_quiz, Toast.LENGTH_SHORT).show();



        }
    };


}