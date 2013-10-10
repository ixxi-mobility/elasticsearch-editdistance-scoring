test:
	mvn test
package:
	mvn clean package
install:
	sudo $(ES_HOME)/bin/plugin -remove editdistance-scoring && sudo $(ES_HOME)/bin/plugin -url file://$(PWD)/target/releases/elasticsearch-editdistance-scoring-0.0.1.zip -install editdistance-scoring && sudo service elasticsearch restart