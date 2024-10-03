package nh.weather_reader_kafka.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrentWeather {
   private String time;
   private double temperature;
   private double windspeed;
   private int winddirection;

   public String getTime() {
      return time;
   }

   public void setTime(String time) {
      this.time = time;
   }

   public double getTemperature() {
      return temperature;
   }

   public void setTemperature(double temperature) {
      this.temperature = temperature;
   }

   public double getWindspeed() {
      return windspeed;
   }

   public void setWindspeed(double windspeed) {
      this.windspeed = windspeed;
   }

   public int getWinddirection() {
      return winddirection;
   }

   public void setWinddirection(int winddirection) {
      this.winddirection = winddirection;
   }

   @Override
   public String toString() {
      return "CurrentWeather{" +
              "time='" + time + '\'' +
              ", temperature=" + temperature +
              ", windspeed=" + windspeed +
              ", winddirection=" + winddirection +
              '}';
   }
}
