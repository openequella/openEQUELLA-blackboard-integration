package org.apereo.openequella.integration.blackboard.oeqAudit;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Date;

//import javax.servlet.http.HttpServletRequest;
import blackboard.platform.BbServiceManager;
import javax.servlet.http.HttpServletRequest;

public class Audit {
  private volatile static Audit instance;

  protected Audit() {

  }

  public static Audit instance() {
    if (instance != null) {
      return instance;
    }

    instance = new Audit();
    return instance;
  }

  public String getAudit() {
    // Test to pull in the dep.
    String state = BbServiceManager.getState().name();
    return "A successful test on " + (new Date()) + " State: " + state;
  }

  public String getError() {
    return null;// "An error msg here";
  }

}
