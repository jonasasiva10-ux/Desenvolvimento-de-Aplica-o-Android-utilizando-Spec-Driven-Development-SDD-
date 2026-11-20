# 02 — Arquitetura e Planejamento Técnico

## 1. Arquitetura Clean Architecture

O projeto segue estritamente a **Clean Architecture** com três camadas independentes sob o pacote base `com.example`:

```text
┌──────────────────────────────────────────────────────────┐
│                     PRESENTATION                         │
│  Jetpack Compose • Navigation Compose • ViewModels       │
│  (UiState imutável via StateFlow + collectAsStateWithLifecycle)
└───────────────────────────┬──────────────────────────────┘
                            │ depende de
                            ▼
┌──────────────────────────────────────────────────────────┐
│                        DOMAIN                            │
│  Models • Repository Interfaces • UseCases • AppError    │
│  (Kotlin puro — sem dependências de Android/Room/Retrofit)
└───────────────────────────▲──────────────────────────────┘
                            │ implementa
┌───────────────────────────┴──────────────────────────────┐
│                         DATA                             │
│  Retrofit APIs • Moshi DTOs • Room DB/DAOs • Mappers     │
│  RepositoryImpl (Offline-First SSOT) • Connectivity      │
└──────────────────────────────────────────────────────────┘
```

### Regra de Dependência
- **`domain`** não importa classes de `android.*`, `androidx.room.*` ou `retrofit2.*`. Contém exclusivamente modelos de domínio, contratos de repositório (`GameRepository`, `PatchRepository`, `PlayerRepository`), tipos de erro (`AppError`) e casos de uso (`UseCase`).
- **`data`** depende de `domain`, implementando os contratos de repositório e encapsulando Retrofit, Moshi, Room e `ConnectivityManager`.
- **`presentation`** depende apenas de `domain` (nunca acessa DAOs, DTOs ou APIs diretamente), consumindo casos de uso injetados via **Koin**.

---

## 2. Estrutura Modular de Pacotes

```text
app/src/main/java/com/example/
├── PatchPulseApplication.kt       # Inicialização do Koin DI
├── MainActivity.kt                # Edge-to-edge + PatchPulseApp
├── di/
│   └── AppModules.kt              # Módulos Koin (Network, Database, Repository, UseCase, ViewModel)
├── domain/
│   ├── model/                     # GameRank, GameDetail, PatchNote, ProPlayer, PlayerStats, AppError
│   ├── repository/                # Interfaces GameRepository, PatchRepository, PlayerRepository
│   └── usecase/                   # Casos de uso coesos e testáveis
├── data/
│   ├── catalog/                   # Catálogo curado de AppIDs oficiais Steam
│   ├── local/
│   │   ├── PatchPulseDatabase.kt  # RoomDatabase (v1)
│   │   ├── dao/                   # GameDao, PatchNoteDao, PlayerDao, SyncStatusDao
│   │   └── entity/                # Entidades @Entity Room
│   ├── mapper/                    # Conversores DTO -> Entity -> Domain + Sanitizador de Patch Notes
│   ├── network/                   # NetworkConnectivityObserver + safeApiCall
│   ├── remote/
│   │   ├── api/                   # SteamWebApi, SteamStoreApi, OpenDotaApi
│   │   └── dto/                   # DTOs com Moshi Codegen
│   └── repository/                # GameRepositoryImpl, PatchRepositoryImpl, PlayerRepositoryImpl
├── presentation/
│   ├── navigation/                # Screen routes + PatchPulseNavGraph + Adaptive Navigation
│   ├── components/                # Componentes reutilizáveis (Cards, Badges, Estados de UI)
│   ├── trends/                    # Tendências & Rankings de Jogos
│   ├── patches/                   # Feed de Atualizações & Patches
│   ├── players/                   # Pro Leaderboard & Busca de Player Stats
│   ├── favorites/                 # Jogos Favoritos (Offline Hub)
│   ├── gamedetail/                # Telemetria ao vivo e detalhes do jogo
│   └── playerdetail/              # Perfil competitivo, Rank Tier e partidas recentes
└── ui/theme/                      # Color.kt, Type.kt (Fontes locais), Theme.kt
```

---

## 3. Estratégia Offline-First e Sincronização

O banco local **Room** atua como **Single Source of Truth (SSOT)**:

1. **Leitura Reativa:** A interface observa `Flow<T>` diretamente das consultas do Room (via Repository e UseCase), garantindo exibição imediata (< 50ms) na abertura do app.
2. **Sincronização com TTL (Time-To-Live):**
   - Cada domínio de dados registra seu `lastSyncedAt` na tabela `sync_status`.
   - Tendências e Patches possuem TTL de **5 minutos**; Leaderboard Pro possui TTL de **15 minutos**.
   - Ao abrir a tela, se o cache estiver ausente ou expirado, dispara-se uma sincronização assíncrona via `Dispatchers.IO`.
   - O usuário pode acionar sincronização forçada (*force refresh*) a qualquer momento.
3. **Preservação de Preferências Locais no Merge:**
   - Ao atualizar `game_ranks` a partir da API da Steam, o status `isFavorite` salvo pelo usuário é preservado na transação local, impedindo perda de favoritos durante sincronizações.
4. **Resiliência e Degradação Graciosa:**
   - Se a rede estiver indisponível ou a API retornar erro/timeout:
     - Havendo dados em cache: os dados locais continuam visíveis, acompanhados de indicador discreto de **Modo Offline / Cache** e timestamp da última sincronização.
     - Sem dados em cache: exibe tela de erro amigável com diagnóstico claro e botão de retentativa.
5. **Reconexão Automática:**
   - `NetworkConnectivityObserver` expõe `Flow<Boolean>` via `callbackFlow` com `unregisterNetworkCallback` garantido no `awaitClose` (prevenção de memory leak).

---

## 4. Stack Tecnológica

- **Linguagem:** Kotlin 2.4+ com Coroutines & Flow
- **UI:** Jetpack Compose + Material Design 3 (Edge-to-Edge, Layout Adaptativo com `NavigationBar` e `NavigationRail`, fontes locais `Space Grotesk`, `Plus Jakarta Sans` e `JetBrains Mono`)
- **Navegação:** Navigation Compose
- **Injeção de Dependências:** Koin 4.0.4 (`koin-android`, `koin-androidx-compose`)
- **Rede:** Retrofit 2.12 + OkHttp 4 + Moshi Codegen (KSP)
- **Persistência:** Room 2.7 (KSP)
- **Imagens:** Coil Compose
- **Testes:** JUnit 4, `kotlinx-coroutines-test`, Robolectric 4.16
