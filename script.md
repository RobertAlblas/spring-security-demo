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

## Spring's CSRF security
To protect our pets from harm, we remove the SecurityConfig, so that Spring Boot defaults back to it's autoconfiguration.

If we try to do a POST, we will get a 401 again:

```
curl -i -X POST http://localhost:8080/pets -d '{"name": "Dog"}' -H "Content-Type:application/json" -u user:{password}

401
```

## Designing our calls to Spring's CSRF protection
How do we let our backend know that we're really the page that logged on, and not another tab?

### Standard CSRF flow

1. do a GET or OPTIONS call, extract the CSRF token from the Set-Cookie header
2. Store the CSRF token in the frontend JS application
3. When doing a POST, add the CSRF token as X-XSRF-TOKEN header

We send the token back as header, not as cookie. We do this, because other pages can hijack the cookies, just like
the login information. The other page cannot directly read the cookie, but it will be used if an AJAX call is executed
fromt the other page.

Instead we send it back as header. To send it as a header, the web application has to be able to read the cookie.

### Extract CSRF token from cookie

This sounds cool in theory, but when we execute an OPTIONS call, we don't see any CSRF token:

```
curl -X OPTIONS localhost:8080/pets -i -u user:

Set-Cookie: JSESSIONID=3494DE14D339E4AFD94CFE11F40AB328; Path=/; HttpOnly
```

Spring's default CSRF protection is designed for Thymeleaf/JSP. In this case, you can add the header as follows:

```html
<meta name="_csrf" content="${_csrf.token}"/>
<meta name="_csrf_header" content="${_csrf.headerName}"/>
``` 

And add it to the form or AJAX call by adding these variables. This does not work for a separated frontend. We can add
it as Synchronizer Token Pattern (Angular default) by adding the following configuration:

```java
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .anyRequest().authenticated()
                .and()
                .httpBasic()
                .and()
                .csrf().csrfTokenRepository(csrfTokenRepository());
    }

    private CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = withHttpOnlyFalse();
        repository.setCookiePath("/");
        return repository;
    }
}
```

We set httpOnly to false, so that our client side application can read the cookie. We set the cookie path to /, so that
the client side application can access it. Api's usually run at /api, not setting the cookie path will set the cookie path
to /api, making it inaccessible to the client. The session id is added to our cookies. The same session id/csrf token
combination must be sent with the next POST/PUT/DELETE call.

### Usage

Extract the CSRF token, also saving the cookies to a 'cookies' file, so that we store the session id
```
curl -X OPTIONS localhost:8080/pets -i -u user:{password} -c cookies

200
Set-Cookie: XSRF-TOKEN=b57d5106-0a1f-4018-a7e6-8f715105c20e; Path=/
```

Executing GET's doesn't need the CSRF token
```
curl -X GET localhost:8080/pets -i -u user:{password}

200
```

Add a new pet, with the XSRF token and session id though cookies:
```
curl -i -X POST http://localhost:8080/pets -d '{"name": "Dog"}' -H "Content-Type:application/json" -u user:{password} -H "X-XSRF-TOKEN:{token}" -b cookies

200
```

So to successfully POST we need the following:
- Username / password combination
- Session id in cookies
- CSRF token in header, matching the session id

## Additional CSRF protection
- Use the right HTTP verbs
    - NEVER change data using GET (one acceptable exception: auditing, last viewed)
- Validate content types: forms can only send form data or plain text (Spring automagically does this for you)
- Add CSRF token to all POST, PUT, DELETE endpoints
    - PUT and DELETE cannot be used from HTML forms, but there is no telling if this will change in the future
      or other frameworks might allow them.
