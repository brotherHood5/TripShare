package com.example.tripblog.utils;

import android.graphics.Color;
import android.util.Log;

import java.util.Random;

public class ColorUtil {
    public static int randomHexColor(String text) {
        int sum = 0;
        for (char c:
             text.toCharArray()) {
            sum += (int) c;
        }
        sum %= 16777215;

        Random random = new Random();
        int upperColor = 0xf54646;
        int lowerColor = 0xde46f5;
        int nextInt = random.nextInt( upperColor - (lowerColor + 1)) + lowerColor;
        String colorCode = String.format("#%06x", nextInt + sum);

        return Color.parseColor(colorCode);
    }
}
