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

/**
 * Adds a random fact to the page.
 * @returns none
 */
function addRandomFact() {
  const facts = [
    'I have two younger brothers both about a foot taller than me.', 
    'My parents are from Vietnam but I don\'t speak any Vietnamese.',
    'I have never been to anywhere else in Australia except Melbourne, \
    where I was born and raised.'
  ];

  // Pick a random fact.
  const fact = facts[Math.floor(Math.random() * facts.length)];

  // Add it to the page.
  const factContainer = document.getElementById('fact-container');
  factContainer.innerText = fact;
}

/**
 * Show the full blog post text for blog post div with the given ID.
 * @param {String} blogId ID of the div of the blog post to be shown.
 * @returns none
 */
function toggleBlogPost(buttonId) {
  // Remove the '-b' from the button ID to get the ID of div of blog post.
  var blogDivId = buttonId.slice(0, buttonId.length-2);
  var content = document.getElementById(blogDivId);
  var button = document.getElementById(buttonId);

  if (content.style.display === 'none' || content.style.display === "") {
    content.style.display = 'block';
    // Change the label in the button accordingly to what button will do
    button.innerHTML = 'Read less';
  } else {
    content.style.display = 'none';
    button.innerHTML = 'Read more';
  }
}

/**
 * Fetches comments JSON from server at /data URL and adds the message to 
 * the div with id "comment-list". If no comments were returned, adds
 * "No Comments" message to the div.
 * @param quantity [OPTIONAL] The number of comments to fetch from the server. 
 *     Defaults to 5 if not provided.
 * @return none
 */
function fetchComments(quantity=5) {
  
  const endpoint = '/data?';
  var queryString = new URLSearchParams();
  queryString.append('quantity', String(quantity));
  const url = endpoint + queryString.toString();

  fetch(url).then(response => response.json()).then((commentData) => {
    
    let commentContent = ""
    
    if (commentData.length === 0 ) { // if there are no existing comments
      // disable the delete comments button & display "No Comments"
      document.getElementById('delete-comments-button').setAttribute('disabled','true');
      commentContent = '<div class="comment"><p class="body-text">No Comments</p></div>';
    }
    else { // otherwise display the comments and any attached image
      for (let i = 0; i < commentData.length; i++) {
        let commentText = commentData[i].commentText; // The actual comment
        
        // Comment author name: "'Anonymous' if there was not a name submitted."
        let commentAuthor = commentData[i].commentAuthor === "" ? "Anonymous" : commentData[i].commentAuthor; 
        
        // Display image as well, if there is an image
        let imageUrl = commentData[i].attachedImage;
        commentContent += '<div class="comment"><div class="flex-item"><p class="body-text"><b>' + commentAuthor + '</b></p>' 
              + '<p class="body-text">' + commentText + '</p></div>'; // outer <div> not closed yet

        if (imageUrl == null) {
          commentContent += '</div>'; // close outer div
        }
        else {
          commentContent += '<div class="flex-item"><a href=' + imageUrl + ' target="_blank"><img class="comment-image" src=' 
              + imageUrl + '></a></div></div>'; // add image
        }
      }
    }
    document.getElementById('comment-list').innerHTML = commentContent;
  });
}

/**
 * Sends a POST request to servlet at '/delete-data' endpoint
 * to delete all existing comments, then calls fetchComments()
 * to refresh the display of comments.
 * 
 * @returns none
 */
function deleteAllComments() {
  const request = new Request('/delete-data', {'method': 'POST'});
  fetch(request).then(response => {
      fetchComments();  
    }
  );
}

/**
 * Fetchs Blobstore upload URL from servlet at '/blobstore-upload-url',
 * and adds it as the 'action' of the form with id 'comment-form' in index.html.
 * 
 * @returns none
 */
function getBlobstoreUrl() {
  const request = new Request('/blobstore-upload-url', {'method': 'GET'});
  fetch(request).then((response) => {
      return response.text();
    }).then((imageUploadUrl) => {
      const commentForm = document.getElementById('comment-form');
      commentForm.action = imageUploadUrl;
    });
}
