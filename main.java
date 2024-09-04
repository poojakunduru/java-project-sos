import java.io.Console;
import java.sql.*;
import java.util.*;

class InsufficientBalanceException extends Exception {
    public String getMessage() {
        return "Insufficient balance";
    }
}

class main {
    public static void main(String args[]) throws Exception {
        try {
            Scanner sc = new Scanner(System.in);
            Connection conn = DriverManager.getConnection("jdbc:mySql://localhost:3306/sos", "root",
                    "shree@24poo@04ja");
            Statement st = conn.createStatement(1004, 1007);
            ResultSet rs = st.executeQuery("select * from sos");
            Statement st1 = conn.createStatement(1004, 1007);
            ResultSet rs1 = st1.executeQuery("select * from cardholders");
            int i = -1;
            int colval1 = 0;
            String colval2 = null;
            double colval3 = 0;
            while (rs.next()) {
                colval1 = rs.getInt("ID");
                colval2 = rs.getString("Dish_Name");
                colval3 = rs.getDouble("price");
                i++;
                if (i % 5 == 0) {
                    System.out.println();
                }
                System.out.println(colval1 + "     " + colval2 + " " + colval3 + "/-");
            }
            double bill = 0;

            Map<String, Double> orderedItems = new HashMap<>();
            loop: while (true) {
                System.out.println();
                System.out.print("Do you want to order(y/n) : ");
                String choice = sc.next();
                switch (choice) {
                    case "y":
                        System.out.print("enter the item id : ");
                        int id = sc.nextInt();
                        rs.absolute(id);
                        String menu = rs.getString("Dish_Name");
                        double price = rs.getDouble("price");
                        bill += price;
                        orderedItems.put(menu, price);
                        break;
                    case "n":
                        break loop;
                }
            }
            System.out.println("+-----------------------+-------+");
            for (Map.Entry<String, Double> entry : orderedItems.entrySet()) {
                String item = entry.getKey();
                double price = entry.getValue();
                System.out.printf("| %-21s | %5.2f |\n", item, price);
            }
            System.out.println("+-----------------------+-------+");
            System.out.println("Bill is : " + bill);
            System.out.println("GST(5%) : " + (bill * 0.05));
            double t = bill + (bill * 0.05);
            System.out.println("Total Bill including GST : " + t);
            System.out.println("Mode Of Payment(1/2)");
            System.out.println("1.Cash\n2.TapToPayCard");
            int c = sc.nextInt();
            if (c == 1) {
                System.out.print("Enter the amount you paid : ");
                double a = sc.nextDouble();
                if (a > t) {
                    double change = a - t;
                    System.out.println("Please accept your change : " + change);
                } else {
                    System.out.println("Please provide the required bill amount");
                }
            } else if (c == 2) {
                System.out.println();
                System.out.print("Are you a smart cardholder?(y/n) : ");
                String ch = sc.next();
                switch (ch) {
                    case "y":
                        System.out.println("Enter your account number : ");
                        int acc = sc.nextInt();
                        Console console = System.console();
                        System.out.println("Enter password");
                        char[] input = console.readPassword();
                        String passwd = new String(input);
                        java.util.Arrays.fill(input, ' ');
                        rs1.absolute(acc);
                        String dbpwd = rs1.getString("pwd");
                        if (dbpwd.equals(passwd)) {
                            double bal = rs1.getDouble("balance");
                            if (t < bal) {
                                double x = bal - t;
                                String sql = "UPDATE cardholders SET balance = " + x + " WHERE cardno = " + acc;
                                int rowsAffected = st1.executeUpdate(sql);
                                System.out.println("Rows affected: " + rowsAffected);
                                System.out.println("The bill amount of " + t
                                        + " is debited from card with card no " + acc);
                                System.out.println("Your current account balance : " + x);
                            } else {
                                throw new InsufficientBalanceException();
                            }
                        } else
                            System.out.print("Wrong password");
                        break;
                    case "n":
                        System.out.println("Please enter your details to get a new card");
                        System.out.print("Your name please : ");
                        String name = sc.next();
                        System.out.print("Please enter a password");
                        String password = sc.next();
                        System.out.print("Enter the amount you want to add to your card : ");
                        double amt = sc.nextDouble();
                        try (Connection conn1 = DriverManager.getConnection("jdbc:mysql://localhost:3306/sos", "root",
                                "shree@24poo@04ja")) {
                            String insertQuery = "INSERT INTO cardholders (name, balance, pwd) VALUES ('" + name + "', "
                                    + amt + "', " + password + ")";
                            Statement stmt = conn1.createStatement();
                            int insertedRows = stmt.executeUpdate(insertQuery, Statement.RETURN_GENERATED_KEYS);

                            if (insertedRows > 0) {
                                // Retrieve the auto-generated ID of the inserted row
                                ResultSet generatedKeys = stmt.getGeneratedKeys();
                                int insertedId = -1;
                                if (generatedKeys.next()) {
                                    insertedId = generatedKeys.getInt(1);
                                } else {
                                    throw new SQLException("Failed to get the inserted ID.");
                                }
                                System.out.println("Dear customer, your smart card is created succesfully.");
                                System.out.println("Your card no. : " + insertedId);
                                String selectQuery = "SELECT balance FROM cardholders WHERE cardno = " + insertedId;
                                ResultSet resultSet = stmt.executeQuery(selectQuery);
                                if (resultSet.next()) {
                                    double originalBalance = resultSet.getDouble("balance");
                                    System.out.println(
                                            "Original Balance of cardno : " + insertedId + " is : " + originalBalance);
                                    double updatedBalance = originalBalance - t;
                                    String updateQuery = "UPDATE cardholders SET balance = " + updatedBalance
                                            + " WHERE cardno = " + insertedId;
                                    int rowsUpdated = stmt.executeUpdate(updateQuery);

                                    if (rowsUpdated > 0) {
                                        System.out.println("The bill amount of " + t + " is debited from card no. : "
                                                + insertedId);
                                        System.out.println(
                                                "Current Balance of " + insertedId + " is : " + updatedBalance);
                                    } else {
                                        System.out.println("Failed to update the balance of ID " + insertedId + ".");
                                    }
                                } else {
                                    System.out.println("Row with ID " + insertedId + " not found.");
                                }
                            }
                            stmt.close();
                        } catch (SQLException e) {
                            System.err.println("Error: " + e.getMessage());
                        }
                }
            }
            System.out.println("Thank You");
            rs1.close();
            rs.close();
            st1.close();
            st.close();
            conn.close();

        } catch (SQLException e) {
            System.err.println("Error connecting to the database or executing query!");
            e.printStackTrace();
        }
    }
}
