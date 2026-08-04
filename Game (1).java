package game;

import game.gamestate.GameState;
import game.strategies.GameStrategy;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Observable;

public class Game extends Observable {
    private static Game currentGame;
    public static Game getCurrentGame() {
        return currentGame;
    }
    public static final String GAME_STOPPED = "game.Game.GAME_STOPPED";
    public static final String SCORE_CHANGED = "game.Game.SCORE_CHANGED";

    private GameState state;
    private int score;
    private int highScore;
    private GameStrategy strategy;


    public Game(GameStrategy strategy) {
        this.strategy = strategy;
        this.score = 0;
        String fileName = strategy.toString() + "_high_score";
        try {
            DataInputStream inputStream = new DataInputStream(new FileInputStream(fileName));
            highScore = inputStream.readInt();
            inputStream.close();
        } catch (IOException e) {
            highScore = 0;
        }
        currentGame = this;
        strategy.initialize();
    }

    public void addScore(int toAdd) {
        score += toAdd;
        if(score > highScore)
            highScore = score;
        setChanged();
        notifyObservers(SCORE_CHANGED);
    }
    public void stopGame() {
        setChanged();
        notifyObservers(GAME_STOPPED);
        deleteObservers();
    }

    public GameStrategy getStrategy() {
        return strategy;
    }
    public GameState getState() {
        return state;
    }
    public void setState(GameState state) {
        this.state = state;
    }
    public int getScore() {
        return score;
    }
    public int getHighScore() {
        return highScore;
    }
}
