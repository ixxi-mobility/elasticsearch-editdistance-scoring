test:
	mvn test
package:
	mvn clean package
install:
	sudo $(ES_HOME)/bin/plugin -remove editdistance-script && sudo $(ES_HOME)/bin/plugin -url file://$(PWD)/target/releases/elasticsearch-editdistance-script-0.0.1.zip -install editdistance-script && sudo service elasticsearch restart