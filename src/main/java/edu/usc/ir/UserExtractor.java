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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;

import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;


public class UserExtractor {

	
  private static HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
  // public static void main(String[] args) throws
  // FailingHttpStatusCodeException, MalformedURLException, IOException {

	@SuppressWarnings("deprecation")
	public HashMap<String, ArrayList<String>> persons(String host) throws FailingHttpStatusCodeException, MalformedURLException, IOException, SolrServerException {
		
		
		Properties prop = new Properties();
	    InputStream input = null;
	    input = UserExtractor.class.getClassLoader().getResourceAsStream("config.properties");

        // load a properties file
        prop.load(input);

        // get the property value and print it out
        String username_server=prop.getProperty("username");
        String password_server=prop.getProperty("password");
	
        String pattern = prop.getProperty(host);
        String domain = host.split("\\.")[1];
        System.out.println(host+username_server+password_server);
        System.out.println(pattern);
		WebClient webclient = new WebClient();
		DefaultCredentialsProvider provider = new DefaultCredentialsProvider();
		provider.addCredentials(username_server, password_server);
		webclient.setCredentialsProvider(provider);
		
		webclient.getOptions().setJavaScriptEnabled(false);
		
		webclient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		webclient.getOptions().setThrowExceptionOnScriptError(false);
		
		
		String urlString = "http://imagecat.dyndns.org/solr/imagecatdev";
		HttpSolrServer server = new HttpSolrServer(urlString);
		
		HttpClientUtil.setBasicAuth((DefaultHttpClient) server.getHttpClient(), username_server, password_server);
		
		SolrQuery query = new SolrQuery();
		query.setQuery("host:"+host);
		query.set("fl", "id,persons");
		query.setStart(0);
		query.setRows(10);
		QueryResponse response = server.query(query);
		//System.out.println(response);
		SolrDocumentList docResults = response.getResults();
		//System.out.println(docResults);
		List<String> htmldocs = new ArrayList<String>();
		for(int i=0;i<docResults.size();i++)
		{
		String id = docResults.get(i).getFieldValue("id").toString();
		
		int index=id.lastIndexOf('/');
  	   String lastString=(id.substring(index +1));
		
		htmldocs.add(lastString);
		}		
		
		for(int i=0;i<htmldocs.size();i++)
		{
			System.out.println(htmldocs.get(i));
		}
			
		
		for(int j=0;j<htmldocs.size();j++)
		{
			try
			{
				
			
			ArrayList<String> username = new ArrayList<String>();
		
		String url = "http://imagecat.dyndns.org/weapons/alldata/com/"+domain+"/www/"+htmldocs.get(j);
		System.out.println(j+"\t"+url);
		HtmlPage htmlPage = webclient.getPage(url);
		
		
		@SuppressWarnings("unchecked")
		
	//	List<HtmlAnchor> anchor = (List<HtmlAnchor>) htmlPage.getByXPath("//*[contains(@href,'members')]");
		List<HtmlAnchor> anchor = (List<HtmlAnchor>) htmlPage.getByXPath(pattern);
		//List<HtmlAnchor> anchor = (List<HtmlAnchor>) htmlPage.getByXPath("//')]");
		for(int i=0;i<anchor.size();i++)
		{
			
		//	System.out.println(anchor.get(i).getHrefAttribute());
			String link = anchor.get(i).getHrefAttribute();
			if(link.toLowerCase().contains("php"))
			{
				continue;
			}
			else if(link.toLowerCase().contains("?"))
			{
				continue;
			}
			else
			{
			int index=link.lastIndexOf('/');
     	   String lastString=(link.substring(index +1));
     	//   System.out.println(lastString);
     	   username.add(lastString);
			}
		}
		
		
		
		LinkedHashSet<String> users = new LinkedHashSet<String>();
	       users.addAll(username);
	       username.clear();
	       username.addAll(users);
	   
	       
	       
	       map.put(htmldocs.get(j), (ArrayList<String>) username);
			}
			catch(ClassCastException c)
			{
				continue;
			}
			catch (SocketTimeoutException e) {
				// TODO: handle exception
				continue;
			}
			catch (HttpHostConnectException e) {
				// TODO: handle exception
				continue;
			}
			catch (Exception e) {
				// TODO: handle exception
				continue;
			}
		}
		
		webclient.closeAllWindows();
		
		return map;	
		
	}
}
