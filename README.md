REST to SOAP Transformation with Fuse 7.13
==========================================

This example demonstrates how to use Apache Camel to frontend an exisiting legacy SOAP service with a RESTFUL interface.  This can be run standalone or deployed to OpenShift 4.x.

## Prerequisites

1. Java 11+
2. Maven 3.8+
3. A running OpenShift 4.x instace.  For testing, I use ROSA.

## Build

You can build this example using

```
    mvn package
```

## Run the example

Using the shell:

 1. Start the springboot service:

```
  $ mvn spring-boot:run
```

## Test the example:

Using HTTPie, import the Swagger spec using the following url:

```
     http://localhost:8080/camel/api-doc
```

You can test the transformation using any number you like in the URL:

```bash
curl -X GET "http://localhost:8080/camel/calculator/2/201" -H "accept: application/json" | jq
```

     
## Deploying to OpenShift

To deploy to OpenShift, we can follow the s2i binary method via the CLI.

## Prerequisites

1. Java 11+
2. Mave 3.8+ 
3. oc CLI

## Method

1. Via the CLI, login to OpenShift:

```bash
oc login -u developer -p developer
```

2. Create a new OpenShift project:

```bash
oc new-project rest-soap
```

3. Deploy to OpenShift using the s2i binary method:

```bash
mvn clean -DskipTests oc:deploy -Popenshift
```

4. Using the above hostname, test the service with the following command:

```bash
curl http://<ocp route>/camel/api-doc
```

7.  You can import the above URL into Insomnia and test the endpoint using the OpenAPI spec.

You can test the transformation using any number you like in the URL:

```bash
curl -X GET "http://<ocp route>/camel/calculator/2/201" -H "accept: application/json" | jq
```

8.  If the request is successful, you should receive the following response:

```json
{
  "AddResponse": {
    "AddResult": 203
  }
}
```