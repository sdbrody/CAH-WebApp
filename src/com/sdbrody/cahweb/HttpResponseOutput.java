package com.sdbrody.cahweb;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import org.apache.geronimo.mail.util.StringBufferOutputStream;

public class HttpResponseOutput {
  public HttpResponseOutput() {
    outString = new StringBuffer();
    out = new PrintWriter(new StringBufferOutputStream(outString));
    
    errString = new StringBuffer();
    err = new PrintWriter(new StringBufferOutputStream(errString));
  }
  
  public boolean setResponse(HttpServletResponse resp) throws IOException {
    err.close();
    if (errString.length() > 0) {
      //resp.getWriter().println(new JSONObject().put("error", errString.toString()).toString());
      System.err.println("responding with error: " + errString.toString());
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, errString.toString());
      return false;
    }
    out.close();
    resp.getWriter().println(outString.toString());
    System.out.println("response: " + outString.toString());
    //resp.sendError(HttpServletResponse.SC_OK);
    return true;      
  }
  
  public PrintWriter out;
  public PrintWriter err;
  protected StringBuffer outString;
  protected StringBuffer errString;
}
