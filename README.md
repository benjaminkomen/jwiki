# jwiki
[![Build Status](https://travis-ci.org/fastily/jwiki.svg?branch=master)](https://travis-ci.org/fastily/jwiki)
![JDK-1.8+](https://upload.wikimedia.org/wikipedia/commons/7/75/Blue_JDK_1.8%2B_Shield_Badge.svg)
[![MediaWiki 1.27+](https://upload.wikimedia.org/wikipedia/commons/2/2c/MediaWiki_1.27%2B_Blue_Badge.svg)](https://www.mediawiki.org/wiki/MediaWiki)
[![License: GPL v3](https://upload.wikimedia.org/wikipedia/commons/8/86/GPL_v3_Blue_Badge.svg)](https://www.gnu.org/licenses/gpl-3.0.en.html)

jwiki is a simple, lightweight Java framework for interacting with a [MediaWiki](https://www.mediawiki.org/wiki/MediaWiki) instance.

The MediaWiki [API](https://www.mediawiki.org/wiki/API:Main_page) is complicated and difficult to use effectively.  Clients are forced to deal with nasty details such as pagination, credential management, JSON, and unexpected errors.  I created jwiki to manage and hide away these nasty bits while providing effortless access to all the powerful features of the MediaWiki API.  jwiki allows complex API queries and actions to be executed with _one_ straightforward function call consisting of simple objects and/or primitive types.  It's so easy that _anyone_ (new developers included) can create an application that works with MediaWiki.


## Getting Started
* Main class: [Wiki.java](https://github.com/fastily/jwiki/blob/master/src/main/java/fastily/jwiki/core/Wiki.java)
* [Javadocs](https://fastily.github.io/jwiki/docs/jwiki/)

## Download
jwiki is [available](https://bintray.com/fastily/maven/jwiki) on [jcenter](https://bintray.com/bintray/jcenter)

Maven:
```xml
<dependency>
  <groupId>fastily</groupId>
  <artifactId>jwiki</artifactId>
  <version>1.4.0</version>
  <type>pom</type>
</dependency>
```

Gradle:
```groovy
compile 'fastily:jwiki:1.4.0'
```

## Build
Build and publish jwiki on your local machine with
```bash
./gradlew build publishToMavenLocal
```

## Examples
### Get Page Text
```java
// Gets the text of the main page and prints it.
Wiki wiki = new Wiki("en.wikipedia.org");
System.out.println(wiki.getPageText("Main Page"));
```

### Login
```java
// Logs in to the English Wikipedia (you must have an account for this to work).
Wiki wiki = new Wiki("en.wikipedia.org");
boolean success = wiki.login("<YOUR_USERNAME>", "<YOUR_PASSWORD>");

if(success)
  System.out.printf("Login as %s was successful!\n", wiki.conf.uname);
```

### Edit
```java
// Test edits Wikipedia:Sandbox.
Wiki wiki = new Wiki("en.wikipedia.org");
boolean success = wiki.edit("Wikipedia:Sandbox", "Some test text", "Edit Summary");

if(success)
  System.out.println("Success!");
```

### Get Page Author
```java
// Gets the author of the Main Page and prints it
Wiki wiki = new Wiki("en.wikipedia.org");
System.out.println(wiki.getPageCreator("Main page"));
```

### Upload a file
```java
// Uploads a file.  You must have an account for this to work
Wiki wiki = new Wiki("<YOUR_USERNAME>", "<YOUR_PASSWORD>", "en.wikipedia.org");
boolean success = wiki.upload(Paths.get("<PATH_TO_YOUR_TEST_FILE>"), "<TITLE_TO_UPLOAD_FILE_TO>", "This is a test", "test summary");

if(success)
  System.out.println("Success!");
```

### Get categories of a page
```java
// Gets the categories on the Main Page and prints them
Wiki wiki = new Wiki("en.wikipedia.org");
ArrayList<String> l = wiki.getCategoriesOnPage("Main Page");

System.out.println(l);
```

### Get links pointing to a page
```java
// Gets all wiki-links pointing to the the Stack Overflow page and prints them
Wiki wiki = new Wiki("en.wikipedia.org");
ArrayList<String> l = wiki.whatLinksHere("Stack Overflow");

System.out.println(l);
```

### Check if multiple pages exist
```java
// Uses an efficient multi-query to check if the following pages exist, and then prints the ones that do.
Wiki wiki = new Wiki("en.wikipedia.org");
ArrayList<String> l = MQuery.exists(wiki, true, FL.toSAL("Stack Overflow", "BlahBlahBlahDoesNotExist", "Main Page"));

System.out.println(l);
```

### Gets the first paragraph of a page
```java
// Gets the first paragraph of the Stack Overflow page and prints it.
Wiki wiki = new Wiki("en.wikipedia.org");
System.out.println(wiki.getTextExtract("Stack Overflow"));
```