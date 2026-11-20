# SeuJogo — Radar de Jogos, Patches e Stats em Tempo Real

**SeuJogo** é uma aplicação Android nativa construída em **Kotlin** e **Jetpack Compose**, desenvolvida seguindo a metodologia **Spec-Driven Development (SDD)**, **Clean Architecture** e persistência **Offline-First** com Room e Retrofit.

---

## 1. Objetivo e Problema Resolvido

Jogadores competitivos e entusiastas de e-sports frequentemente precisam alternar entre diferentes sites, clientes de desktop e fóruns para descobrir quais jogos estão em alta, quantos jogadores estão ativos no momento, o que mudou no último patch de balanceamento e qual o desempenho recente de jogadores profissionais ou de suas próprias contas. Além disso, essas informações deixam de estar acessíveis em redes móveis instáveis ou sem internet.

O **SeuJogo** resolve esse problema unificando, em uma única interface moderna e responsiva, dados em tempo real obtidos diretamente de **APIs oficiais de jogos**, mantendo um cache estruturado localmente para consulta imediata mesmo em modo offline.

---

## 2. Funcionalidades Implementadas

- **Tendências e Ranking Oficial de Jogos (`Tendências`)**
  - Consumo em tempo real do ranking oficial de jogos mais jogados da Steam (`GetMostPlayedGames`), exibindo posição atual, pico simultâneo, jogadores online agora (`GetNumberOfCurrentPlayers`) e variação semanal (`▲ +N`, `▼ -N`, `● ESTÁVEL`, `★ NOVO`).
  - Busca instantânea por título/gênero e filtros rápidos (*Todos*, *Em Alta*, *Favoritos*).
- **Telemetria Detalhada do Jogo (`GameDetailScreen`)**
  - Detalhes oficiais localizados em Português (`appdetails`), desenvolvedora, gêneros, nota Metacritic, contagem ao vivo de jogadores e linha do tempo dedicada de patches do título.
- **Feed de Patches, Modificações e Hotfixes (`Patches`)**
  - Agregação em tempo real de notas de atualização oficiais (`GetNewsForApp`) dos principais jogos em destaque e favoritos.
  - Filtro por jogo, busca textual e alternância **Somente Patches** (separa changelogs/balanceamentos oficiais de notícias gerais).
  - Sanitizador próprio (`PatchContentSanitizer`) que limpa tags BBCode/HTML da Steam para leitura confortável em `ModalBottomSheet`, com botão para abrir a publicação oficial no navegador.
- **Ranks e Estatísticas de Jogadores (`Jogadores` & `PlayerDetailScreen`)**
  - Leaderboard de jogadores profissionais em atividade via **OpenDota Official API**.
  - Atalhos rápidos de perfis famosos (Yatoro, Miracle-, Arteezy, Topson, Dendi) e consulta direta por qualquer **Steam32 / Dota 2 Account ID**.
  - Exibição de medalha de rank decodificada (`Immortal #N`, `Divine 5`, `Ancient 3`, etc.), barra visual de **Win Rate**, contagem de vitórias/derrotas, média **K/D/A** e histórico das partidas recentes.
- **Hub Offline (`Salvos`)**
  - Permite fixar jogos favoritos localmente no Room com acesso imediato aos dados e patches salvos em cache mesmo sem conexão.
- **Interface Adaptativa e Estados Completos**
  - Suporte a telas compactas (`NavigationBar`) e tablets/dobráveis (`NavigationRail`), `enableEdgeToEdge()`, e tratamento explícito dos estados de **Carregamento**, **Sucesso**, **Ausência de Dados (Empty)**, **Erro com Retentativa** e **Indicador Online/Offline**.

---

## 3. Tecnologias e Bibliotecas Utilizadas

| Categoria | Tecnologia / Biblioteca |
|---|---|
| Linguagem & Assincronismo | Kotlin 2.4, Kotlin Coroutines, Kotlin Flow / StateFlow |
| Interface & Navegação | Jetpack Compose (Material Design 3), Navigation Compose, Coil Compose |
| Arquitetura | Clean Architecture (Presentation, Domain, Data) + MVVM |
| Injeção de Dependências | Koin 4.0.4 (`koin-android`, `koin-androidx-compose`) |
| Rede & Serialização | Retrofit 2.12, OkHttp 4, Moshi + Moshi KSP Codegen |
| Persistência Local | Room 2.7 (KSP, `Flow`, `withTransaction`) |
| Tipografia | Fontes Google Fonts empacotadas localmente (`Space Grotesk`, `Plus Jakarta Sans`, `JetBrains Mono`) |
| Testes Automatizados | JUnit 4, `kotlinx-coroutines-test`, Robolectric 4.16 |

---

## 4. APIs REST Oficiais Utilizadas e Justificativa

Foram escolhidas **duas APIs oficiais e públicas de jogos** que fornecem telemetria real em tempo real sem exigir chaves proprietárias restritas, permitindo execução imediata do aplicativo por qualquer avaliador:

1. **Valve Steam Web API & Steam Store API (Oficial)**
   - **Endereços base:** `https://api.steampowered.com/` e `https://store.steampowered.com/`
   - **Documentação oficial:** [Steam Web API Documentation](https://developer.valvesoftware.com/wiki/Steam_Web_API)
   - **Endpoints consumidos:**
     - `GET ISteamChartsService/GetMostPlayedGames/v1/` — Ranking oficial em tempo real, variação semanal e pico de jogadores.
     - `GET ISteamUserStats/GetNumberOfCurrentPlayers/v1/?appid={id}` — Contagem instantânea de jogadores simultâneos.
     - `GET ISteamNews/GetNewsForApp/v2/?appid={id}&count=12&format=json` — Notas de patch, modificações e comunicados oficiais.
     - `GET api/appdetails?appids={id}&l=brazilian` — Metadados oficiais localizados, gêneros e Metacritic.
   - **Justificativa:** É a principal fonte oficial de telemetria de PC gaming no mundo, entregando rankings, picos simultâneos e notas de patch reais diretamente dos servidores da Valve.

2. **OpenDota Official Public API**
   - **Endereço base:** `https://api.opendota.com/api/`
   - **Documentação oficial:** [OpenDota API Docs](https://docs.opendota.com/)
   - **Endpoints consumidos:**
     - `GET proPlayers` — Diretório de jogadores profissionais e equipes ativas.
     - `GET players/{account_id}`, `/wl` e `/recentMatches` — Rank Tier, posição no leaderboard, vitórias/derrotas e histórico de partidas com K/D/A.
   - **Justificativa:** Oferece estatísticas reais, abertas e atualizadas de jogadores competitivos e partidas oficiais sem necessidade de credenciais fechadas.

---

## 5. Arquitetura e Separação das Camadas

O projeto aplica **Clean Architecture** com respeito estrito à regra de dependência (`Presentation -> Domain <- Data`):

- **`domain/` (Kotlin puro — sem dependências de Android/Room/Retrofit):**
  - `model/`: `GameRank`, `GameDetail`, `PatchNote`, `ProPlayer`, `PlayerStats`, `PlayerMatch`, `AppError`, `SyncOutcome`.
  - `repository/`: Contratos `GameRepository`, `PatchRepository`, `PlayerRepository`.
  - `usecase/`: `GetTrendingGamesUseCase`, `RefreshTrendingGamesUseCase`, `GetGameDetailUseCase`, `ToggleFavoriteGameUseCase`, `GetPatchNotesUseCase`, `RefreshPatchNotesUseCase`, `GetProPlayersUseCase`, `GetPlayerStatsUseCase`, `FormatRankTierUseCase`, `CalculatePlayerMetricsUseCase`.
- **`data/` (Implementação de rede, cache local e conectividade):**
  - `remote/`: Interfaces Retrofit (`SteamWebApi`, `SteamStoreApi`, `OpenDotaApi`) e DTOs Moshi com `@JsonClass(generateAdapter = true)`.
  - `local/`: `PatchPulseDatabase` (Room), 6 entidades (`game_ranks`, `patch_notes`, `pro_players`, `player_stats`, `player_matches`, `sync_status`) e DAOs com queries reativas `Flow<T>`.
  - `mapper/`: Conversores DTO → Entity → Domain e `PatchContentSanitizer`.
  - `network/`: `NetworkConnectivityObserver` (`callbackFlow` com remoção segura de callback em `awaitClose`) e `safeApiCall`.
  - `repository/`: `GameRepositoryImpl`, `PatchRepositoryImpl` e `PlayerRepositoryImpl`.
- **`presentation/` (Jetpack Compose + StateFlow):**
  - ViewModels com `StateFlow` imutável (`@Immutable`) coletados com `collectAsStateWithLifecycle()`.
  - Navegação declarativa com `Navigation Compose` (`PatchPulseNavGraph`).
- **`di/` (Koin):**
  - `AppModules.kt` modularizado em `networkModule`, `databaseModule`, `repositoryModule`, `useCaseModule` e `viewModelModule`.

A documentação completa das etapas de **Spec-Driven Development** encontra-se na pasta `docs/`:
- [`docs/01-especificacao.md`](docs/01-especificacao.md)
- [`docs/02-arquitetura-planejamento.md`](docs/02-arquitetura-planejamento.md)
- [`docs/03-decomposicao-tarefas.md`](docs/03-decomposicao-tarefas.md)

---

## 6. Estratégia de Funcionamento Offline-First

1. **Single Source of Truth (SSOT):** Todas as telas observam exclusivamente `Flow` vindo do banco local **Room**.
2. **Política de Sincronização por TTL:** A tabela `sync_status` controla o instante da última sincronização (TTL de 5 minutos para Tendências e Patches; 15 minutos para Pro Players). A atualização ocorre em `Dispatchers.IO` sem bloquear a UI, podendo também ser forçada pelo usuário a qualquer momento.
3. **Merge Seguro de Favoritos:** Durante sincronizações de ranking, a transação Room (`withTransaction`) preserva os jogos marcados como favoritos (`isFavorite = true`), evitando perda de estado local.
4. **Fallback Gracioso:** Em caso de ausência de conexão ou erro HTTP/timeout, os dados previamente salvos no Room continuam navegáveis normalmente com o banner `MODO OFFLINE • CACHE LOCAL`. Somente quando não há cache disponível a tela exibe o estado de erro tipado com botão de retentativa.

---

## 7. Configuração e Execução no Android Studio (Projeto Exportado)

### Pré-requisitos
- Android Studio Narwhal / Ladybug ou superior com **JDK 17 ou 21** (embutido no Android Studio).
- Android SDK 36 (compatível com dispositivos a partir do Android 7.0 / API 24).
- **Zero chaves de API obrigatórias:** todas as APIs oficiais consumidas (Steam Web API, Steam Store API e OpenDota Public API) são públicas e funcionam em tempo real imediatamente após abrir o app.

### Passos para Executar após Extrair o ZIP
1. Extraia o ZIP do projeto e abra a pasta raiz no **Android Studio** (*File → Open*).
2. Aguarde a sincronização automática do **Gradle** (o `gradle-wrapper.properties` baixa a versão compatível automaticamente).
3. Caso o arquivo `debug.keystore` não acompanhe o ZIP exportado fora do ambiente em nuvem, gere-o em 1 segundo na raiz do projeto antes do primeiro *Run*:
   ```bash
   keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
   ```
4. Selecione um emulador (com acesso à internet) ou dispositivo físico Android e clique em **Run (`app`)**.

---

## 8. Execução dos Testes Unitários e Evidências

### Comando para executar toda a suíte de testes
```bash
gradle :app:testDebugUnitTest
```
*(No Android Studio local, também é possível executar via `./gradlew :app:testDebugUnitTest` ou clicando com o botão direito em `app/src/test` → **Run Tests**.)*

### Evidências dos Testes Executados
Execução validada com sucesso (`BUILD SUCCESSFUL`) contemplando **19 casos de teste** distribuídos em **6 classes de teste**:

| Classe de Teste | Escopo Validado | Resultado |
|---|---|---|
| `ApiDeserializationContractTest` (4 testes) | Contrato real de desserialização Moshi para payloads oficiais da Steam (`GetMostPlayedGames`, `GetNumberOfCurrentPlayers`, `GetNewsForApp`, `appdetails`) e OpenDota (`proPlayers`, `players`, `wl`, `recentMatches` com campos nulos) | **PASSOU** |
| `GameUseCasesTest` (4 testes) | Cálculo de variação de ranking (`RISING`, `FALLING`, `STEADY`, `NEW`), filtros *Em Alta*/*Favoritos*, busca textual e propagação de erro offline | **PASSOU** |
| `PatchAndPlayerUseCasesTest` (4 testes) | Decodificação de medalhas (`Immortal #14`, `Divine 5`, `Ancient 3`, não calibrado), cálculo de Win Rate & K/D/A (incluindo divisão por 0 mortes), sanitização de BBCode/HTML e filtro de patches oficiais | **PASSOU** |
| `TrendsViewModelTest` (2 testes) | Transições de `StateFlow` com `kotlinx-coroutines-test`: preservação do cache local ao falhar offline e emissão de `AppError.NetworkUnavailable` quando o cache está vazio | **PASSOU** |
| `OfflineFirstGameRepositoryTest` (2 testes Robolectric) | Integração real Room em memória + Fake Steam API: persistência de rankings, preservação de favoritos após nova sincronização e integridade do cache em queda de rede | **PASSOU** |
| `ExampleRobolectricTest` + `ExampleUnitTest` (3 testes) | Validação do recurso `app_name` (`SeuJogo`), persistência Room de jogos/patches favoritos e sanidade unitária | **PASSOU** |

Resumo da saída do Gradle:
```text
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL
```

---

## 9. Limitações Conhecidas e Evoluções Futuras

- **Limites de taxa (*rate limit*) de APIs públicas:** A API pública do OpenDota possui limite de requisições por minuto sem chave; o cache local Room com TTL mitiga esse limite no uso normal.
- **Estatísticas de jogadores focadas em Steam/Dota 2:** A consulta detalhada de *Rank Tier* e partidas recentes utiliza o ecossistema público OpenDota (Steam32 Account ID), pois APIs de outros jogos (como Riot/Valorant) exigem chave privada por conta de desenvolvedor com aprovação manual.
- **Próximos passos planejados:** Sincronização em segundo plano periódica via `WorkManager`, notificações locais quando um jogo favorito receber novo patch oficial e histórico gráfico de variação de jogadores simultâneos.
