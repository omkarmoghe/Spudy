package com.spuds.spudy.app;

import android.app.Activity;

import java.util.ArrayList;

/**
 * Created by Omkar on 9/6/2014.
 */
public class StringMaster extends Activity {
    private String story;
    private ArrayList<String> readyToRead;

    public StringMaster(String string){
        story = string;
        readyToRead = new ArrayList<String>();
    }

    public String getStory(){
        return story;
    }

    public void setStory(String string){
        story = string;
    }

    public boolean parseString(){
        if(story.length() != 0) {
            String tempList[] = story.split("\\s+");

            for (int i = 0; i < tempList.length; i++) {
               readyToRead.add(tempList[i]);
            }
            return true;
        } else {
            return false;
        }
    }

    public ArrayList<String> getList(){
        return readyToRead;
    }

    public void test(ArrayList<String> tester){
        for(String s : tester){
            System.out.println(s);
        }
    }
}
