CAH-WebApp
==========

Web-app for Inhumane Card Games (2014-09-24)

## Backend

The backend has the following dependencies:
* Maven: `sudo apt-get install maven2`
* A Java 7 SDK.
* Setting the `$JAVA_HOME` environment variable to the right place.

There's some preconfigured eclipse project launchers which probably
work (but I haven't tried them).

Note that the AppEngine SDK isn't necessary - maven will take care
of installing that for you.

### Running the dev server
`mvn appengine:devserver`

And open http://localhost:8080

## Frontend
Coming soon.
