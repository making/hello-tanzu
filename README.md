# Hello Tanzu

## How to build a docker image

Install [`pack`](https://buildpacks.io/docs/tools/pack/cli/install/) CLI, then

```
./mvnw clean package -DskipTests
pack build ghcr.io/making/hello-tanzu --builder paketobuildpacks/builder:base
```

Run the docker image

```
docker run --rm -p 8080:8080 -m 768m ghcr.io/making/hello-tanzu
```

![image](https://user-images.githubusercontent.com/106908/106774002-0675d080-6685-11eb-9d22-e0bf143f0fd6.png)

`/metrics` is the prometheus endpoint

![image](https://user-images.githubusercontent.com/106908/106778334-37580480-6689-11eb-97fa-42f64f954bab.png)


## Connect to PostgreSQL


```
psql postgres -c 'CREATE DATABASE tanzu'
```

```
docker run --rm -p 8080:8080 -e "JDBC_URL=jdbc:postgresql://host.docker.internal:5432/tanzu?user=${USER}&password=${PASSWORD}" -m 768m ghcr.io/making/hello-tanzu
```

![image](https://user-images.githubusercontent.com/106908/106778481-5fdffe80-6689-11eb-9cf4-8a294ce5c7d0.png)
