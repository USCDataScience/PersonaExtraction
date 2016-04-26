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

import org.apache.http.HttpHost;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
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
    Map<String, Persona> personas = new HashMap<String, Persona>();

    if (this.pageDir.exists() && this.pageDir.isDirectory()) {
      for (File page : this.pageDir.listFiles()) {
        extractor.setPage(page);

        Map<String, Persona> persMap = extractor.obtainPersonasForAllHosts();
        merge(persMap, personas);
      }
    }

    LOG.info("Obtained personas: [" + personas.toString() + "]: indexing.");
    for (String pageIdKey : personas.keySet()) {
      if (!personas.get(pageIdKey).getUsernames().isEmpty()) {
        LOG.info("Adding personas for: [" + pageIdKey + "]");
        indexPersona(personas.get(pageIdKey));
      } else {
        LOG.warning("Page Id: [" + pageIdKey + "]: No personas extracted.");
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

  private void merge(Map<String, Persona> from, Map<String, Persona> to) {
    for (String fromKey : from.keySet()) {
      if (to.containsKey(fromKey)) {
        to.get(fromKey).getUsernames().addAll(from.get(fromKey).getUsernames());
      } else {
        to.put(fromKey, from.get(fromKey));
      }
    }
  }

}
