# Spring Boot Security demo script

## Intro
Spring Boot security adds a lot of security out of the box. The programmer does not have to think about these security
measures anymore and often does not realise that the vulnerabilities would still be there if Spring Boot Security
wouldn't have fixed them. Lets take a look at what Spring Boot Security does for you.

## Project setup
https://start.spring.io/

Add deps: web

Download file & move to dev folder

```
> git init
> git commit -am "initial commit"

## Download the internet and run
> ./mvnw clean install spring-boot:run

## See if the app runs (404 whitelabel error)
> curl localhost:8080
```


## PetStore

- Implement ```PetController``` with in memory database (HashMap)

```
curl -i http://localhost:8080/pets

200
[]

curl -i -X POST http://localhost:8080/pets -d '{"name": "Dog"}' -H "Content-Type:application/json"

200
{"id":0,"name":"Dog"}
```

## Adding security
