package com.example.photochemistry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class Segmentation {
    private final Mat img = new Mat();
    private final int SIZE = 45; //pixels

    public Segmentation(Context ctx, Bitmap bmp32) {
        Utils.bitmapToMat(bmp32, img);

    }

    //returns each symbol segmented
    public List<Mat> segment() {
        List<Mat> segImgs = new ArrayList<>();
        Mat gray = new Mat();
        Mat thresh = new Mat();

        Imgproc.cvtColor(img, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(gray, thresh, 127, 255, Imgproc.THRESH_BINARY_INV);
        List<ArrayList<Integer>> divs = getDivs(thresh, 5);

        int eps = 5;
        String v = "";
        List<ArrayList<Integer>> newd = null;
        for (int i = 0; i < divs.size(); i++) {

            //for each colums divisor, find the row divisor
            // and reshape the image to fit the model shape (45x45)
            int width = divs.get(i).get(1) - divs.get(i).get(0);
            Mat divided = thresh.submat(new Rect(divs.get(i).get(0), 0, width, thresh.rows()));
            newd = getDivs(transpose(divided), 5);

            divided = divided.submat(new Rect(0, newd.get(0).get(0), divided.cols(), newd.get(0).get(1) - newd.get(0).get(0)));
            Imgproc.threshold(divided, divided, 127, 255, Imgproc.THRESH_BINARY_INV);

            divided = make_square(divided.rows(), divided.cols(), divided);
            Imgproc.resize(divided, divided, new Size(SIZE, SIZE));

            segImgs.add(divided);
        }

        return segImgs;
    }
    private Mat make_square(int w, int h, Mat img) {

        int dim = Math.max(w, h);
        int nH = (int) Math.floor(h * 1.0 / 2);
        int nW = (int) Math.floor(w * 1.0 / 2);
        int nD = (int) Math.floor(dim * 1.0 / 2);
        Mat res = Mat.zeros(new Size(dim, dim), img.type());

        //fill with whites...
        for (int i = 0; i < dim; i++)
            for (int j = 0; j < dim; j++)
                res.put(i, j, 255);

        for (int i = 0; i < w; i++)
            for (int j = 0; j < h; j++)
                res.put(nD - nW + i, nD - nH + j, img.get(i, j));


        return res;
    }

    private Mat transpose(Mat img) {
        //transpose a matrix
        Mat temp = new Mat(img.cols(), img.rows(), img.type());
        for (int i = 0; i < img.rows(); i++)
            for (int j = 0; j < img.cols(); j++)
                temp.put(j, i, img.get(i, j));

        return temp;
    }

    private List<ArrayList<Integer>> getDivs(Mat img, double thd) {

        List<ArrayList<Integer>> divs = new ArrayList<>();
        boolean newd = true;
        int whites = 0;

        //Convert the image to a boolean array where True represents non-zero values
        boolean[] colSums = new boolean[img.cols()];
        for (int j = 0; j < img.cols(); j++)
            for (int i = 0; i < img.rows(); i++)
                if (img.get(i, j)[0] > 0.003)
                    colSums[j] = true;

        for (int j = 0; j < img.cols(); j++) {
            if (newd && colSums[j]) {
                divs.add(new ArrayList<>());
                divs.get(divs.size() - 1).add(j);
                divs.get(divs.size() - 1).add(-1);
                newd = false;
            } else if (!newd && colSums[j]) {
                ArrayList<Integer> temp = divs.get(divs.size() - 1);
                temp.remove(temp.size() - 1);
                temp.add(j);
            }

            if (!colSums[j]) {
                whites++;
                if (whites >= thd) {
                    newd = true;
                    whites = 0;
                }
            }

        }

        return divs;

    }
}


