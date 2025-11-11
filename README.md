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

- Choose AI Provider:
  - GitHub Model: Setup [GitHub models](https://docs.github.com/en/github-models/use-github-models/prototyping-with-ai-models) (free): Create your Fine-grained personal access tokens in GitHub https://github.com/settings/personal-access-tokens. The token needs to have **models:read** permissions.
  - Google Gemini: Setup [Gemini API](https://aistudio.google.com/): Create your API key in Google AI Studio https://aistudio.google.com/api-keys
- Enable the base URL of your provider in application.properties
  - GitHub Models: `spring.ai.openai.base-url=https://models.github.ai/inference`
  - Google Gemini: `spring.ai.openai.chat.base-url=https://generativelanguage.googleapis.com`
- Update the application.properties with your GitHub token or Gemini API key.

### Run with Docker
- Make sure Docker and Docker Compose are installed on your machine.
- Update the OPEN_API_KEY value in your .env file with your GitHub personal access token.
- Open a terminal of your choice, navigate to the ntg-jvm-agent directory, and run:
      **docker compose up**

### Run Locally
Backend:
- Open the authorization-service project.
  Start the application by running the class:Open mcp-server project, start application by running class:
  + **MCPServerApplication**
- Open the mcp-server project.
  Start the application by running the class:
    + **MCPServerApplication**
- Open the orchestration-service project.
  Update the property spring.ai.openai.api-key in application.properties with your GitHub token.
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
- PgAdmin: http://localhost:3560/ Account login: admin@ntg.com / admin. Register a server: postgres, port 5432, username admin, password admin.
- The Postgresql server: servername: localhost, port: 5432, username: admin, password: admin

## Contributing

- Give us a star
- Reporting a bug
- Participate discussions
- Propose new features
- Submit pull requests. If you are new to GitHub, consider to [learn how to contribute to a project through forking](https://docs.github.com/en/get-started/quickstart/contributing-to-projects)

By contributing, you agree that your contributions will be licensed under MIT license.
