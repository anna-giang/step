// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that returns some example content. TODO: modify this file to handle comments data */
@WebServlet("/data")
public class DataServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    
    String queryString = request.getQueryString();
    HashMap<String,String> fieldValues = getFieldValues(queryString);

    // Limit number of comments fetched from server to numOfComments
    int numOfComments = Integer.parseInt(fieldValues.get("quantity"));
    FetchOptions fetchOptions = FetchOptions.Builder.withLimit(numOfComments); 
    
    Query query = new Query("Comment");
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    List<HashMap<String, String>> commentList = new ArrayList<HashMap<String, String>>();

    for (Entity entity : results.asIterable(fetchOptions)) {
      // Each comment and the associated data (author, etc.) will be a HashMap, which converts to JSON object
      HashMap<String, String> comment = new HashMap<String, String>();
      String commentText = (String) entity.getProperty("commentText");
      String commentAuthor = (String) entity.getProperty("commentAuthor");
      comment.put("commentText", commentText);
      comment.put("commentAuthor", commentAuthor);
      commentList.add(comment);
    }

    String commentsJson = convertToJson(commentList);
    response.setContentType("application/json");
    response.getWriter().println(commentsJson);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get input from the form
    String commentText = request.getParameter("comment");
    String commentAuthor = request.getParameter("author-name");

    // Store in DataStore
    Entity commentEntity = new Entity("Comment");
    commentEntity.setProperty("commentText", commentText);
    commentEntity.setProperty("commentAuthor", commentAuthor);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(commentEntity);

    // Redirect back to the HTML page.
    response.sendRedirect("/index.html");
  }

  /**
   * Convert List to JSON string using the Gson library.
   * @param list the List of HashMap<String, String> to be converted to JSON
   * @return a JSON String with the ArrayList contents.
   */
  private String convertToJson(List<HashMap<String, String>> list) {
    Gson gson = new Gson();
    String json = gson.toJson(list);
    return json;
  }

  /**
   * Converts queryString into HashMap of field and value pairs obtained 
   * from the queryString
   * @param queryString query string of which the field value pair are to be processed
   * @return HashMap of field:value mappings
   */
  private HashMap<String, String> getFieldValues(String queryString) {
    String[] fieldValueStr = queryString.split("&");
    HashMap<String, String> fieldValues = new HashMap<String, String>();

    for (String param : fieldValueStr) {
      String[] fieldAndValue = queryString.split("=");
      String field = fieldAndValue[0];
      String value = fieldAndValue[1];
      fieldValues.put(field, value);
    }
    return fieldValues;
  }
}
