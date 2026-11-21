package io.github.worldsmiths.prismatrix.puzzle;

/** Fail-closed level load failure (missing file, malformed JSON, or invalid fields). */
public final class LevelLoadException extends Exception {

    public LevelLoadException(String message) {
        super(message);
    }

    public LevelLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
