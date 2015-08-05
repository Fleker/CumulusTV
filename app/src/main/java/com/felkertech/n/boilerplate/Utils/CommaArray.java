package com.felkertech.n.boilerplate.Utils;

import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by N on 5/31/2015.
 */
public class CommaArray {

    private String source;
    private ArrayList<String> array;
    public CommaArray(String data) {
        source = data;
        String[] list = data.split(",");
        array = new ArrayList<>();
        for(int i=0;i<list.length;i++) {
            if(!list[i].isEmpty())
                array.add(list[i]);
        }
    }
    public int size() {
        if(source.isEmpty())
            return 0;
        return array.size();
    }
    public String get(int pos) {
        if(pos >= 0)
            return array.get(pos);
        return "";
    }
    public void set(int pos, String val) {
        if(array.size() > pos && pos >= 0)
            array.set(pos, val);
        else {
            Log.d("virtualpets:CommaArrray", "Position "+pos+" with "+val+" invalid for "+array.toString());
        }
        //else do nothing
    }
    public void add(String val) {
        array.add(val);
        Log.d("virtualpets:CommaArray", array.toString()+" "+array.size());
    }
    public void remove(String val) {
        Iterator<String> i = array.iterator();
        int index = 0;
        while(i.hasNext()) {
            String v = i.next();
            if(v.equals(val)) {
                array.remove(index);
                return;
            }
            index++;
        }
    }
    public boolean contains(String val) {
        return array.contains(val);
    }

    public void setArray(ArrayList<String> arrayList) {
        array = arrayList;
    }

    public String toString() {
        String out = "";
        Iterator<String> iterator = array.iterator();
        while(iterator.hasNext()) {
            String i = iterator.next();
            out += i+",";
        }
        if(out.length() == 0)
            return "";
        return out.substring(0, out.length()-1);
    }
    public String arrayToString() {
        return array.toString();
    }
    public long sum() {
        long sum = 0;
        Iterator<String> iterator = array.iterator();
        while(iterator.hasNext()) {
            String i = iterator.next();
            if(i.isEmpty())
                i = "0";
            sum += Long.parseLong(i);
        }
        return sum;
    }
    public Iterator<String> getIterator() {
        return array.iterator();
    }

    /**
     * Resizes the array, taking the last n items
     * @param spots The final max size of the array
     */
    public void truncate(int spots) {
        if(spots < array.size()) {
            int trimSize = spots - array.size();
            for(int i=0;i<trimSize;i++) {
                array.remove(i);
            }
        }
    }

    public CommaArray getRange(int start) {
        int in;
        int out;
        if(start < 0) {
            out = array.size();
            in = array.size()-start;
        } else {
            out = array.size();
            in = start;
        }
        CommaArray template = new CommaArray("");
        for(int i=in;i<out;i++) {
            template.add(array.get(i));
        }
        return template;
    }
}