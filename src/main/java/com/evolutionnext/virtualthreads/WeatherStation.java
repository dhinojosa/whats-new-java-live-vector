package com.evolutionnext.virtualthreads;

/*
 * 5. Virtual Threads can be composed
 */
public class WeatherStation {

    protected static String getCity() {
        System.out.printf("[%s] Getting city%n", Thread.currentThread());
        return "Phoenix, Arizona";
    }

    protected static int getTemperature(String city) {
        System.out.printf("[%s] Getting temperature for %s%n", Thread.currentThread(), city);
        return 104;
    }
}
