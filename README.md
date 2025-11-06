# NTG JVM Agent
This project aims to practice building a chatbot in Kotlin

## Technologies and frameworks
- Kotlin
- Spring Boot
- Spring AI
- GitHub models
- PostgreSQL

## Getting started
- Setup [GitHub models](https://docs.github.com/en/github-models/use-github-models/prototyping-with-ai-models) (free): Create your Fine-grained personal access tokens in GitHub https://github.com/settings/personal-access-tokens. The token needs to have **models:read** permissions.
- Update the application.properties with your GitHub token
- Open terminal of your choice, go to ntg-jvm-agent directory, run docker compose up

### Services
- PgAdmin: http://localhost:3560/ Account login: admin@ntg.com / admin. Register a server: postgres, port 5432, username admin, password admin.
- The Postgresql server: servername: localhost, port: 5432, username: admin, password: admin
- Grafana: http://localhost:3030/ Account login: admin / admin. Used to visualize metrics, logs, and traces from Prometheus, Loki, and Tempo.
- Prometheus: http://localhost:9090/ Used to collect and store metrics.
- Loki: http://localhost:3100/ Used to collect and store logs.
- Tempo: http://localhost:3200/ Used to collect and store traces.

## Contributing

- Give us a star
- Reporting a bug
- Participate discussions
- Propose new features
- Submit pull requests. If you are new to GitHub, consider to [learn how to contribute to a project through forking](https://docs.github.com/en/get-started/quickstart/contributing-to-projects)

By contributing, you agree that your contributions will be licensed under MIT license.
