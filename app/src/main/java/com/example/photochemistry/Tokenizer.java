package com.example.photochemistry;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

public class Tokenizer {

    private ArrayList<String> tokens;
    public Tokenizer(String str){
        tokens = new ArrayList<>(Arrays.asList(str.split(" ")));
    }

    public Optional<String> getNextToken(){

        return !tokens.isEmpty() ? Optional.of(tokens.remove(0)) : Optional.empty();
    }

    public Optional<String> peekNextElement(int num){
        return tokens.size() >= num ? Optional.of(tokens.get(num-1)) : Optional.empty();
    }

    @NonNull
    @Override
    public String toString(){
        return "La stringa tokenizzata è\n"+ tokens.toString();
    }
}

