package com.example.photochemistry;

import android.content.Context;
import android.util.Log;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.support.metadata.schema.Content;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class AudioImprover {

    private LevenshteinDistance distance;
    private String textToImprove;
    private Map<String, String> names;

    public AudioImprover(String textToImprove, Context context) throws IOException {
        this.textToImprove = textToImprove;
        this.distance = LevenshteinDistance.getDefaultInstance();

        // Load spoken words json
        InputStream is = context.getAssets().open("spoken_elem.json");
        String jsontxt = new Scanner(is).useDelimiter("\\A").next();
        JSONObject json;

        try{
            json = new JSONObject(jsontxt);
            names = jsonToMap(json);

        }catch(JSONException e ){
            Log.e("JSONERROR", e.toString());
        }

    }

    private Map<String, String> jsonToMap(JSONObject json) throws JSONException{
        Map<String, String> resMap = new LinkedHashMap<>();

        if(json != JSONObject.NULL)
            resMap = toMap(json);

        return resMap;
    }

    private Map<String, String> toMap(JSONObject json)  throws JSONException{
        Map<String, String> map = new LinkedHashMap<>();

        Iterator<String> keys = json.keys();
        while(keys.hasNext()){
            String key = keys.next();
            map.put((String) key, (String) json.get(key));
        }

        return map;
    }

    public String improve(){
        String corrected = computeSimilarity(this.textToImprove, 2.0);
        return corrected.replace(" ", "").replace("+", " + ").replace("=", " = ");
    }

    private String namesToFormula(String corrected) {
        return null;
    }

    private String computeSimilarity(String textToImprove, Double exp) {

        String[] elems = textToImprove.split(" ");
        StringBuilder res = new StringBuilder();

        List<Tuple> scores = new ArrayList<>();

        for (String elem : elems) {

            scores.clear();

            for (Map.Entry<String, String> entry : names.entrySet()) {
                int dist = distance.apply(elem, entry.getKey());

                // normalize the distance to be 0-1 (now it's a similarity)
                double ratio = (elem.length() + entry.getKey().length() - dist) * 1.0f
                        / (elem.length() + entry.getKey().length());

                scores.add(new Tuple(entry.getValue(), Math.pow(ratio, exp) * 100));
            }

            res.append(getMaxGuess(softmax(scores))).append(" ");


        }
        return res.toString();
    }

    private String getMaxGuess(List<Tuple> probabilities){

        double max = 0;
        String res = "";

        for(Tuple entry : probabilities){
            if(entry.getScore() > max){
                max = entry.getScore();
                res = entry.getValue();
            }
        }

        return res;
    }
    private List<Tuple> softmax(List<Tuple> scores){

        double sums = 0;
        List<Tuple> res = new ArrayList<>();
        for (Tuple score: scores)
            sums+=Math.exp(score.getScore());

        // compute the softmax for each score
        for (Tuple score: scores)
            res.add(new Tuple(score.getValue(), score.getScore()/sums));

        return res;
    }
    public String improve(String textToImprove){
        this.textToImprove = textToImprove;
        return this.improve();
    }
}
