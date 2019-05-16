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

We now have an application without any security. Let's add spring security to pom.xml

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

```
./mvnw clean install spring-boot:run

curl localhost:8080/pets

401
{"timestamp":"2019-05-16T09:09:09.349+0000","status":401,"error":"Unauthorized","message":"Unauthorized","path":"/pets"}
```

We are denied to all resources. Spring automagically created the 'user' and printed the password on startup.

```
2019-05-16 11:08:21.121  INFO 5379 --- [           main] .s.s.UserDetailsServiceAutoConfiguration :
Using generated security password: 326853f2-a617-445a-aca3-2fedeca6c516
```

We will use the password provided by the startup log and user 'user'

```
curl localhost:8080/pets -u user:{password}
```

We can also browse to the endpoint and see that we get a login page. After logging in, we can execute all GET calls. The browser will remember the credentials.

Note:
By default, Spring security uses basic authentication. It is automatically added to all calls of the domain where the user logged on.

## Posting data
Let's add a pet to see if we can see any pets on screen

```
curl -i -X POST http://localhost:8080/pets -d '{"name": "Dog"}' -H "Content-Type:application/json" -u user:{password}
```

Does not work, why!?. After some googling, we find that csrf is blocking our way. Let's disable it:

SecurityConfig.java
```java
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        http.csrf().disable();
    }

}
```

It works now!
```
curl -i -X POST http://localhost:8080/pets -d '{"name": "Dog"}' -H "Content-Type:application/json" -u user:{password}
```

## Enter CSRF

The user of our application had an issue with his Pet store, and had to put all pets down. We create a new endpoint to facilitate this.
This seems like an endpoint that needs to be protected, but hey, we're using authentication. No problem.

Right!?

Let's navigate to localhost:8080/pets and take a look at our cute pets.
A coworker sends a cool link to pictures of kittens, let's take a look.

Browser: file://{dev-folder}/kittens.html

Awesome. kittens.

We take another look at our own pets, just checking if they're okay. We messed up, they are all dead.

How is this possible? How could another website know our credentials? The other website did not know, the browser knew.
If we take a look at kittens.html, we can see that the page hides a form that submits to an iframe on load. The target is our genocidal api.

The browser recognizes that the credentials of our api should be used, because you logged on earlier. Any other website can hijack the stored credentials to make an api call.

