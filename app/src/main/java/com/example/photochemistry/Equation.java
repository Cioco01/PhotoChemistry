package com.example.photochemistry;

import org.opencv.core.Mat;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Equation {

    private ArrayList<ArrayList<String>> elems;
    private ArrayList<ArrayList<HashMap<String, Integer>>> atoms;
    private ArrayList<ArrayList<Integer>> A;
    public Equation(){

        elems = new ArrayList<>();
        elems.add(new ArrayList<>());
        elems.add(new ArrayList<>());
    }

    public void add(int side, String form){
        elems.get(side).add(form);
    }

    public ArrayList<ArrayList<String>> getElems(){
        return this.elems;
    }

    public void divide(){

        ArrayList<ArrayList<HashMap<String, Integer>>> temp = new ArrayList<>();
        temp.add(new ArrayList<HashMap<String, Integer>>());
        temp.add(new ArrayList<HashMap<String, Integer>>());

        //for each side of the equation
        for(int i=0;i<2;i++){

            //for each formula

            for(String form: elems.get(i)){
                temp.get(i).add(new HashMap<>());
                int size = temp.get(i).size();
                int param = 1;
                ArrayList<String> li = reFindAll("[A-Z][a-z]?\\d*|\\(.*?\\)\\d+", form);

                //for each element in the formula
                HashMap<String, Integer> sub;
                for(String elem : li){

                    //if it contains a (
                    if(elem.contains("(")){
                        int mult = Character.getNumericValue(elem.charAt(elem.indexOf(')')+1));
                        sub = reFindAll("([A-Z][a-z]?)(\\d*)",
                                elem.substring(1,elem.indexOf(')')), mult, param);

                        for(Map.Entry<String,Integer> e: sub.entrySet())
                            temp.get(i).get(size-1).put(e.getKey(), e.getValue());
                    }

                    //if ends with a number
                    else if (Character.isDigit(elem.charAt(elem.length()-1))){
                        sub = reFindAll("([A-Z][a-z]?)(\\d*)", elem, 1,1);

                        for(Map.Entry<String,Integer> e: sub.entrySet())
                            temp.get(i).get(size-1).put(e.getKey(), e.getValue());
                    }

                    else
                        temp.get(i).get(size-1).put(elem, 1);
                }
            }
        }

        atoms = temp;
    }

    private ArrayList<String> reFindAll(String regex, String form){
        Matcher matcher = Pattern.compile(regex).matcher(form);
        ArrayList<String> res= new ArrayList<>();
        while(matcher.find()){
            res.add(matcher.group());
        }
        return res;
    }
    private HashMap<String, Integer> reFindAll(String regex, String form, int mult, int param){
        Matcher matcher = Pattern.compile(regex).matcher(form);
        HashMap<String, Integer> hm = new HashMap<>();

        while(matcher.find()){
            if(matcher.group(0).equals(matcher.group(1))){
                hm.put(matcher.group(1), mult);
            }else{
                hm.put(matcher.group(1),
                        Integer.parseInt(matcher.group(0).replaceAll("[^\\d]", ""))*mult*param);
            }
        }
        return hm;
    }
    public String getAtoms(){
        StringBuilder res= new StringBuilder();
        for(int i=0;i<2;i++)
            for(HashMap<String, Integer> formule: atoms.get(i))
                res.append(formule.toString()).append("\n");

        return res.toString();
    }

    public void createMatrix(){
        //from atoms retrieve the matrix A used for optimization

        ArrayList<Set<String>> allElems = new ArrayList<>();
        ArrayList<ArrayList<Integer>> A = new ArrayList<>();

        for(int i=0;i<2;i++){
            allElems.add(new HashSet<>());
            for(HashMap<String, Integer> formula: atoms.get(i)){
                Set<String> combined = Stream.concat(formula.keySet().stream(), allElems.get(i).stream())
                        .collect(Collectors.toSet());
                allElems.set(i, combined);
            }
        }

        if(allElems.get(0).equals(allElems.get(1))){
            //left and right side are equals

            List<String> myl = new ArrayList<>(allElems.get(0));

            for(int i=0;i<2;i++){
                for(HashMap<String, Integer> r: atoms.get(i)){
                    A.add(new ArrayList<>());

                    for(int j=0;j<myl.size();j++){
                        if(r.get(myl.get(j))!=null)
                            A.get(A.size()-1).add(r.get(myl.get(j))*(1+i*-2));
                        else
                            A.get(A.size()-1).add(0);
                    }
                }
            }

        }
        this.A = transpose(A);


    }
    private ArrayList<ArrayList<Integer>> transpose(ArrayList<ArrayList<Integer>> matrix) {
        int rows = matrix.size();
        int cols = matrix.get(0).size();

        ArrayList<ArrayList<Integer>> transposedMatrix = new ArrayList<>();

        for (int i = 0; i < cols; i++) {
            ArrayList<Integer> newRow = new ArrayList<>();
            for (int j = 0; j < rows; j++) {
                newRow.add(matrix.get(j).get(i));
            }
            transposedMatrix.add(newRow);
        }

        return transposedMatrix;
    }
    public ArrayList<ArrayList<Integer>> getMatrix(){
        return A;
    }


}
