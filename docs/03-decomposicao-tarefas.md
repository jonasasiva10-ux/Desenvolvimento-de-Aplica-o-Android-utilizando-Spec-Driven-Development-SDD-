# 03 — Decomposição em Tarefas (SDD Backlog)

Sequência lógica e incremental de execução das entregas do projeto **SeuJogo**:

## Etapa 1 — Especificação e Planejamento
- [x] Definir escopo funcional, APIs oficiais e requisitos (`docs/01-especificacao.md`)
- [x] Planejar Clean Architecture, Offline-First e stack técnica (`docs/02-arquitetura-planejamento.md`)
- [x] Decompor implementação em tarefas rastreáveis (`docs/03-decomposicao-tarefas.md`)

## Etapa 2 — Configuração Base, Identidade Visual e DI
- [x] Configurar dependências no Gradle (`libs.versions.toml` e `app/build.gradle.kts`): Koin, Navigation Compose, Coil, Room, Retrofit, Moshi
- [x] Configurar `AndroidManifest.xml` (permissões `INTERNET`, `ACCESS_NETWORK_STATE`, `PatchPulseApplication`)
- [x] Criar identidade visual: ícone adaptativo, fontes customizadas locais, paleta Obsidian/Neon Telemetry e banners
- [x] Estruturar camada de Tema M3 (`Color.kt`, `Type.kt`, `Theme.kt`)

## Etapa 3 — Camada Domain (Regras de Negócio Puras)
- [x] Modelar entidades de domínio (`GameRank`, `GameDetail`, `PatchNote`, `ProPlayer`, `PlayerStats`, `PlayerMatch`, `AppError`, `SyncOutcome`)
- [x] Definir contratos de repositório (`GameRepository`, `PatchRepository`, `PlayerRepository`)
- [x] Implementar casos de uso (`GetTrendingGamesUseCase`, `RefreshTrendingGamesUseCase`, `GetGameDetailUseCase`, `GetPatchNotesUseCase`, `RefreshPatchNotesUseCase`, `ToggleFavoriteGameUseCase`, `GetProPlayersUseCase`, `GetPlayerStatsUseCase`, `FormatRankTierUseCase`, `CalculatePlayerMetricsUseCase`)

## Etapa 4 — Camada Data (Room + Retrofit + Offline-First)
- [x] Criar entidades Room, DAOs com `Flow<T>` e `PatchPulseDatabase`
- [x] Implementar interfaces Retrofit (`SteamWebApi`, `SteamStoreApi`, `OpenDotaApi`) e DTOs Moshi
- [x] Criar mappers, catálogo base de títulos Steam e sanitizador de texto de patch notes
- [x] Implementar `NetworkConnectivityObserver` e `safeApiCall`
- [x] Implementar repositórios Offline-First (`GameRepositoryImpl`, `PatchRepositoryImpl`, `PlayerRepositoryImpl`)
- [x] Configurar módulos de Injeção de Dependências com Koin (`AppModules.kt`)

## Etapa 5 — Camada Presentation (Jetpack Compose + Navigation)
- [x] Implementar sistema de navegação adaptativo (`PatchPulseNavGraph` com `NavigationBar` e `NavigationRail`)
- [x] Desenvolver componentes compartilhados (banners de status offline, cartões de telemetria, estados Loading/Empty/Error)
- [x] Implementar tela **Tendências** (`TrendsScreen` + `TrendsViewModel`)
- [x] Implementar tela **Patches & Modificações** (`PatchesScreen` + `PatchesViewModel`)
- [x] Implementar tela **Jogadores & Ranks** (`PlayersScreen` + `PlayersViewModel`)
- [x] Implementar tela **Salvos (Offline)** (`FavoritesScreen` + `FavoritesViewModel`)
- [x] Implementar telas de detalhes **GameDetailScreen** e **PlayerDetailScreen**

## Etapa 6 — Validação, Testes Automatizados e Documentação Final
- [x] Implementar testes unitários para UseCases e regras de cálculo de Rank/WinRate/KDA
- [x] Implementar testes unitários para comportamento Offline-First do Repository
- [x] Implementar testes de ViewModel com `kotlinx-coroutines-test`
- [x] Implementar teste de integração Room com Robolectric
- [x] Compilar APK, executar suíte completa de testes (`gradle :app:testDebugUnitTest`) e registrar evidências reais no `README.md`
