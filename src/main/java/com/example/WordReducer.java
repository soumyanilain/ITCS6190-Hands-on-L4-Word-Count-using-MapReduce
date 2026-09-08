package com.example;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import java.io.IOException;
import java.util.*;

public class WordReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
    private Map<Text, Integer> countMap = new HashMap<>();

    public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
        int sum = 0;
        for (IntWritable val : values) {
            sum += val.get();
        }
        countMap.put(new Text(key), sum);
    }

    protected void cleanup(Context context) throws IOException, InterruptedException {
        List<Map.Entry<Text, Integer>> sortedList = new ArrayList<>(countMap.entrySet());
        sortedList.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        for (Map.Entry<Text, Integer> entry : sortedList) {
            context.write(entry.getKey(), new IntWritable(entry.getValue()));
        }
    }
    
}