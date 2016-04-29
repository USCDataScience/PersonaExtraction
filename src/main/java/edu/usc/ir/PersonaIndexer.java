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

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;

public class PersonaIndexer {

  private Map<String, String> patterns = new HashMap<String, String>();

  @Option(name = "-d", usage = "Web Page Directory", aliases = {
      "--webPageDir" }, required = true)
  private File pageDir = null;

  @Option(name = "-c", usage = "Config File with Xpath selectors", aliases = {
      "--configFile" }, required = true)
  private File configFile = null;

  @Option(name = "-h", usage = "Hostname patterns to use to parse this page", aliases = {
      "--hostPatterns" }, required = false)
  private String host;

  @Option(name = "-u", usage = "Solr Username", aliases = { "--user" })
  private String username = null;

  @Option(name = "-p", usage = "Solr Password", aliases = { "--pass" })
  private String password = null;

  @Option(name = "-s", usage = "Solr URL (full url including corename)", aliases = {
      "--solrUrl" }, required = true)
  private URL solrUrl = null;

  // receives other command line parameters than options
  @Argument
  private List<String> arguments = new ArrayList<String>();

  private static final String USAGE = "java PersonaIndexer [options...] arguments...";

  private Logger LOG = Logger.getLogger(PersonaIndexer.class.getName());

  public void indexAllPersonas() throws FailingHttpStatusCodeException,
      MalformedURLException, IOException, SolrServerException {
    PersonaExtractor extractor = new PersonaExtractor();
    extractor.setConfigFile(this.configFile);
    extractor.setHost(this.host);

    if (this.pageDir.exists() && this.pageDir.isDirectory()) {
      for (File page : this.pageDir.listFiles()) {
        extractor.setPage(page);

        if (this.host != null && 
            !this.host.equals("")){
          Persona persona = extractor.obtainPersonas(this.host);
          LOG.info("Obtained personas: [" + persona.toString() + "]: for page: ["+persona.getPageId()+"] indexing.");
          indexPersona(persona);          
        }
        else{
          Map<String, Persona> personas = extractor.obtainPersonasForAllHosts();
          Persona aggregate = collect(personas);
          if(!aggregate.getUsernames().isEmpty()) {
              LOG.info("Obtained personas: [" + aggregate.toString() + "]: for page: ["+aggregate.getPageId()+"] indexing.");
              indexPersona(aggregate);
          }
          
        }
        
      }
    }


  }

  @SuppressWarnings("deprecation")
  public void indexPersona(Persona persona)
      throws FileNotFoundException, IOException, SolrServerException {

    String pageId = persona.getPageId();
    SolrServer server = null;
    if (this.username != null && this.password != null) {
      BasicCredentialsProvider provider = new BasicCredentialsProvider();
      UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
          this.username, this.password);
      provider.setCredentials(AuthScope.ANY, credentials);
      CloseableHttpClient client = HttpClientBuilder.create()
          .setDefaultCredentialsProvider(provider).build();
      server = new HttpSolrServer(solrUrl.toString(), client);
    } else {
      server = new HttpSolrServer(solrUrl.toString());
    }

    if (this.patterns == null
        || (this.patterns != null && this.patterns.isEmpty()))
      initPatterns();

    if (persona.getUsernames().size() > 0) {
      SolrInputDocument doc = new SolrInputDocument();
      List<String> users = persona.getUsernames();
      doc.addField("id", pageId);
      doc.addField("persons", users);
      doc.addField("host", host);
      server.add(doc);
      LOG.info("Indexing: Page Id: ["+pageId+"]: Host: [" + host + "]: Personas: " + users
          + " to Solr: [" + this.solrUrl.toString() + "]");
    } else {
      LOG.info("Page Id: [" + pageId + "]: No persons extracted.");
    }

    server.commit();
  }

  public static void main(String[] args) throws FailingHttpStatusCodeException,
      MalformedURLException, IOException, SolrServerException {
    PersonaIndexer indexer = new PersonaIndexer();
    try {
      indexer.processArgs(args);
      indexer.indexAllPersonas();
    } catch (CmdLineException e) {
      // don't go on
    }
  }

  /**
   * @return the pageDir
   */
  public File getPageDir() {
    return pageDir;
  }

  /**
   * @param pageDir the pageDir to set
   */
  public void setPageDir(File pageDir) {
    this.pageDir = pageDir;
  }

  /**
   * @return the configFile
   */
  public File getConfigFile() {
    return configFile;
  }

  /**
   * @param configFile the configFile to set
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
   * @param host the host to set
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   * @param username the username to set
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * @param password the password to set
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * @return the solrUrl
   */
  public URL getSolrUrl() {
    return solrUrl;
  }

  /**
   * @param solrUrl the solrUrl to set
   */
  public void setSolrUrl(URL solrUrl) {
    this.solrUrl = solrUrl;
  }
  

  private void processArgs(String[] args) throws CmdLineException {
    CmdLineParser parser = new CmdLineParser(this);
    try {
      parser.parseArgument(args);
      if (arguments.isEmpty()
          && (pageDir == null || configFile == null || solrUrl == null)) {
        throw new CmdLineException("Usage Error!");
      }

    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      System.err.println(USAGE);
      parser.printUsage(System.err);
      throw e;
    }
  }
  
  private Persona collect(Map<String, Persona> personas){
    Persona aggregate = new Persona(); 
    
    for (String hostPatternKey : personas.keySet()) {
      Persona persona = personas.get(hostPatternKey);
      if (aggregate.getPageId() == null){
        aggregate.setPageId(persona.getPageId()); // only once
      }
      if (!persona.getUsernames().isEmpty()) {
        LOG.info("Obtained personas: [" + persona.toString() + "]: for page: ["+persona.getPageId()+"]: host pattern: ["+hostPatternKey+"] collecting.");
        aggregate.getUsernames().addAll(persona.getUsernames());
      }
      else{
        LOG.warning("Page Id: [" + persona.getPageId() + "]: No personas extracted.");
      }
    }
    
    return aggregate;

  }

  private void initPatterns() throws FileNotFoundException, IOException {
    if (this.configFile != null) {
      Properties props = new Properties();
      props.load(new FileInputStream(this.configFile));
      for (Object prop : props.keySet()) {
        String propString = (String) prop;
        LOG.finest("Adding pattern: [" + props.getProperty(propString)
            + "] for host: [" + propString + "]");
        this.patterns.put(propString, props.getProperty(propString));
      }
    }
  }

}
