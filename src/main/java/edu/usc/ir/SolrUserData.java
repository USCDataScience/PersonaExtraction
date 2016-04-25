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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;

public class SolrUserData {

  @Option(name = "-h", usage = "Host Name")
  private String host;

  @Option(name = "-s", usage = "Solr URL")
  private String solrurl;

  public void processArgs(String[] args)
      throws IOException, ClassNotFoundException {
    CmdLineParser parser = new CmdLineParser(this);
    try {
      parser.parseArgument(args);
    } catch (CmdLineException e) {
      e.printStackTrace();
      System.err.println("java Executor [options...] arguments...");
      parser.printUsage(System.err);
      return;
    }
  }

  public void gedata() throws SolrServerException, IOException {
    UserExtractor user = new UserExtractor();
    HashMap<String, ArrayList<String>> Map = user.persons(host);

    String urlString = solrurl;

    HttpSolrServer server = new HttpSolrServer(urlString);

    for (Entry<String, ArrayList<String>> entry : Map.entrySet()) {
      System.out.print("HTML DOC ID - " + entry.getKey() + "\n");
      System.out.println("--USERNAME--");
      for (String id : entry.getValue()) {
        System.out.print(id + "\n");
      }
      System.out.println();
    }

    for (Entry<String, ArrayList<String>> entry : Map.entrySet()) {
      if (entry.getValue().isEmpty()) {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", entry.getKey());
        doc.addField("persons", "");
        server.add(doc);
      } else {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", entry.getKey());
        doc.addField("persons", entry.getValue());

        server.add(doc);
      }
    }

    server.commit();
  }

  public static void main(String[] args)
      throws FailingHttpStatusCodeException, MalformedURLException, IOException,
      SolrServerException, ClassNotFoundException {
    SolrUserData executor = new SolrUserData();
    executor.processArgs(args);
    executor.gedata();

  }

}
