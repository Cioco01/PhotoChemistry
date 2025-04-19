package com.example.photochemistry;

public class Tuple {

    private String value;
    private Double score;

    public Tuple(String val, Double score){
        this.value = val;
        this.score = score;
    }

    public String getValue(){
        return value;
    }

    public Double getScore(){
        return score;
    }
}
