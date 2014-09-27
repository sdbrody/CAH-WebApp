package com.sdbrody.cahweb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public class DatastoreHelpers {
  public static final String kPropertyName = "data";
  public static class Writer {

    public Writer() {
      bytes = new ByteArrayOutputStream();
      try {
        out = new ObjectOutputStream(bytes);
      } catch (IOException e) {
        System.err.println("Failed to create byte stream ???");
        e.printStackTrace();
        out = null;
      }
    }

    public Entity done(String kind, String name, Key gameId) {
      try {
        out.close();
        System.err.println("Wrote " + bytes.toByteArray().length);
      } catch (IOException e) {
        System.err.println("Error writing to stream");
        return null;
      }
      Key key = KeyFactory.createKey(gameId, kind, name);
      Blob blob = new Blob(bytes.toByteArray());
      System.err.println("writing to key " + key.toString());
      System.err.println("content: " + blob.toString());
      Entity entity = new Entity(key);
      entity.setProperty(kPropertyName, blob);
      return entity;
    }

    protected ByteArrayOutputStream bytes;
    public ObjectOutputStream out;
  }
  
  public static class Reader {
    public Reader(Entity entity) {
      System.err.println("Reading from key: " + entity.getKey().toString());
      Blob data = (Blob) entity.getProperty(kPropertyName);
      System.err.println("read contents: " + data.toString());
      try {
        in = new ObjectInputStream(new ByteArrayInputStream(data.getBytes()));
      } catch (IOException e) {
        System.err.println("error reading data from entity " + entity.toString());
        e.printStackTrace();
      }
    }
    
    public ObjectInputStream in;
  }
}
