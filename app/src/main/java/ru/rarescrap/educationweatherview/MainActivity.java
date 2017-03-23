package ru.rarescrap.educationweatherview;

// MainActivity.java
// Вывод 16-дневного прогноза погоды для заданного города

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager; // Для сокрытия клавиатуры по нажатию FAB
import android.widget.EditText;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    // Список объектов Weather, представляющих прогноз погоды
    private List<Weather> weatherList = new ArrayList<>();

    // ArrayAdapter связывает объекты Weather с элементами ListView
    private WeatherArrayAdapter weatherArrayAdapter;
    private ListView weatherListView; // Для вывода информации

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Сгенерированный код для заполнения макета и настройки Toolbar
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // ArrayAdapter для связывания weatherList с weatherListView
        weatherListView = (ListView) findViewById(R.id.weatherListView);
        weatherArrayAdapter = new WeatherArrayAdapter(this, weatherList);
        weatherListView.setAdapter(weatherArrayAdapter);

        // FAB скрывает клавиатуру и выдает запрос к веб-сервису
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Получить текст из locationEditText и создать URL веб-сервисы
                EditText locationEditText = (EditText) findViewById(R.id.locationEditText);
                URL url = createURL(locationEditText.getText().toString());

                // Скрыть клавиатуру и запустить GetWeatherTask для получения
                // погодных данных от OpenWeatherMap.org в отдельном потоке
                if (url != null) {
                    dismissKeyboard(locationEditText);
                    GetWeatherTask getLocalWeatherTask = new GetWeatherTask();
                    getLocalWeatherTask.execute(url);
                }else {
                    Snackbar.make(findViewById(R.id.coordinatorLayout),
                            R.string.invalid_url, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    // Клавиатура закрывается при касании кнопки FAB
    private void dismissKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE); // Этот метод может возвращать объекты многих разных типов, поэтому возвращаемое значение необходимо преобразовать к нужному типу
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /***
     * Создание URL веб-сервисы openweathermap.org для названия города
     * @param city - Город, для которого ищется погодный прогноз
     * @return URL
     */
    private URL createURL(String city) {
        String apiKey = getString(R.string.api_key);
        String baseUrl = getString(R.string.web_service_url);

        try {
            // Создание URL для заданного города и температурной шкалы (Фаренгейт)
            /* Параметр units может принимать значения imperial (для шкалы Фаренгейта),
            metric (для шкалы Цельсия) или standard (для шкалы Кельвина) — если параметр
            units не указан, по умолчанию используется значение standard. Параметр cnt
            определяет количество дней в прогнозе. Максимальное значение равно 16,
            значение по умолчанию равно 7 (при некорректном количестве дней возвращается
            прогноз на 7 дней). По умолчанию прогноз возвращается в формате JSON, хотя
            вы можете добавить параметр mode со значением XML или HTML, чтобы получить
            данные в формате XML или веб-страницы соответственно.*/
            String urlString = baseUrl + URLEncoder.encode(city, "UTF-8") + "&units=imperial&cnt=16&APPID=" + apiKey;
            return new URL(urlString);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null; // Некорректный URL
    }

    /* Обращение к REST-совместимому веб-сервису за погодными данными
    и сохранение данных в локальном файле HTML */
    private class GetWeatherTask
    extends AsyncTask<URL, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(URL... params) {
            HttpURLConnection connection = null;

            try {
                connection = (HttpURLConnection) params[0].openConnection(); // Для выдачи запроса достаточно открыть объект подключения
                int response = connection.getResponseCode(); // Получить код ответа от веб-сервера

                if (response == HttpURLConnection.HTTP_OK) {
                    StringBuilder builder = new StringBuilder();

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            builder.append(line);
                        }
                    }
                    catch (IOException e) {
                        Snackbar.make(findViewById(R.id.coordinatorLayout),
                                R.string.read_error, Snackbar.LENGTH_LONG).show();
                        e.printStackTrace();
                    }

                    connection.disconnect();
                    return new JSONObject(builder.toString());
                }else {
                    Snackbar.make(findViewById(R.id.coordinatorLayout),
                            R.string.connect_error, Snackbar.LENGTH_LONG).show();
                }
            }
            catch (Exception e) {
                Snackbar.make(findViewById(R.id.coordinatorLayout),
                        R.string.connect_error, Snackbar.LENGTH_LONG).show();
                e.printStackTrace();
            }
            finally {
                connection.disconnect(); // Закрыть HttpURLConnection
            }

            return null;
        }

        // Обработка ответа JSON и обновление ListView
        @Override
        protected void onPostExecute(JSONObject weather) {
            if (weather != null) {
                convertJSONtoArrayList(weather); // Заполнение weatherList
                weatherArrayAdapter.notifyDataSetChanged(); // Связать с ListView
                weatherListView.smoothScrollToPosition(0); // Прокрутить до верха
            }
        }
    }

    // Создание объектов Weather на базе JSONObject с прогнозом
    private void convertJSONtoArrayList(JSONObject forecast) {
        weatherList.clear(); // Стирание старых погодных данных

        try {
            // Получение свойства "list" JSONArray
            JSONArray list = forecast.getJSONArray("list");

            // Преобразовать каждый элемент списка в объект Weather
            for (int i = 0; i < list.length(); ++i) {
                JSONObject day = list.getJSONObject(i); // Данные за день
                // Получить JSONObject с температурами дня ("temp")
                JSONObject temperatures = day.getJSONObject("temp");

                // Получить JSONObject c описанием и значком ("weather")
                JSONObject weather = day.getJSONArray("weather").getJSONObject(0);

                // Добавить новый объект Weather в weatherList
                weatherList.add(new Weather(
                        day.getLong("dt"), // Временная метка даты/времени
                        temperatures.getDouble("min"), // Мин. температура
                        temperatures.getDouble("max"), // Макс. температура
                        day.getDouble("humidity"), // Процент влажности
                        weather.getString("description"), // Погодные условия
                        weather.getString("icon"))); // Имя значка
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
