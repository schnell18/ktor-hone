# Introduction

The purpose of this project is to apply the following technologies in
the context of ktor framework:

- Routing
- JSON
- JWT Authentication
- CORS
- Content Negotiation

## Test the application

You need get a JWT token as the first step:

    curl                                            \
      --request POST                                \
      --header "Content-Type: application/json"     \
      --data '{"user": "test", "password": "test"}' \
      http://127.0.0.1:8080/register

You can post snippet to this application using the command as follows:

    curl                                                       \
      --request POST                                           \
      --header "Content-Type: application/json"                \
      --header "Authorization: Bearer {{jwt_token_from_prev}}" \
      --data '{"snippet" : {"text" : "mysnippet"}}'            \
      http://127.0.0.1:8080/snippets

Or you do the above commands in one go:

    curl                                                       \
      --silent                                                 \
      --request POST                                           \
      --header "Content-Type: application/json"                \
      --header "Authorization: Bearer $(curl -s --request POST --header "Content-Type: application/json" --data '{"user": "jack", "password": "jack"}' http://127.0.0.1:8080/register | jq -r .token)" \
      --data '{"snippet" : {"text" : "mysnippet"}}'            \
      http://127.0.0.1:8080/snippets

Or to delete snippet:

    curl                                                       \
      --silent                                                 \
      --request DELETE                                         \
      --header "Content-Type: application/json"                \
      --header "Authorization: Bearer $(curl -s --request POST --header "Content-Type: application/json" --data '{"user": "jack", "password": "jack"}' http://127.0.0.1:8080/register | jq -r .token)" \
      --data '200'             \
      http://127.0.0.1:8080/snippets



Then get the snippets as follows:

    curl http://localhost:8080/snippets
