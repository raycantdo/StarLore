# 🌌 StarLore

### A Gamified Astronomy RPG Desktop Application

**StarLore** is a JavaFX-based desktop application that combines **astronomy, mythology, exploration, and interactive gaming** into one immersive experience.

The project is designed to make learning about constellations and their mythology more engaging by transforming educational content into interactive game modes, challenges, battles, and competitive experiences.


## ✨ Project Overview

StarLore takes players on a journey through the world of stars and constellations.

Instead of simply reading about astronomical objects, players can interact with them through different game modes. The application combines educational information with gameplay mechanics such as quizzes, battles, score systems, leaderboards, and multiplayer interaction.

### 🎯 Objectives

* Make astronomy learning more **interactive and enjoyable**
* Introduce players to **constellations and their mythology**
* Combine educational content with **game-based learning**
* Provide multiple interactive game experiences
* Implement player progression, scoring, and leaderboard functionality
* Develop a complete desktop application using Java and JavaFX


## 🚀 Features

### 🌌 Constellation Exploration

Players can explore different constellations and learn about their astronomical and mythological backgrounds.

The project includes constellation-related information for celestial figures such as:

* Aries
* Taurus
* Orion
* Perseus
* Cassiopeia
* Andromeda
* Ursa Major
* Gemini
* and others

---

### ⚔️ Mythological Battle

StarLore includes a story-based battle experience inspired by Greek mythology.

Players can take the role of **Perseus** and engage in battles against mythological enemies such as **Medusa**.

The battle system includes:

* Character movement
* Attacks
* Health and damage mechanics
* Enemy interaction
* Visual effects
* Battle animations
* Sound effects

---

### 🧠 Mythology Quiz

The Mythology Quiz allows players to test their knowledge through interactive questions related to astronomy and mythology.

Features include:

* Multiple-choice questions
* Score tracking
* Lifelines / special abilities
* Progression mechanics
* Interactive visual effects

---

### 🌠 Shooting Star

A fast-paced arcade-style game where players attempt to catch falling stars.

The game includes:

* Falling objects
* Player-controlled basket
* Normal and special stars
* Score system
* Combo mechanics
* Lives
* Levels
* Timer
* Bonus objects
* Sound effects
* Background music

---

### ⚡ Online Duel Mode

StarLore also includes an online multiplayer duel system.

Players can connect through a network and compete against another player in a constellation-based duel.

The project implements:

* Client-server communication
* Socket-based networking
* Online sessions
* Player interaction
* Multiplayer game logic

---

### 🏆 Leaderboard & Player Progress

The application includes a player management and scoring system.

Players can:

* Maintain player profiles
* Save scores
* Track game statistics
* View leaderboard rankings
* Store information using a database

---

## 🛠️ Technologies Used

| Technology       | Purpose                                            |
| ---------------- | -------------------------------------------------- |
| **Java 26**      | Core programming language                          |
| **JavaFX 26**    | Graphical user interface and application framework |
| **FXML**         | UI layout and scene design                         |
| **Maven**        | Dependency and project management                  |
| **MySQL**        | Player data, scores and database storage           |
| **JDBC**         | Database connectivity                              |
| **Java Sockets** | Multiplayer networking                             |
| **JUnit 5**      | Unit testing                                       |
| **Git & GitHub** | Version control and collaboration                  |

---

## 🏗️ Project Structure

```text
StarLore/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/starlore/starlore/
│   │   │       ├── BattleController.java
│   │   │       ├── DuelController.java
│   │   │       ├── DuelServer.java
│   │   │       ├── DuelSocketClient.java
│   │   │       ├── GameHubController.java
│   │   │       ├── MythQuizController.java
│   │   │       ├── ShootingStarController.java
│   │   │       ├── LeaderboardController.java
│   │   │       ├── DatabaseConnection.java
│   │   │       ├── PlayerDAO.java
│   │   │       ├── ScoreService.java
│   │   │       └── ...
│   │   │
│   │   └── resources/
│   │       └── com/starlore/starlore/
│   │           ├── BattleScreen.fxml
│   │           ├── DuelMode.fxml
│   │           ├── GameHubView.fxml
│   │           ├── MythQuizView.fxml
│   │           ├── ShootingStar.fxml
│   │           ├── Leaderboard.fxml
│   │           └── images/
│   │
│   └── test/
│       └── java/
│
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

---

## ⚙️ Getting Started

### Prerequisites

Before running StarLore, make sure you have:

* **JDK 26**
* **Maven** (or use the included Maven Wrapper)
* **MySQL Server**
* **IntelliJ IDEA** or another Java IDE

---

### 1. Clone the Repository

```bash
git clone https://github.com/raycantdo/StarLore.git
```

Then enter the project directory:

```bash
cd StarLore
```

---

### 2. Configure the Database

StarLore uses MySQL for storing player-related information and scores.

Update the database configuration in:

```text
src/main/java/com/starlore/starlore/DatabaseConnection.java
```

Configure your:

* Database URL
* Username
* Password


### 3. Run the Application

Using Maven:

```bash
mvn clean javafx:run
```

Or on Windows using the Maven Wrapper:

```bash
mvnw.cmd clean javafx:run
```

---

## 🎮 Application Flow

```text
                    ┌─────────────────┐
                    │    StarLore     │
                    │     Launch      │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │    Game Hub     │
                    └────────┬────────┘
                             │
          ┌──────────────────┼──────────────────┐
          │                  │                  │
          ▼                  ▼                  ▼
   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
   │ Constellation│    │ Mythology   │    │   Shooting  │
   │ Exploration │    │    Quiz     │    │     Star    │
   └─────────────┘    └─────────────┘    └─────────────┘
          │                  │                  │
          └──────────────────┼──────────────────┘
                             │
                    ┌────────▼────────┐
                    │     Battle /    │
                    │   Duel Modes    │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │ Score & Player  │
                    │    Progress     │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │   Leaderboard   │
                    └─────────────────┘
```

---

## 🧪 Testing

The project includes unit testing using **JUnit 5**.

Tests can be executed through Maven with:

```bash
mvn test
```

---

## 👥 Team Member        
Raida Hannan Raya 230041124
Tasnim Reza 230041126
Noorya Taj Ayshi 230041150

