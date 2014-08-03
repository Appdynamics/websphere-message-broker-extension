package com.appdynamics.extensions.wmb.resourcestats.json;


import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ResourceStatsAdapter implements JsonDeserializer<ResourceStatsObj> {

    public static final String RESOURCE_STATISTICS = "ResourceStatistics";
    public static final String RESOURCE_TYPE = "ResourceType";
    public static final String RESOURCE_IDENTIFIER = "resourceIdentifier";
    public static final String NAME = "name";

    public ResourceStatsObj deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if(json != null){
            JsonObject root = json.getAsJsonObject();
            if(root != null){
                ResourceStatsObj resourceStatsObj = new ResourceStatsObj();
                JsonObject resStatsJsonObj = root.getAsJsonObject(RESOURCE_STATISTICS);
                buildResourceStatistics(resStatsJsonObj,resourceStatsObj);
                return resourceStatsObj;
            }
        }
        return null;
    }

    private void buildResourceStatistics(JsonObject resStatsJsonObj, ResourceStatsObj resourceStatsObj) {
        if(resStatsJsonObj != null){
            ResourceStatistics resourceStatistic = new ResourceStatistics();
            JsonArray resTypeJsonArr = resStatsJsonObj.getAsJsonArray(RESOURCE_TYPE);
            buildResourceTypes(resTypeJsonArr,resourceStatistic);
            resourceStatsObj.setResourceStatistics(resourceStatistic);
        }
    }

    private void buildResourceTypes(JsonArray resTypeJsonArr, ResourceStatistics resourceStatistic) {
        if(resTypeJsonArr != null){
            List<ResourceType> resourceTypes = new ArrayList<ResourceType>();
            Gson gson = new Gson();
            for(JsonElement jsonElement : resTypeJsonArr){
                JsonObject resTypeObj = jsonElement.getAsJsonObject();
                if(resTypeObj != null && resTypeObj.get(NAME) != null){
                    ResourceType resourceType = new ResourceType();
                    resourceType.setName(resTypeObj.get(NAME).getAsString());
                    JsonArray resIdentifierJsonArr = resTypeObj.getAsJsonArray(RESOURCE_IDENTIFIER);
                    buildResourceIdentifiers(resIdentifierJsonArr,resourceType,gson);
                    resourceTypes.add(resourceType);
                }
            }
            resourceStatistic.setResourceType(resourceTypes);
        }
    }

    private void buildResourceIdentifiers(JsonArray resIdentifierJsonArr, ResourceType resourceType,Gson gson) {
        if(resIdentifierJsonArr != null){
            List<ResourceIdentifier> resourceIdentifiers = new ArrayList<ResourceIdentifier>();
            for(JsonElement resIdentifierJsonElem : resIdentifierJsonArr){
                Map<String,String> metrics = gson.fromJson(resIdentifierJsonElem.getAsJsonObject(),
                        new TypeToken<Map<String, String>>() {
                        }.getType()
                );
                ResourceIdentifier resourceIdentifier = new ResourceIdentifier();
                resourceIdentifier.setResourceMetrics(metrics);
                resourceIdentifiers.add(resourceIdentifier);
            }
            resourceType.setResourceIdentifier(resourceIdentifiers);
        }
    }


}
