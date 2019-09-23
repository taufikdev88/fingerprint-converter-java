package com.company;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.machinezoo.sourceafis.*;

import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static List<UserDetails> userDetailsList = new ArrayList<>();

    public static void main(String[] args) {
    }

    public static String findMatch(String imageFile){
        getPayload();
        String json = img2json("fingerprint.bmp");
        System.out.println(json);
        FingerprintTemplate template = json2template(json);
        UserDetails userMatch = find(template, userDetailsList);
        if(userMatch instanceof UserDetails){
            return userMatch.id;
        } else {
            return "tidak ketemu";
        }
    }

    public static String img2json(String path){
        try {
            byte[] image = Files.readAllBytes(Paths.get(path));
            FingerprintTemplate template = new FingerprintTemplate()
                    .dpi(500)
                    .create(image);
            String json = template.serialize();

            FileWriter fileWriter = new FileWriter("fingerTemplate.json");
            fileWriter.write(json);
            fileWriter.flush();

            return  json;
        } catch (Exception ex) {
            return "gagal: " + ex.toString();
        }
    }

    private static FingerprintTemplate json2template(String json){
        try {
            FingerprintTemplate template = new FingerprintTemplate()
                    .deserialize(json);
            return template;
        } catch (Exception ex) {
            return null;
        }
    }

    private static void getPayload(){
        try {
            String URL_JSON = "http://localhost/kantinian/index.php";
            URL url = new URL(URL_JSON);
            URLConnection request = url.openConnection();
            request.connect();

            userDetailsList.clear();

            JsonParser jsonParser = new JsonParser();
            JsonElement jsonElement = jsonParser.parse(new InputStreamReader((InputStream) request.getContent()));
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonArray jsonArray = jsonObject.getAsJsonArray("result");

            for (Object o: jsonArray) {
                if(o instanceof JsonObject){
                    String id = ((JsonObject) o).get("id").toString();
                    String nama = ((JsonObject) o).get("nama").toString();
                    String template = ((JsonObject) o).get("template").toString();
                    addUser2List(id, nama, template);
                }
            }
        } catch (Exception ex) {
            System.out.println("Error getPayload: " + ex.toString());
        }
    }

    private static void addUser2List(String id, String name, String json){
        UserDetails userDetails = new UserDetails();
        userDetails.id = id;
        userDetails.name = name;
        FingerprintTemplate template = json2template(json);

        userDetails.template = template;
        userDetailsList.add(userDetails);
    }

    private static UserDetails find(FingerprintTemplate probe, Iterable<UserDetails> candidates) {
        FingerprintMatcher matcher = new FingerprintMatcher()
                .index(probe);
        UserDetails match = null;
        double high = 0;
        for (UserDetails candidate : candidates) {
            double score = matcher.match(candidate.template);
            if(score > high) {
                high = score;
                match = candidate;
            }
        }
        double threshold = 30;
        System.out.println(high);
        return high >= threshold ? match : null;
    }

}

class UserDetails {
    String id, name;
    FingerprintTemplate template;
}