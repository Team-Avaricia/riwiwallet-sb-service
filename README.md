# RiwiWallet - AI Financial Assistant Microservice

AI-powered financial assistant microservice for RiwiWallet. Integrates with Telegram to provide conversational financial management using Spring AI and OpenAI GPT-4.

## 🚀 Features

- **Telegram Chat Integration**: Telegram Bot API
- **Voice Message Support**: Audio transcription via OpenAI Whisper
- **AI-Powered Intent Classification**: GPT-4o-mini for understanding financial intents
- **Conversation History**: Context-aware responses with 30-minute TTL
- **Mock Mode**: In-memory testing without external API dependencies

## 📋 Supported Intents

| Intent | Description | Example |
|--------|-------------|---------|
| `create_expense` | Register an expense | "Gasté 50k en comida" |
| `create_income` | Register income | "Recibí mi sueldo de 2M" |
| `list_transactions` | View recent transactions | "Muéstrame mis gastos" |
| `list_transactions_by_date` | Query by specific date | "¿Cuánto gasté el 15 de noviembre?" |
| `list_transactions_by_range` | Query by date range | "Gastos del 1 al 15 de diciembre" |
| `search_transactions` | Search by description | "¿Cuánto he pagado de Netflix?" |
| `get_balance` | Check current balance | "¿Cuánto dinero tengo?" |
| `get_summary` | Expense summary by category | "¿En qué gasto más?" |
| `delete_transaction` | Delete last transaction | "Elimina mi último gasto" |
| `create_rule` | Create budget rule | "Pon un límite de 500k para comida" |
| `list_rules` | View budget rules | "¿Cuáles son mis límites?" |
| `validate_expense` | Expense consultation | "¿Debería gastar 200k en ropa?" |
| `question` | General financial questions | "¿Cómo puedo ahorrar más?" |

## 🛠️ Tech Stack

- **Java 21** + **Spring Boot 3.5.8**
- **Spring AI 1.1.0** (OpenAI integration)
- **OpenAI GPT-4o-mini** (intent classification)
- **OpenAI Whisper** (audio transcription)
- **Telegram Bot API**

## 📁 Project Structure

```
src/main/java/com/avaricia/sb_service/assistant/
├── controller/
│   ├── TelegramController.java       # Telegram webhook
│   └── NotificationController.java   # Notification API
├── dto/
│   └── IntentResult.java             # Intent classification result
└── service/
    ├── AudioTranscriptionService.java   # Whisper API integration
    ├── ConversationHistoryService.java  # Chat context management
    ├── CoreApiService.java              # MS Core REST client
    ├── IntentClassifierService.java     # GPT intent classification
    ├── MessageProcessorService.java     # Main orchestrator
    ├── MockCoreApiService.java          # In-memory mock backend
    ├── OpenAIService.java               # ChatClient wrapper
    ├── TelegramService.java             # Telegram message sender
    └── UserMappingService.java          # Platform user mapping
```

## ⚙️ Configuration

Create a `.env` file or set environment variables:

```properties
# OpenAI
OPENAI_API_KEY=sk-xxx

# Telegram
TELEGRAM_BOT_TOKEN=123456:ABC-xxx
TELEGRAM_BOT_USERNAME=@YourBot

# MS Core Backend
MS_CORE_BASE_URL=http://localhost:8080
MS_CORE_USE_MOCK=true
```

### application.properties

```properties
# OpenAI
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4o-mini

# Telegram
telegram.bot.token=${TELEGRAM_BOT_TOKEN}
telegram.bot.username=${TELEGRAM_BOT_USERNAME}

# MS Core API
ms.core.base-url=${MS_CORE_BASE_URL}
ms.core.use-mock=${MS_CORE_USE_MOCK:true}
```

## 🚀 Running

### Development (Mock Mode)

```bash
# Set mock mode
export MS_CORE_USE_MOCK=true

# Run with Maven
./mvnw spring-boot:run
```

### Production

```bash
# Disable mock mode
export MS_CORE_USE_MOCK=false

# Build and run
./mvnw clean package
java -jar target/sb-service-0.0.1-SNAPSHOT.jar
```

## 🔌 API Endpoints

### Telegram

```
POST /telegram/webhook
```

Receives Telegram updates. Configure webhook via:
```
https://api.telegram.org/bot<TOKEN>/setWebhook?url=https://your-domain.com/telegram/webhook
```

### Notifications

```
POST /api/notifications/telegram  # Send notification to user via Telegram
```

### Swagger UI

```
GET /swagger-ui.html              # Interactive API documentation
GET /v3/api-docs                  # OpenAPI JSON specification
```

## 🧪 Mock Mode

When `ms.core.use-mock=true`, the service uses `MockCoreApiService` with:
- In-memory transaction storage
- Simulated user balances
- No external API dependencies
- Perfect for local development and testing

## 📝 Account Linking

Users link their accounts via deep link:
1. Web dashboard generates link code
2. User clicks `t.me/YourBot?start=LINK_<code>`
3. Bot validates and links Telegram ID to user account

## 🤝 Contributing

1. Create feature branch from `develop`: `git checkout -b feature/your-feature`
2. Use conventional commits: `feat:`, `fix:`, `docs:`, `refactor:`
3. Create PR to `develop` branch
4. Squash merge when approved

## 📄 License

Private - RiwiWallet Team Avaricia
