package com.mooo.amksoft.amkmcauth.tools;

import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.common.collect.ImmutableList;

//code taken from: https://gist.github.com/evilmidget38/26d70114b834f71fb3b4

public class UUIDFetcher implements Callable<Map<String, UUID>> {
    //private static final int MAX_SEARCH = 100;
    //private static final String PROFILE_URL = "https://api.mojang.com/profiles/page/";
    private static final double PROFILES_PER_REQUEST = 100;
    private static final String PROFILE_URL = "https://api.mojang.com/profiles/";
    private static final String AGENT = "minecraft";
    private final JSONParser jsonParser = new JSONParser();
    private final List<String> names;

    public UUIDFetcher(List<String> names) {
        this.names = ImmutableList.copyOf(names);
    }

    private static void writeBody(HttpURLConnection connection, String body) throws Exception {
        DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
        writer.write(body.getBytes());
        writer.flush();
        writer.close();
    }

    //private static HttpURLConnection createConnection(int page) throws Exception {
    private static HttpURLConnection createConnection() throws Exception {
        //URL url = new URL(PROFILE_URL); // + page);
        URL url = new URL(PROFILE_URL + AGENT);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        return connection;
    }

	public Map<String, UUID> call() throws Exception {
		Map<String, UUID> uuidMap = new HashMap<String, UUID>();
		//String body = buildBody(names);

		//String PlayerName = names.get(0); // We only have 1 !

		UUID uuid;
		
		int requests = (int) Math.ceil(names.size() / PROFILES_PER_REQUEST );
		for (int i = 0; i < requests; i++) {
			HttpURLConnection connection = createConnection();
	        String body = JSONArray.toJSONString(names.subList(i * 100, Math.min((i + 1) * 100, names.size())));
			writeBody(connection, body);
			//System.err.println("So far, so good");
			JSONArray array = (JSONArray) jsonParser.parse(new InputStreamReader(connection.getInputStream()));
			for (Object profile : array) {
				JSONObject jsonProfile = (JSONObject) profile;
				String id = (String) jsonProfile.get("id");
				String name = (String) jsonProfile.get("name");
				//UUID uuid = UUIDFetcher.getUUID(id);
				uuid = UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-" + id.substring(16, 20) + "-" + id.substring(20, 32));
				uuidMap.put(name, uuid);
			}
		}
		return uuidMap;
	}
        
    // Een methode om OffLine Spelers naar UUID om te zetten ????
    // UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(Charsets.UTF_8))
}
