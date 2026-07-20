# 💰 MoneyLens

**AI-Powered Personal Finance Intelligence Platform**

> Upload your bank transactions → AI categorizes, predicts your spending, detects anomalies, and generates daily financial digests.

---

## 🎯 What It Does

```
┌──────────────┐     ┌──────────────────┐     ┌────────────────┐
│  Bank CSV    │────▶│  Spring Boot     │────▶│  AI Analysis   │
│  Upload      │     │  Backend         │     │  (Gemini)      │
└──────────────┘     └──────────────────┘     └───────┬────────┘
                                                      │
                              ┌────────────────────────┤
                              │            │           │
                        ┌─────▼─────┐ ┌────▼────┐ ┌───▼──────┐
                        │ Dashboard │ │ Digest  │ │ Predict  │
                        │ BI View   │ │ Daily   │ │ Forecast │
                        └───────────┘ └─────────┘ └──────────┘
```

### Features

| Feature | Description |
|---------|-------------|
| 📤 **CSV Upload** | Upload bank statement CSVs → AI categorizes every transaction |
| 🧠 **AI Categorization** | Gemini AI categorizes: Food, Transport, Subscription, etc. |
| 📊 **Spending Predictions** | Linear regression + AI predicts next month's expenses |
| 🚨 **Anomaly Detection** | Flags unusual spending spikes automatically |
| 📱 **Daily Digest** | AI-generated morning financial briefing (scheduled at 8 AM) |
| 🎯 **Savings Goals** | Set goals with AI feasibility analysis |
| 📈 **Business Dashboard** | Full BI dashboard with trends, top merchants, category breakdown |
| ⚡ **Caffeine Cache** | Caches AI responses to minimize API calls |
| 🔒 **Rate Limiting** | Bucket4j rate limiting on API endpoints |
| 📖 **Swagger API Docs** | Full OpenAPI documentation at `/swagger-ui.html` |

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Node.js 18+ (for React UI)
- A **free Gemini API key** — [Get yours here](https://aistudio.google.com/apikey)

### 1. Start the Backend

**Option A: Linux / Mac (Git Bash)**
```bash
cd moneylens

# Set your Gemini API key:
export GEMINI_API_KEY=your-key-here

# Run Spring Boot
mvn spring-boot:run
```

**Option B: Windows CMD**
```cmd
cd C:\Users\YourName\Desktop\JAVA\moneylens

REM If Maven is not in PATH, use the full path:
set PATH=C:\Users\YourName\AppData\Local\Temp\apache-maven-3.9.6\bin;%PATH%

REM Run Spring Boot
mvn spring-boot:run -q
```

> ⚠️ **Maven not installed?** The project auto-downloads Maven to `%LOCALAPPDATA%\Temp\apache-maven-3.9.6\bin\mvn` when first run via Git Bash. Use that full path in CMD:
> ```cmd
> C:\Users\YourName\AppData\Local\Temp\apache-maven-3.9.6\bin\mvn spring-boot:run -q
> ```

Backend runs at **http://localhost:8080**

### 2. Start the React UI
```bash
cd moneylens/ui

npm install
npm run dev
```
Frontend runs at http://localhost:3000 (auto-proxies to backend)

### 3. Open
- **Frontend UI:** http://localhost:3000
- **Swagger API Docs:** http://localhost:8080/swagger-ui.html
- **H2 Console:** http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:moneylens`)

---

## 📡 API Endpoints

### Users
```
POST   /api/users                  — Create user
GET    /api/users/{id}             — Get user
PUT    /api/users/{id}/budget      — Set monthly budget
```

### Transactions
```
POST   /api/transactions/upload    — Upload CSV (AI categorizes!)
GET    /api/transactions/{userId}  — List all transactions
GET    /api/transactions/{userId}/category-breakdown — Spending by category
```

### AI Insights
```
GET    /api/insights/predictions/{userId}       — Spending predictions
GET    /api/insights/anomalies/{userId}          — Anomaly detection
GET    /api/insights/digest/{userId}             — Today's AI digest
GET    /api/insights/digest/{userId}/{date}      — Digest for specific date
```

### Goals
```
POST   /api/goals/{userId}         — Create savings goal (AI feasibility check)
GET    /api/goals/{userId}         — List goals
PUT    /api/goals/{goalId}/progress — Update progress
```

### Dashboard
```
GET    /api/dashboard/{userId}     — Full BI dashboard data
```

---

## 📋 Usage Example

### Step 1: Create a User
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Priya", "email": "priya@example.com", "monthlyBudget": 50000}'
```

### Step 2: Upload Transactions
```bash
curl -X POST "http://localhost:8080/api/transactions/upload?userId=1" \
  -F "file=@sample-transactions.csv"
```

Response:
```json
{
  "totalTransactions": 30,
  "successCount": 30,
  "categoryBreakdown": {
    "FOOD": 8,
    "TRANSPORT": 4,
    "SUBSCRIPTION": 3
  },
  "anomalies": [],
  "totalSpent": 48985,
  "totalEarned": 87000
}
```

### Step 3: Get AI Predictions
```bash
curl http://localhost:8080/api/insights/predictions/1
```

### Step 4: Get Today's Digest
```bash
curl http://localhost:8080/api/insights/digest/1
```

### Step 5: View Dashboard
```bash
curl http://localhost:8080/api/dashboard/1
```

---

## 🏗️ Architecture

```
moneylens/
├── ui/                                   # React Frontend (Vite + React 18)
│   ├── src/
│   │   ├── components/                   # Layout, Sidebar, Login
│   │   ├── pages/                        # Dashboard, Upload, Predictions, Goals
│   │   ├── services/api.js               # Axios API layer
│   │   └── context/AppContext.jsx        # Auth + global state
│   └── package.json
│
├── src/main/java/com/moneylens/
│   ├── MoneyLensApplication.java          # Entry point
│   ├── config/
│   │   ├── AppConfig.java                 # RestTemplate + Caffeine cache
│   │   └── SwaggerConfig.java             # OpenAPI docs
│   ├── controller/
│   │   ├── TransactionController.java     # Upload + query transactions
│   │   ├── InsightsController.java        # Predictions + digest
│   │   ├── GoalController.java            # Savings goals
│   │   ├── UserController.java            # User management
│   │   └── DashboardController.java       # Full BI dashboard
│   ├── service/
│   │   ├── GeminiService.java             # Core Gemini API wrapper
│   │   ├── AICategorizationService.java   # AI transaction categorizer
│   │   ├── TransactionService.java        # CSV parsing + CRUD
│   │   ├── PredictionService.java         # Linear regression + AI
│   │   ├── DigestService.java             # Scheduled daily digests
│   │   └── GoalService.java               # Goals + feasibility
│   ├── model/
│   │   ├── User.java
│   │   ├── Transaction.java
│   │   ├── Goal.java
│   │   ├── DailyDigest.java
│   │   ├── Category.java                  # Enum with 12 categories
│   │   └── TransactionType.java           # CREDIT / DEBIT
│   ├── dto/                               # Request/Response DTOs
│   ├── repository/                        # Spring Data JPA repos
│   └── exception/                         # Global error handling
├── src/main/resources/
│   ├── application.yml                    # Config
│   └── sample-transactions.csv            # Demo data
└── pom.xml
```

---

## 🔧 Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Frontend | React 18 + Vite + Recharts | Interactive dashboard UI |
| Styling | Custom CSS (dark indigo theme) | Professional dark mode UI |
| Framework | Spring Boot 3.3 | Backend API |
| AI | Google Gemini 2.0 Flash | Free tier: 60 req/min |
| Database | H2 (dev) / PostgreSQL (prod) | Transaction storage |
| Cache | Caffeine | Cache AI responses |
| Docs | SpringDoc OpenAPI | Swagger UI |
| CSV | OpenCSV | Parse bank statements |
| Stats | Apache Commons Math | Statistical analysis |
| Build | Maven | Dependency management |

---

## 🌍 Deploy (Free)

### Render.com
```bash
# Push to GitHub, then:
# 1. Go to render.com → New → Web Service
# 2. Connect your repo
# 3. Build command: mvn clean package -DskipTests
# 4. Start command: java -jar target/moneylens-1.0.0.jar
# 5. Add env var: GEMINI_API_KEY = your-key
```

### Railway
```bash
# 1. railway login
# 2. railway init
# 3. railway up
# 4. railway variables set GEMINI_API_KEY=your-key
```

---

## 📸 Sample CSV Format

```csv
date,description,amount,type
2025-07-01,SALARY ACME CORP,75000,CREDIT
2025-07-02,SWIGGY BANGALORE,450,DEBIT
2025-07-03,NETFLIX SUBSCRIPTION,649,DEBIT
```

---

## 🎤 Interview Talking Points

> **Q: Tell me about this project.**
> "MoneyLens is a Spring Boot backend that uses Google Gemini AI to analyze personal finances. Users upload bank CSVs, and the AI categorizes every transaction, predicts next month's spending using linear regression, detects anomalies, and generates daily financial digests."

> **Q: What AI did you use?**
> "Gemini 2.0 Flash free tier for categorization, predictions, and daily summaries. I engineered prompts to get structured JSON responses from the LLM."

> **Q: How did you handle API limits?**
> "Implemented Caffeine caching for repeated patterns and Bucket4j rate limiting. Most common categories are cached, not re-sent to AI."

> **Q: What's the database design?**
> "Normalized schema: Users → Transactions with indexes on date and category. DailyDigest table for cached summaries. Goal table with AI-generated feasibility notes."

---

## 📝 License

MIT
