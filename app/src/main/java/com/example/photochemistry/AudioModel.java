package com.example.photochemistry;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class AudioModel {

    private final Module module;
    private Map<Integer, String> vocab;

    public AudioModel(Context context) throws IOException {
        module = LiteModuleLoader.load(MainActivity.assetFilePath(context, "wav2vec2_quantized_lite.ptl"));
        InputStream is = context.getAssets().open("vocab.json");

        String jsontxt = new Scanner(is).useDelimiter("\\A").next();
        JSONObject json;

        try{
            json = new JSONObject(jsontxt);
            vocab = jsonToMap(json);

        }catch(JSONException e ){
            Log.e("JSONERROR", e.toString());
        }
    }

    private Map<Integer, String> jsonToMap(JSONObject json) throws JSONException{
        Map<Integer, String> resMap = new HashMap<>();

        if(json != JSONObject.NULL)
            resMap = toMap(json);

        return resMap;
    }

    private Map<Integer, String> toMap(JSONObject json)  throws JSONException{
        Map<Integer, String> map = new HashMap<>();

        Iterator<String> keys = json.keys();
        while(keys.hasNext()){
            String key = keys.next();
            map.put((Integer) json.get(key), (String) key);
        }

        return map;
    }

    public String getTranscription(List<Float> audio){

        float[] init_audio = listToFloat(audio);

        //apply normalization over all the samples
        float[] myaudio = normalize(init_audio);

        //prepare tensor for the model
        Tensor inputTensor = Tensor.fromBlob(myaudio, new long[]{1, myaudio.length});

        //apply inference
        Map<String, IValue> outputs = module.forward(IValue.from(inputTensor)).toDictStringKey();
        Tensor logits = outputs.get("logits").toTensor();

        //apply argmax
        float[][] logits_float = tensorToDoubleArray(logits);
        int[] argmaxs = argmaxOverTime(logits_float);

        //decode the output
        return decode(argmaxs);
    }

    private float[] listToFloat(List<Float> audio){
        float[] res = new float[audio.size()];

        for(int i=0;i<audio.size();i++)
            res[i] = audio.get(i);

        return res;
    }

    private static float[] normalize(float[] audio) {
        int N = audio.length;
        if (N == 0) return new float[0];

        // avg
        float sum = 0;
        for (float sample : audio) {
            sum += sample;
        }
        float mean = sum / N;

        // 2. variance and std
        float varianceSum = 0;
        for (float sample : audio) {
            varianceSum += Math.pow(sample - mean, 2);
        }
        float stdDev = (float) Math.sqrt(varianceSum / N);

        // prevent 0 div
        if (stdDev == 0) stdDev = 1e-8f;

        // 3. normalize
        float[] normalizedAudio = new float[N];
        for (int i = 0; i < N; i++) {
            normalizedAudio[i] = (audio[i] - mean) / stdDev;
        }

        return normalizedAudio;
    }

    private float[][] tensorToDoubleArray(Tensor t){

        float[] flat_t = t.getDataAsFloatArray();
        long[] shape = t.shape();
        int T = (int) shape[1];
        int V = (int) shape[2];

        float[][] res = new float[T][V];

        for(int i=0;i<T;i++)
            System.arraycopy(flat_t, i*V, res[i], 0, V);

        return res;
    }

    private int[] argmaxOverTime(float[][] logits){
        int T = logits.length;
        int V = logits[0].length;
        int[] pred_tokens = new int[T];

        for(int t=0; t<T; t++){
            float maxVal = logits[t][0];
            int maxInd = 0;

            for(int v = 1; v<V; v++){
                if(logits[t][v] > maxVal){
                    maxVal = logits[t][v];
                    maxInd = v;
                }
            }

            pred_tokens[t] = maxInd;

        }

        return pred_tokens;
    }

    private String decode(int[] logit){

        int[] filtered_logit = filterCTC(logit);
        int T = filtered_logit.length;
        Log.i("logitsize", Arrays.toString(filtered_logit));
        StringBuilder res= new StringBuilder();
        for(int t=0; t<T; t++){
            String value = vocab.get(filtered_logit[t]);
            if(value.equals("|"))
                res.append(" ");
            else if(!value.contains("[PAD]"))
                res.append(value);
        }

        return res.toString();
    }

    private int[] filterCTC(int[] logit){

        ArrayList<Integer> res = new ArrayList<>();
        for(int i=1; i< logit.length; i++){
            if(logit[i] != logit[i-1])
                res.add(logit[i]);

        }

        int[] myres = new int[res.size()];
        for(int i=0;i<res.size(); i++)
            myres[i] = res.get(i);

        return myres;
    }

}
