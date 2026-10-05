# NTG JVM Agent
This project aims to practice building a chatbot in Kotlin

## Technologies and frameworks
- Kotlin
- Spring Boot 4.0.1
- Spring AI 2.0.1
- OpenAI
- PostgreSQL

## Getting started
- Setup [OpenAI API](https://platform.openai.com/api-keys): Create an API key.
- Configure the OpenAI provider in application.properties:
  - `llm.providers.openai.base-url=https://api.openai.com/v1`
  - `llm.providers.openai.api-key=<your-api-key>`
  - `llm.providers.openai.completions-path=/chat/completions`
  - `llm.providers.openai.embeddings-path=/embeddings`

### Run with Docker
- Make sure Docker and Docker Compose are installed on your machine.
- Update the OPEN_API_KEY value in your .env file with your OpenAI API key.
- Open a terminal of your choice, navigate to the ntg-jvm-agent directory, and run:
      **docker compose up**

### Run Locally
Backend:
- Open the authorization-service project.
  Start the application by running the class:
  + **AuthorizationServerApplication**
- Open the mcp-server project.
  Start the application by running the class:
    + **MCPServerApplication**
- Open the orchestration-service project.
  Update the property llm.providers.openai.api-key in application.properties with your OpenAI API key.
  Start the application by running the class:
  + **OrchestratorApplication**

FrontEnd
- Open chat-ui project, start application by running command:
  + npm run build then
  + npm run dev
- Open admin-ui project, start application by running command: npm run build then npm run dev
  + npm run build then
  + npm run dev

### Services
- MCP server: http://localhost:9003/mcp. It uses Streamable HTTP and `/mcp` endpoint.
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
