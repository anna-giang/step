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

import java.io.IOException;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.sps.data.Nickname;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Checks whether user has set a nickname (GET requests), and faciliates the 
 * setting the user nickname (POST requests) */
@WebServlet("/nickname")
public class NicknameServlet extends HttpServlet {
  
  /** Checks whether user has set a nickname previously. Directs user to /createUserProfile.html to
   * set their nickname, otherwise redirects back to /index.html */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    UserService userService = UserServiceFactory.getUserService();

    if (!userService.isUserLoggedIn()) { // If user not logged in, redirect them to login before coming back
      String loginUrl = userService.createLoginURL("/nickname");
      response.sendRedirect(loginUrl);
      return;
    }

    String nickname = Nickname.getUserNickname(userService.getCurrentUser().getUserId());
    if (nickname == null) {
      response.sendRedirect("/createUserProfile.html");
      return;
    } 

    // User is logged in and their nickname is set, go back to home
    response.sendRedirect("/index.html");
  }

  /** Sets user nickname */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    
    UserService userService = UserServiceFactory.getUserService();
    if (!userService.isUserLoggedIn()) { // If user not logged in, redirect them to the login page
      String loginUrl = userService.createLoginURL("/nickname");
      response.sendRedirect(loginUrl);
      return;
    }

    String nickname = request.getParameter("nickname");
    String id = userService.getCurrentUser().getUserId();

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity entity = new Entity("UserInfo", id);
    entity.setProperty("id", id);
    entity.setProperty("nickname", nickname);
    datastore.put(entity);

    response.sendRedirect("/index.html");
  }
}
