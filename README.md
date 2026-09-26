# Codenames Clone

Clone multijoueur du jeu **Codenames** : parties en temps réel, plateau partagé, rôles Espion / Agent, et synchronisation via WebSocket.

Monorepo full-stack — backend Java / Spring Boot, frontend Vue 3, PostgreSQL, orchestré avec Docker Compose.

## Features

- Création et rejoindre une salle par ID
- Attribution d’équipes (Rouge / Bleu) et de rôles (Espion / Agent)
- Plateau de 25 mots, indices, sélection / révélation de cartes
- Conditions de victoire (mots d’équipe ou carte Assassin)
- Mises à jour live via STOMP / SockJS
- Interface responsive (desktop & mobile)

## Stack

| Couche | Technologies |
| --- | --- |
| Frontend | Vue 3, TypeScript, Vite, Pinia, Vue Router, Tailwind CSS 4, Vitest, Playwright |
| Backend | Java 21, Spring Boot 4, Spring Web MVC, Spring Data JPA, Spring WebSocket |
| Données | PostgreSQL |
| Infra | Docker, Docker Compose, Nginx (prod) |

## Structure

```text
codenames-clone/
├── backend/                 # Spring Boot (hexagonal)
│   ├── src/main/java/.../domain/
│   ├── src/main/java/.../application/
│   ├── src/main/java/.../adapter/
│   └── src/test/java/
├── frontend/                # Vue 3 + Vite
│   ├── src/domain/
│   ├── src/application/
│   ├── src/infrastructure/
│   ├── src/components/ | views/ | stores/
│   └── e2e/
├── docker-compose-dev.yml
├── docker-compose-prod.yml
├── start-dev.sh
└── start-prod.sh
```

## Prérequis

- [Docker](https://docs.docker.com/get-docker/) & Docker Compose
- (Optionnel, hors Docker) Node.js 22+, JDK 21, Maven Wrapper inclus

## Démarrage

### 1. Variables d’environnement

À la racine et dans chaque service, partir des fichiers d’exemple :

```bash
cp .env.example .env
cp backend/.env.development.example backend/.env.development
cp frontend/.env.development.example frontend/.env.development
```

Exemple minimal :

```env
# .env (Postgres / pgAdmin)
POSTGRES_DB=codenames
POSTGRES_USER=codenames
POSTGRES_PASSWORD=changeme
PGADMIN_DEFAULT_EMAIL=admin@example.com
PGADMIN_DEFAULT_PASSWORD=changeme
```

```env
# frontend/.env.development
VITE_API_URL_DEV=http://localhost:8080/
VITE_WEBSOCKET_URL_DEV=http://localhost:8080/ws
CHOKIDAR_USEPOLLING=true
```

```env
# backend/.env.development
POSTGRES_DB=codenames
POSTGRES_USER=codenames
POSTGRES_PASSWORD=changeme
CHOKIDAR_USEPOLLING=true
```

### 2. Lancer la stack (développement)

```bash
bash start-dev.sh
# équivalent :
# docker compose -p codenames -f docker-compose-dev.yml up -d --build
```

| Service | URL |
| --- | --- |
| Frontend (Vite) | http://localhost:5173 |
| API / WebSocket | http://localhost:8080 |
| pgAdmin | http://localhost:8888 |
| PostgreSQL | `localhost:5432` |

Arrêt :

```bash
docker compose -p codenames -f docker-compose-dev.yml down
```

### Production

```bash
cp backend/.env.production.example backend/.env.production
cp frontend/.env.production.example frontend/.env.production
# renseigner les valeurs, puis :
bash start-prod.sh
```

Services exposés notamment sur le port **80** (Nginx / frontend prod) et **8080** (API). Adapter les URLs Vite (`VITE_API_URL_PROD`, `VITE_WEBSOCKET_URL_PROD`) à votre domaine.

### Développement local (sans Docker pour le code)

Utile pour itérer plus vite une fois Postgres disponible (via Compose ou local).

Backend :

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Frontend :

```bash
cd frontend
npm install
npm run dev
```

## Scripts

### Frontend (`frontend/`)

| Commande | Description |
| --- | --- |
| `npm run dev` | Serveur de développement Vite |
| `npm run build` | Type-check + build de production |
| `npm run type-check` | Vérification TypeScript (`vue-tsc`) |
| `npm run lint` | ESLint |
| `npm run format` | Prettier |
| `npm run test:unit` | Tests unitaires Vitest |
| `npm run test:e2e` | Tests E2E Playwright |

### Backend (`backend/`)

| Commande | Description |
| --- | --- |
| `./mvnw spring-boot:run` | Démarrer l’API |
| `./mvnw test` | Tests unitaires / WebMvc / contexte |
| `./mvnw -DskipTests package` | JAR `target/app.jar` |

## Architecture

Architecture **hexagonale légère** :

- **Backend** — `domain` (règles pures) → `application` (use cases + ports) → `adapter` (HTTP, JPA, WebSocket, liste de mots)
- **Frontend** — `domain` / `application` / `infrastructure` / UI (composants & stores)

### API (aperçu)

Base : `http://localhost:8080/room`

| Méthode | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/room/create` | Créer une salle (`{ "username": "…" }`) |
| `GET` | `/room/{id}` | État de la salle |
| `DELETE` | `/room/{id}` | Supprimer la salle |
| `PUT` | `/room/{id}` | Action de jeu |

Actions `PUT` (champ `action`) : `join`, `leave`, `start`, `shuffle-players`, `reset-players`, `change-host`, `select-team`, `select-role`, `change-username`, `manual-team-turn`, `select-word`, `click-word`, `add-clue`, `replay`.

Topic WebSocket (STOMP) : `/topic/room/{id}` — endpoint SockJS : `/ws`.

## Licence

MIT License — see [LICENSE](./LICENSE).

Codenames est une marque de Czech Games Edition ; ce dépôt est un clone non officiel à des fins d’apprentissage.
