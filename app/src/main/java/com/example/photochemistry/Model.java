package com.example.photochemistry;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class Model {

    private List<Mat> images;
    private Context context;
    private Interpreter tflite; // Interpreter di TensorFlow Lite
    private String modelPath = "converted_model_v12.tflite"; // Nome del tuo file .tflite
    private String[] dic = {"(", ")", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "b", "C", "d", "e", "f", "G", "H", "i", "j", "k", "l", "M", "N", "o", "p", "plus", "q", "R", "rightarrow", "S", "T", "u", "v", "w", "X", "y", "z"};

    public Model(List<Mat> images, Context context) throws IOException {
        this.images = images;
        this.context = context;
        Arrays.sort(dic);
        // Inizializza l'Interpreter di TensorFlow Lite
        tflite = getTfliteInterpreter(modelPath);
    }

    private Interpreter getTfliteInterpreter(String modelPath) throws IOException {
        try {
            return new Interpreter(loadModelFile(context.getAssets(), modelPath));
        } catch (Exception e) {
            Logger.getLogger("Model").warning("Could not load model: " + e.getMessage());
            return null;
        }
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        java.io.FileDescriptor fileDescriptor = assetManager.openFd(modelPath).getFileDescriptor();
        FileInputStream inputStream = new FileInputStream(fileDescriptor);
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = assetManager.openFd(modelPath).getStartOffset();
        long declaredLength = assetManager.openFd(modelPath).getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public List<String> getPredictions(Mat img, int maxs) {

        Map<String, Float> r = new HashMap<>();
        List<String> res = new ArrayList<>();
        int c = 0;
        int height = 45;
        int width = 45;
        int channels = 1; // use 3 for RGB

        img.convertTo(img, CvType.CV_32S);
        int[] rgba = new int[(int) (img.total() * img.channels())];
        img.get(0, 0, rgba);
/*
        float[][] inputArray = new float[1][45 * 45];
        int index = 0;
        for (int value : rgba) {
            inputArray[0][index++] = value / 255.0f;
        }

        float[][][][] reshapedInput = new float[1][height][width][channels];
        int index2 = 0;
        for (int j = 0; j < width; j++) {
            for (int i = 0; i < height; i++) {
                for (int k = 0; k < channels; k++) {
                    reshapedInput[0][i][j][k] = inputArray[0][index2++];
                }
            }
        }
        */

        float[][][][] reshapedInput = new float[1][height][width][channels];
        int index = 0;

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                for (int k = 0; k < channels; k++) {
                    // Calculate the index into rgba (assuming row-major order)
                    int rgbaIndex = i * width + j;
                    if (rgbaIndex < rgba.length){
                        reshapedInput[0][i][j][k] = rgba[rgbaIndex] / 255.0f; // Normalize directly
                    }
                }
            }
        }

        // Assicurati che il modello sia stato caricato
        if (tflite == null) {
            return null;
        }
        // Assicurati che l'input abbia la forma corretta per il tuo modello
        float[][] output = new float[1][40];

        tflite.run(reshapedInput, output);

        for (int i = 0; i < output[0].length; i++)
            r.put(dic[i], output[0][i]);

        r = sortByValue(r);

        for (Map.Entry<String, Float> e : r.entrySet())
            if (c < maxs) {
                if (e.getKey().equals("plus"))
                    res.add("+");
                else if (e.getKey().equals("rightarrow"))
                    res.add("=");
                else
                    res.add(e.getKey());
                c++;
            }

        return res;
    }

    private List<String> readElems() throws IOException {
        // legge tutti gli elementi dal file
        List<String> elems = new ArrayList<>();
        InputStream forms = context.getResources().openRawResource(R.raw.periodictable);
        BufferedReader br = new BufferedReader(new InputStreamReader(forms, "UTF-8"));

        for (String line = br.readLine(); line != null; line = br.readLine())
            elems.add(line.trim());

        return elems;
    }

    private String errCorrection(String temp, List<String> cl, boolean cbl, boolean cbn, String df) {
        String res = "";
        try {
            List<String> elems = readElems();
            List<String> firstChar = new ArrayList<>();
            boolean could_be_letter = cbl;
            boolean could_be_number = cbn;

            for (String e : elems)
                firstChar.add(e.charAt(0) + "");

            if (cl.isEmpty())
                return df;

            if (Objects.equals(cl.get(0), "0"))
                return "O";

            if (cl.size() == 1 && cl.get(0).matches("[a-zA-Z]")) {
                // il simbolo è una lettera, se è nella tabella (come O) restituisci,
                // altrimenti controlla se con il precedente è nella tabella
                if (elems.contains(cl.get(0)) || (firstChar.contains(cl.get(0).toUpperCase())))
                    return cl.get(0);
                else if (!temp.isEmpty() &&
                        elems.contains(temp.charAt(temp.length() - 1) + cl.get(0).toLowerCase()))
                    return cl.get(0);
                else
                    could_be_letter = false;
            }

            if (cl.size() == 1 && cl.get(0).matches("[0-9]+")) {
                // il simbolo è un numero, se è all'inizio o non è dopo una ) o dopo un elemento allora non può essere un numero
                if (temp.isEmpty() ||
                        (!elems.contains(temp.charAt(temp.length() - 1) + "") && temp.charAt(temp.length() - 1) == ')'
                                && temp.length() > 1 && !elems.contains(temp.substring(temp.length() - 2, temp.length() - 1)))
                )
                    could_be_number = false;
                else
                    return cl.get(0);
            }
            if (could_be_letter && could_be_number) {
                // non è né una lettera né un numero
                return cl.get(0); // per ora...
            }
            return errCorrection(temp, cl.subList(1, cl.size()), could_be_letter, could_be_number, df);

        } catch (Exception e) {
            return df;
        }
    }

    // ordina una mappa per valore (decrescente)
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Collections.reverseOrder(Map.Entry.comparingByValue()));

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    public String getStringPredicted() {
        String temp = "";
        String res = "";
        for (int i = 0; i < images.size(); i++) {
            List<String> pr = getPredictions(images.get(i), 3);
            temp += errCorrection(temp, pr, true, true, pr.get(0));
        }

        for (int i = 0; i < temp.length(); i++) {
            if (temp.charAt(i) == '+' || temp.charAt(i) == '=')
                res += " " + temp.charAt(i) + " ";
            else
                res += temp.charAt(i);
        }
        return res;
    }
}
