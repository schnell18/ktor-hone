# Introduction

The purpose of this project is to apply the following technologies in
the context of ktor framework:

- Routing
- JSON
- JWT Authentication
- CORS
- Content Negotiation

## Test the application

You can post snippet to this application using the command as follows:

    curl                                            \
      -u user:password                              \
      --request POST                                \
      --header "Content-Type: application/json"     \
      --data '{"snippet" : {"text" : "mysnippet"}}' \
      http://127.0.0.1:8080/snippets

Then get the snippets as follows:

    curl http://localhost:8080/snippets
