# 01 — Especificação do Projeto (Spec-Driven Development)

## 1. Visão Geral

**SeuJogo** é uma aplicação Android nativa desenvolvida em Kotlin e Jetpack Compose voltada para jogadores, analistas e entusiastas de e-sports que desejam acompanhar, em tempo real e em um único lugar:

- **Rankings e Tendências de Jogos:** Jogos mais jogados no momento com variação semanal de posição no ranking, pico de jogadores simultâneos e contagem ao vivo.
- **Patches, Modificações e Atualizações:** Notas de atualização oficiais (*patch notes*), balanceamentos, *hotfixes* e comunicados dos desenvolvedores diretamente das fontes oficiais.
- **Estatísticas e Ranks de Jogadores:** Consulta em tempo real de perfis competitivos, ranking profissional (Pro Leaderboard), medalha de rank (*Rank Tier*), taxa de vitória (*Win Rate*), média K/D/A e histórico de partidas recentes.
- **Experiência Offline-First:** Todo o conteúdo sincronizado permanece disponível localmente via banco de dados **Room**, permitindo consulta de jogos salvos, tendências em cache, patches lidos e jogadores consultados mesmo sem conexão com a internet.

---

## 2. Problema que a Aplicação Resolve

Informações sobre o ecossistema de jogos competitivos e atualizações costumam estar fragmentadas em múltiplos sites, fóruns e clientes de desktop, além de perderem disponibilidade quando o usuário está offline ou em redes móveis instáveis. O **SeuJogo** centraliza dados oficiais em tempo real com cache local estruturado, garantindo leitura rápida, confiável e resiliente a falhas de rede.

---

## 3. Fontes de Dados Oficiais (APIs REST)

1. **Valve Steam Web API (Oficial e Pública)**
   - `ISteamChartsService/GetMostPlayedGames/v1/`: Ranking oficial em tempo real dos jogos mais jogados, pico de jogadores e posição na semana anterior.
   - `ISteamUserStats/GetNumberOfCurrentPlayers/v1/`: Contagem ao vivo de jogadores simultâneos por `appid`.
   - `ISteamNews/GetNewsForApp/v2/`: Feed oficial de patch notes, atualizações e comunicados por jogo.
2. **Valve Steam Store API (Oficial)**
   - `store.steampowered.com/api/appdetails`: Metadados oficiais localizados em Português (`l=brazilian`), desenvolvedora, gêneros, descrição e nota Metacritic.
3. **OpenDota Public API (Oficial para Dados Competitivos Dota 2 / Steam)**
   - `api.opendota.com/api/proPlayers`: Ranking e diretório de jogadores profissionais.
   - `api.opendota.com/api/players/{account_id}`, `/wl`, `/recentMatches`: Estatísticas reais de perfil, medalha de rank (*Rank Tier*), posição no leaderboard global, vitórias/derrotas e partidas recentes.

---

## 4. Requisitos Funcionais (RF)

| ID | Requisito | Critério de Aceite |
|---|---|---|
| **RF01** | **Ranking de Tendências ao Vivo** | Exibir lista ordenada dos jogos mais jogados com posição atual, delta de tendência semanal (`+N`, `-N`, `=`), pico simultâneo e capa oficial. |
| **RF02** | **Busca e Filtragem de Jogos** | Permitir busca instantânea por nome/gênero e filtros rápidos (*Todos*, *Em Alta*, *Favoritos*). |
| **RF03** | **Detalhes e Telemetria em Tempo Real** | Ao selecionar um jogo, carregar contagem de jogadores online agora, detalhes oficiais da loja e patches recentes daquele título. |
| **RF04** | **Feed de Patches e Modificações** | Listar atualizações recentes dos principais jogos, permitindo filtrar por jogo específico e alternar o filtro "Somente Patches" (balanceamentos/changelogs). |
| **RF05** | **Leitura de Patch Notes** | Exibir o conteúdo sanitizado do patch em visualização expandida e oferecer abertura do link oficial no navegador. |
| **RF06** | **Ranking e Estatísticas de Jogadores** | Exibir jogadores profissionais em destaque e permitir busca por nome/time ou consulta direta por **Account ID** (Steam32/Dota 2) exibindo Rank Tier, Win Rate, K/D/A e partidas recentes. |
| **RF07** | **Jogos Favoritos / Fixados Offline** | Permitir fixar jogos favoritos localmente no Room com aba dedicada de acesso rápido offline. |
| **RF08** | **Sincronização Manual e Automática** | Exibir status de conectividade (Online / Modo Offline), horário da última sincronização e botão de atualização manual com feedback visual. |

---

## 5. Requisitos Não Funcionais (RNF)

| ID | Requisito | Estratégia |
|---|---|---|
| **RNF01** | **Arquitetura Limpa** | Separação estrita em `domain`, `data` e `presentation`, respeitando a regra de dependência. |
| **RNF02** | **Injeção de Dependências** | Gerenciamento completo do grafo de dependências utilizando **Koin**. |
| **RNF03** | **Programação Assíncrona** | Uso exclusivo de `Kotlin Coroutines`, `Flow` e `StateFlow`, sem bloqueio da Main Thread. |
| **RNF04** | **Persistência Offline-First** | Banco local **Room** atuando como *Single Source of Truth (SSOT)* com política de expiração (TTL) e fallback transparente. |
| **RNF05** | **Estados de UI Completos** | Todas as telas devem tratar explicitamente: Carregamento, Sucesso, Ausência de Dados (Empty) e Erro com retentativa. |
| **RNF06** | **Prevenção de Memory Leaks e Crashes** | Cancelamento automático via `viewModelScope`, liberação de `NetworkCallback` em `awaitClose`, `collectAsStateWithLifecycle` e tratamento tipado de exceções (`AppError`). |
| **RNF07** | **Testabilidade e Qualidade** | Testes unitários cobrindo casos de uso, regras de negócio, repositório Offline-First, ViewModel e persistência Room com Robolectric. |
