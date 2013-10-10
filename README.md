# elasticsearch-editdistance-scoring

ElasticSearch script for scoring by edit distance (Levenstein). Molto alpha!

## Install

First, make sure `ES_HOME` is set. If not, export it, something like this:
```
export ES_HOME="/usr/share/elasticsearch"
```

Then, run the following commands after having cloned the repo:
```
make package
make install
```

## Sample usage

```
query = {
    "query": {
        "custom_score": {
            "query": {
                "match": {
                    "_all": {
                        "query": "my search string",
                        "analyzer": "ixxisearch",
                        "operator": "and"
                    }
                }
            },
            "script": "editdistance",
            "lang": "native",
            "params": {
                "field": "name",  # field to use for editdistance computing against
                "search": "my search string"
            }
        }
    }
}
```