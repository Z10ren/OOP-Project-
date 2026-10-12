package peerflow;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class ConsoleInput {
    private final Scanner scanner;
    private final boolean echoInput;

    public ConsoleInput(Scanner scanner) {
        this.scanner = scanner;
        this.echoInput = System.console() == null;
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            throw new NoSuchElementException("end of input");
        }
        String line = scanner.nextLine();
        if (echoInput) {
            System.out.println(line);
        }
        return line;
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            String text = readLine(prompt).trim();
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("  [INPUT ERROR] Choose a number from " + min + " to " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  [INPUT ERROR] \"" + text + "\" is not a whole number.");
            }
        }
    }

    public double readDouble(String prompt) {
        while (true) {
            String text = readLine(prompt).trim();
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException e) {
                System.out.println("  [INPUT ERROR] \"" + text + "\" is not a number.");
            }
        }
    }

    public LocalDate readDate(String prompt) {
        while (true) {
            String text = readLine(prompt).trim();
            try {
                return LocalDate.parse(text);
            } catch (DateTimeParseException e) {
                System.out.println("  [INPUT ERROR] \"" + text + "\" is not a valid date. Use YYYY-MM-DD.");
            }
        }
    }
}
