package com.example.photochemistry;

import org.ssclab.pl.milp.*;
import org.ssclab.log.SscLogger;

import java.util.ArrayList;

public class SolveReaction {

    public static String solve(ArrayList<ArrayList<Integer>> matrice){

        double [][]A = new double[matrice.size()+2][((ArrayList<Integer>)matrice.get(0)).size()];
        String res = "";

        for(int i=0;i<matrice.size();i++)
            for(int j=0; j<matrice.get(0).size();j++)
                A[i][j]= matrice.get(i).get(j).doubleValue();


        for(int i=matrice.size();i<2+matrice.size();i++){
            for(int j=0; j<A[0].length;j++){
                A[i][j] = 1;
            }
        }

        double []b = new double[A.length];
        //double b[]  = {0,0,0,0,1,Double.NaN};

        ConsType[] rel = new ConsType[A.length];
        //ConsType[] rel = {ConsType.EQ, ConsType.EQ, ConsType.EQ, ConsType.EQ, ConsType.GE, ConsType.INT};

        for(int i = 0; i< A.length-2;i++){
            b[i] = 0;
            rel[i] = ConsType.EQ;
        }
        b[A.length-2] = 1;
        b[A.length-1] = Double.NaN;
        rel[A.length-2] = ConsType.GE;
        rel[A.length-1] = ConsType.INT;

        double []c = new double[A[0].length];
        //double c[] = {1,1,1,1,1};

        for(int i=0;i<A[0].length;i++){
            c[i] = 1;
        }


        try {
            LinearObjectiveFunction f = new LinearObjectiveFunction(c, GoalType.MIN);

            ArrayList <Constraint> constraints = new ArrayList<>();
            for(int i = 0; i< A.length; i++){
                constraints.add(new Constraint(A[i], rel[i], b[i]));
            }

            MILP lp = new MILP(f,constraints);
            SolutionType solution_Type = lp.resolve();


            if(solution_Type == SolutionType.OPTIMUM){
                Solution solution = lp.getSolution();
                for(Variable var:solution.getVariables())
                    res+=(int)var.getValue()+" ";


            }// fine if
            else {
                res="La soluzione non è feasible";
            }


        } catch (Exception e) {
            System.out.println(e.getMessage());
            res = "error!";
        }

        return res;
    }

}
