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
 * Adds a random greeting to the page.
 
function addRandomGreeting() {
  const greetings =
      ['Hello world!', '¡Hola Mundo!', '你好，世界！', 'Bonjour le monde!'];

  // Pick a random greeting.
  const greeting = greetings[Math.floor(Math.random() * greetings.length)];

  // Add it to the page.
  const greetingContainer = document.getElementById('greeting-container');
  greetingContainer.innerText = greeting;
}
*/

function addRandomFact() {
  const facts =
    ["I have two younger brothers both about a foot taller than me.", 
     "My parents are from Vietnam but I don't speak any Vietnamese.",
     "I have never been to anywhere else in Australia except Melbourne, where I was born and raised."
    ];

  // Pick a random greeting.
  const fact = facts[Math.floor(Math.random() * facts.length)];

  // Add it to the page.
  const factContainer = document.getElementById('fact-container');
  factContainer.innerText = fact;
}

/**
 * Show the full blog post text for blog post div with the given ID.
 * @param {*} blogId ID of the div of the blog post to be shown.
 * @returns none
 */
function toggleBlogPost(buttonId) {
    var blogDivId = buttonId.slice(0, buttonId.length-2);
    var content = document.getElementById(blogDivId);
    var button = document.getElementById(buttonId);

    if (content.style.display === "none" || content.style.display === "") {
        content.style.display = "block";
        // Change the label in the button accordingly to what button will do
        button.innerHTML = "Read less";
    } else {
        content.style.display = "none";
        button.innerHTML = "Read more";
    }

}