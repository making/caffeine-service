[Caffeine](https://github.com/ben-manes/caffeine) as a Service

### Run locally

``` console
$ ./mvnw clean package
$ java -jar target/caffeine-service-0.0.1-SNAPSHOT.jar
```

### Create a cache

``` console
$ curl -v -u master:master -XPOST localhost:8080/caffeine \
        -d service_id=3beccd82-21d1-45b7-8a2c-1b498f233acd \
        -d expire_second=60 \
        -d maximum_size=1000
> POST /caffeine HTTP/1.1
> Host: localhost:8080
> Authorization: Basic bWFzdGVyOm1hc3Rlcg==
> User-Agent: curl/7.43.0
> Accept: */*
> Content-Length: 82
> Content-Type: application/x-www-form-urlencoded
>
< HTTP/1.1 201 Created
< Server: Apache-Coyote/1.1
< X-Content-Type-Options: nosniff
< X-XSS-Protection: 1; mode=block
< Cache-Control: no-cache, no-store, max-age=0, must-revalidate
< Pragma: no-cache
< Expires: 0
< X-Frame-Options: DENY
< X-Application-Context: application
< Content-Type: text/plain;charset=UTF-8
< Content-Length: 13
< Date: Sat, 02 Apr 2016 14:09:47 GMT
<
Created Cache
```

### Delete a cache

``` console
$ curl -v -u master:master -XDELETE localhost:8080/caffeine/3beccd82-21d1-45b7-8a2c-1b498f233acd
> DELETE /caffeine/3beccd82-21d1-45b7-8a2c-1b498f233acd HTTP/1.1
> Host: localhost:8080
> Authorization: Basic bWFzdGVyOm1hc3Rlcg==
> User-Agent: curl/7.43.0
> Accept: */*
>
< HTTP/1.1 204 No Content
< Server: Apache-Coyote/1.1
< X-Content-Type-Options: nosniff
< X-XSS-Protection: 1; mode=block
< Cache-Control: no-cache, no-store, max-age=0, must-revalidate
< Pragma: no-cache
< Expires: 0
< X-Frame-Options: DENY
< X-Application-Context: application
< Date: Sat, 02 Apr 2016 14:08:13 GMT
<
```

### Create a credential

``` console
$ curl -v -u master:master -XPOST localhost:8080/credentials \
        -d service_id=3beccd82-21d1-45b7-8a2c-1b498f233acd \
        -d username=db5d3d51-8e19-4766-b478-5efd0ea3923f
> POST /credentials HTTP/1.1
> Host: localhost:8080
> Authorization: Basic bWFzdGVyOm1hc3Rlcg==
> User-Agent: curl/7.43.0
> Accept: */*
> Content-Length: 93
> Content-Type: application/x-www-form-urlencoded
>
< HTTP/1.1 201 Created
< Server: Apache-Coyote/1.1
< X-Content-Type-Options: nosniff
< X-XSS-Protection: 1; mode=block
< Cache-Control: no-cache, no-store, max-age=0, must-revalidate
< Pragma: no-cache
< Expires: 0
< X-Frame-Options: DENY
< X-Application-Context: application
< Content-Type: application/json;charset=UTF-8
< Transfer-Encoding: chunked
< Date: Sat, 02 Apr 2016 14:15:32 GMT
<
{"username":"db5d3d51-8e19-4766-b478-5efd0ea3923f","password":"69739cd5-13d7-4938-b96d-c090fe9baa42"}
```

### Delete a credential

``` console
$ curl -v -u master:master -XDELETE localhost:8080/credentials/3beccd82-21d1-45b7-8a2c-1b498f233acd/db5d3d51-8e19-4766-b478-5efd0ea3923f
> DELETE /credentials/3beccd82-21d1-45b7-8a2c-1b498f233acd/db5d3d51-8e19-4766-b478-5efd0ea3923f HTTP/1.1
> Host: localhost:8080
> Authorization: Basic bWFzdGVyOm1hc3Rlcg==
> User-Agent: curl/7.43.0
> Accept: */*
>
< HTTP/1.1 204 No Content
< Server: Apache-Coyote/1.1
< X-Content-Type-Options: nosniff
< X-XSS-Protection: 1; mode=block
< Cache-Control: no-cache, no-store, max-age=0, must-revalidate
< Pragma: no-cache
< Expires: 0
< X-Frame-Options: DENY
< X-Application-Context: application
< Date: Sat, 02 Apr 2016 14:14:55 GMT
<
```

### Put a value

``` console
$ curl -v -u db5d3d51-8e19-4766-b478-5efd0ea3923f:69739cd5-13d7-4938-b96d-c090fe9baa42 \
       -XPUT \
       -H 'Content-Type: text/plain' \
       -d 'Hello World' \
       localhost:8080/caffeine/3beccd82-21d1-45b7-8a2c-1b498f233acd/foo
> PUT /caffeine/3beccd82-21d1-45b7-8a2c-1b498f233acd/foo HTTP/1.1
> Host: localhost:8080
> Authorization: Basic ZGI1ZDNkNTEtOGUxOS00NzY2LWI0NzgtNWVmZDBlYTM5MjNmOjY5NzM5Y2Q1LTEzZDctNDkzOC1iOTZkLWMwOTBmZTliYWE0Mg==
> User-Agent: curl/7.43.0
> Accept: */*
> Content-Type: text/plain
> Content-Length: 11
>
* upload completely sent off: 11 out of 11 bytes
< HTTP/1.1 201 Created
< Server: Apache-Coyote/1.1
< X-Application-Context: application
< Content-Type: text/plain;charset=UTF-8
< Content-Length: 3
< Date: Sat, 02 Apr 2016 14:18:05 GMT
<
Put
```

### Get a value

``` console
$ curl -v -u db5d3d51-8e19-4766-b478-5efd0ea3923f:69739cd5-13d7-4938-b96d-c090fe9baa42 \
       -XGET \
       localhost:8080/caffeine/3beccd82-21d1-45b7-8a2c-1b498f233acd/foo
> GET /caffeine/3beccd82-21d1-45b7-8a2c-1b498f233acd/foo HTTP/1.1
> Host: localhost:8080
> Authorization: Basic ZGI1ZDNkNTEtOGUxOS00NzY2LWI0NzgtNWVmZDBlYTM5MjNmOjY5NzM5Y2Q1LTEzZDctNDkzOC1iOTZkLWMwOTBmZTliYWE0Mg==
> User-Agent: curl/7.43.0
> Accept: */*
>
< HTTP/1.1 200 OK
< Server: Apache-Coyote/1.1
< X-Application-Context: application
< Content-Type: text/plain;charset=UTF-8
< Content-Length: 11
< Date: Sat, 02 Apr 2016 14:20:42 GMT
<
Hello World
```

### Delete a value

``` console
$ curl -v -u db5d3d51-8e19-4766-b478-5efd0ea3923f:69739cd5-13d7-4938-b96d-c090fe9baa42 \
       -XDELETE \
       localhost:8080/caffeine/3beccd82-21d1-45b7-8a2c-1b498f233acd/foo
> DELETE /caffeine/3beccd82-21d1-45b7-8a2c-1b498f233acd/foo HTTP/1.1
> Host: localhost:8080
> Authorization: Basic ZGI1ZDNkNTEtOGUxOS00NzY2LWI0NzgtNWVmZDBlYTM5MjNmOjY5NzM5Y2Q1LTEzZDctNDkzOC1iOTZkLWMwOTBmZTliYWE0Mg==
> User-Agent: curl/7.43.0
> Accept: */*
>
< HTTP/1.1 204 No Content
< Server: Apache-Coyote/1.1
< X-Application-Context: application
< Date: Sat, 02 Apr 2016 14:21:57 GMT
<
```

### Deploy Service

``` console
$ cf cs p-mysql 512mb caffeine-credentials
$ cf push -b java_buildpack
```

Deploy [caffeine-broker](https://github.com/making/caffeine-broker)