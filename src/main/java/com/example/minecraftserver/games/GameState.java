package com.example.minecraftserver.games;

public enum GameState {
    WAITING,    // Waiting for players or for the game to start
    STARTING,   // Countdown or initial setup phase
    ACTIVE,     // Game in progress
    ENDING,     // Game finishing up, showing scores, etc.
    INACTIVE    // Game is not running or available
}
