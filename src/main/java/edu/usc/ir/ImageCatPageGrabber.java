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
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class ImageCatPageGrabber {

  @Option(name = "-u", usage = "Username", aliases = { "--user" })
  private String username = null;

  @Option(name = "-p", usage = "Password", aliases = { "--pass" })
  private String password = null;

  @Option(name = "-i", usage = "Imagecat URL", aliases = { "--imagecatUrl" }, required=true)
  private URL imagecatUrl = null;

  @Option(name = "-o", usage = "Output Directory", aliases = { "--outputDir" }, required=true)
  private File outDir = null;
  
  @Option(name="-hp", usage = "Host Part (e.g., slickguns)", aliases = { "--hostPart" }, required=true)
  private String hostPart = null;
  
  @Option(name="-pp", usage = "Page Prefix (e.g., http://somehost/webmount/", aliases = { "--pagePrefix" }, required=true)
  private String pagePrefix = null;
  
  @Option(name="-s", usage = "Starting Solr Row e.g., 0, default 0", aliases = { "--rowStart" }, required=false)
  private int start = 0;
  
  @Option(name="-r", usage = "Rows in Solr, e.g., 10, default 10", aliases = { "--numRows" }, required=false)
  private int rows = 10;
  
  // receives other command line parameters than options
  @Argument
  private List<String> arguments = new ArrayList<String>();

  private static final String USAGE = "java ImageCatPageGrabber [options...] arguments...";

  private static Logger LOG = Logger
      .getLogger(ImageCatPageGrabber.class.getName());
  
  

  /**
   * Default constructor.
   */
  public ImageCatPageGrabber() {
  }

  /**
   * Grabs pages from the JPL ImageCat Web mount using a Solr query for
   * {@link #hostPart}, starting from {@link #start} and fetching
   * {@link #rows} rows. {@link #pagePrefix} is used to define the JPL
   * ImageCat web mount HTTP prefix.
   * 
   * @throws IOException
   *           If there is a HtmlUnit error.
   * @throws SolrServerException
   *           If there is a Solr error.
   */
  public void grabPages()
      throws IOException, SolrServerException {
    WebClient webclient = new WebClient();
    if (this.username != null && this.password != null){
      LOG.info("Credentials enabled.");
      DefaultCredentialsProvider provider = new DefaultCredentialsProvider();
      provider.addCredentials(this.username, this.password);
      webclient.setCredentialsProvider(provider);      
    }
    
    webclient.getOptions().setJavaScriptEnabled(false);
    webclient.getOptions().setThrowExceptionOnFailingStatusCode(false);
    webclient.getOptions().setThrowExceptionOnScriptError(false);
    HttpSolrServer server = new HttpSolrServer(this.imagecatUrl.toString());

    HttpClientUtil.setBasicAuth((DefaultHttpClient) server.getHttpClient(),
        this.username, this.password);
    LOG.info("Host: [" + hostPart + "]: pagePrefix: [" + pagePrefix
        + "]: start: [" + start + "]: rows: [" + rows + "]");
    SolrQuery query = new SolrQuery();
    query.setQuery("host:" + hostPart);
    query.set("fl", "id,persons");
    query.setStart(start);
    query.setRows(rows);
    QueryResponse response = server.query(query);
    SolrDocumentList docResults = response.getResults();
    List<String> docIds = new ArrayList<String>();
    for (int i = 0; i < docResults.size(); i++) {
      String id = docResults.get(i).getFieldValue("id").toString();
      int index = id.lastIndexOf('/');
      String docId = id.substring(index + 1);
      docIds.add(docId);
    }

    LOG.info("Obtained doc ids: [" + docIds + "]");

    String domain = hostToReversePath(hostPart);
    for (int j = 0; j < docIds.size(); j++) {
      ArrayList<String> username = new ArrayList<String>();
      String docId = docIds.get(j);
      String url = pagePrefix + domain + docId;
      HtmlPage htmlPage = webclient.getPage(url);
      WebResponse webResponse = htmlPage.getWebResponse();
      String content = webResponse.getContentAsString();
      
      File outPage = new File(
          this.outDir.getAbsoluteFile() + File.separator + docId);
      LOG.info("Writing page: ["+docId+"] to ["+outPage.getAbsolutePath()+"]");
      FileUtils.writeStringToFile(outPage, content);
    }

    webclient.close();
  }

  public static void main(String[] args) throws IOException, SolrServerException {    
    ImageCatPageGrabber grabber = new ImageCatPageGrabber();
    try {
      grabber.processArgs(args);
      grabber.grabPages();
    } catch (CmdLineException e) {
      // don't proceed
    }

  }

  private String hostToReversePath(String host) {
    String[] parts = host.split("\\.");
    StringBuilder reversePath = new StringBuilder();
    for (int i = parts.length - 1; i >= 0; i--) {
      reversePath.append(parts[i]);
      reversePath.append("/");
    }

    return reversePath.toString();

  }

  private void processArgs(String[] args) throws CmdLineException {
    CmdLineParser parser = new CmdLineParser(this);
    try {
      parser.parseArgument(args);
      if (arguments.isEmpty() && 
          (outDir == null || imagecatUrl == null || hostPart == null || pagePrefix == null)){
        throw new CmdLineException("Usage Error!");
      }
      
      // make outputDir
      if (!outDir.exists() && outDir.isDirectory()){
        LOG.info("Creating ["+outDir.getAbsolutePath()+"]");
        outDir.mkdirs();
      }
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      System.err.println(USAGE);
      parser.printUsage(System.err);
      throw e;
    }
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
   * @return the imagecatUrl
   */
  public URL getImagecatUrl() {
    return imagecatUrl;
  }

  /**
   * @param imagecatUrl the imagecatUrl to set
   */
  public void setImagecatUrl(URL imagecatUrl) {
    this.imagecatUrl = imagecatUrl;
  }

  /**
   * @return the outDir
   */
  public File getOutDir() {
    return outDir;
  }

  /**
   * @param outDir the outDir to set
   */
  public void setOutDir(File outDir) {
    this.outDir = outDir;
  }

  /**
   * @return the hostPart
   */
  public String getHostPart() {
    return hostPart;
  }

  /**
   * @param hostPart the hostPart to set
   */
  public void setHostPart(String hostPart) {
    this.hostPart = hostPart;
  }

  /**
   * @return the pagePrefix
   */
  public String getPagePrefix() {
    return pagePrefix;
  }

  /**
   * @param pagePrefix the pagePrefix to set
   */
  public void setPagePrefix(String pagePrefix) {
    this.pagePrefix = pagePrefix;
  }

  /**
   * @return the start
   */
  public int getStart() {
    return start;
  }

  /**
   * @param start the start to set
   */
  public void setStart(int start) {
    this.start = start;
  }

  /**
   * @return the rows
   */
  public int getRows() {
    return rows;
  }

  /**
   * @param rows the rows to set
   */
  public void setRows(int rows) {
    this.rows = rows;
  }

  /**
   * @return the arguments
   */
  public List<String> getArguments() {
    return arguments;
  }

  /**
   * @param arguments the arguments to set
   */
  public void setArguments(List<String> arguments) {
    this.arguments = arguments;
  }

}
