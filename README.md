# PersonaExtraction
Extraction of user details from weapons dataset.

#Build Details
1) Create Solr Collection and add field "id" and "persons" <br/>
2) git clone https://github.com/shivensaiwal/PersonaExtraction.git <br/>
3) mvn assembly:assembly

# Execution Details
1) Jar created at target folder <br/>
3) Require username and password in config.properties <br/> 
2) Pass host and solr url as parameter to jar <br/>
	Example: java -jar target/PersonaExtraction-0.0.1-SNAPSHOT-jar-with-dependencies.jar -h "www.slickguns.com" -s "http://localhost:8983/solr/WeaponsData"

#Results
Solr collection created with HTML-Document Id and Usernames

#Note
Working for hostnames <br/>
1) www.hipointfirearmsforums.com <br/>
2) www.slickguns.com
