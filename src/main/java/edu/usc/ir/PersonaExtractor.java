/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.usc.ir;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class PersonaExtractor {

  private Map<String, String> patterns = new HashMap<String, String>();

  @Option(name = "-p", usage = "Web Page", aliases = {
      "--webPage" }, required = true)
  private File page = null;

  @Option(name = "-c", usage = "Config File with Xpath selectors", aliases = {
      "--configFile" }, required = true)
  private File configFile = null;

  @Option(name = "-h", usage = "Hostname patterns to use to parse this page", aliases = {
      "--hostPatterns" }, required = false)
  private String host;

  // receives other command line parameters than options
  @Argument
  private List<String> arguments = new ArrayList<String>();

  private static final String USAGE = "java PersonaExtractor [options...] arguments...";

  private static Logger LOG = Logger
      .getLogger(PersonaExtractor.class.getName());

  /**
   * Default constructor.
   */
  public PersonaExtractor() {
  }

  public Map<String, Persona> obtainPersonasForAllHosts()
      throws FailingHttpStatusCodeException, MalformedURLException,
      IOException {
    if (this.patterns == null || (this.patterns != null && this.patterns.isEmpty()))
      initPatterns();

    Map<String, Persona> personaMap = new HashMap<String, Persona>();
    for (String host : this.patterns.keySet()) {
      personaMap.put(host, obtainPersonas(host));
    }

    return personaMap;
  }

  public Persona obtainPersonas(String host)
      throws FailingHttpStatusCodeException, MalformedURLException,
      IOException {
    if (this.patterns == null || (this.patterns != null && this.patterns.isEmpty()))
      initPatterns();
    
    WebClient webClient = new WebClient();
    webClient.getOptions().setJavaScriptEnabled(false);
    webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
    webClient.getOptions().setThrowExceptionOnScriptError(false);
    HtmlPage htmlPage = webClient.getPage(page.toURL());
    Persona persona = new Persona();
    persona.setUrl(new URL("http://"+host));

    List<HtmlAnchor> anchor = (List<HtmlAnchor>) htmlPage
        .getByXPath(patterns.get(host));
    for (int i = 0; i < anchor.size(); i++) {
      String link = anchor.get(i).getHrefAttribute();
      if (isUserLink(link)) {
        int index = link.lastIndexOf('/');
        String username = link.substring(index + 1);
        persona.getUsernames().add(username);
      }
    }

    webClient.closeAllWindows();
    return persona;
  }

  public static void main(String[] args) throws FailingHttpStatusCodeException,
      MalformedURLException, IOException {
    PersonaExtractor extractor = new PersonaExtractor();
    Map<String, Persona> personas = null;
    Persona persona = null;
    try {
      extractor.processArgs(args);
      String host = extractor.getHost();
      if (host == null) {
        LOG.info("Extracting all personas");
        personas = extractor.obtainPersonasForAllHosts();

        for (String personaHost : personas.keySet()) {
          LOG.info("Host: [" + personaHost + "]: Personas: "
              + personas.get(host).getUsernames());
        }
      } else {
        persona = extractor.obtainPersonas(extractor.getHost());
        LOG.info(
            "Host: [" + host + "]: Personas: " + persona.getUsernames());
      }

    } catch (CmdLineException e) {
      // don't move on
    }
  }

  private void processArgs(String[] args) throws CmdLineException {
    CmdLineParser parser = new CmdLineParser(this);
    try {
      parser.parseArgument(args);
      if (arguments.isEmpty() && (page == null || configFile == null)) {
        throw new CmdLineException("Usage Error!");
      }

    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      System.err.println(USAGE);
      parser.printUsage(System.err);
      throw e;
    }
  }

  private void initPatterns() throws FileNotFoundException, IOException {
    if (this.configFile != null) {
      Properties props = new Properties();
      props.load(new FileInputStream(this.configFile));
      for (Object prop :  props.keySet()) {
        String propString = (String)prop;
        LOG.finest("Adding pattern: ["+props.getProperty(propString)+"] for host: ["+propString+"]");
        this.patterns.put(propString, props.getProperty(propString));
      }
    }
  }

  private boolean isUserLink(String link) {
    if (link.toLowerCase().contains("php")) {
      return false;
    } else if (link.toLowerCase().contains("?")) {
      return false;
    } else
      return true;

  }

  /**
   * @return the page
   */
  public File getPage() {
    return page;
  }

  /**
   * @param page
   *          the page to set
   */
  public void setPage(File page) {
    this.page = page;
  }

  /**
   * @return the configFile
   */
  public File getConfigFile() {
    return configFile;
  }

  /**
   * @param configFile
   *          the configFile to set
   */
  public void setConfigFile(File configFile) {
    this.configFile = configFile;
  }

  /**
   * @return the host
   */
  public String getHost() {
    return host;
  }

  /**
   * @param host
   *          the host to set
   */
  public void setHost(String host) {
    this.host = host;
  }
}
