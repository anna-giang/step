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

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import com.google.sps.data.Nickname;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Handles storing and fetching of user comments. 
 * Linked to <form> element on index.html with id='comment-form' */
@WebServlet("/data")
public class DataServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    
    String queryString = request.getQueryString();
    HashMap<String,String> fieldValues = getFieldValues(queryString);

    // Limit number of comments fetched from server to numOfComments
    int numOfComments = Integer.parseInt((String) fieldValues.get("quantity"));
    FetchOptions fetchOptions = FetchOptions.Builder.withLimit(numOfComments); 
    
    Query query = new Query("Comment");
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    List<HashMap<String, Object>> commentList = new ArrayList<HashMap<String, Object>>();

    for (Entity entity : results.asIterable(fetchOptions)) {
      // Each comment and the associated data (author, etc.) will be a HashMap, which converts to JSON object
      HashMap<String, Object> comment = new HashMap<String, Object>();
      String commentText = (String) entity.getProperty("commentText");
      String commentAuthor = (String) entity.getProperty("commentAuthor");
      String blobKey = (String) entity.getProperty("blobKey");
      Boolean showEmail = (Boolean) entity.getProperty("showEmail");
      if (showEmail) {
        String email = (String) entity.getProperty("authorEmail");
        comment.put("authorEmail", email);
      }
      comment.put("commentText", commentText);
      comment.put("commentAuthor", commentAuthor);
      comment.put("blobKey", blobKey);
      comment.put("showEmail", showEmail);
      commentList.add(comment);
    }

    String commentsJson = convertToJson(commentList);
    response.setContentType("application/json");
    response.getWriter().println(commentsJson);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Retrieve the uploaded image blobkey
    BlobKey blobKey = getBlobKey(request, "image-upload");

    // Get input from the form
    String commentText = request.getParameter("comment");
    Boolean showEmail = Boolean.parseBoolean(request.getParameter("show-email"));

    // Get the user email and store with their comment
    // NOTE do not need to check login status, because only logged in users can access comment form
    UserService userService = UserServiceFactory.getUserService();
    String email = userService.getCurrentUser().getEmail();

    // Get user nickname and store with their comment
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    String nickname = Nickname.getUserNickname(userService.getCurrentUser().getUserId());

    // Store in DataStore
    Entity commentEntity = new Entity("Comment");
    commentEntity.setProperty("commentText", commentText);
    commentEntity.setProperty("commentAuthor", nickname);
    commentEntity.setProperty("authorEmail", email);
    commentEntity.setProperty("showEmail", showEmail);
    if (blobKey != null) {
      commentEntity.setProperty("blobKey", blobKey.getKeyString()); 
    }
    
    datastore.put(commentEntity);

    // Redirect back to the HTML page.
    response.sendRedirect("/index.html");
  }

  /**
   * Convert List to JSON string using the Gson library.
   * @param list the List of HashMap<String, String> to be converted to JSON
   * @return a JSON String with the ArrayList contents.
   */
  private String convertToJson(List<HashMap<String, Object>> list) {
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
      String[] fieldAndValue = param.split("=");
      String field = fieldAndValue[0];
      String value = fieldAndValue[1];
      fieldValues.put(field, value);
    }
    return fieldValues;
  }

  /**
   * Returns the BlobKey of the stored the uploaded image, or null if there was no uploaded image.
   * @param request the request sent to the doPost of this servlet
   * @param formInputElementName the form id of the comment form
   */
  private BlobKey getBlobKey(HttpServletRequest request, String formInputElementName) {
    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(request);
    List<BlobKey> blobKeys = blobs.get(formInputElementName);

    // User submitted form without selecting a file
    if (blobKeys == null || blobKeys.isEmpty()) {
      return null;
    }

    // Our form only contains a single file input, so get the first index and return
    BlobKey blobKey = blobKeys.get(0);
    return blobKey;
  }
}
