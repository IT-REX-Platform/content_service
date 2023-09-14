# Content service

## Package structure

The service is structured similar to the microservice template.

## Integration tests

For integration tests, a H2 database is used instead of a separate Postgresql database server because of the following reasons:

- the database content is easier to manage as it is kept in memory and created from scratch
- it is faster
- database is automatically started and stopped when tests are executed or the application is started

For Postgresql and H2, two different configurations are required. 
For this purpose, a separate configuration file `application-dev.properties` is used which contains all the configuration
settings for development. To activate this, the following environment variable has to be set: `SPRING_CONFIG_NAME=dev`.

## API description

The GraphQL API is described in the [api.md file](api.md).

The endpoint for the GraphQL API is `/graphql`. The GraphQL Playground is available at `/graphiql`.

## How to run

How to run services locally is described in
the [wiki](https://gits-enpro.readthedocs.io/en/latest/dev-manuals/backend/get-started.html).

