package com.nizar.atm;

import com.nizar.atm.service.ATMService;
import com.nizar.atm.service.impl.ATMServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Scanner;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static final ATMService atmService = new ATMServiceImpl();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        boolean running = true;
        while (running) {
            printMenu();
            String input = scanner.nextLine().trim();
            running = processCommand(input);
        }
        scanner.close();
    }

    private static void printMenu() {
        System.out.println("\nATM Menu:");
        System.out.println("1. register");
        System.out.println("2. login [name]");
        System.out.println("3. withdraw [amount]");
        System.out.println("4. transfer [account number] [amount]");
        System.out.println("5. logout");
        System.out.println("6. exit");
        System.out.print("> ");
    }

    private static boolean processCommand(String input) {
        String[] parts = input.split("\\s+");
        String command = parts[0].toLowerCase();

        try {
            switch (command) {
                case "register": {
                    System.out.print("Enter name: ");
                    String name = scanner.nextLine().trim();
                    System.out.print("Enter initial balance: ");
                    BigDecimal balance = new BigDecimal(scanner.nextLine().trim());

                    String result = atmService.register(name, balance);
                    System.out.println(result);
                    break;
                }

                case "login": {
                    if (parts.length < 2) {
                        System.out.println("Usage: login [name]");
                        break;
                    }
                    System.out.print("Enter PIN: ");
                    String pin = scanner.nextLine().trim();

                    String result = atmService.login(parts[1], pin);
                    System.out.println(result);
                    break;
                }

                case "withdraw": {
                    if (parts.length < 2) {
                        System.out.println("Usage: withdraw [amount]");
                        break;
                    }
                    BigDecimal amount = new BigDecimal(parts[1]);

                    String result = atmService.withdraw(amount);
                    System.out.println(result);
                    break;
                }

                case "transfer": {
                    if (parts.length < 3) {
                        System.out.println("Usage: transfer [account number] [amount]");
                        break;
                    }
                    BigInteger targetAccount = new BigInteger(parts[1]);
                    BigDecimal amount = new BigDecimal(parts[2]);

                    String result = atmService.transfer(targetAccount, amount);
                    System.out.println(result);
                    break;
                }

                case "logout": {
                    String result = atmService.logout();
                    System.out.println(result);
                    break;
                }

                case "exit":
                    System.out.println("Goodbye!");
                    return false;

                default:
                    System.out.println("Unknown command");
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid number format");
            logger.error("Number format error", e);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            logger.error("Command processing error", e);
        }

        return true;
    }
}
