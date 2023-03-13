/*
 * Copyright (C) 2023 Sebastian Krieter, Elias Kuiter
 *
 * This file is part of FeatJAR-base.
 *
 * base is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3.0 of the License,
 * or (at your option) any later version.
 *
 * base is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with base. If not, see <https://www.gnu.org/licenses/>.
 *
 * See <https://github.com/FeatureIDE/FeatJAR-base> for further information.
 */
package de.featjar.base.cli;

import de.featjar.base.FeatJAR;
import de.featjar.base.data.Result;
import de.featjar.base.extension.AExtensionPoint;
import de.featjar.base.extension.IExtension;
import de.featjar.base.io.IO;
import de.featjar.base.io.format.IFormat;
import de.featjar.base.io.format.IFormatSupplier;
import de.featjar.base.log.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helpers for running commands.
 *
 * @author Sebastian Krieter
 * @author Elias Kuiter
 */
public class Commands extends AExtensionPoint<ICommand> {
    /**
     * The default verbosity on startup, if not specified elsewhere.
     */
    public static final Log.Verbosity DEFAULT_VERBOSITY = Log.Verbosity.INFO;

    /**
     * A magic string that identifies the standard input stream.
     */
    public static final String STANDARD_INPUT = "<stdin>";

    /**
     * A magic string that identifies the standard output stream.
     */
    public static final String STANDARD_OUTPUT = "<stdout>";

    /**
     * A magic string that identifies the standard error stream.
     */
    public static final String STANDARD_ERROR = "<stderr>";

    /**
     * A pattern that matches the standard input identifier, accepting an optional file extension.
     */
    public static final Pattern STANDARD_INPUT_PATTERN = Pattern.compile("<stdin>(\\.(.+))?");

    /**
     * Runs the command supplied in the given option input.
     *
     * @param optionInput the option input
     */
    public static void run(IOptionInput optionInput) {
        LinkedHashSet<ICommand> matchingCommands = optionInput.getCommands();
        if (optionInput.isHelp() || matchingCommands.isEmpty()) {
            System.out.println(optionInput.getHelp());
        } else if (optionInput.isVersion()) {
            System.out.println(FeatJAR.LIBRARY_NAME + ", development version");
        } else {
            FeatJAR.log()
                    .info("running matching commands: "
                            + matchingCommands.stream()
                            .map(IExtension::getIdentifier)
                            .collect(Collectors.joining(", ")));
            matchingCommands.forEach(command -> {
                FeatJAR.log().problem(optionInput.validate(Stream.concat(
                                optionInput.getOptions().stream(),
                                command.getOptions().stream())
                        .collect(Collectors.toList())).getProblems());
                command.run(optionInput);
            });
        }
    }

    /**
     * Runs a given function in a new thread, aborting it when it is not done after a timeout expires.
     *
     * @param fn      the function
     * @param timeout the timeout in milliseconds
     * @param <T>     the type of the result
     * @return the result of the function, if any
     */
    public static <T> Result<T> runInThread(Callable<T> fn, Duration timeout) {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<T> future = executor.submit(fn);
        try {
            return Result.of(timeout == null ? future.get() : future.get(timeout.toMillis(), TimeUnit.MILLISECONDS));
        } catch (final TimeoutException | ExecutionException | InterruptedException e) {
            return Result.empty(e);
        } finally {
            executor.shutdownNow();
        }
    }


    /**
     * {@return an object loaded from the given path or the standard input stream}
     *
     * @param pathOrStdin    the path or reference to the standard input stream
     * @param formatSupplier the format supplier
     * @param <T>            the type of the result
     */
    public static <T> Result<T> loadFile(String pathOrStdin, IFormatSupplier<T> formatSupplier) {
        Matcher matcher = STANDARD_INPUT_PATTERN.matcher(pathOrStdin.toLowerCase());
        if (matcher.matches()) {
            Path path = Paths.get(matcher.group(2) != null ? "stdin." + matcher.group(2) : "stdin");
            String content = new BufferedReader(new InputStreamReader(System.in, IO.DEFAULT_CHARSET))
                    .lines()
                    .collect(Collectors.joining("\n"));
            return IO.load(content, path, formatSupplier);
        } else {
            return IO.load(Paths.get(pathOrStdin), formatSupplier);
        }
    }

    /**
     * {@return whether the given path or reference to the standard input stream is valid}
     *
     * @param pathOrStdin the path or reference to the standard input stream
     */
    public static boolean isValidInput(String pathOrStdin) {
        return STANDARD_INPUT_PATTERN.matcher(pathOrStdin.toLowerCase()).matches()
                || Files.exists(Paths.get(pathOrStdin));
    }

    //todo: allow to load many files

    /**
     * Saves the given object to the given path or the standard output stream.
     *
     * @param object       the object
     * @param pathOrStdout the path or reference to the standard output stream
     * @param format       the format
     * @param <T>          the type of the object
     */
    public static <T> void saveFile(T object, String pathOrStdout, IFormat<T> format) {
        try {
            if (pathOrStdout.equalsIgnoreCase(STANDARD_OUTPUT)) {
                IO.save(object, System.out, format);
            } else {
                IO.save(object, Paths.get(pathOrStdout), format);
            }
        } catch (final IOException e) {
            FeatJAR.log().error(e);
        }
    }
}
