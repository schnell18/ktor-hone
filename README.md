# Introduction

Collections of simple [ktor][1] projects for study and learning purpose.
Each project is based on maven build tool. You need maven installed and
configured properly.

# Sub Project Catalog

| subproject     | comment                         |
| -------------- | ------------------------------- |
| hello          | The classic hello world program |

## The `hello` project

You can build the `hello` project as follows:

    mvn install

Launch the application using the follow command:

    java -jar xxx.jar

Then you can test the application using:

    curl http://localhost:8080/

or

    curl http://localhost:8080/demo


[1]: http://ktor.io/
