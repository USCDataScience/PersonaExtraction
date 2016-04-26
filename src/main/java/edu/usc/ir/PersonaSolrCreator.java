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
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.http.auth.AuthScope;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.kohsuke.args4j.Option;

public class PersonaSolrCreator {

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
  
  @Option(name = "-u", usage = "Solr Username", aliases = { "--user" })
  private String username = null;

  @Option(name = "-p", usage = "Solr Password", aliases = { "--pass" })
  private String password = null;

  @Option(name = "-s", usage = "Solr URL (full url including corename)", aliases = { "--solrUrl" }, required=true)
  private URL solrUrl = null;
  
  private Logger LOG = Logger.getLogger(PersonaSolrCreator.class.getName());


  @SuppressWarnings("deprecation")
  public void indexPersona(Persona persona) throws FileNotFoundException, IOException, SolrServerException {

    HttpSolrServer server = new HttpSolrServer(solrUrl.toString());
    HttpClientUtil.setBasicAuth((DefaultHttpClient) server.getHttpClient(), this.username, this.password);
    PersonaExtractor extractor = new PersonaExtractor();
    extractor.setConfigFile(this.configFile);
    extractor.setPage(this.page);
    if (this.patterns == null || (this.patterns != null && this.patterns.isEmpty()))
      initPatterns();
    
    
    if (persona.getUsernames().size() >  0){
        SolrInputDocument doc = new SolrInputDocument();
        String host = persona.getUrl().toString();
        List<String> users = persona.getUsernames();
        doc.addField("id", host);
        doc.addField("persons", users);
        server.add(doc);
        LOG.info("Indexing: Host: ["+host+"]: Personas: "+users+" to Solr: ["+this.solrUrl.toString()+"]");
      } else {
       LOG.info("Host: ["+host+"]: No persons extracted.");
      }
    

    server.commit();
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

}
