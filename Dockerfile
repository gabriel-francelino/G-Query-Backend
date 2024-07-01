# Etapa de construção
FROM ubuntu:latest AS build

# Instalar dependências
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    docker.io \
    curl \
    maven

# Instalar docker-compose
RUN curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose && \
    chmod +x /usr/local/bin/docker-compose

# Copiar arquivos do projeto
COPY . .

# Rodar o container do Elasticsearch
WORKDIR /docker
RUN service docker start && \
    docker-compose up -d && \
    sleep 30 && \
    curl -X PUT "http://localhost:9200/wikipedia" -H 'Content-Type: application/json' --user "elastic:user123" --insecure -d'\
    { \
      "settings": { \
        "index": { \
          "sort.field": [ "title", "reading_time", "dt_creation" ], \
          "sort.order": [ "asc", "asc", "asc" ] \
        }, \
        "number_of_shards": 4, \
        "number_of_replicas": 1, \
        "index.mapping.coerce": false, \
        "analysis": { \
          "analyzer": { \
            "analyzer_for_content": { \
              "type": "custom", \
              "char_filter": [ ], \
              "tokenizer": "standard", \
              "filter": [ \
                "asciifolding", \
                "lowercase", \
                "snowball" \
              ] \
            } \
          } \
        } \
      }, \
      "mappings": { \
        "dynamic": "strict", \
        "properties": { \
          "title": { \
            "type": "text", \
            "analyzer": "analyzer_for_content" \
          }, \
          "url": { \
            "type": "keyword", \
            "doc_values": false, \
            "index": false \
          }, \
          "content": { \
            "type": "text", \
            "analyzer": "analyzer_for_content" \
          }, \
          "dt_creation": { \
            "type": "date" \
          }, \
          "reading_time": { \
            "type": "integer" \
          } \
        } \
      } \
    }' && \
    curl -X PUT "http://localhost:9200/wikipedia_fav" -H 'Content-Type: application/json' --user "elastic:user123" --insecure -d'\
    { \
      "settings": { \
        "index": { \
          "sort.field": [ "title", "reading_time", "dt_creation" ], \
          "sort.order": [ "asc", "asc", "asc" ] \
        }, \
        "number_of_shards": 4, \
        "number_of_replicas": 1, \
        "index.mapping.coerce": false, \
        "analysis": { \
          "analyzer": { \
            "analyzer_for_content": { \
              "type": "custom", \
              "char_filter": [ ], \
              "tokenizer": "standard", \
              "filter": [ \
                "asciifolding", \
                "lowercase", \
                "snowball" \
              ] \
            } \
          } \
        } \
      }, \
      "mappings": { \
        "dynamic": "strict", \
        "properties": { \
          "title": { \
            "type": "text", \
            "analyzer": "analyzer_for_content" \
          }, \
          "url": { \
            "type": "keyword", \
            "doc_values": false, \
            "index": false \
          }, \
          "content": { \
            "type": "text", \
            "analyzer": "analyzer_for_content" \
          }, \
          "dt_creation": { \
            "type": "date" \
          }, \
          "reading_time": { \
            "type": "integer" \
          } \
        } \
      } \
    }' && \
    curl -H "Content-Type: application/x-ndjson" -XPOST "http://localhost:9200/wikipedia/_bulk" --data-binary "@wiki.json" --user "elastic:user123" --insecure

# Voltar ao diretório do projeto
WORKDIR /../

# Compilar o projeto com Maven
RUN mvn clean install

# Etapa final
FROM openjdk:17-jdk-slim

# Copiar o JAR compilado da etapa de construção
COPY --from=build /target/*.jar app.jar

# Comando para rodar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]
