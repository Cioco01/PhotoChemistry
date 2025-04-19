package com.example.photochemistry;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class AudioImproverTest {
    @Test
    public void testSimilarity(){
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        try{
            AudioImprover myimpr = new AudioImprover("portasio clourro osigen tre ugual potassio cloro piu osssigeo due", appContext);

            Assert.assertEquals("stringa corretta",
                    "KClO3 = KCl + O2",
                    myimpr.improve());
        } catch (IOException e) {

        }

    }

    @Test
    public void testCom(){
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        try{
            AudioImprover myimpr = new AudioImprover("portasio clourro osigen tre ugual potassio cloro piu osssigeo due", appContext);

            String res = myimpr.computeSimilarity("potarsio", 2.0);


            Assert.assertEquals("stringa corretta",
                    "K ",
                        res);
        } catch (IOException e) {

        }

    }
}
