FROM ubuntu:latest AS build

# Install java
RUN apt-get update
RUN apt-get install openjdk-17-jdk -y
COPY . .

# Install docker
RUN apt-get install docker -y

# Rodar o container do elastic
RUN cd ./docker/ && docker-compose up -d
RUN cd ./../

# Install curl
RUN apt-get install curl -y
RUN curl -X PUT "localhost:9200/wikipedia" -H 'Content-Type: application/json' --user "elastic:user123" --insecure -d'
    {
      "settings": {
        "index": {
          "sort.field": [ "title", "reading_time", "dt_creation" ],
          "sort.order": [ "asc", "asc", "asc" ]
        },
        "number_of_shards": 4,
        "number_of_replicas": 1,
        "index.mapping.coerce": false,
        "analysis": {
          "analyzer": {
            "analyzer_for_content": {
              "type": "custom",
              "char_filter": [ ],
              "tokenizer": "standard",
              "filter": [
                "asciifolding",
                "lowercase",
                "snowball"
              ]
            }
          }
        }
      },
      "mappings": {
        "dynamic": "strict",
        "properties": {
          "title": {
            "type": "text",
            "analyzer": "analyzer_for_content"
          },
          "url": {
            "type": "keyword",
            "doc_values": false,
            "index": false
          },
          "content": {
            "type": "text",
            "analyzer": "analyzer_for_content"
          },
          "dt_creation": {
            "type": "date"
          },
          "reading_time": {
            "type": "integer"
          }
        }
      }
    }'
RUN curl -X PUT "localhost:9200/wikipedia_fav" -H 'Content-Type: application/json' --user "elastic:user123" --insecure -d'
    {
      "settings": {
        "index": {
          "sort.field": [ "title", "reading_time", "dt_creation" ],
          "sort.order": [ "asc", "asc", "asc" ]
        },
        "number_of_shards": 4,
        "number_of_replicas": 1,
        "index.mapping.coerce": false,
        "analysis": {
          "analyzer": {
            "analyzer_for_content": {
              "type": "custom",
              "char_filter": [ ],
              "tokenizer": "standard",
              "filter": [
                "asciifolding",
                "lowercase",
                "snowball"
              ]
            }
          }
        }
      },
      "mappings": {
        "dynamic": "strict",
        "properties": {
          "title": {
            "type": "text",
            "analyzer": "analyzer_for_content"
          },
          "url": {
            "type": "keyword",
            "doc_values": false,
            "index": false
          },
          "content": {
            "type": "text",
            "analyzer": "analyzer_for_content"
          },
          "dt_creation": {
            "type": "date"
          },
          "reading_time": {
            "type": "integer"
          }
        }
      }
    }'
curl -H "Content-Type: application/x-ndjson" -XPOST https://localhost:9200/wikipedia/_bulk --data-binary "@wiki.json" --user "elastic:user123" --insecure

# Install maven
RUN apt-get install maven -y
RUN mvn clean install

FROM openjdk:17-jdk-slim

COPY --from=build /target/*.jar app.jar

CMD ["java", "-jar", "app.jar"]