### G-Query Search Tool -  Back-End

Bem-vindo à documentação do projeto backend final da disciplina de elasticsearch desenvolvido em Java e Spring Boot, que utiliza Elasticsearch para implementar funcionalidades de busca avançada. Este README fornece uma visão detalhada sobre o projeto, suas funcionalidades, endpoints disponíveis e exemplos de uso.

---

#### **Sumário**

1. [Introdução](#introdução)
2. [Tecnologias Utilizadas](#tecnologias-utilizadas)
3. [Funcionalidades](#funcionalidades)
4. [Endpoints](#endpoints)
5. [Filtros e Parâmetros de Query](#filtros-e-parâmetros-de-query)
6. [Exemplos de Uso](#exemplos-de-uso)
7. [Retorno das APIs](#retorno-das-apis)
8. [Como Executar](#como-executar)
9. [Colaboradores](#colaboradores)

---

#### **Introdução**

Este projeto é um sistema de busca backend que permite realizar consultas avançadas em documentos armazenados no Elasticsearch. Ele oferece funcionalidades como busca simples, busca por frase exata, filtros por tempo de leitura e data de criação, ordenação dos resultados e sugestões para frases incorretas.

---

#### **Tecnologias Utilizadas**

![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![Apache Maven](https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-005571?style=for-the-badge&logo=elasticsearch&logoColor=white)

---

#### **Funcionalidades**

- **Busca com match simples:** Realiza uma busca básica nos documentos.
- **Busca com match phrase:** Permite buscar frases exatas nos documentos.
- **Filtros por tempo de leitura e data de criação:** Filtra os resultados com base em critérios de tempo de leitura e data de criação.
- **Ordenação dos resultados:** Ordena os resultados por tempo de leitura ou data de criação.
- **Sugestão para frases erradas:** Oferece sugestões quando a consulta contém erros.

---

#### **Endpoints**

- **GET `/v1/search?queryParameters`:** Realiza uma busca com base nos parâmetros fornecidos.
- **GET `/v1/search/fav:** Retorna os documentos marcados como favoritos.
- **POST `/v1/search/fav/{documentId}`:** Marca um documento como favorito.
- **DELETE `/v1/search/fav/{documentId}`:** Remove um documento dos favoritos.
- **POST `/v1/search/send-email`:** Envia os resultados da busca por e-mail.

---

#### **Filtros e Parâmetros de Query**

| Parâmetro             | Descrição                                                   |
|-----------------------|-------------------------------------------------------------|
| `query`               | Query a ser submetida para busca.                           |
| `pageNumber`          | Número da página atual.                                     |
| `filterReadingTime`   | Informa se o filtro para `reading_time` está ativo.         |
| `filterMinValue`      | Valor mínimo para `reading_time`.                           |
| `filterMaxValue`      | Valor máximo para `reading_time`.                           |
| `filterDateCreation`  | Informa se o filtro para `dt_creation` está ativo.          |
| `filterMinDate`       | Valor mínimo para `dt_creation`.                            |
| `filterMaxDate`       | Valor máximo para `dt_creation`.                            |
| `sortByReadingTIme`   | "asc" para ordenar de forma crescente, "desc" para decrescente. |
| `sortByDateCreation`  | "asc" para ordenar de forma crescente, "desc" para decrescente. |

*Obs: Os parâmetros podem ser nulos.*

---

#### **Exemplos de Uso**

**Buscar documentos:**
```
GET http://localhost:8080/v1/search?pageNumber=1&query=square%20root&sortByReadingTime=desc&sortByDateCreation=asc
```

**Marcar um documento como favorito:**
```
POST http://localhost:8080/v1/search/fav/8404
```

**Remover um documento dos favoritos:**
```
DELETE http://localhost:8080/v1/search/fav/8404
```

**Enviar resultados por e-mail:**
```json
POST http://localhost:8080/v1/search/send-email
{
    "receiver": "gabrielfrancelino3c@gmail.com",
    "results": [
        {
            "id": "8404",
            "title": "Kac–Moody algebra",
            "url": "https://en.wikipedia.org/wiki/Kac–Moody_algebra",
            "abs": "In mathematics a KacMoody algebra named for Victor Kac and Robert Moody who independently and simultaneously discovered them in is a Lie algebra usually infinitedimensional that can be defined by generators and relations through a generalized Cartan matrix These algebras form a generalization of finitedimensional semisimple Lie algebras and many properties related to the structure of a Lie algebra such as its root system irreducible representations and connection to flag manifo",
            "highlight": null,
            "reading_time": 4,
            "date_creation": "2019-01-27",
            "isFavorite": true
        }
    ]
}
```

---

#### **Retorno das APIs**

**GET `/v1/search` e GET `/v1/search/fav`:**
Retorna um objeto `Results`.

```json
Result: 
{
    "total_hits": 10,
    "total_pages": 1,
    "suggestion": "Você quis dizer 'Kac-Moody algebra'?",
    "search_time": 123,
    "results": [
        {
            "id": "8404",
            "currentPage": 1,
            "title": "Kac–Moody algebra",
            "url": "https://en.wikipedia.org/wiki/Kac–Moody_algebra",
            "abs": "In mathematics a KacMoody algebra...",
            "highlight": null,
            "reading_time": 4,
            "date_creation": "2019-01-27",
            "isFavorite": true
        }
    ]
}
```

**POST `/v1/search/fav/{documentId}` e DELETE `/v1/search/fav/{documentId}`:**
Retorna uma string informando que o documento foi favoritado ou desfavoritado.

---
Este projeto oferece uma solução robusta e eficiente para buscas avançadas em documentos utilizando Elasticsearch. Com uma variedade de funcionalidades e filtros, ele se adapta às necessidades de diferentes tipos de consultas e oferece uma interface amigável para usuários finais.

#### **Como executar**

Para executar o projeto, é necessário ter o Java 17, Maven e Docker instados instalados. Em seguida, basta seguir os seguintes comandos:

1. Clone o repositório:
    ```bash
    git clone https://github.com/gabriel-francelino/G-Query-Backend.git
    ```

2. Execute os containers do Elasticsearch:
    ```bash
    cd ./docker/
    ```
    ```bash
    docker compose up -d
    ```
    Obs: Caso deseje parar os containers, execute o comando `docker compose down`.


3. Execute o projeto:
    ```bash
    cd ..
    ```
    ```bash
    mvn spring-boot:run
    ```

#### **Colaboradores**

<a href="https://github.com/gabriel-francelino" target="_blank"><img src="https://img.shields.io/static/v1?label=Github&message=Gabriel Francelino&color=f8efd4&style=for-the-badge&logo=GitHub"></a>
<a href="https://github.com/gabriel-piva" target="_blank"><img src="https://img.shields.io/static/v1?label=Github&message=Gabriel Piva&color=f8efd4&style=for-the-badge&logo=GitHub"></a>