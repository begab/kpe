package hu.u_szeged.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class WikiQuery {

  public enum QueryType {
    IDS, CATEGORY, LINKS
  }

  public static Object performQuery(String query, QueryType qt) {
    StringBuffer response = new StringBuffer();
    try {
      URL obj = new URL("http://rgai.inf.u-szeged.hu/kpe_rest/wiki/" + qt.toString().toLowerCase() + "?query=" + URLEncoder.encode(query, "UTF-8"));
      HttpURLConnection con = (HttpURLConnection) obj.openConnection();
      // optional default is GET
      con.setRequestMethod("GET");

      BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
      String responseLine;
      while ((responseLine = in.readLine()) != null) {
        response.append(responseLine);
      }
      in.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    Gson g = new Gson();
    return g.fromJson(response.toString(), new TypeToken<Collection<Object>>() {
    }.getType());
  }
}