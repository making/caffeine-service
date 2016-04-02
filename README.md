[Caffeine](https://github.com/ben-manes/caffeine) as a Service

### Deploy Service

``` console
$ ./mvnw clean package
$ cf cs p-mysql 512mb caffeine-credentials
$ cf push -b java_buildpack
```

### Create a credential

``` console
$ curl -u master:master caffeine-service.local2.pcfdev.io/credentials \
        -d service_id=3beccd82-21d1-45b7-8a2c-1b498f233acd \
        -d expire_second=60 \
        -d maximum_size=1000

{"password":"05b4a7e7-f83f-4382-90ed-d1e802dbacef","username":"db5d3d51-8e19-4766-b478-5efd0ea3923f"}
```

### Delete a credential

``` console
$ curl -u master:master caffeine-service.local2.pcfdev.io/credentials/3beccd82-21d1-45b7-8a2c-1b498f233acd

{"password":"05b4a7e7-f83f-4382-90ed-d1e802dbacef","username":"db5d3d51-8e19-4766-b478-5efd0ea3923f"}
```

### Put a value

``` console
$ curl -u db5d3d51-8e19-4766-b478-5efd0ea3923f:05b4a7e7-f83f-4382-90ed-d1e802dbacef \
       -XPUT \
       -H 'Content-Type: text/plain' \
       -d 'Hello World' \
       caffeine-service.local2.pcfdev.io/caffeine/3beccd82-21d1-45b7-8a2c-1b498f233acd/foo
```

### Get a value


``` console
$ curl -u db5d3d51-8e19-4766-b478-5efd0ea3923f:05b4a7e7-f83f-4382-90ed-d1e802dbacef \
       -XGET \
       caffeine-service.local2.pcfdev.io/caffeine/3beccd82-21d1-45b7-8a2c-1b498f233acd/foo

Hello World
```