package ru.rarescrap.educationweatherview;

// Используются для преобразования временной метки каждого дня в название дня недели
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

class Weather {
    // объекты String в Java неизменяемы (immutable), поэтому несмотря на такое объявление, их значения измениться не могут
    public final String dayOfWeek;
    public final String minTemp;
    public final String maxTemp;
    public final String humidity;
    public final String description;
    public final String iconURL;

    // Конструктор
    public Weather(long timeStamp, double minTemp, double maxTemp, double humidity, String description, String iconName) {
        // NumberFormat для форматирования температур в целое число
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(0); // Запрещает числа почле запятой

        this.dayOfWeek = convertTimeStampToDay(timeStamp); // Получение названия дня недели и инициализации dayOfWeek
        this.minTemp = numberFormat.format(minTemp) + "\u00B0F"; // Минимальная температура по Фаренгейту
        this.maxTemp = numberFormat.format(maxTemp) + "\u00B0F"; // Максимальная температура по Фаренгейту
        this.humidity = NumberFormat.getPercentInstance().format(humidity / 100.0);
        this.description = description; // Инициализирует описание погодных условий
        this.iconURL = "http://openweathermap.org/img/w/" + iconName + ".png"; // Изображение погодных условий для погоды
    }

    // Преобразование временной метки в название дня недели (Monday, ...)
    private static String convertTimeStampToDay(long timeStamp) {
        Calendar calendar = Calendar.getInstance(); // Объект Calendar
        calendar.setTimeInMillis(timeStamp * 1000); // Получение времени
        TimeZone tz = TimeZone.getDefault(); // Часовой пояс устройства

        // Поправка на часовой пояс устройства
        calendar.add(Calendar.MILLISECOND, tz.getOffset( calendar.getTimeInMillis() ));

        // Объект SimpleDateFormat, возвращающий название дня недели
        SimpleDateFormat dateFormatter = new SimpleDateFormat("EEEE"); // EEEE - первый четыре буквы для недели
        return dateFormatter.format(calendar.getTime());
    }
}