USC Persona Extraction
======================

This project provides an XPath related approach to automatically 
extracting usernames, persona information and other data from 
unstructured text.

Getting Started
===============

 1. `git clone https://github.com/USCDataScience/PersonaExtraction.git`
 2. `cd PersonaExtraction`
 3. `mvn install assembly:assembly`
 
Persona Extraction
==================

To run the extractor, do the following:

 1. Run `src/main/bin/persona_extract $HOST $PAGE`

The `$HOST` parameter defines what host patterns to pull from 
`src/main/resources/patterns.properties`, e.g., `www.hipointfirearmsforums.com`.
The `$PAGE` parameter is a path to a downloaded or already available web
page that you would like to extract from.

Persona Indexing
================

You can run the PersonaExtractor on a directory full of documents and then
index into an [Apache Solr](http://lucene.apache.org/solr/) server. To do
so, run the following:

 1. Run `src/main/bin/persona_indexer $HOST $DIR $SOLR_URL`
 
$HOST should be the patterns to select based on `src/main/resources/patterns.properties`.
$DIR is a directory of HTML files to scan and then perform persona extraction on,
for each file.
$SOLR_URL should be the full path to your solr index, e.g., 
`http://localhost:8080/solr/persona-agora`

Questions, comments?
===================
Send them to [Chris Mattmann](chris.a.mattmann@jpl.nasa.gov).

Contributors
============
* Chris A. Mattmann, USC & JPL
* Shiven Saiwal, USC

License
=======
[Apache License, version 2](http://www.apache.org/licenses/LICENSE-2.0)
