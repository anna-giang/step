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

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.HashMap;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Returns JSON containing logged in status and login/logout links as appropriate */
@WebServlet("/login-status")
public class LoginStatusServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");

    UserService userService = UserServiceFactory.getUserService();
    HashMap<String, String> userData = new HashMap<String, String>();

    if (userService.isUserLoggedIn()) {
      
      String logoutRedirectUrl = "/index.html";
      String logoutUrl = userService.createLogoutURL(logoutRedirectUrl);
      userData.put("loggedIn", "true");
      userData.put("logoutUrl", logoutUrl);
      
    } else {
      String loginRedirectUrl = "/index.html";
      String loginUrl = userService.createLoginURL(loginRedirectUrl);
      userData.put("loggedIn", "false");
      userData.put("logoutUrl", loginUrl);
    }
    
    // Convert to JSON and return
    Gson gson = new Gson();
    response.getWriter().println(gson.toJson(userData));
  }
}
