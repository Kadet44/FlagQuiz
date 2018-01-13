package ru.garaz.flaqquiz;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;


/**
 * Created by Dmitriy on 1/12/2018.
 */

public class MainActivityFragment extends Fragment {

    private static final String TAG = "FlagQuiz Activity"; // Переменная для логов
    private static final int FLAGS_IN_QUIZ = 10; // Количество флагов в киторине

    private List<String> fileNameList;//Имена файлов с флагами
    private List<String> quizCountriesList; // Страны текущей викторины
    private Set<String> regionsSet;// Регионы текущей викторины
    private String correctAnswer;// Правильный овет
    private int totalGuesses;// колво попыток
    private int correctAnswers;// колво правильных ответов
    private int guessRows; // Количество строк с кнопками вариантов
    private SecureRandom random;// Генератор случайных чисел
    private Handler handler;// Для задержки загрузки след флага
    private Animation shakeAnimation;// Анимация неправильного ответа

    private LinearLayout quizLinerLayout;// Макет с викториной
    private TextView questionNumberTextView;//Номер текущего вопроса
    private ImageView flagImageView;//Для выводв флага
    private LinearLayout[] guessLinerLayouts;//Строки с кнопками
    private TextView answerTextView;// Для правильного ответа


    @Override
    public View onCreateView(LayoutInflater inflater,  ViewGroup container,  Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_main, container, false); // Получение View от layout файла\

        fileNameList = new ArrayList<>();
        random = new SecureRandom();
        handler = new Handler();


        shakeAnimation = AnimationUtils.loadAnimation(getActivity(),R.anim.incorrect_shake); // Animation for wrog answer
        shakeAnimation.setRepeatCount(3); // repeat 3 times

        quizLinerLayout= (LinearLayout)view.findViewById(R.id.quizLinearLayout);
        questionNumberTextView= (TextView)view.findViewById(R.id.questionNumberTextView);
        flagImageView = (ImageView) view.findViewById(R.id.flagImageView);

        guessLinerLayouts = new LinearLayout[4]; // Максимально 4 строки по 2 кнопки на каждой
        guessLinerLayouts[0] = (LinearLayout)view.findViewById(R.id.row1LinerLayout);
        guessLinerLayouts[1] = (LinearLayout)view.findViewById(R.id.row2LinerLayout);
        guessLinerLayouts[2] = (LinearLayout)view.findViewById(R.id.row3LinerLayout);
        guessLinerLayouts[3] = (LinearLayout)view.findViewById(R.id.row4LinerLayout);

        answerTextView = (TextView)view.findViewById(R.id.answerTextView);

        for(LinearLayout row: guessLinerLayouts){
            for( int colum = 0; colum < row.getChildCount();colum++){ // Возвращает количество дочернинх элемнтов LinerLayout
                Button button = (Button) row.getChildAt(colum);
                button.setOnClickListener(guessButtonListener);

            }
        }

        questionNumberTextView.setText(getString(R.string.question,1 ,FLAGS_IN_QUIZ));
        return view;
    }
    // Обновление guessRows на основании SharedPreference
    public void updateGuessRows(SharedPreferences sharedPreferences){
        String choises = sharedPreferences.getString(MainActivity.CHOICES, null);
        guessRows = Integer.parseInt(choises) /2;

        //Скрыть все компоненты linerLayout
        for (LinearLayout layout: guessLinerLayouts){
            layout.setVisibility(View.GONE);

            //Отображение нужных компонентов
            for( int row = 0; row<guessRows;row++){
                guessLinerLayouts[row].setVisibility(View.VISIBLE);
            }
        }

    }

    //Обновление выбранных регионов по данным из SharedPreference
    public void updateRegions(SharedPreferences sharedPreferences){
        regionsSet = sharedPreferences.getStringSet(MainActivity.REGIONS, null);

    }
    //Настройка и запуск следущей серии вопросов
    public void resetQuiz(){
        AssetManager assets = getActivity().getAssets();
        fileNameList.clear(); // Пустой список имён файлов изоображений

        try{
            //Перебрать всё регионы
             for (String region: regionsSet){
                 String[] paths = assets.list(region);

                 for (String path: paths){
                     fileNameList.add(path.replace(".png",""));
                 }
             }
        }

        catch(IOException exception){
            Log.e(TAG, "Error loading image file names", exception);
        }
        correctAnswers = 0 ;
        totalGuesses = 0 ;// Сброс общего количества попыток
        quizCountriesList.clear(); // Очитскаа предыдущего списка стран

        int flagCounter =1 ;
        int numberOfFlags = fileNameList.size();

        while (flagCounter <= FLAGS_IN_QUIZ){
            int randonIndex = random.nextInt(numberOfFlags);

            String filename = fileNameList.get(randonIndex);

            //Если регион включен,но еще не выбран
            if(!quizCountriesList.contains(filename)) {
                quizCountriesList.add(filename);
                ++flagCounter;
            }
        }

        loadNextFlag();  //Запуск викторины загрузкой первого флага

    }


    private void loadNextFlag(){
        String nextImage = quizCountriesList.remove(0);
        correctAnswer = nextImage;
        answerTextView.setText("");

        questionNumberTextView.setText( getString(R.string.question, (correctAnswers +1 ), FLAGS_IN_QUIZ)); // Отображение номера текущего вопроса

        String region = nextImage.substring(0, nextImage.indexOf('-')); // Извлечение региона из имени следующего изоображения

        AssetManager assets = getActivity().getAssets();

        try (InputStream stream = assets.open(region + "/"+ nextImage +".png")){
            Drawable flag = Drawable.createFromStream(stream, nextImage);
            flagImageView.setImageDrawable(flag); // Объект флаг назнгачается источником для окна flagImageView

            animate(false);
        }
        catch (IOException exception){
            Log.e(TAG, "Eroor loading " + nextImage, exception);
        }

        Collections.shuffle(fileNameList); // Перестановка имён файлов

        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        for (int row = 0; row< guessRows; row++){
            for (int column = 0; column<guessLinerLayouts[row].getChildCount();column++){
                Button newGuessButton = (Button)guessLinerLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);

                String filename = fileNameList.get((row*2) + column);
                newGuessButton.setText(getCountryName(filename));

            }
        }

        int row = random.nextInt(guessRows);
        int column = random.nextInt(2);

        LinearLayout randomRow = guessLinerLayouts[row];
        String countryName = getCountryName(correctAnswer);
        ((Button)randomRow.getChildAt(column) ).setText(contryName);

    }

    private String getCountryName(String name){
        return name.substring(name.indexOf('-')+1).replace('_',' ');
    }

    private void animate(boolean animateOut){
        if (correctAnswers == 0 ) return; //Первый раз анимация не срабатывает

        //Координаты центра
        int centerX = (quizLinerLayout.getLeft() + quizLinerLayout.getRight()) / 2;
        int centerY = (quizLinerLayout.getTop() + quizLinerLayout.getBottom()) / 2;


        int radius = Math.max(quizLinerLayout.getWidth(), quizLinerLayout.getHeight());

        Animator animator;
        if (animateOut){
            animator = ViewAnimationUtils.createCircularReveal(quizLinerLayout,centerX,centerY,radius,0);

            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    loadNextFlag();
                }
            });
        }
        else{
            animator = ViewAnimationUtils.createCircularReveal(quizLinerLayout,centerX,centerY,0,radius);
        }
        animator.setDuration(500);
        animator.start();




    }
}
