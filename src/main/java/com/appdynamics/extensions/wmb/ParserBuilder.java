package com.appdynamics.extensions.wmb;


import com.appdynamics.extensions.wmb.resourcestats.json.ResourceStatsAdapter;
import com.appdynamics.extensions.wmb.resourcestats.json.ResourceStatsObj;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

public class ParserBuilder {

    public Gson getParser(Class clazz,JsonDeserializer deserializer) {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(clazz,deserializer);
        return builder.create();
    }
}
