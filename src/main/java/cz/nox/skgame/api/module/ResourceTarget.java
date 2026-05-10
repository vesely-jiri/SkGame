package cz.nox.skgame.api.module;

import java.io.File;

/**
 * Describes where a bundled resource should be installed on disk.
 *
 * @param directory    Parent directory (created if absent).
 * @param relativePath Path of the file within {@code directory}, preserving any subdirectories.
 */
public record ResourceTarget(File directory, String relativePath) {}
