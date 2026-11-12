import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

// User-defined checked exception
class InsufficientFundsException extends Exception {

    InsufficientFundsException(String message) {
        super(message);
    }
}

// Bank Account class
class BankAccount {

    private final long accountNumber;
    private int balance;

    BankAccount(long accountNumber, int balance) {
        this.accountNumber = accountNumber;
        this.balance = balance;
    }

    // Withdraw money
    public void withdraw(int amount)
            throws InsufficientFundsException {

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Withdrawal amount must be positive"
            );
        }

        if (amount > balance) {
            throw new InsufficientFundsException(
                    "Insufficient account balance"
            );
        }

        balance = balance - amount;

        System.out.println("Withdrawal successful");
        System.out.println("Withdrawn Amount: Rs. " + amount);
        System.out.println("Remaining Balance: Rs. " + balance);
    }
}

// Main ATM class
public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        BufferedReader cardReader = null;

        try {

            System.out.println("ATM WITHDRAWAL SYSTEM");

            System.out.print("Enter account number: ");

            long accountNumber =
                    Long.parseLong(sc.nextLine());

            if (accountNumber <= 0) {
                throw new IllegalArgumentException(
                        "Invalid account number"
                );
            }

            System.out.print("Enter withdrawal amount: ");

            int amount =
                    Integer.parseInt(sc.nextLine());

            if (amount <= 0) {
                throw new IllegalArgumentException(
                        "Withdrawal amount must be positive"
                );
            }

            // Read card-reader file
            cardReader = Files.newBufferedReader(
                    Path.of("card-reader.txt")
            );

            String status = cardReader.readLine();

            if (!"READY".equalsIgnoreCase(status)) {
                throw new IllegalStateException(
                        "Card reader is not ready"
                );
            }

            // Create bank account
            BankAccount account =
                    new BankAccount(accountNumber, 5000);

            // Perform withdrawal
            account.withdraw(amount);

        }

        catch (NumberFormatException e) {

            System.out.println(
                    "Invalid input: Please enter numeric values."
            );
        }

        catch (IllegalArgumentException e) {

            System.out.println(
                    "Invalid withdrawal: " + e.getMessage()
            );
        }

        catch (InsufficientFundsException e) {

            System.out.println(
                    "Transaction declined: " + e.getMessage()
            );
        }

        catch (IllegalStateException e) {

            System.out.println(
                    "ATM error: " + e.getMessage()
            );
        }

        catch (IOException | SecurityException e) {

            System.out.println(
                    "Card-reader error: " + e.getMessage()
            );
        }

        finally {

            // Close the card-reader resource
            if (cardReader != null) {

                try {
                    cardReader.close();

                } catch (IOException e) {

                    System.out.println(
                            "Error closing card-reader file"
                    );
                }
            }

            sc.close();

            System.out.println("ATM session closed.");
        }
    }
}
